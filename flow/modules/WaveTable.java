// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import flow.utilities.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.*;
import org.json.*;

/**
   A Unit which provides the equivalent of a classic sparse wavetable.
   This class, along with Wave.java was built originally to play the wavetables of the Waldorf Microwave XT,
   for which I found some harmonics online (https://hkwad.home.xs4all.nl/waldorf/romwaves.html)
   and many wavetable index definitions (http://www.carbon111.com/xtwavetables.html)
   But I have been unable to convince Waldorf to give me permission to include them,
   and though I am probably in the clear copyright-wise, I am not a jerk.
   So we're not going to include them.
        
   In general, the [sparse] wavetables go 0...60 but only some slots are populated with waves.
   Unpopulated slots are interpolated between the neighboring populated slots.  The
   modulation "Position" specifies which slot is being played (it's smooth interpolation).
*/



public class WaveTable extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_POSITION = 0;

    public static final String FILENAME_EXTENSION = ".WAV";
    
    public String name = null;

    int currentPos = -1;
        
    double[][] waveTable;
        
    public Object clone()
        {
        WaveTable obj = (WaveTable)(super.clone());
        obj.waveTable = (double[][])(obj.waveTable);
        for(int i = 0; i < obj.waveTable.length; i++)
            {
            obj.waveTable[i] = (double[])(obj.waveTable[i].clone());
            }
        return obj;
        }

    public WaveTable(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Position" });
        setClearOnReset(false);
        waveTable = new double[2][NUM_PARTIALS];
        waveTable[0][0] = 1;
        waveTable[1][0] = 1;
        for(int i = 1; i < NUM_PARTIALS; i++)
            {
            waveTable[0][i] = 0;
            waveTable[1][i] = 0;
            }
        }

    public void go()
        {
        super.go();
                
        double mod = modulate(MOD_POSITION);
        if (mod != currentPos)  // gotta update
            {
            double[] amplitudes = getAmplitudes(0);
            if (mod == 1.0)
                {
                System.arraycopy(waveTable[waveTable.length - 1], 0, amplitudes, 0, amplitudes.length);
                }
            else
                {
                double d = mod * (waveTable.length - 1);
                int wave = (int) d;
                double alpha = (d - wave);
                double[] wt0 = waveTable[wave];
                double[] wt1 = waveTable[wave + 1];
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = wt0[i] * (1-alpha) + wt1[i] * alpha;
                    }
                }
            }
        }

    void distributeToAllSounds(double[][] wt)
        {
        int index = sound.findRegistered(this);
        Output output = sound.getOutput();
        int numSounds = output.getNumSounds();

// perhaps we could share this if we were careful...
        for(int i = 0; i < numSounds; i++)
            {
            WaveTable unit = (WaveTable)(output.getSound(i).getRegistered(index));
            unit.waveTable = new double[wt.length][];
            for(int j = 0; j < wt.length; j++)
                unit.waveTable[j] = (double[]) wt[j].clone();
            }
        }

    public ModulePanel getPanel()
        {
        final ModulePanel[] pan = new ModulePanel[1];
        
        pan[0] = new ModulePanel(WaveTable.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                                
                for(int i = 0; i < unit.getNumModulations(); i++)
                    {
                    box.add(new ModulationInput(unit, i, this));
                    }
                
                final PushButton button[] = new PushButton[1];
                box.add(button[0] = new PushButton(name == null ? "Load..." : name)
                    {
                    public void perform()
                        {
                        File f = pan[0].doLoad("Load a Wavetable from https://waveeditonline.com/", FILENAME_EXTENSION);
                        if (f != null)
                            {
                            name = AppMenu.removeExtension(f.getName());
                            button[0].getButton().setText(name);
                            }
                        }
                    });
                return box;
                }

            public void loadFile(File file, Rack rack)
                { 
                try
                    {
                    WavFile wavFile = WavFile.openWavFile(file);
                    wavFile.display();
                    int numChannels = wavFile.getNumChannels();
                    if (numChannels != 1)
                        {
                        AppMenu.showSimpleError("Invalid WAV File", "WAV files must have only one channel.", rack);
                        }
                    else
                        {
                        int MULTIPLES = 2;
                        ArrayList<double[]> buf = new ArrayList<>();
                        double[] buffer = new double[256 * MULTIPLES];
                        double[] lastFinished = null;
                        int count = 0;
                        while(true)
                            {
                            double[] finished = new double[128 * MULTIPLES];
        
                            // Read frames into buffer
                            int framesRead = wavFile.readFrames(buffer, 256 * MULTIPLES);
                            if (framesRead != 256 * MULTIPLES) break;
                                
                            // Note no window.  Should still be okay (I think?)
                            double[] harmonics = FFT.getHarmonics(FFT.applyHanningWindow(buffer));
                            for (int s = 1 ; s < harmonics.length / 2 + 1; s++)
                                {
                                finished[s - 1] = (harmonics[s] >= MINIMUM_AMPLITUDE ? harmonics[s]  : 0 );
                                }
                            
                            double[] f = new double[128];
                            for(int i = 0; i < 128; i++)
                            	f[i] = finished[i];
                            buf.add(f);
                            }

                        double max = 0;
                        double[][] done = new double[buf.size()][];
                                        
                        for(int i = 0; i < buf.size(); i++)
                            {
                            done[i] = (double[])(buf.get(i));
                            for(int j = 0; j < done[i].length; j++)
                                if (max < done[i][j])
                                    max = done[i][j];
                            }
                                                
                        // maximize over all waves [with max = 1.0]
                        if (max > 0)
                            {
                            for(int i = 0; i < done.length; i++)
                                {
                                for(int j = 0; j < done[i].length; j++)
                                    {
                                    done[i][j] /= max;
                                    }
                                }
                            }

                        rack.getOutput().lock();
                        try
                            {
                            waveTable = new double[done.length][NUM_PARTIALS];
                            // load the wavetable independent of the number of partials
                            for(int i = 0; i < waveTable.length; i++)
                                {
                                for(int j = 0; j < waveTable[i].length; j++)
                                    {
                                    waveTable[i][j] = 0;
                                    }
                                System.arraycopy(done[i], 0, waveTable[i], 0, Math.min(done[i].length, waveTable[i].length));
                                }

                            distributeToAllSounds(waveTable);
                            }
                        finally 
                            {
                            rack.getOutput().unlock();
                            }
                        }
                    }
                catch (Exception ex)
                    {
                    ex.printStackTrace();
                    }
                }
            };
        return pan[0];
        }


    static final double MINIMUM_AMPLITUDE = 0.001;


    //// SERIALIZATION STUFF

    public JSONObject getData()
        {
        JSONObject obj = new JSONObject();
        JSONArray wt = new JSONArray();
        for(int i = 0; i < waveTable.length; i++)
            for(int j = 0; j < waveTable[i].length; j++)
                {
                wt.put(waveTable[i][j]);
                }
        obj.put("wt", wt);
        obj.put("name", (name == null ? "Load..." : name));
        obj.put("x", waveTable.length);
        obj.put("y", waveTable[0].length);
        return obj;
        }
        
    public void setData(JSONObject data, int moduleVersion, int patchVersion)
        {
        JSONArray wt = data.getJSONArray("wt");
        name = data.getString("name");
        int x = data.getInt("x");
        int y = data.getInt("y");
        waveTable = new double[x][NUM_PARTIALS];
        int c = 0;
        for(int i = 0; i < x; i++)
            {
            for(int j = 0; j < y; j++)
                {
                double d = wt.optDouble(c++, 0);
                if (j < NUM_PARTIALS) waveTable[i][j] = d;  // if we have fewer partials than is listed, we skip this one.
                }
            }
        } 
        
    }

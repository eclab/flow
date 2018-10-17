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

    int currentPos = -1;
        
    double[][] waveTable;
        
    public WaveTable(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Position" });
        setClearOnReset(false);
        waveTable = new double[2][128];
        waveTable[0][0] = 1;
        waveTable[1][0] = 1;
        for(int i = 1; i < 128; i++)
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
                box.add(button[0] = new PushButton("Load...")
                    {
                    public void perform()
                        {
                        File f = pan[0].doLoad("Load a Wavetable from https://waveeditonline.com/", FILENAME_EXTENSION);
                        if (f != null)
                            button[0].getButton().setText(AppMenu.removeExtension(f.getName()));
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
                        ArrayList<double[]> buf = new ArrayList<>();
                        double[] buffer = new double[256];
                        int count = 0;
                        while(true)
                            {
                            double[] finished = new double[128];
        
                            // Read frames into buffer
                            int framesRead = wavFile.readFrames(buffer, 256);
                            if (framesRead != 256) break;
                                
                            // Note no window.  Should still be okay (I think?)
                            double[] harmonics = FFT.getHarmonics(buffer);
                            for (int s=1 ; s<harmonics.length / 2 + 1; s++)
                                {
                                finished[s - 1] = (harmonics[s] >= MINIMUM_AMPLITUDE ? harmonics[s]  : 0 );
                                }
                            buf.add(finished);
                            System.err.println(++count);
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
                            waveTable = done;
                            }
                        finally 
                            {
                            rack.getOutput().unlock();
                            }
                        distributeToAllSounds(waveTable);
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
        
    }

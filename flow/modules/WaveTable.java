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

    public static final String[] FILENAME_EXTENSIONS = new String[] { ".WAV", ".wav", ".syx", ".SYX" };
    public static final String[] SAMPLE_EXTENSIONS = new String[] { ".WAV", ".wav" };

    public static final double MINIMUM_AMPLITUDE = 0.001;
    
    public String name = null;

    int currentPos = -1;
    boolean interpolate = true;
                
    double[/*Wave*/][/*Partial*/] waveTable;
    
    // 1 and 2 are too bouncy, 8 sounds too... distant and wrong.
    public static final int RESAMPLING = 4;
    public static final int WAVETABLE_SIZE = 256;
        
    public Object clone()
        {
        WaveTable obj = (WaveTable)(super.clone());
        obj.waveTable = (double[][])(obj.waveTable.clone());
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
        defineOptions(new String[] { "Interpolate" }, new String[][] { { "Interpolate" } });
        setClearOnReset(false);
        // Set to a fundamental sine wave
        waveTable = new double[1][NUM_PARTIALS];
        waveTable[0][0] = 1;
        for(int i = 1; i < NUM_PARTIALS; i++)
            {
            waveTable[0][i] = 0;
            }
        }

    public boolean getInterpolate() { return interpolate; }
    public void setInterpolate(boolean val) { interpolate = val; }
        
    public static final int OPTION_INTERPOLATE = 0;

    public static String getName() { return "Wavetable"; }

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_INTERPOLATE: return getInterpolate() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_INTERPOLATE: setInterpolate(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public void go()
        {
        super.go();
                
        double mod = modulate(MOD_POSITION);
        if (mod != currentPos)  // gotta update
            {
            double[] amplitudes = getAmplitudes(0);
            if (mod == 1.0 || waveTable.length == 1)
                {
                System.arraycopy(waveTable[waveTable.length - 1], 0, amplitudes, 0, amplitudes.length);
                }
            else
                {
                double d = mod * (waveTable.length - 1);
                int wave = (int) d;
                double alpha = (d - wave);
                if (interpolate)
                    {
                    double[] wt0 = waveTable[wave];
                    double[] wt1 = waveTable[wave + 1];
                    for(int i = 0; i < amplitudes.length; i++)
                        {
                        amplitudes[i] = wt0[i] * (1-alpha) + wt1[i] * alpha;
                        }
                    }
                else
                    {
                    double[] wt = waveTable[wave];
                    if (alpha >= 0.5)
                        wt = waveTable[wave + 1];
                    System.arraycopy(wt, 0, amplitudes, 0, amplitudes.length);
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
            Sound s = output.getSound(i);
            if (s.getGroup() == Output.PRIMARY_GROUP)
                {
                WaveTable unit = (WaveTable)(s.getRegistered(index));
                unit.waveTable = new double[wt.length][];
                for(int j = 0; j < wt.length; j++)
                    unit.waveTable[j] = (double[]) wt[j].clone();
                }
            }
        }



    public ModulePanel getPanel()
        {
        final ModulePanel[] pan = new ModulePanel[1];
        
        pan[0] = new ModulePanel(WaveTable.this)
            {
            boolean sampled = false;
            
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                                
                for(int i = 0; i < unit.getNumModulations(); i++)
                    {
                    box.add(new ModulationInput(unit, i, this));
                    }
                
                for(int i = 0; i < unit.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(unit, i));
                    }

                final PushButton button[] = new PushButton[1];
                final PushButton button2[] = new PushButton[1];
                box.add(button[0] = new PushButton(name == null ? "Wavetable..." : name)
                    {
                    public void perform()
                        {
                        sampled = false;

                        blofeldName = null;
                        File f = pan[0].doLoad("Load a Wavetable", FILENAME_EXTENSIONS);
                        if (blofeldName != null)
                            {
                            name = blofeldName;
                            button[0].getButton().setText(name);
                            button2[0].getButton().setText("Sample...");
                            }
                        else if (f != null)
                            {
                            name = AppMenu.removeExtension(f.getName());
                            button[0].getButton().setText(name);
                            button2[0].getButton().setText("Sample...");
                            }
                        }
                    });

                box.add(button2[0] = new PushButton(name == null ? "Sample..." : name)
                    {
                    public void perform()
                        {
                        sampled = true;
                        
                        File f = pan[0].doLoad("Convert a Sample into a Wavetable", SAMPLE_EXTENSIONS);
                        if (f != null)
                            {
                            name = AppMenu.removeExtension(f.getName());
                            button2[0].getButton().setText(name);
                            button[0].getButton().setText("Wavetable...");
                            }
                        }
                    });
                return box;
                }

            public void loadFile(File file, Rack rack)
                {
                String n = file.getName();
                if (n.endsWith(".syx") || n.endsWith(".SYX"))
                    loadBlofeldSysex(file, rack);
                else
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
                            if (sampled)
                                {
                                int sampleSize = WAVETABLE_SIZE * RESAMPLING;
                                double[] a = new double[sampleSize];
                                double[] b = new double[sampleSize];
                                double[] buffer = new double[WAVETABLE_SIZE];
                                while(true)
                                    {
                                    // Read frames into buffer
                                    int framesRead = wavFile.readFrames(buffer, WAVETABLE_SIZE);
                                    if (framesRead != WAVETABLE_SIZE) break;
                                                        
                                    System.arraycopy(b, WAVETABLE_SIZE, b, 0, sampleSize - WAVETABLE_SIZE);
                                    System.arraycopy(buffer, 0, b, sampleSize - WAVETABLE_SIZE, WAVETABLE_SIZE);
                                    System.arraycopy(b, 0, a, 0, sampleSize);
                                                      
                                    // is Hanning COLA?     
                                    a = FFT.applyHanningWindow(a);
                                    double[] harmonics = FFT.getHarmonics(a);
                                    double[] finished = new double[harmonics.length / 2 / RESAMPLING];
                                    for (int s=1 ; s < harmonics.length / 2 / RESAMPLING + 1; s++)
                                        {
                                        finished[s - 1] = (harmonics[s * RESAMPLING - 1] >= MINIMUM_AMPLITUDE ? harmonics[s * RESAMPLING - 1]  : 0 );
                                        }
                                    buf.add(finished);
                                    }
                                }
                            else
                                {
                                double[] buffer = new double[WAVETABLE_SIZE];
                                while(true)
                                    {
                                    // Read frames into buffer
                                    int framesRead = wavFile.readFrames(buffer, WAVETABLE_SIZE);
                                    if (framesRead != WAVETABLE_SIZE) break;
                                                        
                                    // Note no window.  Should still be okay (I think?)
                                    double[] harmonics = FFT.getHarmonics(buffer);
                                    double[] finished = new double[harmonics.length / 2];
                                    for (int s=1 ; s < harmonics.length / 2; s++)                           // we skip the DC offset (0) and set the Nyquist frequency bin (harmonics.length / 2) to 0
                                        {
                                        finished[s - 1] = (harmonics[s] >= MINIMUM_AMPLITUDE ? harmonics[s]  : 0 );
                                        }
                                    buf.add(finished);
                                    }
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
                        warnAlways("modules/WaveTable.java", "IOException in loading file: " + ex);
                        ex.printStackTrace();
                        }
                    }
                }
            };
        return pan[0];
        }




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
        
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_POSITION)
                {
                double d = value * (waveTable.length - 1);
                int wave = (int) d;
                double alpha = (d - wave);
                if (alpha >= 0.5) wave = wave + 1;
                return "" + wave;
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    public static final int BLOFELD_WAVE_SIZE = 128;
    public static final int BLOFELD_WAVETABLE_SIZE = 64;


    public void warnBlofeld(int pos)
        {
        System.err.println("WARNING (modules/WaveTable.java): improper data in wave " + pos);
        }

    public void failBlofeld(int pos)
        {
        System.err.println("ERROR (modules/WaveTable.java): bad data, giving up on wavetable as of wave " + pos);
        }
                

    String blofeldName = null;
    public void loadBlofeldSysex(File file, Rack rack)
        {
        try 
            {
            InputStream f = new BufferedInputStream(new FileInputStream(file));
            waveTable = new double[64][NUM_PARTIALS];

            for(int i = 0; i < BLOFELD_WAVETABLE_SIZE; i++)
                {
                // read a wave
                // 0. we find 0xF0
                int b = f.read();
                if (b != 0xF0) { warnBlofeld(i); continue; }
                        
                // 1. Waldorf ID
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b != 0x3E) { warnBlofeld(i); continue; }

                // 2. Blofeld ID
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b != 0x13) { warnBlofeld(i); continue; }

                // 3. Device ID
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                // The value doesn't matter
                        
                // 4. Wavetable Dump Command
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b != 0x12) { warnBlofeld(i); continue; }

                // 5. Wavetable Number
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b < 0x50 || b > 0x76) { warnBlofeld(i); continue; }
                int wt = b;

                // 6. Wave Number
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b > 0x3F) { warnBlofeld(i); continue; }
                int wv = b;

                // 7. Wave "Format", always 0
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                if (b != 0x0) { warnBlofeld(i); continue; }
                        
                // Data
                double[] data = new double[BLOFELD_WAVE_SIZE];
                for(int j = 0; j < data.length; j++)
                    {
                    int h = f.read();
                    if (h < 0 || h > 127) { failBlofeld(i); break; }
                    int m = f.read();
                    if (m < 0 || m > 127) { failBlofeld(i); break; }
                    int l = f.read();
                    if (l < 0 || l > 127) { failBlofeld(i); break; }
                                
                    // Form the 21-bit triplet
                    int d = (h << 14) | (m << 7) | l;

                    // we're going to shift this to the very top, and then shift it down
                    // with >> so we have a signed fill
                    d = (d << 11) >> 11;
                    data[j] = d / 1048576.0;                        // 2^20
                    }
                                
                // Name
                byte[] n = new byte[14];
                boolean failedName = false;
                for(int j = 0; j < n.length; j++)
                    {
                    b = f.read();
                    if (b < 0 || b > 127) { failBlofeld(i); failedName = true; break; }
                    n[j] = (byte)b;    
                    }
                if (failedName) break;
                blofeldName = new String(n, "US-ASCII");

                // Reserved junk, checksum, and F7
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                b = f.read();
                if (b < 0 || b > 127) { failBlofeld(i); break; }
                b = f.read();
                if (b != 0xF7) { warnBlofeld(i); continue; }
                        
                // Perform FFT.  I am assuming that the wavetable is properly constructed such that
                // we do not have to window first to force it to 0.
                data = FFT.getHarmonics(data);
                for(int j = 1; j < data.length / 2; j++)                // remove dc offset
                    {
                    waveTable[wv][j - 1] = data[j];
                    }
                }
                
            f.close();
            }
        catch (IOException ex) { }
        }
    }

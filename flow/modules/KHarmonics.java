// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

/**
   A Unit which generates a wide range of partials derived from pubic domain and factory
   patches from the Kawai K5.  At present these partials are hard-coded, but in the future
   they may be loaded specially.
*/

public class KHarmonics extends Unit implements UnitSource
{
    private static final long serialVersionUID = 1;

    public static final int MAX_HARMONICS = 128;  // regardless of number of partials
    public static final double MAX_AMPLITUDE = 100.0;  // highest legal amplitude

    public static final int NUM_HARMONICS = 484;
    static final double[][] HARMONICS = new double[NUM_HARMONICS][MAX_HARMONICS];
    static final String[] NAMES = new String[NUM_HARMONICS];

    boolean normalize;
    boolean oldNormalize;
    public boolean getNormalize() { return normalize; }
    public void setNormalize(boolean val) { normalize = val; setHarmonics(getHarmonics()); }
    
    double mod = Double.NaN;
        
    int harmonics = 0;
    int oldHarmonics = 0;
    public int getHarmonics() { return harmonics; }
    public void setHarmonics(int harm) { harmonics = harm; }

    int harmonics2 = 0;
    int oldHarmonics2 = 0;
    public int getHarmonics2() { return harmonics2; }
    public void setHarmonics2(int harm) { harmonics2 = harm; }

    public static final int OPTION_HARMONICS = 0;
    public static final int OPTION_HARMONICS_2 = 1;
    public static final int OPTION_NORMALIZE = 2;

    public int getOptionValue(int option) 
    { 
        switch(option)
            {
            case OPTION_HARMONICS: return getHarmonics();
            case OPTION_HARMONICS_2: return getHarmonics2();
            case OPTION_NORMALIZE: return getNormalize() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
    }
                
    public void setOptionValue(int option, int value)
    { 
        switch(option)
            {
            case OPTION_HARMONICS: setHarmonics(value); return;
            case OPTION_HARMONICS_2: setHarmonics2(value); return;
            case OPTION_NORMALIZE: setNormalize(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
    }
        
    static boolean done = false;
    public void doStatic()
    {
        if (done) return;
        done = true;
        
        Scanner scan = new Scanner(Harmonics.class.getResourceAsStream("waves/kharmonics.out"), "US-ASCII");
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
                NAMES[i] = scan.nextLine();
                for(int j = 0; j < MAX_HARMONICS; j++)
                    {
                        HARMONICS[i][j] = (scan.nextInt() / MAX_AMPLITUDE);
                    }
                scan.nextLine();
            }
        scan.close();
    }

    public void go()
    {
        super.go();
        
        double m = modulate(0);
        if (m != mod || normalize != oldNormalize || oldHarmonics != harmonics || oldHarmonics2 != harmonics2)
            {
                mod = m;
                oldNormalize = normalize;
                oldHarmonics = harmonics;
                oldHarmonics2 = harmonics2;
                
                double[] amplitudes = getAmplitudes(0);
                        
                if (mod == 0)
                    {
                        System.arraycopy(HARMONICS[harmonics], 0, amplitudes, 0, Math.min(HARMONICS[harmonics].length, amplitudes.length));
                    }
                else if (mod == 1)
                    {
                        System.arraycopy(HARMONICS[harmonics2], 0, amplitudes, 0, Math.min(HARMONICS[harmonics2].length, amplitudes.length));
                    }
                else
                    {
                        double[] h1 = HARMONICS[harmonics];
                        double[] h2 = HARMONICS[harmonics2];
                                
                        for(int i = 0; i < HARMONICS[harmonics].length; i++)
                            {
                                amplitudes[i] = (1 - m) * h1[i] + m * h2[i];
                            }
                    }
                                
                if (normalize) 
                    normalizeAmplitudes();
            }
    }
                
    public KHarmonics(Sound sound) 
    {
        super(sound);
        doStatic();

        defineOptions(new String[] { "Sound" , "Sound 2", "Normalize" }, new String[][] { NAMES , NAMES, { "Normalize" } } );
        defineModulations(new Constant[] { new Constant(0) }, new String[] { "Mix" });
        setClearOnReset(false);
    }
                
}

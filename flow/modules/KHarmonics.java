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
    public boolean getNormalize() { return normalize; }
    public void setNormalize(boolean val) { normalize = val; setHarmonics(getHarmonics()); }

    public static final int OPTION_HARMONICS = 0;
    public static final int OPTION_NORMALIZE = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_HARMONICS: return getHarmonics();
            case OPTION_NORMALIZE: return getNormalize() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_HARMONICS: setHarmonics(value); return;
            case OPTION_NORMALIZE: setNormalize(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    static boolean done = false;
    public void doStatic()
        {
        if (done) return;
        done = true;
        
        Scanner scan = new Scanner(Harmonics.class.getResourceAsStream("waves/kharmonics.out"));
        for(int i = 0; i < 484; i++)
            {
            NAMES[i] = scan.nextLine();
            for(int j = 0; j < 128; j++)
                {
                HARMONICS[i][j] = (scan.nextInt() / MAX_AMPLITUDE);
                }
            scan.nextLine();
            }
        scan.close();
        }
        
    int harmonics = 0;

    public int getHarmonics() { return harmonics; }
    public void setHarmonics(int harm)
        {
        double[] amplitudes = getAmplitudes(0);
                
        System.arraycopy(HARMONICS[harm], 0, amplitudes, 0, Math.min(HARMONICS[harm].length, amplitudes.length));
        harmonics = harm;
        if (normalize) 
            normalizeAmplitudes();
        
        }
                
    public KHarmonics(Sound sound) 
        {
        super(sound);
        doStatic();

        defineOptions(new String[] { "Sound" , "Normalize" }, new String[][] { NAMES , { "Normalize" } } );
        setHarmonics(0);
        setClearOnReset(false);
        }
                
    }

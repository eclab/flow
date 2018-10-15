// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which allows you to set the amplitudes of
   up to the first 16 harmonics, but not their frequency.
*/

public class Harmonics extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;
        
    public static final int NUM_HARMONICS = 16;

    public Harmonics(Sound sound) 
        {
        super(sound);
        Constant[] con = new Constant[NUM_HARMONICS];
        String[] str = new String[NUM_HARMONICS];
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
            con[i] = Constant.ZERO;
            str[i] = "Amp " + (i + 1);
            }
        defineModulations(con, str);
        }

    public void go()
        {
        super.go();
 
        double[] amplitudes = getAmplitudes(0);        
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
            amplitudes[i] = modulate(i);
            }
        }
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which compresses or expands its input -- that is, either squeezes its amplitudes all towards 1.0
   or pushes them all away from 1.0.  The squeezing/expanding is exponential, and the degree is 
   specified by SCALE.  Compression occurs when SCALE is greater than 0.5; expansion occurs when SCALE
   is less than 0.5.
*/


public class Compress extends Unit
    {
    private static final long serialVersionUID = 1;

    double lastModulation = Double.NaN;
    double[] lastAmps;
        
    public static final int MOD_SCALE = 0;

    public Compress(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.HALF }, new String[] { "Scale" });
        lastAmps = new double[NUM_PARTIALS];
        }
                
    public void go()
        {
        super.gate();

        // we don't copy the amplitudes
        pushFrequencies(0);
        
        double[] amplitudes = getAmplitudes(0);
                
        double mod = 1.0 - modulate(MOD_SCALE);
        if (mod >= 0.5) mod = (mod - 0.5) * 8 + 1;
        else mod = 1.0 / ((0.5 - mod) * 8 + 1);
        
        // mod now ranges from 0.2 to 1.0 to 5.0
        
        double[] amps = getAmplitudesIn(0);
                
        for(int i = 0; i < amplitudes.length; i++)
            {
            if (lastModulation != mod || amps[i] != lastAmps[i])
                {
                amplitudes[i] = Utility.hybridpow(amps[i], mod);
                lastAmps[i] = amplitudes[i];
                }
            else
                amplitudes[i] = lastAmps[i];
            }
        lastModulation = mod;
                        
        constrain();
        }
    }

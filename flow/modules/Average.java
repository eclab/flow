// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/** 
    A Unit which moves the amplitudes of all the partials towards their average amplitude.
    If INCLUDE ZEROS is true, then zero-length amplitudes are considered in the averaging, 
    else they (and their partials) are disregarded (and also unchanged).  The degree
    of averaging is set by SCALE.
*/

public class Average extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_SCALE = 0;

    boolean includeZeros = false;
    boolean getIncludeZeros() { return includeZeros; }
    void setIncludeZeros(boolean val) { includeZeros = val; }
        
    public static final int OPTION_INCLUDE_ZEROS = 0;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_INCLUDE_ZEROS: return getIncludeZeros() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_INCLUDE_ZEROS: setIncludeZeros(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
        
    public Average(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Scale" });
        defineOptions(new String[] { "Include Zeros" }, new String[][] { { "Include Zeros" } } );
        }
                
    public void go()
        {
        super.go();
                
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double mod = modulate(MOD_SCALE);
        copyFrequencies(0);
        copyAmplitudes(0);

        // compute average
        double avg = 0.0;
        for(int i = 0; i < amplitudes.length; i++)
            {
            if (includeZeros || amplitudes[i] != 0)
                avg += amplitudes[i];
            }
        avg /= amplitudes.length;

        // move towards it
        for(int i = 0; i < amplitudes.length; i++)
            {
            if (includeZeros || amplitudes[i] != 0)
                amplitudes[i] = avg * mod + amplitudes[i] * (1.0 - mod);
            }

        constrain();
        }       
    }

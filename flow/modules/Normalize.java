// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which normalizes the amplitudes of its incoming
   partials, or standardizes their frequencies, and then emits them.  
   Normalization adjusts the amplitudes so that they sum to 1.0.
   Standardization adjusts the frequencies so that they are all integer
   multiples starting at the base pitch.
*/

public class Normalize extends Unit
    {
    private static final long serialVersionUID = 1;

    public Normalize(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] { "Scaling", "Standardize" }, new String[][] { {"None", "Normalize", "Maximize"}, {"Standardize"} });
        }

    public static final int N_OFF = 0;
    public static final int N_NORMALIZE = 1;
    public static final int N_MAXIMIZE = 2;

    int normalize = N_NORMALIZE;
    boolean standardize = false;
        
    public void setNormalize(int val) { normalize = val; }
    public int getNormalize() { return normalize; }
    public void setStandardize(boolean val) { standardize = val; }
    public boolean getStandardize() { return standardize; }
        
    public static final int OPTION_NORMALIZE = 0;
    public static final int OPTION_STANDARDIZE = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_NORMALIZE: return getNormalize();
            case OPTION_STANDARDIZE: return getStandardize() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_NORMALIZE: setNormalize(value); return;
            case OPTION_STANDARDIZE: setStandardize(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public void go()
        {
        super.go();
                
        
        if (normalize == N_NORMALIZE)
            {
            copyAmplitudes(0);
            normalizeAmplitudes();
            }
        else if (normalize == N_MAXIMIZE)
            {
            copyAmplitudes(0);
            maximizeAmplitudes();
            }
        else
            {
            pushAmplitudes(0);
            }
                
        if (standardize)
            {
            copyFrequencies(0);
            standardizeFrequencies();
            }
        else
            {
            pushFrequencies(0);
            }
                                                
        if (constrain() && standardize)
            simpleSort(0, normalize == N_OFF);
        }       
    }

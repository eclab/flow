// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides low-pass, high-pass, band-pass, and notch filters.  The filters are not
   resonant (for the moment).  You can specify the cutoff frequency and the dropoff (between 
   0 and 8 poles, with 4 poles being the 0.5 position).
*/

public class Filter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int TYPE_LP = 0;
    public static final int TYPE_HP = 1;
    public static final int TYPE_BP = 2;
    public static final int TYPE_NOTCH = 3;
    
    int type = TYPE_LP;
    
    public Filter(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] { "Type" }, new String[][] { { "LP", "HP", "BP", "Notch" } });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE }, new String[] { "Frequency", "Dropoff" });
        }

    public int getType() { return type; }
    public void setType(int val) { type = val; }
        
    public static final int OPTION_TYPE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);
                        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double cutoff = modToFrequency(modulate(0));
        double drop = modToFilterDropPerOctave(modulate(1));
                
        switch (type)
            {
            case TYPE_LP:
                {                
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    if (frequencies[i] > cutoff)
                        // Maybe use Apache Commons FastMath?  Maybe use Utilities.fastmath etc.?
                        amplitudes[i] = amplitudes[i] * Utility.hybridpow(drop, (frequencies[i] - cutoff));
                    }
                }
            break;
            case TYPE_HP:
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    if (frequencies[i] < cutoff)
                        // Maybe use Apache Commons FastMath?  Maybe use Utilities.fastmath etc.?
                        amplitudes[i] = amplitudes[i] * Utility.hybridpow(drop, (cutoff - frequencies[i]));
                    }
                }
            break;
            case TYPE_BP:
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    if (frequencies[i] > cutoff)
                        // Maybe use Apache Commons FastMath?  Maybe use Utilities.fastmath etc.?
                        amplitudes[i] = amplitudes[i] * Utility.hybridpow(drop, (frequencies[i] - cutoff));
                    else if (frequencies[i] < cutoff)
                        {
                        amplitudes[i] = amplitudes[i] * Utility.hybridpow(drop, (cutoff - frequencies[i]));
                        }
                    }
                }
            break;
            case TYPE_NOTCH:
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    if (frequencies[i] > cutoff)
                        // Maybe use Apache Commons FastMath?  Maybe use Utilities.fastmath etc.?
                        amplitudes[i] = amplitudes[i] * (1.0 - Utility.hybridpow(drop, (frequencies[i] - cutoff)));
                    else if (frequencies[i] < cutoff)
                        {
                        amplitudes[i] = amplitudes[i] * (1.0 - Utility.hybridpow(drop, (cutoff - frequencies[i])));
                        }
                    }
                }
            break;
            }

        constrain();
        }       
    }

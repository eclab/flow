// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which swaps its partials with nearby partials.  The meaning of "nearby" depends on a 
   modulation ("distance").  You have the option swapping the fundamental or keeping it out
   of the process.
   
   The modulation doesn't have to be a integral value -- if the distance is "in-between" partials,
   then partials are smoothly mixed.
*/
   
public class Swap extends Unit
    {
    private static final long serialVersionUID = 1;

    boolean swapFundamental;
        
    public boolean getSwapFundamental() { return swapFundamental; }
    public void setSwapFundamental(boolean val) { swapFundamental = val; }
        
    public static final int OPTION_SWAP_FUNDAMENTAL = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_SWAP_FUNDAMENTAL: return getSwapFundamental() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_SWAP_FUNDAMENTAL: setSwapFundamental(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public Swap(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Distance" });
        defineOptions(new String[] { "Fundamental" }, new String[][] { { "Fundamental" } } );
        setSwapFundamental(false);
        }
        
    double[] amp2 = new double[NUM_PARTIALS];
        
    public void go()
        {
        super.go();
                        
        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);        
        
        double val = modulate(0) * (amplitudes.length - (swapFundamental ? 1 : 2)) + 1;
        int swap = (int)val;
        double by = val - swap;

        int start = 1;
        if (swapFundamental) start = 0; 


        // I don't think I should be swapping orders here....

        for(int i = start; i < amplitudes.length; i += swap)
            {
            for(int j = 0; j < swap / 2; j++)
                {
                if (i + swap - j - 1 < amplitudes.length)
                    {
                    double temp = amplitudes[i + j];
                    amplitudes[i + j] = amplitudes[i + swap - j - 1];
                    amplitudes[i + swap - j - 1] = temp;
                    }
                }
            }

        if (swap != NUM_PARTIALS)
            {
            swap++;
            System.arraycopy(getAmplitudes(0), 0, amp2, 0, amp2.length);
            for(int i = start; i < amp2.length; i += swap)
                {
                for(int j = 0; j < swap / 2; j++)
                    {
                    if (i + swap - j - 1 < amp2.length)
                        {
                        double temp = amp2[i + j];
                        amp2[i + j] = amp2[i + swap - j - 1];
                        amp2[i + swap - j - 1] = temp;
                        }
                    }
                }
            }
                        
        for(int i = start; i < amplitudes.length; i++)
            {
            amplitudes[i] = (1.0 - by) * amplitudes[i] + by * amp2[i];
            }

        constrain();
        }       
    }

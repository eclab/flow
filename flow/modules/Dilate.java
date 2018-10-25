// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which increases the amplitude of partials next to higher-amplitude
   partials.  This is sort of, but not exactly, the opposite of Skeletonize.
   Dilate has the option of increasing partials only to the right
   of higher-amplitude partials, rather than on both sides of them.
*/

public class Dilate extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_CUT = 0;
     
    boolean right;
    public boolean getRight() { return right; }
    public void setRight(boolean val) { right = val; }
     
    public static final int OPTION_RIGHT = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_RIGHT: return getRight() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_RIGHT: setRight(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }


    public Dilate(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Boost" });
        defineOptions(new String[] { "Right" }, new String[][] { { "Right" } });
        }
                
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);
        
        double cut = 1.0 - modulate(MOD_CUT);

        double[] upcuts = new double[amplitudes.length];
                
        upcuts[0] = amplitudes[0];
        for(int i = 1; i < amplitudes.length; i++)
            {
            if (amplitudes[i] <= upcuts[i - 1])
                {  upcuts[i] = cut * upcuts[i] + (1 - cut) * upcuts[i - 1]; }
            else
                { upcuts[i] = amplitudes[i]; }
            }
                
        if (!right)
            {
            double[] downcuts = new double[amplitudes.length];
            downcuts[amplitudes.length - 1] = amplitudes[amplitudes.length - 1];
            for(int i = amplitudes.length - 2; i >= 0; i--)
                {
                if (amplitudes[i] <= downcuts[i + 1])
                    { downcuts[i] = cut * downcuts[i] + (1 - cut) * downcuts[i + 1]; }
                else
                    { downcuts[i] = amplitudes[i]; }
                }
                                
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = Math.max(upcuts[i], downcuts[i]);
                }
            }
        else
            {
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = upcuts[i];
                }
            }
        
        constrain(); 
        }       
    }

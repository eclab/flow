// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which stretches the frequency of partials away from a specific numbered partial.
   The stretching function can be linear, squared, or cubed.  Additionally, if the
   function is not FREE, then the stretching is bounded so that the most extreme partials
   at each end stay fixed to their former positions; this has the effect of squeezing the
   interior partials towards them in a sort of springlike fashion.
   
   The degree of stretching is defined by the modulation AMOUNT (to which you might
   attach an LFO etc.), with a scaling defined by MAX AMOUNT.  The target partial
   in question is defined by the modulation PARTIAL.  Alternatively, if LARGEST is true
   then the target partial is simply the highest amplitude partial, breaking ties
   by lower frequency partials.
*/

public class Stretch extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_AMOUNT = 0;
    public static final int MOD_MAX_AMOUNT = 1;
    public static final int MOD_PARTIAL = 2;

    public static final double MAX_STRETCH = 4;
        
    public static final int TARGET_LARGEST = 0;
    public static final int TARGET_PARTIAL = 1;
        
    int target = TARGET_LARGEST;
    public void setTarget(int val) { target = val; }
    public int getTarget() { return target; }

    public static final int STYLE_X_LOCKED = 0;
    public static final int STYLE_X_FREE = 1;
    public static final int STYLE_X_TIMES_X_LOCKED = 2;
    public static final int STYLE_X_TIMES_X_FREE  = 3;
    public static final int STYLE_X_TIMES_X_TIMES_X_LOCKED = 4;
    public static final int STYLE_X_TIMES_X_TIMES_X_FREE  = 5;
        
    public static final String[] STYLES = new String[] { "X", "X Free", "X^2", "X^2 Free", "X^3", "X^3 Free" };

    int style = STYLE_X_LOCKED;
    public void setStyle(int val) { style = val; }
    public int getStyle() { return style; }

    public Stretch(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.HALF, Constant.ZERO }, 
            new String[] { "Amount", "Max Amount", "Partial" });
        defineOptions(new String[] { "Style", "Largest" }, new String[][] { STYLES, { "Largest" } } );
        }
        
    public static final int OPTION_STYLE = 0;
    public static final int OPTION_TARGET = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_STYLE: return getStyle();
            case OPTION_TARGET: return getTarget();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_STYLE: setStyle(value); return;
            case OPTION_TARGET: setTarget(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void go()
        {
        super.go();
                
        copyFrequencies(0);
        pushAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        
        double mod = modulate(MOD_AMOUNT);
        double maxStretch = modulate(MOD_MAX_AMOUNT) * MAX_STRETCH;
        
        double targetHarm = 0;

        if (target == TARGET_PARTIAL)
            {
            targetHarm = (frequencies.length - 1) * modulate(MOD_PARTIAL);
            }
        else // if (target == TARGET_LARGEST)
            {
            // find maximum partial
            double maxVal = amplitudes[0];
            for(int i = 1; i < amplitudes.length; i++)
                {
                if (amplitudes[i] > maxVal)
                    {
                    targetHarm = i;
                    maxVal = amplitudes[i];
                    }
                }
            }

        // move away from it
        double targetFreq = 0;
        if (targetHarm == frequencies.length - 1)
            targetFreq = frequencies[(int)targetHarm];
        else
            {
            double alpha = targetHarm - (int)targetHarm;
            targetFreq = (1.0 - alpha) * frequencies[(int)targetHarm] + alpha * frequencies[((int)targetHarm) + 1];
            }
        
        boolean locked = (style == STYLE_X_LOCKED || style == STYLE_X_TIMES_X_LOCKED || style == STYLE_X_TIMES_X_TIMES_X_LOCKED);

        for(int i = 0; i < frequencies.length; i++)
            {
            double m = mod;
            switch (style)
                {
                case STYLE_X_LOCKED: case STYLE_X_FREE:
                    m = m * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS)));
                    break;
                case STYLE_X_TIMES_X_LOCKED: case STYLE_X_TIMES_X_FREE:
                    m = m * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS))) * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS)));
                    break;
                case STYLE_X_TIMES_X_TIMES_X_LOCKED: case STYLE_X_TIMES_X_TIMES_X_FREE:
                    m = m * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS))) * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS))) * (1 - (Math.abs(i - targetHarm) / ((double)Unit.NUM_PARTIALS)));
                    break;
                default: // never happens
                    {
                    warn("modules/Stretch.java", "default should never occur");
                    break;
                    }
                }
            if (locked)
                {
                double alpha = Math.min(i - 0, ((double)Unit.NUM_PARTIALS) - 1.0 - i) / (((double)Unit.NUM_PARTIALS) - 1.0 );
                m = m * alpha;
                }

            frequencies[i] = (maxStretch * frequencies[i] - targetFreq) * m + frequencies[i] * (1.0 - m);
            }

        if (constrain()) simpleSort(0, true);
        }       
    }

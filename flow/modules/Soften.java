// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Modulation which smooths over a modulation signal.  The degree of smoothing is specified by Amount.
   You can choose to keep the softening free, or reset it on gate.
*/

public class Soften extends Modulation
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_SIGNAL = 0;
    public static final int MOD_AMOUNT = 1;
    public static final int MOD_SCALE = 2;

    public static String getName() { return "Soften"; }

    public static final double SCALE_MAXIMUM = 10;
        
    double current;

    boolean free;
    public void setFree(boolean val) { free = val; }
    public boolean getFree() { return free; }

    public static final int OPTION_FREE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return getFree() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public Soften(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.HALF, Constant.ZERO }, 
            new String[] { "Signal", "Amount", "Scale" });
        }

    public void reset()
        {
        super.reset();
        current = Double.NaN;
        }
                
    public void gate()
        {
        super.gate();
        if (!free) current = Double.NaN;
        }

    public void go()
        {
        super.go();
        double signal = modulate(MOD_SIGNAL);
        if (current != current)  // NaN
            current = signal;
        else
            {
            double alpha = makeInsensitive(modulate(MOD_AMOUNT));
            current = alpha * current + (1 - alpha) * signal;
            }

        double scale = modulate(MOD_SCALE);
        double val = (current - 0.5) * (scale * (SCALE_MAXIMUM - 1) + 1) + 0.5;
        if (val < 0) val = 0;
        if (val > 1) val = 1;
        setModulationOutput(0, val);
        if (isTriggered(MOD_SIGNAL))
            updateTrigger(0);
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation == 2 && isConstant)
            return String.format("%.4f", value * (SCALE_MAXIMUM - 1) + 1);
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides an amplifier.  It takes a single input Unit, multiplies all its
   amplitudes by a given value, and ouptuts the result.  The multiplier is defined by
   the modulation "Mod", which goes from 0...1, which is then multiplied against "Scale",
   which goes from 0...8 with 1.0 at its midpoint.  "Mod" is linear, so you should
   use that to modulate via an envelope or LFO.  "Scale" ought to  be dialed in by hand.
*/

public class VCA extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final double MAX_SCALE = 8.0;
        
    public VCA(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ONE, Constant.HALF }, new String[] { "Mod", "Scale" });
        }
                
    public void go()
        {
        super.go();

        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);
                
        double mod = modulate(0);
        double scale = modulate(1);
        if (scale == 0.5)
            scale = 1.0;
        else if (scale > 0.5) 
            scale = (scale - 0.5) * 2 * (MAX_SCALE - 1) + 1;
        else
            scale = scale * 2.0;
        
        for(int i = 0; i < amplitudes.length; i++)
            amplitudes[i] = amplitudes[i] * mod * scale; 
            
        constrain();
        }       

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 1)  // Scale
                {
                if (value >= 0.5)
                    value = (value - 0.5) * 2 * (MAX_SCALE - 1) + 1;
                else
                    value = value * 2.0;
                return String.format("%.4f", value);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
    }

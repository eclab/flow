// Copyright 2021 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

import flow.*;

/**
   An Alias or Foldover generator.
*/

public class Alias extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_CUTOFF = 0;
    public static final double MINIMUM_FREQUENCY = 0.000001;  // seems reasonable

    public Alias(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ONE }, new String[] { "Cutoff" });
        }
        
    public double fold(double val, double cutoff)
    	{
    	double v = val / cutoff;
    	int times = (int)v;
    	double frac = v - times;
    	if (times % 2 == 1) frac = 1.0 - frac;
    	return frac * cutoff;
    	}
    	
    public void go()
        {
        super.go();
                
        pushAmplitudes(0);
        copyFrequencies(0);
                        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        double pitch = sound.getPitch();
                
        double cutoff = modToFrequency(makeVeryInsensitive(modulate(MOD_CUTOFF))); // Note that this is in angular frequency, but we don't divide by 2 PI to get Hertz because that's done at the end of the day when we add up the sine waves
        if (cutoff < MINIMUM_FREQUENCY) cutoff = MINIMUM_FREQUENCY;  // so we're never 0
        
		for(int i = 0; i < frequencies.length; i++)
			{
			frequencies[i] = fold(frequencies[i] * pitch, cutoff) / pitch;
			}

        constrain();
        }       


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_CUTOFF)
                {
                return String.format("%.4f", modToFrequency(makeVeryInsensitive(value)));
                }
            else return "";
            }
        else return "";
        }

    public static String getName() { return "Alias"; }
    }

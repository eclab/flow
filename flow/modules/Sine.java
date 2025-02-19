// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides a single partial at 1.0, whose
   frequency is determined by the "Frequency" modulation.
*/

public class Sine extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_PARTIALS = 0;

	public static final double NORMALIZED_GAIN = 1.0;
	public static final double GAIN_MULTIPLIER = 4.0;
    public static final int MOD_GAIN = 1;
	double oldGain = NORMALIZED_GAIN;

	void buildAmplitudes(double mod, double gain)
		{
        double[] amplitudes = getAmplitudes(0);
		int p = (int)(mod * (Unit.NUM_PARTIALS - 1.0));
		for(int i = 0; i < Unit.NUM_PARTIALS; i++)
			{
			if (i == p) amplitudes[i] = GAIN_MULTIPLIER * gain;
			else amplitudes[i] = 0.0;
			}
		}
		
    public Sine(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, new Constant(NORMALIZED_GAIN / GAIN_MULTIPLIER) }, 
        	new String[] { "Partial", "Gain" });

//        double[] amplitudes = getAmplitudes(0);
        
        setClearOnReset(false);
//        sine = 0;
//        amplitudes[0] = 1;
        }

    double lastMod = Double.NaN;
    int sine = 0;
        
    public void go()
        {
        super.go();
 
        //double[] frequencies = getFrequencies(0);        
        double mod = modulate(MOD_PARTIALS);
        double gain = modulate(MOD_GAIN);
        
        if (lastMod != mod || gain != oldGain)
            {
            /*
            frequencies[sine] = mod * ((double)Unit.NUM_PARTIALS - 1.0) + 1;
            simpleSort(0, false);
                        
            for(int i = 0; i < Unit.NUM_PARTIALS; i++)
                if (amplitudes[i] > 0)  // found it
                    {
                    sine = i;
                    break;
                    }
            */
            buildAmplitudes(mod, gain);
            lastMod = mod;
            oldGain = gain;
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_PARTIALS)
                {
                return "" + (int)(value * (Unit.NUM_PARTIALS - 1.0));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides positive partials for a Triangle wave.
   Traditionally triangle waves have some negative partials
   (that is, ones with a phase offset of Pi), but this one does
   not: it relies on us not being able to distinguish phase.
*/

public class Triangle extends Unit implements UnitSource
    {       
    private static final long serialVersionUID = 1;

	public static final double NORMALIZED_GAIN = 0.8118547451597639;
	public static final double GAIN_MULTIPLIER = 4.0;
    public static final int MOD_GAIN = 0;
	double oldGain = NORMALIZED_GAIN;
	
	void buildAmplitudes(double gain)
		{
        double[] amplitudes = getAmplitudes(0);
        for(int i = 0; i < amplitudes.length; i += 2)
            {
            amplitudes[i] = gain * (double)(1.0 / ((i+1) * (i+1)));
            }
                
		}
		
    public Triangle(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { new Constant(NORMALIZED_GAIN / GAIN_MULTIPLIER) }, 
        		new String[] { "Gain" });

        setClearOnReset(false);
                
        buildAmplitudes(NORMALIZED_GAIN);
        //normalizeAmplitudes();
        //System.err.println(amplitudes[0]);
        }

    public void go()
    	{
    	super.go();
    	double gain = modulate(MOD_GAIN);
    	if (oldGain != gain)	// Need to change values
    		{
    		buildAmplitudes(gain * GAIN_MULTIPLIER);
    		oldGain = gain;
    		}
    	}
    }

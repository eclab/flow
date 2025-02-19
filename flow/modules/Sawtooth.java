// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides partials for a Sawtooth Wave.
*/

public class Sawtooth extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

	public static final double NORMALIZED_GAIN = 0.1632827683729929;
	public static final double GAIN_MULTIPLIER = 4.0;
	
    public static final int MOD_GAIN = 0;
	double oldGain = NORMALIZED_GAIN;


	void buildAmplitudes(double gain)
		{
        double[] amplitudes = getAmplitudes(0);
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = gain * (double)(1.0 / (i+1));
            }
		}
		
    public Sawtooth(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { new Constant(NORMALIZED_GAIN / GAIN_MULTIPLIER) }, 
        		new String[] { "Gain" });
        
        double[] amplitudes = getAmplitudes(0);
        
        setClearOnReset(false);
        buildAmplitudes(NORMALIZED_GAIN);
        // normalizeAmplitudes();
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

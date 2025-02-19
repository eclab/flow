// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which outputs the partials corresponding to a Rectified Sine wave,
   that is, a wave of the form Abs(Sin(x)).  
*/

public class Rectified extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

	public static final double NORMALIZED_GAIN = 0.6679687500000003;
	public static final double GAIN_MULTIPLIER = 8.0;					// twice that of triangle!
    public static final int MOD_GAIN = 0;
	double oldGain = NORMALIZED_GAIN;

	void buildAmplitudes(double gain)
		{
        // From http://www.dspguide.com/ch13/4.htm
        // We ignore the negative sign, since that's 
        // eliminated in the Sqrt(a^2 + b^2).  We
        // ignore the 4/pi since that's normalized away.
                
        double[] amplitudes = getAmplitudes(0);
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = gain * 1.0 / (4 * (i + 1) * (i + 1) - 1);
            }
                
		}
		
    // A rectified Sine Wave, that is, Abs(Sin(x))
    public Rectified(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { new Constant(NORMALIZED_GAIN / GAIN_MULTIPLIER) }, 
        		new String[] { "Gain" });

        setClearOnReset(false);

		buildAmplitudes(NORMALIZED_GAIN);
        // normalizeAmplitudes();
        //double[] amplitudes = getAmplitudes(0);
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

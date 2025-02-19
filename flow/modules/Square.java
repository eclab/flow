// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides partials for a Square Wave, or with a given 
   pulse width.  Pulse width is modulated by the Modulation "Pulse Width",
   with a standard square wave at 0.5.
*/

public class Square extends Unit implements UnitSource //, Parameterizable
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_PULSE_WIDTH = 0;
    public static final int MOD_GAIN = 1;

    double lastMod = -1;
        
	public static final double NORMALIZED_GAIN = 0.29344691229918385;
	public static final double GAIN_MULTIPLIER = 4.0;
	
	double oldGain = -1;

    public Square(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.HALF, new Constant(NORMALIZED_GAIN / GAIN_MULTIPLIER) }, 
        		new String[] { "Pulse Width", "Gain" });
        setClearOnReset(false);
        buildPWM();
        }

    public void go()
        {
        super.go();
                
        buildPWM();
        }
                
    public void buildPWM()
        {
        double mod = modulate(MOD_PULSE_WIDTH);
    	double gain = modulate(MOD_GAIN) * GAIN_MULTIPLIER;
        if (lastMod != mod || gain != oldGain)
            {
	        double[] amplitudes = getAmplitudes(0);
                
            if (mod == 0.5)  // easy case!
                {
                for(int i = 1; i < amplitudes.length; i+=2)
                    {
                    amplitudes[i] = 0;
                    }
                                        
                for(int i = 0; i < amplitudes.length; i+=2)
                    {
                    amplitudes[i] = gain * (double)(1.0 / (i+1));
                    }
                }
            else
                {
                // From here:   http://www.dspguide.com/ch13/4.htm
                // I presume I don't do a0
                // The 'a' component of Harmonic n is 2/(pi n) sin(pi n d) 
                // where d is the pulse width (0...1)
                // The 'b' component is 0
                // The magnitude, I believe, is Sqrt(a^2 + b^2), so
                // this means that we just do Math.abs(a).
                // The outer 2.0 / Math.PI is gonna get normalized away anyway...
                                
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = (mod == 0 || mod == 1 ? 0 : Math.abs(1.0 / (i+1) * Utility.fastSin(Math.PI * (i + 1) * mod)));
                    }
                }
                                
            //normalizeAmplitudes();
            lastMod = mod;
            oldGain = gain;
            }
        }
    }

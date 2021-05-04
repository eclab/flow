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

    // A rectified Sine Wave, that is, Abs(Sin(x))
    public Rectified(Sound sound) 
        {
        super(sound);

        double[] amplitudes = getAmplitudes(0);
                
        setClearOnReset(false);

        // From http://www.dspguide.com/ch13/4.htm
        // We ignore the negative sign, since that's 
        // eliminated in the Sqrt(a^2 + b^2).  We
        // ignore the 4/pi since that's normalized away.
                
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = 1.0 / (4 * (i + 1) * (i + 1) - 1);
            }
                        
        normalizeAmplitudes();
        }
    }

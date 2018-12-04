// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides all partials at 1.0 amplitude,
   with standardized frequencies.
*/

public class All extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public All(Sound sound) 
        {
        super(sound);
        
        double[] amplitudes = getAmplitudes(0);
                
        setClearOnReset(false);
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = 0.25;
            }
        }
    }

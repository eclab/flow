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

    public Sawtooth(Sound sound) 
    {
        super(sound);
        
        double[] amplitudes = getAmplitudes(0);
        
        setClearOnReset(false);
        for(int i = 0; i < amplitudes.length; i++)
            {
                amplitudes[i] = (double)(1.0 / (i+1));
            }
        normalizeAmplitudes();
    }
}

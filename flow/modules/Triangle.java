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

    public Triangle(Sound sound) 
    {
        super(sound);

        double[] amplitudes = getAmplitudes(0);
        
        setClearOnReset(false);
                
        for(int i = 0; i < amplitudes.length; i += 2)
            {
                amplitudes[i] = (double)(1.0 / ((i+1) * (i+1)));
            }
                
        normalizeAmplitudes();
    }
}

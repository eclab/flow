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

    public Sine(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Frequency" });

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        
        setClearOnReset(false);
        amplitudes[0] = 1;
        frequencies[0] = 1;
        }

    public void go()
        {
        super.go();
 
        double[] frequencies = getFrequencies(0);        
               
        double mod = modulate(0);
        frequencies[0] = mod * ((double)Unit.NUM_PARTIALS - 1.0) + 1;
        }
    }

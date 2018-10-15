// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**  
     A Unit which provides a simple 4-unit amplitude mixer.  The frequencies
     are copied from the first unit.  You can provide gains for each of the units
     as Modulations. 
*/
        
public class Mix extends Unit
    {
    private static final long serialVersionUID = 1;

    // Mixes the harmonics of up to four units, using the frequencies of the first unit
        
    public Mix(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE }, new String[] { "Gain A", "Gain B", "Gain C", "Gain D" });
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B", "Input C", "Input D" });
        }
        
    public void go()
        {
        super.go();
        
        pushFrequencies(0);

        double[] amplitudes = getAmplitudes(0);
        
        double[] amp1 = getAmplitudesIn(0);
        double[] amp2 = getAmplitudesIn(1);
        double[] amp3 = getAmplitudesIn(2);
        double[] amp4 = getAmplitudesIn(3);
        double gain1 = modulate(0);
        double gain2 = modulate(1);
        double gain3 = modulate(2);
        double gain4 = modulate(3);
                                
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = gain1 * amp1[i] + gain2 * amp2[i] + gain3 * amp3[i] + gain4 * amp4[i];
            }
                        
        constrain();
        }
    }

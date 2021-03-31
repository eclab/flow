// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which scales all the partials towards or away from the minimum partial.
   The degree of scaling depends on the Modulation SCALE.  The maximum scaling is 3.0.
*/

public class Scale extends Unit
{
    private static final long serialVersionUID = 1;

    public static final int MOD_SCALE = 0;

    public Scale(Sound sound) 
    {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.HALF }, new String[] { "Scale" });
    }

    public static final double MAX_BOUND = 3.0;
        
    public void go()
    {
        super.go();
                
        copyFrequencies(0);
        pushAmplitudes(0);

        double[] frequencies = getFrequencies(0);
        
        double mod = modulate(MOD_SCALE);
        if (mod == 0.5) mod = 1.0;
        else if (mod > 0.5) mod = ((mod - 0.5) * 2) * MAX_BOUND + 1.0;
        else mod = (mod * 2);
        double lowest = frequencies[getLowestPartial(0)];
                
        for(int i = 0; i < frequencies.length; i++)
            {
                frequencies[i] = (frequencies[i] - lowest) * mod + lowest;
            }

        if (constrain()) simpleSort(0, true);
    }       
}

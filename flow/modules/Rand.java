// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

/**
   A Unit which outputs random partials.  You can specify a seed which defines
   the partials for consistency.  If the seed is 0, then new random partials 
   are generated every single gate.
*/

public class Rand extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_SEED = 0;

    public Random random = null;
        
    public Object clone()
        {
        Rand obj = (Rand)(super.clone());
        if (obj.random != null)
            obj.random = new Random();  // will be reset on gate()
        return obj;
        }

    // for the time being we'll just keep to amplitudes
    public Rand(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ONE }, new String[] { "Seed" });
        }
        
    public void reseed()
        {
        double mod = modulate(MOD_SEED);
                
        if (mod != 0)
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }
        else if (random != null)
            {
            random = null;
            }
        }
                
    // this needs modulation
    public void gate()
        {
        super.gate();
                
        double[] amplitudes = getAmplitudes(0);
                
        reseed();
                
        Random rand = (random == null ? getSound().getRandom() : random);
                
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = rand.nextDouble();
            }
                        
        normalizeAmplitudes();
        }
    }

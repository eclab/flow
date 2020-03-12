// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides partials for a Sawtooth Wave.
*/

public class AudioIn extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final double MAX_GAIN = 4;
    public static final int MOD_GAIN = 0;

    public AudioIn(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { new Constant(1.0 / MAX_GAIN) }, new String[] { "Gain" });
        }

    public void go()
        {
        super.go();
        double[] amplitudes = getAmplitudes(0);
        sound.getOutput().getAudioInput().getAmplitudes(amplitudes);
        double mod = modulate(MOD_GAIN) * MAX_GAIN;
        for(int i = 0; i < amplitudes.length; i++)
            amplitudes[i] *= mod; 
        }       
        
    public static String getName() { return "Audio In"; }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_GAIN)
                {
                return "" + (value * MAX_GAIN);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
    }

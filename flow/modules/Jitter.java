// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

/**
   A Unit which adds jitter to either the frequency, or the amplitude, or both,
   of an incoming sound.  You can modify the variance of the frequency and amplitude
   jitter independently.  The jitter is added randomly each time a trigger arrives
   via TRIGGER.
*/

public class Jitter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int FREQUENCY_VAR = 0;
    public static final int AMPLITUDE_VAR = 1;
    public static final int TRIGGER = 2;
                        
    public double[][] targets = new double[3][];
    public boolean started = false;
    public Random random = null;
        
        
    public Jitter(Sound sound)
        {
        super(sound);

        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ONE }, 
            new String[] { "Freq Var", "Amp Var", "Trigger", "Seed" });

        targets[FREQUENCY_VAR] = new double[getFrequencies(0).length];
        targets[AMPLITUDE_VAR] = new double[getAmplitudes(0).length];
        }
        
    public void reseed()
        {
        double mod = modulate(3);
                
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
                
    public void gate()
        {
        super.gate();
                
        started = false;
        reseed();
        }
                
    public void go()
        {
        super.go();
                
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        
        double[] inputs0frequencies = getFrequenciesIn(0);
        double[] inputs0amplitudes = getAmplitudesIn(0);
        
        Random rand = (random == null ? getSound().getRandom() : random);
                                
        modulate(TRIGGER);
        double frequencyModulation = modulate(FREQUENCY_VAR);
        frequencyModulation = frequencyModulation * frequencyModulation * frequencyModulation * frequencyModulation;
        double amplitudeModulationModulation = modulate(AMPLITUDE_VAR);
        if (!started || isTriggered(TRIGGER))
            {
            for(int i = 0; i < targets[FREQUENCY_VAR].length; i++)
                {
                targets[FREQUENCY_VAR][i] = (rand.nextDouble() * 2.0 - 1.0) * frequencyModulation;
                targets[AMPLITUDE_VAR][i] = (rand.nextDouble() * 2.0 - 1.0) * modulate(AMPLITUDE_VAR);
                }
            started = true;
            }
                        
        for(int i = 0; i < targets[FREQUENCY_VAR].length; i++)
            {
            double f = inputs0frequencies[i] + targets[FREQUENCY_VAR][i] * 20;
            if (f >= 0) frequencies[i] = f;
            amplitudes[i] = inputs0amplitudes[i] + targets[AMPLITUDE_VAR][i] / 4;
            }

        constrain();

        // always sort
        simpleSort(0, false);
        }       
    }




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

        
    public static final int MOD_FREQ_VAR = 0;
    public static final int MOD_AMP_VAR = 1;
    public static final int MOD_TRIGGER = 2;
    public static final int MOD_SEED = 3;

    public double[][] targets = new double[3][];
    public boolean started = false;
    public Random random = null;
        
    boolean ampProportional = true;
    public boolean getAmpProportional() { return ampProportional; }    
    public void setAmpProportional(boolean val) { ampProportional = val; }
        
    boolean nonZero = true;
    public boolean getNonZero() { return nonZero; }
    public void setNonZero(boolean val) { nonZero = val; }
    
    public static final int OPTION_NONZERO = 0;
    public static final int OPTION_AMP_PROPORTIONAL = 1;

    public int getOptionValue(int option) 
    { 
        switch(option)
            {
            case OPTION_NONZERO: return getNonZero() ? 1 : 0;
            case OPTION_AMP_PROPORTIONAL: return getAmpProportional() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
    }
                
    public void setOptionValue(int option, int value)
    { 
        switch(option)
            {
            case OPTION_NONZERO: setNonZero(value != 0); return;
            case OPTION_AMP_PROPORTIONAL: setAmpProportional(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
    }


    public Object clone()
    {
        Jitter obj = (Jitter)(super.clone());
        if (obj.random != null)
            obj.random = new Random();  // will be reset on gate()
        obj.targets = (double[][])(obj.targets.clone());
        for(int i = 0; i < obj.targets.length; i++)
            {
                if (obj.targets[i] != null)
                    obj.targets[i] = (double[])(obj.targets[i].clone());
            }
        return obj;
    }

    public Jitter(Sound sound)
    {
        super(sound);

        defineOptions( new String[] { "Non-Zero", "Amp Ratio" }, new String[][] { { "Non-Zero" }, { "Amp Ratio"} });
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ONE }, 
                          new String[] { "Freq Var", "Amp Var", "Trigger", "Seed" });

        targets[FREQUENCY_VAR] = new double[getFrequencies(0).length];
        targets[AMPLITUDE_VAR] = new double[getAmplitudes(0).length];
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
                                
        double frequencyModulation = modulate(MOD_FREQ_VAR);
        frequencyModulation = frequencyModulation * frequencyModulation * frequencyModulation * frequencyModulation;
                
        boolean ratio = getAmpProportional();
                
        if (!started || isTriggered(MOD_TRIGGER))
            {
                for(int i = 0; i < targets[FREQUENCY_VAR].length; i++)
                    {
                        targets[FREQUENCY_VAR][i] = (rand.nextDouble() * 2.0 - 1.0) * frequencyModulation;
                        targets[AMPLITUDE_VAR][i] = (rand.nextDouble() * 2.0 - 1.0) * modulate(MOD_AMP_VAR);
                    }
                started = true;
            }
                        
        for(int i = 0; i < targets[FREQUENCY_VAR].length; i++)
            {
                if (!nonZero || inputs0amplitudes[i] > 0)
                    {
                        double f = inputs0frequencies[i] + targets[FREQUENCY_VAR][i] * 20;
                        if (f >= 0) frequencies[i] = f;
                        if (ratio)
                            amplitudes[i] = inputs0amplitudes[i] * (1.0 + targets[AMPLITUDE_VAR][i]);
                        else
                            {
                                double a = 0;
                                a = inputs0amplitudes[i] + targets[AMPLITUDE_VAR][i] / 4;
                                if (a >= 0) amplitudes[i] = a;
                            }
                    }
                else
                    {
                        frequencies[i] = inputs0frequencies[i];
                        amplitudes[i] = 0;
                    }
            }

        constrain();

        // always sort
        simpleSort(0, false);
    }       


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (isConstant)
            {
                if (modulation == MOD_SEED)
                    {
                        return (value == 0.0 ? "Free" : String.format("%.4f" , value));
                    }
                else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
    }


}




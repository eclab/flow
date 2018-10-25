// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which takes a source and adds detuning partials.  The partials are culled from the top
   half of the incoming partials from the source.  These partials are then reassigned to the same
   frequency and amplitude as the lower half partials, with some detuning amount added to the frequency.
   You can specify the detuning amount in cents.
*/

public class Fatten extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_DETUNE = 0;

    public Fatten(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Detune" });
        }
                
    double lastCents = Double.NaN;
    double factor = Double.NaN;
        
    public void go()
        {
        super.go();
                
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double cents = makeVerySensitive(modulate(MOD_DETUNE)) * 100;
        if (cents != lastCents)
            {
            lastCents = cents;
            factor = Math.pow(2.0, (cents / 1200.0));
            }
                                        
        // This tells us the cents increase.  I don't know if we should also drop by the same amount
        // or by some log difference.  For now I'm just increasing by cents
                
        copyFrequencies(0);
        copyAmplitudes(0);
                
        // First move over every other partial, reducing amplitude
        for(int i = frequencies.length - 1; i >= 0; i -= 2)
            {
            frequencies[i - 1] = frequencies[(i - 1)/2];
            amplitudes[i] = amplitudes[(i - 1)/2] / 2;
            amplitudes[i - 1] = amplitudes[(i - 1)/2] / 2;
            }
                        
        boolean needToSort = false;
                
        // Next revise partial frequencies
        for(int i = 0; i < frequencies.length; i += 2)
            {
            frequencies[i + 1] = frequencies[i] * factor;
            if (!needToSort && (i + 2 < frequencies.length) && frequencies[i + 2] <= frequencies[i + 1])
                needToSort = true;
            }
                
        if (constrain() || needToSort) simpleSort(0, false);
        }

    // We have to customize here because we have a "last cents", because it is so costly to compute it.
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        return String.format("%.2f", makeVerySensitive(value) * 100) + " Cents";
        }

    public String getModulationValueDescription(int modulation)
        {
        if (isModulationConstant(modulation))
            {
            double c = makeVerySensitive(modulate(MOD_DETUNE)) * 100;
            return String.format("%.2f", c) + " Cents";
            }
        else return "";
        }

    }

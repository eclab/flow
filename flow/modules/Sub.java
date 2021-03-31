// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which shifts the frequency of all partials.  There are three
   options:
   
   <ol>
   <li>Pitch.  Shift all partials by a certain pitch (for example, up an octave).
   This multiplies their frequencies by a certain amount.
   <li>Frequency.  Add a certain amount to the frequency of all partials.
   partials based on their distance from the nearest
   <li>Partials.  Move all partials towards the frequency of the next partial.
   </ol>
   
   The degree of shifting depends on the SHIFT modulation, bounded by the
   BOUND modulation.
*/

public class Sub extends Unit
{
    private static final long serialVersionUID = 1;

    public static final double BOUND = 4;
    public static final int NUM_SUBS = 4;
    public static final double[] SUB_FREQUENCIES = new double[] { 0.5, 0.25, 0.125, .0625 };

    public Sub(Sound sound) 
    {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { new Constant(1.0 / BOUND), Constant.ZERO, Constant.ZERO, Constant.ZERO }, 
                          new String[] { "-1 Oct", "-2 Oct", "-3 Oct", "-4 Oct" });
        setPushOrders(false);
    }
        
    public boolean isConstrainable() { return false; }  

    public void go()
    {
        super.go();
                
        copyFrequencies(0);
        copyAmplitudes(0);
        copyOrders(0);

        double[] frequencies = getFrequencies(0);
        double[] amplitudes = getAmplitudes(0);
        byte[] orders = getOrders(0);
        byte[] topOrders = new byte[NUM_SUBS];
        
        // make room at the bottom so we don't have to sort
        for(int j = 0; j < NUM_SUBS; j++)
            {
                topOrders[j] = orders[orders.length - 1 - j];
            }
                
        for(int j = frequencies.length - 1; j >= NUM_SUBS; j--)
            {
                frequencies[j] = frequencies[j - NUM_SUBS];
                amplitudes[j] = amplitudes[j - NUM_SUBS];
                orders[j] = orders[j - NUM_SUBS];
            }
                
        for(int i = 0; i < NUM_SUBS; i++)
            {
                frequencies[i] = SUB_FREQUENCIES[i];
                amplitudes[i] = modulate(i) * amplitudes[NUM_SUBS] * BOUND;
                orders[i] = topOrders[i];  // reuse the ones we just deleted
            }        

    }       

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (isConstant)
            {
                return super.getModulationValueDescription(modulation, value * 4, isConstant);
            }
        else return "";
    }
}

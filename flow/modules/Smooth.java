// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which smooths both amplitude and frequency changes in successive partials over time.
   The degree of smoothing is defined by AMOUNT.  If the option FREE is FALSE then
   smoothing is reset on each gate.
   
   <p>For smoothing to be effective, Smooth needs to keep track of partials even when they
   cross each other in frequency.  Thus smooth is the primary customer of the ORDERS mechanism,
   in case you were wondering why that existed.  For this to work well, Smooth ought to  receive
   partials which were produced by the <i>first</i> Unit Output of various modules.  Since most
   Unit modules only have one Unit Output, that'll be fine.
*/

public class Smooth extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_AMOUNT = 0;

    boolean free;
    boolean start;
        
    double smoothedFrequencies[];
    double smoothedAmplitudes[];
    byte smoothedOrders[];
        
    public boolean getFree() { return free; }
    public void setFree(boolean free) { this.free = free; }
        
    public static final int OPTION_FREE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return getFree() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public Smooth(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Amount" });
        defineOptions(new String[] { "Free" }, new String[][] { { "Free"} } );
        setFree(true);
        start = true;
        smoothedFrequencies = new double[NUM_PARTIALS];
        smoothedAmplitudes = new double[NUM_PARTIALS];
        smoothedOrders = new byte[NUM_PARTIALS];
        System.arraycopy(getOrders(0), 0, smoothedOrders, 0, smoothedOrders.length);
        setPushOrders(false);
        }
        
    public void reset()
        {
        super.reset();
        start = true;
        }
                
    public void gate()
        {
        super.gate();
                
        if (!free) start = true;
        }

    public void go()
        {
        super.go();
            
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        byte[] orders = getOrders(0);
        
        double[] inputs0frequencies = getFrequenciesIn(0);
        double[] inputs0amplitudes = getAmplitudesIn(0);
        byte[] inputs0orders = getOrdersIn(0);
        
        if (start)
            {
            for(int i = 0; i < frequencies.length; i++)
                {
                int order = inputs0orders[i];
                smoothedFrequencies[order] = inputs0frequencies[i];
                smoothedAmplitudes[order] = inputs0amplitudes[i];
                smoothedOrders[order] = (byte)i;
                }
            start = false;
            }
        else
            {
            double mod = makeVerySensitive(modulate(MOD_AMOUNT));
            for(int i = 0; i < frequencies.length; i++)
                {
                // This elaborate version of (1-mod) * s + mod * q
                // is to make sure we don't hit the subnormals, with a massive performance penalty.
                // I don't have evidence that it will happen here, but it sure did in Output.java
                // under very similar circumstances.  See "difficult bug" in Output.java.
                int order = inputs0orders[i];
                
                double sFreq = smoothedFrequencies[order];
                double iFreq = inputs0frequencies[order];
                if (sFreq > Output.WELL_ABOVE_SUBNORMALS || iFreq > Output.WELL_ABOVE_SUBNORMALS)
                    smoothedFrequencies[order] = (1 - mod) * sFreq + mod * iFreq;
                else
                    smoothedFrequencies[order] = 0;
                    
                double sAmp = smoothedAmplitudes[order];
                double iAmp = inputs0amplitudes[order];
                if (sAmp > Output.WELL_ABOVE_SUBNORMALS || iAmp > Output.WELL_ABOVE_SUBNORMALS)
                    smoothedAmplitudes[order] = (1 - mod) * sAmp + mod * iAmp;
                else
                    smoothedAmplitudes[order] = 0;
                }
            }

        System.arraycopy(smoothedFrequencies, 0, frequencies, 0, frequencies.length);
        System.arraycopy(smoothedAmplitudes, 0, amplitudes, 0, amplitudes.length);
        System.arraycopy(smoothedOrders, 0, orders, 0, orders.length);

        constrain();
        
        // always sort   
        simpleSort(0, false);
        }       
    }

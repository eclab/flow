// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/** 
    A Unit which combines the lower partials of two sources, A, and B.  The way the
    combination occurs depending on whether HALF is true or not.  If HALF is FALSE,
    then the partials are fully combined to form 256 partials, which are then sorted by
    frequency.  We then take the lowest 128 partials.  On the other hand, if HALF is TRUE, 
    then we take the lowest 64 partials from each source and put them together.
        
    <p>If MERGE is true, then if partials from the two sources have identical frequencies, 
    they are merged into a single partial with their combined amplitudes.  This will affect
    both of the previous methods, potentially adding to the number of partials they can add
    to the final mix.
        
    <p>At present Combine includes zero-amplitude partials.
    We might consider changing this in the future.
*/

public class Combine extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int UNIT_INPUT_A = 0;
    public static final int UNIT_INPUT_B = 1;

    public static final int MOD_SCALE_A = 0;
    public static final int MOD_SCALE_B = 1;

    /// Do we attempt to merge identical frequencies into one frequency, or load them independently?
    boolean merge = false;
    /// Do we guarantee that each incoming partials gets 1/2 of the final partials, or just load them both asynchronously until expended?
    boolean half = false;
        
    public boolean getMerge() { return merge; }
    public void setMerge(boolean val) { merge = val; }
        
    public boolean getHalf() { return half; }
    public void setHalf(boolean val) { half = val; }
        
    public static final int OPTION_MERGE = 0;
    public static final int OPTION_HALF = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_MERGE: return getMerge() ? 1 : 0;
            case OPTION_HALF: return getHalf() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_MERGE: setMerge(value != 0); return;
            case OPTION_HALF: setHalf(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    // Combines the partials of two units, then strips back the highest frequency partials
    // until we arrive at the original number of partials.
        
    public Combine(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B" });
        defineOptions(new String[] { "Merge", "Half" }, new String[][] { { "Merge" }, { "Half" } } );
        defineModulations(new Constant[] { Constant.ONE, Constant.ONE }, new String[] { "Scale A", "Scale B" });
        }
                        
    public void go()
        {
        super.go();

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        boolean _merge = merge;
                
        int i0 = 0;
        int i1 = 0;
        double[] frequencies0 = getFrequenciesIn(UNIT_INPUT_A);
        double[] amplitudes0 = getAmplitudesIn(UNIT_INPUT_A);
        double[] frequencies1 = getFrequenciesIn(UNIT_INPUT_B);
        double[] amplitudes1 = getAmplitudesIn(UNIT_INPUT_B);
        double f_out;
        
        double scaleA = modulate(MOD_SCALE_A);
        double scaleB = modulate(MOD_SCALE_B);
                
        double maxIncomingFreqLen = (half ? frequencies0.length / 2 : frequencies0.length);
                
        for(int i = 0; i < frequencies.length; i++)
            {
            if (_merge && i0 < maxIncomingFreqLen && i1 < maxIncomingFreqLen && frequencies0[i0] == frequencies1[i1])
                {
                frequencies[i] = frequencies0[i0];
                amplitudes[i] = amplitudes0[i0] * scaleA + amplitudes1[i1] * scaleB;
                i0++;
                i1++;
                }
            else if (i0 < maxIncomingFreqLen && (i1 >= maxIncomingFreqLen || frequencies0[i0] <= frequencies1[i1]))  // notice <=
                {
                frequencies[i] = frequencies0[i0];
                amplitudes[i] = amplitudes0[i0] * scaleA;
                i0++;
                }
            else // if (i1 < maxIncomingFreqLen && (i0 >= maxIncomingFreqLen || frequencies1[i1] < frequencies0[i0]))
                {
                frequencies[i] = frequencies1[i1];
                amplitudes[i] = amplitudes1[i1] * scaleB;
                i1++;
                }
            }

        // Now I just need to know what the frequency of the NEXT partial would have been

        if (i0 >= frequencies.length || i1 >= frequencies.length)
            {
            // do nothing
            }
        else if (_merge && i0 >= frequencies0.length && i1 >= frequencies1.length)   // dunno
            {
            // do nothing
            }
        else
            {
            if (i0 < maxIncomingFreqLen && (i1 >= maxIncomingFreqLen || frequencies0[i0] <= frequencies1[i1]))  // notice <=
                {
                f_out = frequencies0[i0];
                }
            else // if (i1 < maxIncomingFreqLen && (i0 >= maxIncomingFreqLen || frequencies1[i1] < frequencies0[i0]))
                {
                f_out = frequencies1[i1];
                }

            // To make things relatively smooth, we ramp down the amplitude of the final partial based on
            // the ratio of distance between it and its two neighbors.  This won't work right if all three
            // are the same value though; that will be a discontinuity.

            if ((f_out - frequencies[frequencies.length - 1]) != (f_out - frequencies[frequencies.length - 2]))
                amplitudes[amplitudes.length - 1] *= (f_out - frequencies[frequencies.length - 1]) / (f_out - frequencies[frequencies.length - 2]);
            }
                
        if (constrain()) simpleSort(0, false);
        }
    }

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

    int[] outstandingOrders;
    int[] outstandingOrderPositions;
        
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
        setPushOrders(false);
        }
                        
    public void go()
        {
        super.go();

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        byte[] orders = getOrders(0);
                
                
        /// PROPOSAL FOR COMBINING
        /// 1: Determine WHICH partials of A and B will be preserved 
        /// 2: Map A's partials to low 1/n partials?  Preserving numbers when possible.  When not possible, fill remainder in low-to-high
        /// 3. Map B's partials to low 1/m partials?  In same way
        /// 4. Remap to even-odd afterwards: A -> even, B -> odd, then remainder    // this way, both A and B have low partials in case of further combination
        /// 5. Load partials
        /// 6. Sort by frequency
    
    
                
        // Combine is kind of a mess at preserving orders.  So we're doing it as follows:
        // 1. All of A's orders are preserved
        // 2. Any of B's orders which CAN be preserved without conflicting with #1 will be preserved
        // 3. We arbitrarily assign the remainder
        // To do this we need some arrays, and we'll have to allocate them every time or otherwise
        // zero them out.  This allows us to avoid doing sorts, yay.
        boolean[] filledOrders = new boolean[amplitudes.length];  // all false

        if (outstandingOrders == null) outstandingOrders = new int[amplitudes.length];
        if (outstandingOrderPositions == null) outstandingOrderPositions = new int[amplitudes.length];
                
        boolean _merge = merge;
                
        int iA = 0;
        int iB = 0;
        int numOutstanding = 0;
        double[] frequenciesA = getFrequenciesIn(UNIT_INPUT_A);
        double[] amplitudesA = getAmplitudesIn(UNIT_INPUT_A);
        double[] frequenciesB = getFrequenciesIn(UNIT_INPUT_B);
        double[] amplitudesB = getAmplitudesIn(UNIT_INPUT_B);
        byte[] ordersA = getOrdersIn(UNIT_INPUT_A);
        byte[] ordersB = getOrdersIn(UNIT_INPUT_B);
        double f_out;
        
        double scaleA = modulate(MOD_SCALE_A);
        double scaleB = modulate(MOD_SCALE_B);
                
        double maxIncomingFreqLen = (half ? frequenciesA.length / 2 : frequenciesA.length);

        for(int i = 0; i < frequencies.length; i++)
            {
            if (_merge && iA < maxIncomingFreqLen && iB < maxIncomingFreqLen && frequenciesA[iA] == frequenciesB[iB])
                {
                frequencies[i] = frequenciesA[iA];
                amplitudes[i] = amplitudesA[iA] * scaleA + amplitudesB[iB] * scaleB;
                
                // we'll assign the order from A
                orders[i] = ordersA[iA];
                int o = ordersA[iA] & 0xFF;
                //               if (o < 0) o += 256;
                filledOrders[o] = true;         // this order is now used
                iA++;
                iB++;
                }
            else if (iA < maxIncomingFreqLen && (iB >= maxIncomingFreqLen || frequenciesA[iA] <= frequenciesB[iB]))  // notice <=
                {
                frequencies[i] = frequenciesA[iA];
                amplitudes[i] = amplitudesA[iA] * scaleA;

                // we'll assign the order from A
                orders[i] = ordersA[iA];
                int o = ordersA[iA] & 0xFF;
                //               if (o < 0) o += 256;
                filledOrders[o] = true;         // this order is now used
                iA++;
                }
            else // if (iB < maxIncomingFreqLen && (iA >= maxIncomingFreqLen || frequenciesB[iB] < frequenciesA[iA]))
                {
                frequencies[i] = frequenciesB[iB];
                amplitudes[i] = amplitudesB[iB] * scaleB;
                
                // we won't assign the order from B just yet, but we'll store it in the hopes that we can assign it later
                int o = ordersB[iB] & 0xFF;
                //                if (o < 0) o += 256;
                outstandingOrders[numOutstanding] = o;
                outstandingOrderPositions[numOutstanding] = i;          // this basically says that partial i would *like* to have order o
                numOutstanding++;
                iB++;
                }
            }

        // Now I just need to know what the frequency of the NEXT partial would have been

        if (iA >= frequencies.length || iB >= frequencies.length)
            {
            // do nothing
            }
        else if (_merge && iA >= frequenciesA.length && iB >= frequenciesB.length)   // dunno
            {
            // do nothing
            }
        else
            {
            if (iA < maxIncomingFreqLen && (iB >= maxIncomingFreqLen || frequenciesA[iA] <= frequenciesB[iB]))  // notice <=
                {
                f_out = frequenciesA[iA];
                }
            else // if (iB < maxIncomingFreqLen && (iA >= maxIncomingFreqLen || frequenciesB[iB] < frequenciesA[iA]))
                {
                f_out = frequenciesB[iB];
                }

/*
// To make things relatively smooth, we ramp down the amplitude of the final partial based on
// the ratio of distance between it and its two neighbors.  This won't work right if all three
// are the same value though; that will be a discontinuity.

if ((f_out - frequencies[frequencies.length - 1]) != (f_out - frequencies[frequencies.length - 2]))
{
double d = (f_out - frequencies[frequencies.length - 1]) / Math.abs(f_out - frequencies[frequencies.length - 2]);
amplitudes[amplitudes.length - 1] *= (f_out - frequencies[frequencies.length - 1]) / Math.abs(f_out - frequencies[frequencies.length - 2]);
}
*/
            }

        
        /*
        // Fill in the outstanding orders that we can fill
        for(int j = 0; j < numOutstanding; j++)
        {
        if (!filledOrders[outstandingOrders[j]]) // the requested order isn't being used yet, so we can use it
        {
        filledOrders[outstandingOrders[j]] = true;                                                              // mark it used
        orders[outstandingOrderPositions[j]] = (byte)outstandingOrders[j];              // use it
        outstandingOrders[j] = -1;                                                                                      // eliminate it so we don't try to force it in the next pass
        }
        }
        */
                
        // Force the remaining orders
        int nextOrder = 0;
        for(int j = 0; j < numOutstanding; j++)
            {
            if (outstandingOrders[j] >= 0)  // still hasn't been filled, we need to force it to something
                {
                // find next unfilled order
                while(filledOrders[nextOrder]) nextOrder++;
                        
                // fill
                filledOrders[nextOrder] = true;
                orders[outstandingOrderPositions[j]] = (byte)nextOrder;
                }
            }
                
        if (constrain()) simpleSort(0, false);
        }
    }

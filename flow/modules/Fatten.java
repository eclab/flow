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

    public static final int MOD_WET = 0;
    public static final int MOD_DETUNE = 1;

    public Fatten(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ONE, Constant.ZERO }, new String[] { "Wet", "Detune" });
        setPushOrders(false);
        }
                
    double lastCents = Double.NaN;
    double factor = Double.NaN;
    
    // EXPLANATION
    //
    // I used to do fattening by just matching the top partials with ones in the bottom.  That works fine as long
    // as the bottom partials (and the top partials!) didn't cross one another; otherwise you'd get clicks.  :-(
    // Now we have to do a much more elaborate, and likely slightly more costly, process to deal with clicks.  It's still O(n).
    //
    // The general idea is to map the upper partials, by order, to lower partials, and try to retain that mapping
    // if at all possible.  The procedure for mapping is called remap(), and it's called only when Fatten determines
    // that the partials have crossed one another, or at the very beginning.  remap() tries to retain existing mappings
    // and only fix the broken ones by remapping to available partials.
    //
    // Fatten determines, during go(), that partials have crossed if the previous order of the incoming partials has changed.
    // I does this by retaining the previous order in lastOrders[] and comparing it against the current order.
    //
    // To reorganize the partials, go() must generate three temp arrays, so we have them below so it can reuse them.  They 
    // are tempFrequencies[], tempAmplitudes[], and tempOrders[]
    //
    // remap() internally retains its existing mappings (which it's trying to preserve, so it uses those as notes) in 
    // the arraymapping[].  remap() generates a final array which maps the orders of upper partials to the *location* 
    // of lower partials, in mappingPos[].  mappingPos[] is what's used in go() to actually move the upper partials to
    // their new locations.
    //
    // To do the remapping, remap() must load and scan a bunch of temporary arrays.  Rather than allocate them over and over
    // again if we have lots of constant crossing, we retain them here.  These arrays are: isLower[], isMapped[],
    // and lowerMappingPos[]

    
    // The orders of the PREVIOUS partials. If this is null or doesn't match the current orders (thus some partials
    // have crossed one another) then we have to remap partials.
    byte[] lastOrders = null;
    
    // A temporary storage variable used in remap().  Indicates which partials, by order, are lower partials.
    boolean[] isLower = null;
    
    // A temporary storage variable used in remap().  Indicates which partials, by order, are already mapped to by upper partials.
    boolean[] isMapped = null;
    
    // remap() maintains this array to map upper orders to lower orders.
    // If mapping[i] == -1 then partial order *i* is a lower partial and we don't care
    // about it.  Otherwise, upper partial order *i* has been mapped to the lower partial with order mapping[i].
    int[] mapping = null;

    // remap() uses this temp array to build mappingPos.
    int[] lowerMappingPos = null;

    // remap() builds this array to map upper orders to lower partial *positions*.
    int[] mappingPos = null;
    
    // three scratch arrays for reorganizing the partials, used in go().
    double[] tempFrequencies = null;
    double[] tempAmplitudes = null;
    byte[] tempOrders = null;
    
    public Object clone()
        {
        Fatten fat = (Fatten)(super.clone());
        fat.lastOrders = null;
        fat.isLower = null;
        fat.isMapped = null;
        fat.mapping = null;
        fat.lowerMappingPos = null;
        fat.mappingPos = null;
        fat.tempFrequencies = null;
        fat.tempAmplitudes = null;
        fat.tempOrders = null;
        return fat;
		}
    
    public void remap()
        {
        byte[] orders = getOrders(0);
        int halflen = orders.length / 2;
        
        /// STEP 1: Allocate the arrays if necessary
        if (mapping == null)
            {
            mapping = new int[orders.length];
            mappingPos = new int[orders.length];
            lowerMappingPos = new int[orders.length];
            isLower = new boolean[orders.length];
            isMapped = new boolean[orders.length];
            for(int i = 0; i < mapping.length; i++)
                {
                mapping[i] = -1;                // everyone must be remapped
                mappingPos[i] = -1;             // this isn't necessary but it'll cause us to fail with an exception if something's wrong
                }
            }

        // STEP 2: Build the isLower array, where isLower[i] is true if partial order *i* is in the lower half
        for(int i = 0; i < isLower.length; i++)
            {
            isLower[i] = false;
            isMapped[i] = false;                    // also clean out isMapped
            }

        for(int i = 0; i < halflen; i++)
            {
            int o = orders[i];
            if (o < 0) o += 256;
            isLower[o] = true;
            mapping[o] = -1;                                // also clean out mapping[], otherwise this can cause serious weird bugs
            }
                
        // STEP 3: Find the aleady-mapped lower partials
        for(int i = halflen; i < orders.length; i++)
            {
            int o = orders[i];
            if (o < 0) o += 256;
            int m = mapping[o];
            if (m != -1 &&          // the partial is mapped to someone
                isLower[m])         // this someone is in the lower space
                {
                isMapped[m] = true;
                }
            }
    
//            System.err.println("" + sound + " " + orders + " " + isMapped + " " + isLower);

        // STEP 4: Map the free upper partials to remaining unmapped lower partials
        int l = 0;
        for(int i = halflen; i < orders.length; i++)
            {
            int o = orders[i];
            if (o < 0) o += 256;
            int m = mapping[o];
            if (m == -1 ||          // found a partial marked free in the first place
                !isLower[m])    // found a partial mapped to a partial not in the lower space 
                {
                // find the next unmarked lower partial
                for( ; isMapped[l] || !isLower[l]; l++);
            		             
                // At this point l is now a free lower partial
                mapping[o] = l;
//                print("Mapping " + i + "(" + o + ") -> (" + l + ") " + isMapped[l] + " " + isLower[l]);
                                
                isMapped[l] = true;
                }
            }
                        
        /** Are our top partials uniquely mapped to lower partials? */
        /*      {
                boolean[] iL = new boolean[isLower.length];
                boolean[] iM = new boolean[isMapped.length];
                for(int i = 0; i < isLower.length; i++)
                {
                iL[i] = false;
                iM[i] = false;
                }

                for(int i = 0; i < halflen; i++)
                {
                int o = orders[i];
                if (o < 0) o += 256;
                iL[o] = true;
                }

                for(int i = halflen; i < orders.length; i++)
                {
                int ord = orders[i];
                if (ord < 0) ord += 256;
                int m = mapping[ord];
                if (m == -1) continue;
                        
                if (!iL[m])
                print("+>Mapped to non-lower order " + m);
                if (iM[m])
                print("+>Mapped multiply to " + m);
                iM[m] = true;
                }
                }
        */






        // STEP 5: Map all upper partials, by order, to lower partials by position
        for(int i = 0; i < halflen; i++)
            {
            int o = orders[i];
            if (o < 0) o += 256;
            lowerMappingPos[o] = i;
            }
        for(int i = halflen; i < orders.length; i++)
            {
            int o = orders[i];
            if (o < 0) o += 256;
            mappingPos[o] = lowerMappingPos[mapping[o]];
            }

        /** Do we still have unique orderings? */

        /*
          boolean[] got = new boolean[orders.length];
          for(int i = 0; i < orders.length; i++)
          {
          int ord = orders[i];
          if (ord < 0) ord += 256;
          if (got[ord])
          print("-->Already " + ord);
          got[ord] = true;
          }
          for(int i = 0; i < orders.length; i++)
          {
          if (!got[i])
          print("-->Missing " + i);
          }
        */




        /** Are our top partials uniquely mapped to lower partials? */
        /*
          {
          boolean[] iL = new boolean[isLower.length];
          boolean[] iM = new boolean[isMapped.length];
          for(int i = 0; i < isLower.length; i++)
          {
          iL[i] = false;
          iM[i] = false;
          }

          for(int i = 0; i < halflen; i++)
          {
          int o = orders[i];
          if (o < 0) o += 256;
          iL[o] = true;
          }

          for(int i = halflen; i < orders.length; i++)
          {
          int ord = orders[i];
          if (ord < 0) ord += 256;
          int m = mapping[ord];
          if (m == -1) continue;
                        
          if (!iL[m])
          print("?>Mapped to non-lower order " + m);
          if (iM[m])
          print("?>Mapped multiply to " + m);
          iM[m] = true;
          }
          }
        */

        // We should be good to go at this point
        }
    
    
    public void go()
        {
        super.go();
                
        copyFrequencies(0);
        copyAmplitudes(0);
        copyOrders(0);

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        byte[] orders = getOrders(0);
                
        double cents = makeVerySensitive(modulate(MOD_DETUNE)) * 100;
        if (cents != lastCents)
            {
            lastCents = cents;
            factor = Math.pow(2.0, (cents / 1200.0));
            }
            
        double wet = modulate(MOD_WET);
                                        
        // This tells us the cents increase.  I don't know if we should also drop by the same amount
        // or by some log difference.  For now I'm just increasing by cents
                
        
        // First step: we need to determine if we need to remap
        
        if (lastOrders == null) 
            {
            remap();
            lastOrders = (byte[])(getOrders(0).clone());
            }
        else
            {
            for(int i = 0; i < lastOrders.length; i++)
                {
                if (lastOrders[i] != orders[i])
                    {
                    remap();
                    lastOrders = (byte[])(getOrders(0).clone());
                    break;
                    }
                }
            }

        // Mix the upper and lower partials
        if (tempFrequencies == null)
            {
            tempFrequencies = new double[frequencies.length];
            tempAmplitudes = new double[amplitudes.length];
            tempOrders = new byte[orders.length];
            }
                
        int halflen = orders.length / 2;
        for(int i = 0; i < halflen; i++)
            {
            tempOrders[i * 2] = orders[i];
            tempFrequencies[i * 2] = frequencies[i];
            tempAmplitudes[i * 2] = amplitudes[i];
            }
                
        for(int i = halflen; i < orders.length; i++)
            {
            int ord = orders[i];
            if (ord < 0) ord += 256;
            int pos = mappingPos[ord]; 
                
            tempOrders[pos * 2 + 1] = orders[i];		// notice it's orders[i], not orders[pos*2].  This is correct.
            tempFrequencies[pos * 2 + 1] = tempFrequencies[pos * 2];
            tempAmplitudes[pos * 2 + 1] = tempAmplitudes[pos * 2];
            }
                
        System.arraycopy(tempOrders, 0, orders, 0, orders.length);
        System.arraycopy(tempFrequencies, 0, frequencies, 0, frequencies.length);
        System.arraycopy(tempAmplitudes, 0, amplitudes, 0, amplitudes.length);

        // Next revise partial frequencies
                                        
        boolean needToSort = false;
                
        for(int i = 0; i < frequencies.length; i += 2)
            {
            frequencies[i + 1] = frequencies[i] * factor;
            amplitudes[i + 1] *= wet;
            if (!needToSort && (i + 2 < frequencies.length) && frequencies[i + 2] <= frequencies[i + 1])
                needToSort = true;
            }
                
        if (constrain() || needToSort) simpleSort(0, false);
        }


    // We have to customize here because we have a "last cents", because it is so costly to compute it.
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation == MOD_DETUNE && isModulationConstant(modulation))
            {
            double c = makeVerySensitive(modulate(MOD_DETUNE)) * 100;
            return String.format("%.2f", c) + " Cents";
            }
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }

    }

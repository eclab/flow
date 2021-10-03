// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

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

    static final int NUM_INPUTS = 4; 
    public static final int UNIT_INPUT_A = 0;
    public static final int UNIT_INPUT_B = 1;
    public static final int UNIT_INPUT_C = 2;
    public static final int UNIT_INPUT_D = 3;

    public static final int MOD_SCALE_A = 0;
    public static final int MOD_SCALE_B = 1;
    public static final int MOD_SCALE_C = 2;
    public static final int MOD_SCALE_D = 3;

    static final int INVALID = -1;
    Unit[/*NUM_INPUTS*/] currentInputs = null;						// Which inputs are active?
    int[/*NUM_INPUTS*/][/*Unit.NUM_PARTIALS*/] inToIndex = null;	// For each input, mapping of incoming partial ordering (& 255) -> input index.  Mappings of nonexistent partials INVALID.
    int[/*NUM_INPUTS*/][/*division*/] indexToOut = null;			// For each input, what is (or was) the output ordering of incoming partial #n?  Will be values 0...255.
    int[/*NUM_INPUTS*/][/*division*/] newIndexToOut = null;			// We'll swap this back and forth with indexToOut to avoid reallocation
	int[/*Unit.NUM_PARTIALS*/] outIndex = new int[Unit.NUM_PARTIALS];	// For each output partial, where is it located in the output?
	
	
    void resetOrderMapping()
    	{
    	indexToOut = null;
    	newIndexToOut = null;
    	inToIndex = null;
    	currentInputs = null;
    	}
    	
    void rebuildMappings()
    	{
    	// This code will either BUILD entirely new mappings or will UPDATE them.
    	// Ideally building only happens when we absolutely must, since it will create
    	// clicks.  Updating will happen regularly as small changes occur.
		//
    	// This code works as follows:
    	//
    	// If the inputs have changed, 
    	//     If there are no inputs at all
    	//         Set the mappings to null
    	//     Else
    	//         Build all mappings from scratch
    	// Else we have to fix the mappings:
    	//     If the mappings are not null
    	//         For each input
    	//             If the input's mapping has changed 
    	//                 Fix it
    	//
    	// After calling this method:
    	//     0. currentInputs will not be null and will reflect all the getInput(...) inputs
    	//     1. If indexToOut is null, then there are no inputs
    	//     2. Else indexToOut will reflect mappings of output orderings (and thus indices) corresponding to input partial indices
    	//
    	// The challenge is in doing this method without O(n^2) operations nor with any hashing.  Ideally O(n).
    	// We also would like to avoid allocation, though we can't avoid bzero-style refilling in several places.
    	// So here we go...
    	
    	
    	// If we don't have any inputs specified OR if they have changed
    	if (currentInputs == null ||
    		(getInput(0) != currentInputs[0]) ||
    		(getInput(1) != currentInputs[1]) ||
    		(getInput(2) != currentInputs[2]) ||
    		(getInput(3) != currentInputs[3]))
    		{
    		// Reload the current inputs and count them
    		currentInputs = new Unit[NUM_INPUTS];
	    	int count = 0;
	    	for(int i = 0; i < NUM_INPUTS; i++)
	    		{
	    		currentInputs[i] = getInput(i);
	    		if (currentInputs[i] != Unit.NIL)
	    			{
	    			count++;
	    			}
	    		}
	    	
	    	// Do we have any inputs?
	    	if (count == 0)  // nobody is connected
	    		{
	    		// clear everything
	    		resetOrderMapping();
	    		}
	    	else
	    		{
	    		// Determine which is the first active input
	    		int firstInput = 0;
				for(int i = 0; i < NUM_INPUTS; i++)
					{
					if (getInput(i) != Unit.NIL)
						{
						firstInput = i;
						break;
						}
					}
					
				// How many outgoing partials will be apportioned to each input?  These are "divisions".
				// Compute divisions of the outgoing partials by incoming input.
				// The first active input will have the extra if it's not an even split.
				int division = Unit.NUM_PARTIALS / count;
				int firstInputDivision = division + Unit.NUM_PARTIALS - division * count;

				// Build the mappings
				indexToOut = new int[NUM_INPUTS][];
				newIndexToOut = new int[NUM_INPUTS][];
				inToIndex = new int[NUM_INPUTS][Unit.NUM_PARTIALS];
				int ordOut = 0;
				for(int i = 0; i < NUM_INPUTS; i++)
					{
					int maplen = (i == firstInput) ? firstInputDivision : division;
					byte[] ord = getOrdersIn(i);
					indexToOut[i] = new int[maplen];
					newIndexToOut[i] = new int[maplen];
					int[] ito = indexToOut[i];					
					int[] iti = inToIndex[i];
					Arrays.fill(iti, INVALID);
					for(int j = 0; j < maplen; j++)
						{
						iti[ord[j] & 255] = j;
						ito[j] = ordOut++;
						}
					}
	    		}
    		}
    	else
    		{
    		// If the current mapping is null, we do nothing at all, as there are no inputs
//    		if (indexToOut != null)
//				{
				// Go through every input and determine if its valid partials have changed 
				for(int i = 0; i < NUM_INPUTS; i++)
					{
					byte[] ord = getOrdersIn(i);		// this is the NEW mapping of incoming indices to orders
					int[] iti = inToIndex[i];			// this is the OLD mapping of incoming orders to indices
					int[] ito = indexToOut[i];			// this is the OLD mapping of indices to outgoing orders
					int maplen = ito.length;			// ord is full 256, but we want oti's length instead

					// Maybe nothing has changed at all?
					// We'll not do anything special to test this -- 
					// just check to see that the orderings haven't changed.
					boolean changed = false;
					
					for(int j = 0; j < maplen; j++)
						{
						if (iti[ord[j] & 255] != j)
							{
							changed = true;					// oops something changed
							break;
							}
						}
						
					if (changed)
						{
						// Okay, since something has changed, we have to determine what's what
						
						// Build a new version of ito index->output ordering, initially INVALID. 
						// Also count how many slots we filled.
						int[] newito = newIndexToOut[i];
						Arrays.fill(newito, INVALID);
						int count = 0;
						for(int j = 0; j < maplen; j++)	// we're going through ord, but only up to maplen
							{
							int o = ord[j] & 255;				// new ordering at position j
							if (iti[o] != INVALID)
								{
								// iti[o] is the OLD index associated with this ordering now at index j
								// We need to set newito[j] to be the ito at that old index
								newito[j] = ito[iti[o]];
								// Now we mark out the old output ordering to indicate that it's been used
								ito[iti[o]] = INVALID;
								count++;
								}
							}

						// At this point, newito contains proper mappings or INVALID.  We can use the INVALID
						// regions of newito in two ways: (1) to identify the spots that still need to be filled,
						// and (2) to identify the incoming partials that need to fill then.  We might as well
						// fill a partial with the partial at the same location. 
						//
						// count is the number of proper mappings.   If we don't have any NEW mappings
						// coming and going, then count will equal maplen and we're done.  Otherwise
						// we have to fill in the new mappings							
						if (count != maplen)
							{
							int nextUnmapped = 0;			// next available slot in newito
							int nextOpen = 0;				// next available slot in ito
							for(int x = 0; x < maplen - count; x++)
								{
								// Find the next unmapped input index
								for( ; newito[nextUnmapped] == INVALID; nextUnmapped++);
								// Find the next unused output ordering
								for( ; ito[nextOpen] == INVALID; nextOpen++);
								// Fill in the empty slot with the ordering of at the same position
								// as it so happens
								newito[nextUnmapped] = ito[nextOpen];
								// skip over these so we don't do them again
								nextUnmapped++;
								nextOpen++;
								}
							}
							
						// Swap the new ito with the old one
						int[] temp = newIndexToOut[i];
						newIndexToOut[i] = indexToOut[i];
						indexToOut[i] = temp;
						
						// Build :-( the new iti
						Arrays.fill(iti, INVALID);					// is this really necessary?
						for(int j = 0; j < maplen; j++)
							{
							iti[ord[j] & 255] = j;
							}
						}
					}
//				}
    		}
    	}
    	
    
    	
    // Combines the partials of two units, then strips back the highest frequency partials
    // until we arrive at the original number of partials.
        
    public Combine(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B", "Input C", "Input D" });
        defineModulations(new Constant[] { Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE }, new String[] { "Scale A", "Scale B", "Scale C", "Scale D" });
        setPushOrders(false);
        for(int i = 0; i < outIndex.length; i++)
        	outIndex[i] = i;
        }
                    
    int foo = 0;    
    public void go()
        {
        super.go();

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        byte[] orders = getOrders(0);
        
        rebuildMappings();
        
        // Now we copy over amplitudes, frequencies, orderings
        if (currentInputs != null)
        	{
			for(int i = 0; i < NUM_INPUTS; i++)
				{
				double mod = modulate(i);
				if (currentInputs[i] != Unit.NIL)
					{
					double[] ampIn = getAmplitudesIn(i);
					double[] freqIn = getFrequenciesIn(i);
				
					int[] ito = indexToOut[i];
					for(int j = 0; j < ito.length; j++)
						{
						int position = outIndex[ito[j]];
						amplitudes[position] = ampIn[j] * mod;
						frequencies[position] = freqIn[j];
						orders[position] = (byte)(ito[j]);
						}
					}
				}
			}
			
        // we're probably way out of whack
        //if (constrain() || outOfOrder(0))
        constrain();
        
        if (outOfOrder(0))
        	{
        	bigSort(0, false);
        	
        	orders = getOrders(0);		// reload now that we have sorted
        	for(int i = 0; i < orders.length; i++)
        		{
        		outIndex[orders[i] & 255] = i;
        		}
        	}
        }
    }

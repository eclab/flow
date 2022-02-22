// Copyright 2021 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
   A Unit that converts notes into chords, by stealing from the high partials.
*/

public class Chord extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_GAIN = 0;

    public static final String[] names = 
        { 
        "None", 
        "m2", "M2", "m3", "M3", "4", "TT", "5", "m6", "M6", "m7", "M7", "Oct", "Oct+m3", "Oct+M3", "Oct+5", "2 Oct",
        "min", "min-1", "min-2", "Maj", "Maj-1", "Maj-2", 
        "7", "min7", "Maj7", "dim7", "min+Oct", "Maj+Oct"
        };

    // These are the semitone values for the chords
    public static final int[][] chords = 
        { 
        {0},
        {0, 1},
        {0, 2},
        {0, 3},
        {0, 4},
        {0, 5},
        {0, 6},
        {0, 7},
        {0, 8},
        {0, 9},
        {0, 10},
        {0, 11},
        {0, 12},
        {0, 15},
        {0, 16},
        {0, 19},
        {0, 24},
        {0, 3, 7},
        {0, 4, 9},
        {0, 5, 8},
        {0, 4, 7},
        {0, 3, 8},
        {0, 5, 9},
        {0, 4, 7, 10},
        {0, 3, 7, 10},
        {0, 4, 7, 11},
        {0, 3, 6, 9},
        {0, 3, 7, 12},
        {0, 4, 7, 12},
        };
                

    // Relative frequency ratios for each semitone
    public static final double[] semitoneFrequencyRatios = 
        {
        1.0,
        1.0594630943592953,
        1.1224620483093730,
        1.1892071150027210,
        1.2599210498948732,
        1.3348398541700344,
        1.4142135623730951,
        1.4983070768766815,
        1.5874010519681994,
        1.6817928305074290,
        1.7817974362806785,
        1.8877486253633868,
        2.0,
        2.1189261887185906,
        2.2449240966187460,
        2.3784142300054420,
        2.5198420997897464,
        2.6696797083400687,
        2.8284271247461903,
        2.9966141537533630,
        3.1748021039363990,
        3.3635856610148580,
        3.5635948725613570,
        3.7754972507267740,
        4.0
        };


    // Relative frequency ratios for each semitone
    public static final double[] alignedSemitoneFrequencyRatios = 
        {
        1.0,
        1.0594630943592953,
        1.1224620483093730,
        1.1892071150027210,
        1.2599210498948732,
        1.3348398541700344,
        1.4142135623730951,
        1.5,
        1.5874010519681994,
        1.6817928305074290,
        1.7817974362806785,
        1.8877486253633868,
        2.0,
        2.1189261887185906,
        2.2449240966187460,
        2.3784142300054420,
        2.5198420997897464,
        2.6696797083400687,
        2.8284271247461903,
        3.0,
        3.1748021039363990,
        3.3635856610148580,
        3.5635948725613570,
        3.7754972507267740,
        4.0
        };

    public static String getName() { return "Chord"; }

    int chord = 0;
    public void setChord(int val) { chord = val; }
    public int getChord() { return chord; }

    boolean align = false;
    public void setAlign(boolean val) { align = val; }
    public boolean getAlign() { return align; }

    public static final int OPTION_CHORD = 0;
    public static final int OPTION_ALIGN = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_CHORD: return getChord();
            case OPTION_ALIGN: return (getAlign() ? 1 : 0);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_CHORD: setChord(value); return;
            case OPTION_ALIGN: setAlign(value == 1); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }


	// Chord's strategy for loading chords without having to resort is to
	// Load all the partials for each note into a separate array, then
	// merge the arrays in an O(1) merge kind of like mergesort


	double[][] _freqs;
	double[][] _amps;
	int[] _pos;
	int lastChord = 0;			// 0 ("no chord") is the default
    
    void loadSubarrays(int chord, double[] frequencies, double[] amplitudes, double[] ratios)
    	{
    	if (_freqs == null || lastChord != chord)
    		{
    		int chordSize = chords[chord].length;
    		int numPartials = NUM_PARTIALS / chordSize + 1;
    		_freqs = new double[chordSize][numPartials];
    		_amps = new double[chordSize][numPartials];
    		_pos = new int[chordSize];
    		}
    		
        double gain = modulate(MOD_GAIN);
        int[] _chords = chords[chord];
        int chordSize = _chords.length;
    	int numPartials = NUM_PARTIALS / chordSize + 1;
		for(int j = 0; j < chordSize; j++)
			{
			double[] _freqsj = _freqs[j];
			double[] _ampsj = _amps[j];
			int _chordsj = _chords[j];
    		for(int i = 0; i < numPartials; i++)
    			{
				_freqsj[i] = frequencies[i] * ratios[_chordsj];
				_ampsj[i] = amplitudes[i] * gain;
				}
    		}
    	}
    	
    
    public Chord(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ONE }, new String[] { "Gain" });
        defineOptions(new String[] { "Chord", "Align 5ths" }, new String[][] { names, { "Align 5ths" } } );
        }
        
    public void go()
        {
        super.go();

        if (chord == 0) // no change
            {
            pushFrequencies(0);
            pushAmplitudes(0);
            lastChord = 0;					// reset
            }
        else
            {
            copyFrequencies(0);
            copyAmplitudes(0);
            double[] ratios = align ? alignedSemitoneFrequencyRatios : semitoneFrequencyRatios;
            double[] frequencies = getFrequencies(0);
            double[] amplitudes = getAmplitudes(0);

            loadSubarrays(chord, frequencies, amplitudes, ratios);

			int[] __pos = _pos;
			double[][] __freqs = _freqs;
			double[][] __amps = _amps;
			
			for(int i = 0; i < __pos.length; i++)
				{
				__pos[i] = 0;
				}
				
			for(int i = 0; i < frequencies.length; i++)
				{
				// Find the lowest frequency.
				// We can run out of partials among our subarrays but it should be impossible
				// to run out of partials in ALL of them.
				int best = 0;
				for(int j = 1; j < __pos.length; j++)
					{
					if (__pos[best] >= (__freqs[best].length - 1) ||				// We're out of slots for best
						(__pos[j] < (__freqs[j].length - 1) &&						// We still have slots for j
						__freqs[j][__pos[j]] < __freqs[best][__pos[best]]))
						{
						best = j;
						}
					}	
				
				// load it
				frequencies[i] = __freqs[best][_pos[best]];	
				amplitudes[i] = _amps[best][_pos[best]];
				_pos[best]++;
				}

            if (constrain())
	            simpleSort(0, false);                      // we must always sort
            }
        }       
    }

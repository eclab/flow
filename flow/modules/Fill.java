// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which takes two incoming sources, A and B, and produces a mixed source by including all
   non-zero amplitude partials from A, and then replacing the zero-amplitude partials with the
   lowest non-zero partials from B.
*/

public class Fill extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int UNIT_INPUT_A = 0;
    public static final int UNIT_INPUT_B = 1;

    public static final int MOD_SCALE_A = 0;
    public static final int MOD_SCALE_B = 1;

    // Mixes the harmonics of up to four units, using the frequencies of the first unit
        
    public Fill(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B" });
        defineModulations(new Constant[] { Constant.ONE, Constant.ONE }, new String[] { "Scale A", "Scale B" });
        }
        
    public void go()
        {
        super.go();
        
        copyFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);         // INPUT_A was copied over
        double[] frequencies = getFrequencies(0);               // INPUT_A was copied over
        double[] amp2 = getAmplitudesIn(UNIT_INPUT_B);
        double[] freq2 = getFrequenciesIn(UNIT_INPUT_B);
        double scaleA = modulate(MOD_SCALE_A);
        double scaleB = modulate(MOD_SCALE_B);
                
                    
        int i2 = 0;            
        for(int i = 0; i < amplitudes.length; i++)
            {
            if (amplitudes[i] == 0)  // fill it
                {
                for( ; i2 < amplitudes.length; i2++)
                    {
                    if (amp2[i2] != 0)
                        {
                        amplitudes[i] = amp2[i2] * scaleB;
                        frequencies[i] = freq2[i2];
                        break;
                        }
                    }
                i2++;
                }
            else
                {
                amplitudes[i] *= scaleA;
                }
            }
        
        simpleSort(0, false);            
        }
        
    public boolean isConstrainable() { return false; }  
    }

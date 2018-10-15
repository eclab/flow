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

public class Shift extends Unit
    {
    private static final long serialVersionUID = 1;

    public static String getName() { return "Shift"; }

    double range;
    
    public static final int TYPE_PITCH = 0;
    public static final int TYPE_FREQUENCY = 1;
    public static final int TYPE_PARTIALS = 2;
    
    int type = TYPE_PITCH;
    public void setType(int val) { type = val; }
    public int getType() { return type; }

    public static final int OPTION_TYPE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

        
    public Shift(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.HALF, Constant.ONE }, new String[] { "Shift", "Bound" });
        defineOptions(new String[] { "Type" }, new String[][] { { "Pitch", "Frequency", "Partials" } } );
        }
        
    public static final double MAX_BOUND = 4.0;
        
    public void go()
        {
        super.go();
                
        copyFrequencies(0);
        pushAmplitudes(0);

        double[] frequencies = getFrequencies(0);
        
        double shift = modulate(0);
        double bound = modulate(1);
                
        switch(type)
            {
            case TYPE_PITCH:
                {
                // If it's not Math.pow, but hybridpow, we get weird jumps
                double multiplier = Math.pow(2, (shift - 0.5) * bound * MAX_BOUND);

//                for(int i = 0; i < frequencies.length; i++)
                //                  {
                //                frequencies[i] = frequencies[i] * multiplier;
                //              }
                frequencies[0] = frequencies[0] * multiplier;
                }
            break;
                        
            case TYPE_FREQUENCY:
                {
                double delta = modToSignedFrequency(shift) * bound / sound.getPitch();

                for(int i = 0; i < frequencies.length; i++)
                    {
                    frequencies[i] = frequencies[i] + delta;
                    if (frequencies[i] < 0) frequencies[i] = 0;
                    if (frequencies[i] > Output.SAMPLING_RATE / 2.0) frequencies[i] = Output.SAMPLING_RATE / 2.0;
                    }
                }
            break;
                        
            case TYPE_PARTIALS:
                {
                double[] frequenciesIn = getFrequenciesIn(0);
                shift = (shift - 0.5) * 2 * bound;
                                
                if (shift < 0)
                    {
                    shift = -shift;
                    for(int i = 1; i < frequencies.length; i++)
                        {
                        frequencies[i] = frequenciesIn[i] * (1.0 - shift) + frequenciesIn[i - 1] * shift;
                        }
                    }
                else if (shift > 0)
                    {
                    for(int i = 0; i < frequencies.length - 1; i++)
                        {
                        frequencies[i] = frequenciesIn[i] * (1.0 - shift) + frequenciesIn[i + 1] * shift;
                        }
                    }
                }
            break;
            }

        if (constrain()) 
            {
            simpleSort(0, true);
            }
        }       

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation == 0 && isConstant)
            return String.format("%.4f", 2 * (value - 0.5));
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }

    }

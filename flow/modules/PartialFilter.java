// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import javax.swing.*;
import java.awt.*;
import flow.gui.*;

/**
   A Unit which provides a filter based on the partials of a second incoming filter source.
   The filter source's partials amplitudes define the filter function, and values in-between the partial
   frequencies are filtered using a function which interpolates between the nearest two neighboring
   filter partials amplitudes.  Partials less than the minimum filter partial or greater than the maximum filter
   partial are simply filtered using those partials
   
   <p>The filter partials can be FIXED or not.  If the partials are NOT FIXED, then the frequencies they
   filter are the standard frequencies of the partials (mutiplied as usual against the current pitch).  Thus
   as the note changes so do the filter frequencies.  If the partials are FIXED, then their frequencies do not
   change with the pitch: specifically, a partial of partial-frequency 1.0 (typically the fundamental is this
   partial-frequency) is set to a filter frequency of 0Hz.  A partial of partial-frequency 2.0 is set to a 
   filter frequency of 100Hz.  A partial of partial-frequency 3.0 is set to a frequency of 200Hz, and so on,
   up to a partial-frequency of 128 (for example) being set to 12.7KHz.
   
   <p>For standardized partials, 12.6KHz is probably not enough, so there's an additiona option to DOUBLE those
   fixed frequencies.  Thus a partial of 1.0 is 0HZ, 2.0 is 200Hz, 3.0 is 400Hz, and so on, up to 127 being
   25.4Hz, which is easily beyond Nyquist.
   
   <p>PartialFilter is particularly useful in combination with Draw for defining the filter.
*/


public class PartialFilter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int UNIT_INPUT = 0;
    public static final int UNIT_PARTIALS = 1;
        
    public static String getName() { return "Partial Filter"; }

    boolean fixed;
    public boolean isFixed() { return fixed; }
    public void setFixed(boolean val) { fixed = val; }

    boolean _double;
    public boolean getDouble() { return _double; }
    public void setDouble(boolean val) { _double = val; }

    public static final int OPTION_FIXED = 0;
    public static final int OPTION_DOUBLE = 1;
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FIXED: return isFixed() ? 1 : 0;
            case OPTION_DOUBLE: return getDouble() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FIXED: setFixed(value != 0); return;
            case OPTION_DOUBLE: setDouble(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public PartialFilter(Sound sound) 
        { 
        super(sound);
                
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "Input", "Partials" });
        defineOptions( new String[] { "Fixed", "Double" }, new String[][] { { "Fixed" }, { "Double" } });
        }
    
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(UNIT_INPUT);
        double[] frequencies = getFrequencies(UNIT_INPUT);
        double[] nodeFreq = getFrequenciesIn(UNIT_PARTIALS);
        double[] nodeGain = getAmplitudesIn(UNIT_PARTIALS);
        
        double pitch = sound.getPitch();
        double scale = (_double ? 200.0 : 100.0);               // each frequency integer represents 100HZ or 200Hz
        if (!fixed) { scale = 1.0; pitch = 1.0; }
        
        int node = 0;
        for(int i = 0; i < amplitudes.length; i++)
            {
            // First consider the situation where the frequency is lower than the minimum node
            if (node == 0 && frequencies[i] * pitch <= nodeFreq[0] * scale)
                {
                amplitudes[i] *= nodeGain[0];
                }
            else 
                {
                // Find the pair.  We do this by identifying the larger node which is >= the frequency in question
                while (node + 1 < (NUM_PARTIALS - 1) && frequencies[i] * pitch >= (nodeFreq[node + 1] - 1)  * scale)
                    {
                    node++;
                    }
                
                // next consider the situation where the frequency is higher than the maximum node
                if (node + 1 == (NUM_PARTIALS - 1) && frequencies[i] * pitch >= (nodeFreq[node + 1] - 1) * scale)
                    {
                    double d = nodeGain[node + 1];
                    for(int j = i; j < amplitudes.length; j++)
                        {
                        amplitudes[j] *= d;
                        }
                    break;  // all done
                    }
                
                // don't want to divide by zero...
                else if (nodeFreq[node] == nodeFreq[node + 1])
                    {
                    amplitudes[i] *= nodeGain[node];
                    }
                        
                // finally interpolate between the node and the next node
                else
                    {
                    double pos = (frequencies[i] * pitch - (nodeFreq[node] - 1) * scale) / ((nodeFreq[node + 1] - 1) * scale - (nodeFreq[node] - 1) * scale);
                    double gain = (1 - pos) * nodeGain[node] + pos * nodeGain[node + 1];
                    amplitudes[i] *= gain;
                    }
                }
            }

        constrain();
        }       
    }

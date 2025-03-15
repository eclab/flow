// Copyright 2021 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

import flow.*;

/**
   A resonant ladder 3-, 4-, 6-, or 8-pole low pass ladder filter, plus a non-resonant
   1-pole low pass filter.
*/

public class LadderFilter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_CUTOFF = 0;
    public static final int MOD_RESONANCE = 1;

    public static final double MIDDLE_C_FREQUENCY = 261.6256;    
    public static final double INVERSE_SQRT_2 = 1.0 / Math.sqrt(2.0);
    public static final double MINIMUM_FREQUENCY = 0.000001;  // seems reasonable
    
    public static final int TYPE_6DB_LP = 0;
    public static final int TYPE_18DB_LP = 1;
    public static final int TYPE_24DB_LP = 2;
    public static final int TYPE_36DB_LP = 3;
    public static final int TYPE_48DB_LP = 4;
    
    public LadderFilter(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] { "Poles" , "Relative", "Taper" }, new String[][] { { "1 (6dB)", "3 (18dB)", "4 (24dB)", "6 (36dB)", "8 (48dB)" } , {"Relative"}, {"Taper"}});
        defineModulations(new Constant[] { Constant.ONE, Constant.ZERO }, new String[] { "Cutoff", "Resonance" });
        }
        
    boolean taper = false;
    public boolean getTaper() { return taper; }
    public void setTaper(boolean val) { taper = val; }

    int type = TYPE_24DB_LP;
    public int getType() { return type; }
    public void setType(int val) { type = val; }
        
    boolean relative = false;
    public boolean getRelative() { return relative; }
    public void setRelative(boolean val) { relative = val; }
        
    public static final int OPTION_TYPE = 0;
    public static final int OPTION_RELATIVE = 1;
    public static final int OPTION_TAPER = 2;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            case OPTION_RELATIVE: return getRelative() ? 1 : 0;
            case OPTION_TAPER: return getTaper() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            case OPTION_RELATIVE: setRelative(value != 0); return;
            case OPTION_TAPER: setTaper(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
    
    public double filter(double frequency, double q, double cutoff, int type)
        {
        switch(type)
            {
            case TYPE_6DB_LP:
                {
                return filter6dB(frequency, cutoff);
                }
            case TYPE_18DB_LP:
                {
                return filter18dB(frequency, q, cutoff);
                }
            case TYPE_24DB_LP:
                {
                return filter24dB(frequency, q, cutoff);
                }
            case TYPE_36DB_LP:
                {
                return filter36dB(frequency, q, cutoff);
                }
            case TYPE_48DB_LP:              
                {
                return filter48dB(frequency, q, cutoff);
                }
            default:
                {
                System.err.println("Internal Error (LadderFilter.filter), got " + type);
                return 0;
                }
            }
        } 
        
    public double filter6dB(double frequency, double cutoff)
        {
        double d = Math.sqrt((frequency * frequency) / (cutoff * cutoff) + 1.0);
        return 1.0 / d;
        }
    
    /*
      public double filter12dB(double frequency, double q, double cutoff)
      {
      q *= 16.0;      // 8.0 is the maximum 12dB resonance before the filter goes unstable
      double swo = frequency / cutoff;
      double swo2 = swo * swo;
      double real = 1 - swo2 + q;
      double imag = 2 * swo;
        
      return 1.0 / Math.sqrt(real * real + imag * imag);
      }
    */

    public static final double MAX_18_RESONANCE = 8.0;      // more or less?  
    public double filter18dB(double frequency, double q, double cutoff)
        {
        q *= MAX_18_RESONANCE;
        double swo = frequency / cutoff;
        double swo2 = swo * swo;
        double real = 1 - 3 * swo2 + q;
        double imag = 3 * swo - swo2 * swo;
        
        return 1.0 / Math.sqrt(real * real + imag * imag);
        }

    public static final double MAX_24_RESONANCE = 4.0;      // exactly
    public double filter24dB(double frequency, double q, double cutoff)
        {
        q *= MAX_24_RESONANCE;  // 4.0 is the maximum 24dB resonance before the filter goes unstable
        double swo = frequency / cutoff;
        double swo2 = swo * swo;
        double real = 1 - 6 * swo2 + swo2 * swo2 + q;
        double imag = 4 * swo - 4 * swo2 * swo;
        
        return 1.0 / Math.sqrt(real * real + imag * imag);
        }

    public static final double MAX_36_RESONANCE = 4.0 * 0.589;      // or so
    public double filter36dB(double frequency, double q, double cutoff)
        {
        q *= MAX_36_RESONANCE;  // 2.0 is the maximum 36dB resonance before the filter goes unstable
        double swo = frequency / cutoff;
        double swo2 = swo * swo;
        double swo4 = swo2 * swo2;
        double real = 1 - 15 * swo2 + 15 * swo4 - swo4 * swo2 + q;
        double imag = 6 * swo - 20 * swo2 * swo + 6 * swo4 * swo;
        
        return 1.0 / Math.sqrt(real * real + imag * imag);
        }

    public static final double MAX_48_RESONANCE = 4.0 * 0.471;      // or so
    public double filter48dB(double frequency, double q, double cutoff)
        {
        q *= MAX_48_RESONANCE;
        double swo = frequency / cutoff;
        double swo2 = swo * swo;
        double swo4 = swo2 * swo2;
        double real = 1 - 28 * swo2 + 70 * swo4 - 28 * swo2 * swo4 + swo4 * swo4 + q;
        double imag = 8 * swo - 56 * swo2 * swo + 56 * swo4 * swo - 8 * swo4 * swo2 * swo;
        
        return 1.0 / Math.sqrt(real * real + imag * imag);
        }

       
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);
                        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        double pitch = sound.getPitch();
                
        double cutoff = modToFrequency(makeVeryInsensitive(modulate(MOD_CUTOFF))); // Note that this is in angular frequency, but we don't divide by 2 PI to get Hertz because that's done at the end of the day when we add up the sine waves
        if (cutoff < MINIMUM_FREQUENCY) cutoff = MINIMUM_FREQUENCY;  // so we're never 0
        
        if (relative)
            {
            cutoff = cutoff / MIDDLE_C_FREQUENCY * pitch;
            }
            
        double taperVal = 0.0;                
        double resonance = modulate(MOD_RESONANCE);
        int type = getType();
        
        if (getTaper())
            {
            for(int i = 0; i < amplitudes.length; i++)
                {
                // taper to Nyquist with an N^2 cutdown function
                if (frequencies[i] * pitch > cutoff)
                    {
                    taperVal = (frequencies[i] * pitch - cutoff) / (Output.NYQUIST - cutoff);
                    taperVal = 1.0 - (taperVal * taperVal);
                    if (taperVal < 0) taperVal = 0;
                    }
                else taperVal = 1.0;

                amplitudes[i] = taperVal * amplitudes[i] * filter(frequencies[i] * pitch, resonance, cutoff, type);
                }
            }
        else
            {
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = amplitudes[i] * filter(frequencies[i] * pitch, resonance, cutoff, type);
                }
            }

        constrain();
        boundAmplitudes();			// we can make the amplitudes go high in rare situations, so we need to bound them
        }       


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_CUTOFF)
                {
                return String.format("%.4f", modToFrequency(makeVeryInsensitive(value)));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    public static String getName() { return "Ladder Filter"; }
    }

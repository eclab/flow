// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

import flow.*;

/**
   A state-variable, resonant, 2- 8- or 
*/

public class Filter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_CUTOFF = 0;
    public static final int MOD_STATE = 1;
    public static final int MOD_RESONANCE = 2;

    public static final int TYPE_LP = 0;
    public static final int TYPE_HP = 1;
    public static final int TYPE_BP = 2;
    public static final int TYPE_NOTCH = 3;

    public static final double MIDDLE_C_FREQUENCY = 261.6256;    
    public static final double INVERSE_SQRT_2 = 1.0 / Math.sqrt(2.0);
    public static final double MINIMUM_FREQUENCY = 0.000001;  // seems reasonable
    
    public Filter(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] { "4-Pole" , "Relative" }, new String[][] { { "4-Pole"} , {"Relative"}});
        defineModulations(new Constant[] { Constant.ONE, Constant.HALF, Constant.ZERO }, new String[] { "Cutoff", "State", "Resonance" });
        }

    boolean pole4 = false;
    public boolean get4Pole() { return pole4; }
    public void set4Pole(boolean val) { pole4 = val; }
        
    boolean relative = false;
    public boolean getRelative() { return relative; }
    public void setRelative(boolean val) { relative = val; }
        
    public static final int OPTION_4POLE = 0;
    public static final int OPTION_RELATIVE = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_4POLE: return get4Pole() ? 1 : 0;
            case OPTION_RELATIVE: return getRelative() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_4POLE: set4Pole(value != 0); return;
            case OPTION_RELATIVE: setRelative(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
    
    public double filter(double state, double frequency, double q, double cutoff, boolean pole4)
        {
        if (pole4)
            {
            return numerator(state, frequency, q, cutoff) / denominator(frequency, q, cutoff);
            }
        else
            {
            return Math.sqrt(numerator(state, frequency, q, cutoff) / denominator(frequency, q, cutoff));
            }
        /*
          double pole2 = Math.sqrt(numerator(state, frequency, q, cutoff) / denominator(frequency, q, cutoff));
          if (pole4) return pole2 * pole2;
          else return pole2;
        */
        } 
        
    public double denominator(double frequency, double q, double cutoff)
        {
        double ff = frequency * frequency;
        double cc = cutoff * cutoff;
        double a = 1.0 - ff / cc;
        double b = frequency / (cutoff * q);
        return a * a + b * b;
        }
    
    public double lp(double frequency, double q, double cutoff) { return 1.0; }
    public double hp(double frequency, double q, double cutoff) { double ff = (frequency * frequency); double cc = (cutoff * cutoff); double dd = ff/cc;  return dd * dd;}
    public double bp(double frequency, double q, double cutoff) { double d = frequency / (cutoff * q); return d * d; }
    public double notch(double frequency, double q, double cutoff) { double ff = (frequency * frequency); double cc = (cutoff * cutoff); double dd = (1 - ff / cc); return dd * dd; }
    
    public double numerator(double state, double frequency, double q, double cutoff)
        {
        double alpha = 0.0;

        if (state == 0.50 || state == 0.0 || state == 1.0 || state == 0.25 || state == 0.75)
            {
            if (state == 0.50)
                {
                return lp(frequency, q, cutoff);
                }
            else if (state == 0.0 || state == 1.0)
                {
                return hp(frequency, q, cutoff);
                }
            else if (state == 0.25)
                {
                return notch(frequency, q, cutoff);
                }
            else if (state == 0.75)
                {
                return bp(frequency, q, cutoff);
                }
            else
                return 0.0;  // never happens
            }
        else if (state < 0.5)
            {
            if (state < 0.25)
                {
                //HP <-> Notch
                alpha = state * 4;
                return alpha * notch(frequency, q, cutoff) +
                    (1 - alpha) * hp(frequency, q, cutoff);
                }
            else
                {
                //Notch <-> LP
                alpha = (state - 0.25) * 4;
                return alpha * lp(frequency, q, cutoff) +
                    (1 - alpha) * notch(frequency, q, cutoff);
                }
            }
        else if (state < 0.75)
            {
            //LP <-> BP
            alpha = (state - 0.5) * 4;
            return alpha * bp(frequency, q, cutoff) +
                (1 - alpha) * lp(frequency, q, cutoff);
            }
        else
            {
            //BP <-> HP
            alpha = (state - 0.75) * 4;
            return alpha * hp(frequency, q, cutoff) +
                (1 - alpha) * bp(frequency, q, cutoff);
            }   
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
                
        double state = modulate(MOD_STATE);     
        double resonance = INVERSE_SQRT_2 * Utility.fastpow(10, modulate(MOD_RESONANCE));
        boolean pole4 = get4Pole();
        
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = amplitudes[i] * filter(state, frequencies[i] * pitch, resonance, cutoff, pole4);
            }

        constrain();
        }       


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_CUTOFF)
                {
                return String.format("%.4f", modToFrequency(makeVeryInsensitive(value)));
                }
            else if (modulation == MOD_STATE)
                {
                double alpha = 0.0;
                if (value == 0.50)
                    {
                    return "LP";
                    }
                else if (value == 0.0 || value == 1.0)
                    {
                    return "HP";
                    }
                else if (value == 0.25)
                    {
                    return "Notch";
                    }
                else if (value == 0.75)
                    {
                    return "BP";
                    }
                else if (value < 0.5)
                    {
                    if (value < 0.25)
                        {
                        //HP <-> Notch
                        alpha = value * 4;
                        return "HP<" + String.format("%.2f", alpha) + ">N";
                        }
                    else
                        {
                        //Notch <-> LP
                        alpha = (value - 0.25) * 4;
                        return "N<" + String.format("%.2f", alpha) + ">LP";
                        }
                    }
                else if (value < 0.75)
                    {
                    //LP <-> BP
                    alpha = (value - 0.5) * 4;
                    return "LP<" + String.format("%.2f", alpha) + ">BP";
                    }
                else
                    {
                    //BP <-> HP
                    alpha = (value - 0.75) * 4;
                    return "BP<" + String.format("%.2f", alpha) + ">HP";
                    }                   
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    public static final String[] OPTIONS = new String[] { "HP", "Notch", "LP", "BP" };
    public static final double[] CONVERSIONS = new double[] { 0, 0.25, 0.5, 0.75 };
        
    public ModulePanel getPanel()
        {
        return new ModulePanel(Filter.this)
            {
            public JComponent buildPanel()
                {             
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                box.add(new UnitInput(unit, 0, this));

                for(int i = 0; i < unit.getNumModulations(); i++)
                    {
                    if (i == MOD_STATE)
                        box.add(new ModulationInput(unit, i, this)
                            {
                            public String[] getOptions() { return OPTIONS; }
                            public double convert(int elt) { return CONVERSIONS[elt]; }
                            });
                    else
                        box.add(new ModulationInput(unit, i, this));
                    }
                        
                for(int i = 0; i < unit.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(unit, i));
                    }
                        
                box.add(new ConstraintsChooser(unit, this));

                return box;
                }
            };
        }

    }

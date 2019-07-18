// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

public class Choice extends Unit
    {       
    private static final long serialVersionUID = 1;
        
    public static final int UNIT_TARGET = 0;
    public static final int MOD_TARGET = 0;
    public static final int MOD_OPTION = 1;
    public static final int MOD_VALUE = 2;

    Modulation lastMod = null;
    double lastOption = -1;
    double lastValue = -1;
    String lastOptionName = "";
    String lastValueName = "";

    public Choice(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO }, new String[] { "Target", "Option", "Value" });
        defineInputs(new Unit[] { Unit.NIL }, new String[] { "Target" });
        defineOutputs(new String[] { });
        }


    public void redraw(final ModulationInput m)
        {
        SwingUtilities.invokeLater(new Runnable() { public void run() { m.updateText(); m.repaint(); } });
        }

    public void go()
        {
        super.go();
        
        //First we determine what our modulation target is.  Is it the modulation or the unit?
        Modulation mod = null;
        Unit unit = getInput(UNIT_TARGET);
        if (unit instanceof Nil)
            {
            mod = getModulation(MOD_TARGET);
                {
                if (mod instanceof Constant)
                    {
                    if (lastMod != null)
                        {
                        lastOption = -1;
                        lastValue = -1;
                        lastOptionName = "";
                        lastValueName = "";
                        if (modInput1 != null)
                            redraw(modInput1);
                        if (modInput2 != null)
                            redraw(modInput2);
                        }
                                
                    lastMod = null;
                    return;         // Nothing plugged in to either one
                    }
                }
            }
        else
            {
            mod = unit;
            }
        
        // Get our option and value choices
        double option = modulate(MOD_OPTION);
        double value = modulate(MOD_VALUE);
        
        if (option == lastOption && value == lastValue && mod == lastMod)  // no change
            return;
                
        int numOptions = mod.getNumOptions();
        if (numOptions == 0)
            {
            lastOption = -1;
            lastValue = -1;
            lastOptionName = "";
            lastValueName = "";
            if (modInput1 != null)
                redraw(modInput1);
            if (modInput2 != null)
                redraw(modInput2);
            return;
            }
        
        // If we have changed the modulation target or the option, we need to redo everything
        if (lastMod != mod || lastOption != option)
            {
            if (getModulation(MOD_OPTION) instanceof Constant)
                {
                if (modInput1 != null)
                    redraw(modInput1);
                if (modInput2 != null)
                    redraw(modInput2);
                }
                                
            int newOption = (int)(numOptions * option);
            if (newOption >= numOptions) 
                {
                newOption = numOptions - 1;
                }
                        
            // If the value modulation is a constant, we update it to reflect the current
            // option's value.  Otherwise we update the option's value to reflect the modulation
                        
            int newValue = 0;
            String[] valueNames = mod.getOptionValues(newOption);
            Modulation vmod = getModulation(MOD_VALUE);
            if (vmod instanceof Constant)
                {
                newValue = mod.getOptionValue(newOption);
                ((Constant)vmod).setValue(newValue / (double)valueNames.length);
                                        
                // FIXME: maybe need to repaint here?
                }
            else
                {
                if (valueNames.length <= 1)  // boolean
                    {
                    newValue = (value < 0.5 ? 0 : 1);
                    }
                else
                    {
                    newValue = (int)(valueNames.length * value);
                    if (newValue >= valueNames.length) 
                        {
                        newValue = valueNames.length - 1;
                        }
                    }
                }
            lastOptionName = getOptionName(mod, newOption);
            lastValueName = getValueName(mod, newOption, newValue);
            mod.setOptionValue(newOption, newValue);
            }
        else  // just the value changed.  So we just redo it.
            {
            int newOption = (int)(numOptions * lastOption);
            if (newOption >= numOptions) 
                {
                newOption = numOptions - 1;
                }
                                
            int newValue = 0;
            String[] valueNames = mod.getOptionValues(newOption);
            if (valueNames.length <= 1)  // boolean
                {
                newValue = (value < 0.5 ? 0 : 1);
                }
            else
                {
                newValue = (int)(valueNames.length * value);
                if (newValue >= valueNames.length) 
                    {
                    newValue = valueNames.length - 1;
                    }
                }
                                        
            lastOptionName = getOptionName(mod, newOption);
            lastValueName = getValueName(mod, newOption, newValue);
            mod.setOptionValue(newOption, newValue);
            }
        
        lastMod = mod;
        lastOption = option;
        lastValue = value;
        }

    String getOptionName(Modulation mod, int option)
        {
        if (mod != null)
            {
            int num = mod.getNumOptions();
            if (num > 0)
                {
                if (option >= 0 && option < num)
                    {
                    return mod.getOptionName(option);
                    }
                }
            }
                
        return "";
        }

    String getValueName(Modulation mod, int option, int value)
        {
        if (mod != null)
            {
            int num = mod.getNumOptions();
            if (num > 0)
                {
                if (option >= 0 && option < num)
                    {
                    String[] vals = mod.getOptionValues(option);
                    if (vals.length <= 1)
                        {
                        return (value == 0 ? "Off" : "On");
                        }
                    else if (value >= 0 && value < vals.length)
                        {
                        return vals[value];
                        }
                    }
                }
            }
                
        return "";
        }

    public boolean isConstrainable() { return false; }
        
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_OPTION)
                {
                return lastOptionName;
                }
            else if (modulation == MOD_VALUE)
                {
                return lastValueName;
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    ModulationInput modInput1 = null;
    ModulationInput modInput2 = null;
        
    public ModulePanel getPanel()
        {
        return new ModulePanel(Choice.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitInput(unit, 0, this));
                box.add(new ModulationInput(unit, 0, this));
                box.add(modInput1 = new ModulationInput(unit, 1, this));
                box.add(modInput2 = new ModulationInput(unit, 2, this));
                return box;
                }
            };
        }


    }

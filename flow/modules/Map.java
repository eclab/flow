// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;

/**  
     A Modulation which takes two BOUNDS, called A and B, and maps its incoming signal
     according to them.  If INVERT is FALSE, then the signal is mapped so that 0.0 
     maps to A and 1.0 maps to B, with values in-between mapped in an interpolated fashion
     accordingly.  If INVERT if TRUE, the inverted mapping occurs: values &leq; A are mapped  
     to 0.0, values &geq; B are mapped to 1.0, and values in-between are mapped in an
     interpolated fashion.  If FLIP is true, then the signal is flipped before mapping
     occurs: that is, it is set to 1.0 minus the original signal.
*/
        

public class Map extends Modulation
    {
    private static final long serialVersionUID = 1;

    boolean invert;
    public boolean getInvert() { return invert; }
    public void setInvert(boolean val) { invert = val; }
        
    boolean flip;
    public boolean getFlip() { return flip; }
    public void setFlip(boolean val) { flip = val; }
        
    boolean center;
    public boolean getCenter() { return center; }
    public void setCenter(boolean val) { center = val; }
        
    public static final int OPTION_INVERT = 0;
    public static final int OPTION_FLIP = 1;
    public static final int OPTION_CENTER = 2;
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_INVERT: return getInvert() ? 1 : 0;
            case OPTION_FLIP: return getFlip() ? 1 : 0;
            case OPTION_CENTER: return getCenter() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_INVERT: setInvert(value != 0); return;
            case OPTION_FLIP: setFlip(value != 0); return;
            case OPTION_CENTER: setCenter(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public Map(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ONE }, new String[] { "Signal", "Bound A", "Bound B" });
        defineOptions(new String[] { "Inverse", "Flip", "Center" }, new String[][] { { "Inverse" }, { "Flip" }, { "Center" } } );
        }

    public void go()
        {
        super.go();
        
        setTriggerValues(isTriggered(0), getTrigger(0), 0);

        double mod0 = modulate(0);
        double mod1 = modulate(1);
        double mod2 = modulate(2);
        
        if (center)
            {
            mod2 /= 2.0;
            double m1 = mod1 - mod2;
            double m2 = mod1 + mod2;
            mod1 = (m1 < 0 ? 0 : m1);
            mod2 = (m2 > 1 ? 1 : m2);
            }
        else if (mod1 > mod2)                // swap
            {
            double temp = mod2;
            mod2 = mod1;
            mod1 = temp;
            }
                
        if (invert)
            {
            if (mod1 == mod2)
                setModulationOutput(0, mod1);
            else
                {
                double d = (mod0 - mod1) / (mod2 - mod1);
                if (d < 0) d = 0;
                if (d > 1) d = 1;
                                
                if (flip)
                    {
                    d = 1.0 - d;
                    }

                setModulationOutput(0, d);
                }
            }
        else
            {
            if (flip)
                {
                mod0 = 1.0 - mod0;
                }
                        
            setModulationOutput(0, mod1 + (mod2 - mod1) * mod0);
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 2)  // Bound B
                {
                if (center)  // we're doing var
                    return String.format("%.4f", value / 2.0);
                }
            }
        return super.getModulationValueDescription(modulation, value, isConstant);
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Map.this)
            {
            public JComponent buildPanel()
                { 
                JLabel example = new JLabel("Bound B");     
                example.setFont(Style.SMALL_FONT());
                Box box = new Box(BoxLayout.Y_AXIS);
                Modulation mod = (Modulation) getModulation();
                box.add(new ModulationOutput(mod, 0, this));
                             
                box.add(new ModulationInput(mod, 0, this));     // signal
                ModulationInput a = new ModulationInput(mod, 1, this); // a
                ModulationInput b = new ModulationInput(mod, 2, this); // b
                a.setMinimumSize(example.getMinimumSize());
                b.setMinimumSize(example.getMinimumSize());
                box.add(a);
                box.add(b);
                                
                box.add(new OptionsChooser(mod, 0));
                box.add(new OptionsChooser(mod, 1));
                box.add(new OptionsChooser(mod, 2)
                    {
                    public void optionChanged(int optionNumber, int value)
                        {
                        super.optionChanged(optionNumber, value);
                        if (value == 0)
                            {
                            a.setTitleText("Bound A");
                            b.setTitleText("Bound B");
                            b.updateText();  // redisplay the value
                            }
                        else
                            {
                            a.setTitleText("Center");
                            b.setTitleText("Var");
                            b.updateText();  // redisplay the value
                            } 
                        }
                    });
                    
                return box;
                }
            };
        }


    }

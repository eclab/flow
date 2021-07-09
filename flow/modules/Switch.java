// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;


/**
   A Unit which selects between up to 4 incoming Unit signals.
   
   <p>The easiest situation is two Units, let's call them A and B.  If the 
   modulation is 0.0, then the output is just A.  As the moduation increases,
   both the amplitude and the frequency start changing until ultimately, at 
   1.0, the output is the same as B.  In-between, it's an interpolation between
   the two.
   
   <p>You select the new sound to play by sending a trigger the corresponding modulation input.  
   Select will then interpolate the frequencies and amplitudes between the old sound and the 
   newly chosen sound until old sound is entirely faded out.  You can use this in combination 
   with the individual modulation outputs of an envelope to select one sound, then another, 
   then a final sound, for example.

   <p>Select has an output trigger: this will send a trigger when its sound has been chosen, 
   and will also output the current interpolation value of the sound.

   <p>The first selected sound is always A.

   <p>You can also specify how rapidly the switch occurs (via Rate).   And you can choose whether 
   Switch is FREE, that is, whether its initial selected sound is reset to A on gate or not.
   
*/

public class Switch extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_INPUTS = 8;
    public static final int MOD_NEXT = NUM_INPUTS;
    public static final int MOD_ALPHA = MOD_NEXT + 1;
        
    boolean free = false;
       
    public void setFree(boolean val) { free = val; }
    public boolean getFree() { return free; }
        
    public static final int OPTION_FREE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return getFree() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public Switch(Sound sound)
        {
        super(sound);
        
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B", "Input C", "Input D", "Input E", "Input F", "Input G", "Input H"  });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.HALF }, new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "Next", "Rate" });
        defineOptions(new String[] { "Free" }, new String[][] { { "Frequency" }, { "Amplitude" }, { "Free" } });
        }

    public void reset()
        {
        super.reset();
        currentInput = 0;
        lastInput = 0;
        alpha = 0.0;
        }

    public void gate()
        {
        super.gate();

        if (!free)
            {
            currentInput = 0;
            lastInput = 0;
            alpha = 0.0;
            }
        }

    int currentInput = 0;
    int lastInput = 0;
    double alpha = 0.0;
    public static final double MINIMUM_ALPHA = 0.0001;      // slightly less than 1/8192, for 16-bit
        
    public void go()
        {
        super.go();
        
        if (isTriggered(MOD_NEXT))
            {
            lastInput = currentInput;
            // look for the next non-NIL input
            while(true)
            	{
				currentInput = currentInput + 1;
				if (currentInput >= NUM_INPUTS)
					currentInput = 0;
				if (getInput(currentInput) != Unit.NIL)
					break;
				else if (currentInput == lastInput)
					break;
				}
            alpha = 1.0;
            }
        else
            {
            for(int i = 0; i < getNumModulations() - 1; i++)                        // No
                {
                if (i != currentInput && isTriggered(i))
                    {
                    lastInput = currentInput;
                    currentInput = i;
                    alpha = 1.0;
                    break;
                    }
                }
            }

        double[] p1frequencies = getFrequenciesIn(lastInput);
        double[] p1amplitudes = getAmplitudesIn(lastInput);

        double[] p2frequencies = getFrequenciesIn(currentInput);
        double[] p2amplitudes = getAmplitudesIn(currentInput);
        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        
        for(int i = 0; i < p1frequencies.length; i++)
            {
            frequencies[i] = (p1frequencies[i] * alpha) + (p2frequencies[i] * (1.0 - alpha));
            }

        for(int i = 0; i < p1amplitudes.length; i++)
            {
            amplitudes[i] = (p1amplitudes[i] * alpha) + (p2amplitudes[i] * (1.0 - alpha));
            }

        // always sort    
        simpleSort(0, false);
        
        if (alpha >= MINIMUM_ALPHA)
            alpha = alpha * makeVeryInsensitive(modulate(MOD_ALPHA));
        else
            alpha = 0.0;
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Switch.this)
            {
            public JComponent buildPanel()
                {
                Unit unit =  (Unit) getModulation();
    
                Box outer = new Box(BoxLayout.Y_AXIS);
                outer.add(new UnitOutput(unit, 0, this));
                 
                Box box = new Box(BoxLayout.X_AXIS);
                
                Box box2 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_INPUTS; i++)
                    {
                    box2.add(new UnitInput(unit, i, this));
                    }
                box.add(box2);

                box.add(Strut.makeHorizontalStrut(5));

                box2 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_INPUTS; i++)
                    {
                    ModulationInput m = new ModulationInput(unit, i, this);
                    m.setTitleText("", false);
                    box2.add(m);
                    }
                
                box.add(box2);

                outer.add(box);
                                
                outer.add(new ModulationInput(unit, MOD_NEXT, this));
                outer.add(new ModulationInput(unit, MOD_ALPHA, this));
                
                for(int i = 0; i < unit.getNumOptions(); i++)
                    {
                    outer.add(new OptionsChooser(unit, i));
                    }

                return outer;
                }
            };
        }
        
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation < NUM_INPUTS)
                {
                return "";      // So Trigger modulation values aren't displayed
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    }

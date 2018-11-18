// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
   
   <P>To make pitch shifting a bit easier, if you doiuble-click on a Shift dial,
   a keyboard will pop up which provides translation equivalents (when Bound is 1.0): 
   Middle C is equivalent to no shift.
*/

public class Shift extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_SHIFT = 0;
    public static final int MOD_BOUND = 1;

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
        
    public static final double MAX_PITCH_BOUND = 12.0;
    public static final double MAX_PARTIALS_BOUND = 128.0;
        
    public void go()
        {
        super.go();
                
        double shift = modulate(MOD_SHIFT);
        double bound = modulate(MOD_BOUND);
                
        switch(type)
            {
            case TYPE_PITCH:
                {
				copyFrequencies(0);
				pushAmplitudes(0);

				double[] frequencies = getFrequencies(0);
        
                // If it's not Math.pow, but hybridpow, we get weird jumps
                double multiplier = Math.pow(2, (shift - 0.5) * bound * MAX_PITCH_BOUND);

                for(int i = 0; i < frequencies.length; i++)
                    {
                    frequencies[i] = frequencies[i] * multiplier;
                    }
                }
            break;
                        
            case TYPE_FREQUENCY:
                {
				copyFrequencies(0);
				pushAmplitudes(0);

				double[] frequencies = getFrequencies(0);
        
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
				pushFrequencies(0);
				copyAmplitudes(0);

				double[] amplitudes = getAmplitudes(0);
        
                int delta = (int)((shift - 0.5) * 2 * bound * MAX_PARTIALS_BOUND);
                
                if (delta > 0)
                	{
					for(int i = amplitudes.length - 1; i >= 0; i--)
						{
						int j = i - delta;
						if (j < 0) amplitudes[i] = 0;
						else amplitudes[i] = amplitudes[j];
						}
                	}
                else if (delta < 0)
                	{
					for(int i = 0; i < amplitudes.length; i++)
						{
						int j = i - delta;
						if (j >= amplitudes.length) amplitudes[i] = 0;
						else amplitudes[i] = amplitudes[j];
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
        
    public ModulePanel getPanel()
        {
        ///.... all this work, just to add a keyboard to the popup menu ...
        return new ModulePanel(Shift.this)
            {
            public JComponent buildPanel()
                {
                Unit unit = (Unit) getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new UnitOutput(unit, 0, this));
                box.add(new UnitInput(unit, 0, this));

                final ModulationInput[] m = new ModulationInput[1];
                m[0] = new ModulationInput(unit, MOD_SHIFT, this)
                    {
                    public JPopupMenu getPopupMenu()
                        {
                        final JPopupMenu pop = new JPopupMenu();
                        KeyDisplay display = new KeyDisplay(null, Color.RED, 36, 84, 60, 0)
                            {
                            public void setState(int state)
                                {
                                pop.setVisible(false);
                                m[0].setState(Seq.PITCHES[state - 60 + 24]);
                                }
                            };
                        pop.add(display);

                        String[] options = getOptions();
                        for(int i = 0; i < options.length; i++)
                            {
                            JMenuItem menu = new JMenuItem(options[i]);
                            menu.setFont(Style.SMALL_FONT());
                            final int _i = i;
                            menu.addActionListener(new ActionListener()
                                {
                                public void actionPerformed(ActionEvent e)      
                                    {
                                    double val = convert(_i);
                                    if (val >= 0 && val <= 1)
                                        setState(val);
                                    }       
                                });     
                            pop.add(menu);
                            }    

                        return pop;
                        }
                    };
                box.add(m[0]);
                box.add(new ModulationInput(unit, MOD_BOUND, this));
                                
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

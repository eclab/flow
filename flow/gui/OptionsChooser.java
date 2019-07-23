// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;

/** 
    A widget which displays a labelled options choice.  If the options
    is boolean, then a checkbox is displayed.  Otherwise a JComboBox is displayed.
*/


public class OptionsChooser extends JPanel implements Rebuildable
    {
    JComboBox combo;
    JCheckBox checkbox;
    JLabel label;
    Modulation modulation;
    int optionNumber;
    boolean rebuilding;
    
    /** Returns the combo box, if there is any. */
    public JComboBox getComboBox() { return combo; }

    /** Returns the checkbox, if there is any. */
    public JCheckBox getCheckBox() { return checkbox; }
    
    public void rebuild()
        {
        if (modulation.getOptionValues(optionNumber).length == 1)
            {
            rebuilding = true;
            checkbox.setSelected(modulation.getOptionValue(this.optionNumber) == 1);
            rebuilding = false;
            }
        else
            {
            rebuilding = true;
            combo.setSelectedIndex(modulation.getOptionValue(this.optionNumber));
            rebuilding = false;
            }
        }
    
    /** A hook which is called when an option has been modified to a given value. */
    public void optionChanged(int optionNumber, int value) { }
    
    public OptionsChooser(Modulation mod, final int optionNumber)
        {
        this.modulation = mod;
        this.optionNumber = optionNumber;
        
        if (mod.getOptionValues(optionNumber) == null || mod.getOptionValues(optionNumber).length < 2)
            {
            checkbox = new JCheckBox(mod.getOptionName(optionNumber));
            checkbox.setFont(Style.SMALL_FONT());
            checkbox.putClientProperty("JComponent.sizeVariant", "small");
            checkbox.setSelected(modulation.getOptionValue(this.optionNumber) == 1);
            checkbox.addItemListener(new ItemListener()         // so setSelected also affects us
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    if (rebuilding) return;  // break recursion
                    
                    // distribute to all sounds
                    int index = modulation.getSound().findRegistered(modulation);
                    if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                        {
                        return;
                        }

                    Output output = modulation.getSound().getOutput();
                    output.lock();
                    try
                        {
                        int numSounds = output.getNumSounds();
                        for(int i = 0; i < numSounds; i++)
                            {
                            Sound s = output.getSound(i);
                            if (s.getGroup() == Output.PRIMARY_GROUP)
                                {
                                s.getRegistered(index).setOptionValue(optionNumber, (checkbox.isSelected() ? 1 : 0));
                                }
                            }
                        }
                    finally 
                        {
                        output.unlock();
                        }
                    optionChanged(optionNumber, (checkbox.isSelected() ? 1 : 0));
                    }
                });
            setLayout(new BorderLayout());
            add(checkbox, BorderLayout.CENTER);
            String[] help = mod.wrapHelp(mod.getOptionHelp());
            if (help != null && help.length > optionNumber && help[optionNumber] != null)
                checkbox.setToolTipText(help[optionNumber]);
            }
        else
            {
            label = new JLabel(mod.getOptionName(optionNumber));
            label.setFont(Style.SMALL_FONT());

            combo = new JComboBox(mod.getOptionValues(optionNumber))
                {
                public Dimension getMinimumSize() 
                    {
                    return getPreferredSize(); 
                    }
                };

            combo.putClientProperty("JComponent.sizeVariant", "small");
            combo.setEditable(false);
            combo.setFont(Style.SMALL_FONT());
            combo.setMaximumRowCount(32);

            combo.setSelectedIndex(modulation.getOptionValue(this.optionNumber));

            /// Apparent OS X Java bug: sometimes after you programmatically change
            /// the value of a JComboBox, it no longer sends ActionListener events.  :-(   
            combo.addItemListener(new ItemListener()
                {
                public void itemStateChanged(ItemEvent e)
                    {
                    int val = combo.getSelectedIndex();
                    // distribute to all sounds
                    int index = modulation.getSound().findRegistered(modulation);
                    if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                        {
                        return;
                        }

                    Output output = modulation.getSound().getOutput();
                    output.lock();
                    try
                        {
                        int numSounds = output.getNumSounds();
                        for(int i = 0; i < numSounds; i++)
                            {
                            Sound s = output.getSound(i);
                            if (s.getGroup() == Output.PRIMARY_GROUP)
                                {
                                s.getRegistered(index).setOptionValue(optionNumber, val);
                                }
                            }
                        }
                    finally 
                        {
                        output.unlock();
                        }
                        
                    optionChanged(optionNumber, val);
                    }
                });
                        
            setLayout(new BorderLayout());
            add(combo, BorderLayout.CENTER);
            add(label, BorderLayout.NORTH);
            String[] help = mod.wrapHelp(mod.getOptionHelp());
            if (help != null && help.length > optionNumber && help[optionNumber] != null)
                {
                combo.setToolTipText(help[optionNumber]);
                label.setToolTipText(help[optionNumber]);
                }
            }
        }
    }

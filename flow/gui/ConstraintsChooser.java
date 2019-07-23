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
   A GUI widget which handles constraints choices.  This often appears at the bottom
   of ModulePanels for Units which do filtering rather than generating original partials.
*/ 

public class ConstraintsChooser extends UnitInput
    {
    JComboBox combo;
    JCheckBox checkbox;
    
    // Indicates to our superclass that the input is for constraints, rather than a standard input.
    public static final int INDEX = -1;
    
    public ConstraintsChooser(Unit unit, ModulePanel modPanel)
        {
        super(unit, INDEX, modPanel);
        
        title.setText(" Only");

        combo = new JComboBox(unit.constraintNames)
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

        combo.setSelectedIndex(unit.getConstraint());

        /// Apparent OS X Java bug: sometimes after you programmatically change
        /// the value of a JComboBox, it no longer sends ActionListener events.  :-(   
        combo.addItemListener(new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                int val = combo.getSelectedIndex();
                                        
                int index = unit.getSound().findRegistered(unit);
                if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                    {
                    return;
                    }
                                
                Output output = unit.getSound().getOutput();
                output.lock();                    
                try
                    {
                    int numSounds = output.getNumSounds();
                    for(int i = 0; i < numSounds; i++)
                        {
                        Sound s = output.getSound(i);
                        if (s.getGroup() == Output.PRIMARY_GROUP)
                            {
                            ((Unit)(s.getRegistered(index))).setConstraint(val);
                            }
                        }
                    }
                finally 
                    {
                    output.unlock();
                    }
                }
            });

        checkbox = new JCheckBox("Not");
        checkbox.setFont(Style.SMALL_FONT());
        checkbox.putClientProperty("JComponent.sizeVariant", "small");
        checkbox.setSelected(unit.getInvertConstraints());
        checkbox.addActionListener(new ActionListener()
            {
            public void actionPerformed( ActionEvent e)
                {
                boolean val = checkbox.isSelected();

                int index = unit.getSound().findRegistered(unit);
                if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                    {
                    return;
                    }

                Output output = unit.getSound().getOutput();
                output.lock();                    
                try
                    {
                    int numSounds = output.getNumSounds();
                    for(int i = 0; i < numSounds; i++)
                        {
                        Sound s = output.getSound(i);
                        if (s.getGroup() == Output.PRIMARY_GROUP)
                            {
                            ((Unit)(s.getRegistered(index))).setInvertConstraints(val);
                            }
                        }
                    }
                finally 
                    {
                    output.unlock();  
                    }                  
                }
            });
                        
        removeAll();
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(jack, BorderLayout.WEST);
        panel.add(title, BorderLayout.CENTER);
        panel.add(checkbox, BorderLayout.EAST);
        add(panel, BorderLayout.NORTH);
        add(combo, BorderLayout.CENTER);
        }
        
    public void rebuild()
        {
        super.rebuild();
        combo.setSelectedIndex(unit.getConstraint());
        checkbox.setSelected(unit.getInvertConstraints());
        }

    }

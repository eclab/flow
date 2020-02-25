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


public class Presets extends JPanel
    {
    JComboBox combo;
    ModulePanel modpanel;
    JLabel label;

    public static final String CHOOSE = "<html><i>Presets...</i></html>";
    
    public Presets(ModulePanel modpanel)
        {
        setLayout(new BorderLayout());
        this.modpanel = modpanel;
        String[] presets = new String[0];
        Modulation m = modpanel.getModulation();
        if (m instanceof Presetable)
            {
            presets = ((Presetable)m).getPresets();
            }
        combo = new JComboBox(presets);
        combo.putClientProperty("JComponent.sizeVariant", "small");
        combo.setEditable(false);
        combo.setFont(Style.SMALL_FONT());
        combo.setMaximumRowCount(32);
        combo.setSelectedIndex(-1);

        /// Note that there is an OS X Java bug: sometimes after you programmatically change
        /// the value of a JComboBox, it no longer sends ActionListener events.  :-(   
        /// However we're not doing that -- we're just doing what's told of us.
                                        
        combo.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                final int selection = combo.getSelectedIndex();
                Rack rack = modpanel.getRack();
                Output output = rack.getOutput();                        
                int index = rack.getIndex(modpanel);
                output.lock();
                try
                    {
                    int numSounds = output.getNumSounds();
                    for(int i = 0; i < numSounds; i++)
                        {
                        Sound s = output.getSound(i);
                        if (s.getGroup() == Output.PRIMARY_GROUP)
                            {
                            Modulation m = s.getRegistered(index);
                            if (m instanceof Presetable)
                                {
                                Presetable p = (Presetable) m;
                                p.setPreset(selection);
                                }
                            }
                        }
                    }
                finally 
                    {
                    output.unlock();
                    }
                combo.setSelectedIndex(-1);
                modpanel.rebuild();
                rack.repaint();      // when the ModulePanel repaints we have to also redraw the wires...
                }
            });


        final ListCellRenderer r = combo.getRenderer();
        ListCellRenderer r2 = new ListCellRenderer()
            {
            public Component getListCellRendererComponent(JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus)
                {
                if (index == -1)
                    return new JLabel(CHOOSE);
                else
                    return r.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                }
            };
        combo.setRenderer(r2);
                
        JComboBox example = new JComboBox(new String[] { CHOOSE });
        combo.setPreferredSize(example.getPreferredSize());
        combo.setMinimumSize(example.getMinimumSize());
        combo.revalidate();
        combo.setSelectedItem(CHOOSE);
        
        add(combo, BorderLayout.CENTER);
        }
    }

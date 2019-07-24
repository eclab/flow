// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import flow.modules.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class SubpatchPanel extends JPanel
    {
    Rack rack;
    JComponent title;
    JLabel titleLabel;
    JComponent titlePanel;
    JComponent body;
    JSlider gain;
    JSlider midi;
    JSlider sounds;
    PushButton swap; 
    int group;
        
    public SubpatchPanel(Rack rack, int group)
        {
        buildSubpatchPanel(rack, group);
        }
        
    void buildSubpatchPanel(Rack rack, int group)
        {
        rack.getOutput().lock();
        try
            {
            this.group = group;
            this.rack = rack;
                
            setLayout(new BorderLayout());
            title = buildTitle();
            add(title, BorderLayout.WEST);
                                                
            body = buildPanel();
            add(body, BorderLayout.CENTER);
                                
            /*
            Border border = BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 2, 2, 2),
                BorderFactory.createLineBorder(Color.GRAY));
            border = BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(2, 2, 2, 2));
            */
            Border border = BorderFactory.createLineBorder(Color.GRAY);
            setBorder(border);
                
            Output out = rack.getOutput();
            titleLabel.setText(" " + out.getPatchName(group));
            gain.setValue((int)(out.getGain(group) * GAIN_RESOLUTION));
            sounds.setValue(out.getNumRequestedSounds(group));
            midi.setValue(out.getInput().getChannel(group) + 1);
            }
        finally     
            {
            rack.getOutput().unlock();
            }
        }
    
    public SubpatchPanel(Rack rack, File file)
        {
        rack.getOutput().lock();
        try
            {
            buildSubpatchPanel(rack, rack.getOutput().addGroup(file));
            }
        finally     
            {
            rack.getOutput().unlock();
            }
        }
    
    // close box
    static final ImageIcon I_CLOSE = iconFor("BellyButton.png");
    static final ImageIcon I_CLOSE_PRESSED = iconFor("BellyButtonPressed.png");

    static ImageIcon iconFor(String name)
        {
        return new ImageIcon(ModulePanel.class.getResource(name));
        }

    /** Sets the Rack of the ModulePanel. */
    public void setRack(Rack rack) { this.rack = rack; }

    /** Returns the Rack of the ModulePanel. */
    public Rack getRack() { return rack; }

    public int getGroup() { return group; }
    public void setGroup(int val) { group = val; }

    /** Returns the background color of the title bar.  Override this
        to customize the title bar to something different than the standards. */
    protected Color getTitleBackground()
        {
        return new Color(128, 128, 128);
        }
        
    /** Returns the foreground color of the title bar.  Override this
        to customize the title bar to something different than the standards. */
    protected Color getTitleForeground()
        {
        return Color.WHITE;
        }

    /** Returns the ModPanel's outer title component. */
    public JComponent getTitle()
        {
        return title;
        }

    /** Returns the ModPanel's inner title component (which is colored). */
    public JComponent getTitlePanel()
        {
        return titlePanel;
        }

    /** Returns the ModPanel's title JLabel. */
    public JLabel getTitleLabel()
        {
        return titleLabel;
        }

    // Builds the title bar for the ModulePanel
    JComponent buildTitle()
        {
        JLabel sacrificialLabel = new JLabel(" 88888888888888888888 ");
        sacrificialLabel.setFont(Style.SMALL_FONT());
        final int titleWidth = (int)sacrificialLabel.getPreferredSize().getWidth();
        final int titleHeight = (int)sacrificialLabel.getPreferredSize().getHeight();

        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
                
        titlePanel = new JPanel();
        titlePanel.setBackground(getTitleBackground());
        titlePanel.setLayout(new BorderLayout());
                
        titleLabel = new JLabel(" Yo ")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
                
        titleLabel.setForeground(getTitleForeground());
        titleLabel.setFont(Style.SMALL_FONT());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        JButton removeButton = new JButton(I_CLOSE);
        removeButton.setPressedIcon(I_CLOSE_PRESSED);
        removeButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.addActionListener(new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                close();
                }
            });
        titlePanel.add(removeButton, BorderLayout.WEST);                 

        outer.add(titlePanel, BorderLayout.CENTER);
        outer.add(Strut.makeVerticalStrut(4), BorderLayout.EAST);
		outer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return outer;
        }


    public static final int GAIN_RESOLUTION = 25;
    public static final int MAX_SOUNDS = 16;
    /** Builds the JComponent displayed in the main body (all but the title bar) of the
        ModulePanel.  Override this to customize as you see fit, by creating UnitInputs,
        UnitOutputs, ModulationInputs, ModulationOutputs, OptionsChoosers, and ConstraintsChoosers. */
    protected JComponent buildPanel()
        {               
        Box box = new Box(BoxLayout.X_AXIS);

        JLabel sacrificialLabel = new JLabel("8.88");
        sacrificialLabel.setFont(Style.SMALL_FONT());
        final int titleWidth = (int)sacrificialLabel.getPreferredSize().getWidth();
        final int titleHeight = (int)sacrificialLabel.getPreferredSize().getHeight();

        JLabel soundsTitle = new JLabel("  Num Sounds: ");
        soundsTitle.setFont(Style.SMALL_FONT());
        JLabel soundsLabel = new JLabel("")
            {
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        soundsLabel.setFont(Style.SMALL_FONT());
        sounds = new JSlider(0, MAX_SOUNDS);
        sounds.setBorder(null);
        sounds.putClientProperty( "JComponent.sizeVariant", "small" );
        sounds.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                if (sounds.getValue() == 0)
                    {
                    soundsLabel.setText("Off");
                    }
                else
                    {
                    soundsLabel.setText("" + sounds.getValue());
                    }
                                
                if (!sounds.getValueIsAdjusting())
                    {
                    Output out = rack.getOutput();
                    out.lock();
                    try
                        {
                        out.setNumRequestedSounds(group, sounds.getValue());
                        out.assignGroupsToSounds();
                        }
                    finally 
                        {
                        out.unlock();
                        }
                    }
                }
            });
        box.add(soundsTitle);
        box.add(soundsLabel);
        box.add(sounds);
        box.add(Strut.makeHorizontalStrut(10));

        JLabel midiTitle = new JLabel("MIDI Channel: ");
        midiTitle.setFont(Style.SMALL_FONT());
        JLabel midiLabel = new JLabel("")
            {
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        midiLabel.setFont(Style.SMALL_FONT());
        midi = new JSlider(0, Input.NUM_MIDI_CHANNELS);
        midi.setBorder(null);
        midi.putClientProperty( "JComponent.sizeVariant", "small" );
        midi.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                if (midi.getValue() == 0)
                    {
                    midiLabel.setText("Off");
                    }
                else
                    {
                    midiLabel.setText("" + midi.getValue());
                    }

                if (!midi.getValueIsAdjusting())
                    {
                    Output out = rack.getOutput();
                    out.lock();
                    Input in = out.getInput();
                    try
                        {
                        if (midi.getValue() == 0)
                            {
                            in.setChannel(group, Input.CHANNEL_NONE);
                            }
                        else
                            {
                            in.setChannel(group, midi.getValue() - 1);
                            }
                        in.rebuildMIDI();
                        }
                    finally 
                        {
                        out.unlock();
                        }
                    }
                }
            });
        box.add(midiTitle);
        box.add(midiLabel);
        box.add(midi);
        box.add(Strut.makeHorizontalStrut(10));

        JLabel gainTitle = new JLabel("Gain: ");
        gainTitle.setFont(Style.SMALL_FONT());
        JLabel gainLabel = new JLabel("")
            {
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        gainLabel.setFont(Style.SMALL_FONT());
        gain = new JSlider(0, (int)(Out.MAX_GAIN * GAIN_RESOLUTION), 0);
        gain.setBorder(null);
        gain.putClientProperty( "JComponent.sizeVariant", "small" );
        gain.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                double val = gain.getValue() / (double) GAIN_RESOLUTION;
                gainLabel.setText(String.format("%1.2f", val));

                // this one is real-time
                rack.getOutput().setGain(group, val);
                }
            });
        box.add(gainTitle);
        box.add(gainLabel);
        box.add(gain);
                
        // later
        /*
          swap = new PushButton("Make Primary")
          {
          public void perform()
          {
          rack.swapSubpatch(group);
          }
          };

          box.add(swap);
        */
                        
        box.add(Stretch.makeHorizontalStretch());

        return box;
        }
            
    public void close()
        {
        rack.remove(this);
        rack.getOutput().removeGroup(group);
        rack.rebuildSubpatches();
        }
    }

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
import org.json.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

public class SubpatchPanel extends JPanel implements Transferable
    {
    Rack rack;
    JComponent title;
    JLabel titleLabel;
    JComponent titlePanel;
    JComponent body;
    JSlider gain;
    JSlider midi;
    JSlider sounds;
    JLabel noteLabel;
    JSlider note;
    JLabel allocatedLabel;
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
            title.addMouseListener(new MouseAdapter()
                {
                public void mousePressed(MouseEvent e)
                    {
                    getTransferHandler().exportAsDrag(SubpatchPanel.this, e, TransferHandler.MOVE);
                    }
                });
                                                
            body = buildPanel();
            add(body, BorderLayout.CENTER);
                                
            Border border = BorderFactory.createLineBorder(Color.GRAY);
            setBorder(border);

            this.setTransferHandler(new SubpatchPanelTransferHandler());
            this.setDropTarget(new DropTarget(this, new SubpatchPanelDropTargetListener()));
                
            Output out = rack.getOutput();
            titleLabel.setText(" " + out.getGroup(group).getPatchName());
            gain.setValue((int)(out.getGroup(group).getGain() * GAIN_RESOLUTION));
            sounds.setValue(out.getGroup(group).getNumRequestedSounds());
            midi.setValue(out.getGroup(group).getChannel() + 1);
            int min = out.getGroup(group).getMinNote();
            int max = out.getGroup(group).getMaxNote();
            if (min != max)             // FIXME: we assume that this means they're full range for now.
                {
                note.setValue(0);
                }
            else
                {
                note.setValue(min);
                }
            updateSoundAllocation();
            }
        finally     
            {
            rack.getOutput().unlock();
            }
        }
    
    public SubpatchPanel(Rack rack, JSONObject obj)
        {
        rack.getOutput().lock();
        try
            {
            obj.remove("sub");  // strip out subgroups
            buildSubpatchPanel(rack, rack.getOutput().addGroup(obj));
            }
        finally     
            {
            rack.getOutput().unlock();
            }
        }
    
    // close box
    static final ImageIcon I_CLOSE = iconFor("BellyButton.png");
    static final ImageIcon I_CLOSE_PRESSED = iconFor("BellyButtonPressed.png");
    public static final String[] notes = new String[] { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };

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
    public static final int MAX_SOUNDS = 15;            // we can't achieve 16, so no reason suggesting it...
    /** Builds the JComponent displayed in the main body (all but the title bar) of the
        ModulePanel.  Override this to customize as you see fit, by creating UnitInputs,
        UnitOutputs, ModulationInputs, ModulationOutputs, OptionsChoosers, and ConstraintsChoosers. */
    protected JComponent buildPanel()
        {        
        Box vbox = new Box(BoxLayout.Y_AXIS);       
        Box box = new Box(BoxLayout.X_AXIS);
        vbox.add(box);

        JLabel sacrificialLabel = new JLabel("8.88");
        sacrificialLabel.setFont(Style.SMALL_FONT());
        final int titleWidth = (int)sacrificialLabel.getPreferredSize().getWidth();
        final int titleHeight = (int)sacrificialLabel.getPreferredSize().getHeight();

        JLabel allocatedTitle = new JLabel("Allocated: ");
        allocatedTitle.setFont(Style.SMALL_FONT());
        allocatedLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        allocatedLabel.setFont(Style.SMALL_FONT());

        JLabel soundsTitle = new JLabel("Voices Requested: ");
        soundsTitle.setFont(Style.SMALL_FONT());
        final JLabel soundsLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
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
                        out.getGroup(group).setNumRequestedSounds(sounds.getValue());
                        out.assignGroupsToSounds();
                        SubpatchPanel[] sub = rack.getSubpatches();
                        for(int i = 0; i < sub.length; i++)
                            sub[i].updateSoundAllocation();
                        }
                    finally 
                        {
                        out.unlock();
                        }
                    }
                }
            });
            
        box.add(Strut.makeHorizontalStrut(10));
        box.add(soundsTitle);
        box.add(soundsLabel);
        box.add(sounds);
        box.add(Strut.makeHorizontalStrut(10));
        box.add(allocatedTitle);
        box.add(allocatedLabel);
        box.add(Strut.makeHorizontalStrut(10));

        JLabel midiTitle = new JLabel("MIDI Channel: ");
        midiTitle.setFont(Style.SMALL_FONT());
        JLabel midiLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
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
                    try
                        {
                        if (midi.getValue() == 0)
                            {
                            out.getGroup(group).setChannel(Input.CHANNEL_NONE);
                            }
                        else
                            {
                            out.getGroup(group).setChannel(midi.getValue() - 1);
                            }
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

        JLabel noteTitle = new JLabel("Note: ");
        noteTitle.setFont(Style.SMALL_FONT());
        JLabel noteLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        noteLabel.setFont(Style.SMALL_FONT());
        note = new JSlider(0, 128);
        note.setBorder(null);
        note.putClientProperty( "JComponent.sizeVariant", "small" );
        note.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                if (note.getValue() == 0)
                    {
                    noteLabel.setText("All");
                    }
                else
                    {
                    int v = note.getValue() - 1;
                    noteLabel.setText("" + notes[v % 12] + (v / 12));
                    }

                if (!note.getValueIsAdjusting())
                    {
                    Output out = rack.getOutput();
                    out.lock();
                    try
                        {
                        if (note.getValue() == 0)
                            {
                            out.getGroup(group).setMinNote(0);
                            out.getGroup(group).setMaxNote(127);
                            }
                        else
                            {
                            int v = note.getValue() - 1;
                            out.getGroup(group).setMinNote(v);
                            out.getGroup(group).setMaxNote(v);
                            }
                        }
                    finally 
                        {
                        out.unlock();
                        }
                    }
                }
            });
        box.add(noteTitle);
        box.add(noteLabel);
        box.add(note);
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
                rack.getOutput().getGroup(group).setGain(val);
                }
            });
        box.add(gainTitle);
        box.add(gainLabel);
        box.add(gain);
        box.add(Strut.makeHorizontalStrut(10));
                
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


        box = new Box(BoxLayout.X_AXIS);
        vbox.add(box);

        return vbox;
        }
    
    public void updateSoundAllocation()
        {
        Output out = rack.getOutput();
        out.lock();
        try
            {
            if (out.getOnlyPlayFirstSound())
                {
                allocatedLabel.setText("[M]");
                }
            else
                {
                allocatedLabel.setText("" + out.getNumSounds(group));
                }
            }
        finally 
            {
            out.unlock();
            }
        }
                
    public void close()
        {
        rack.remove(this);
        rack.getOutput().removeGroup(group);
        rack.rebuildSubpatches();
        }

////// DRAG AND DROP JUNK

                
    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(Rack.subpatchflavor))
            return this;
        else
            return null;
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { Rack.subpatchflavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        return (flavor.equals(Rack.subpatchflavor));
        }
        



    }

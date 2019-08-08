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
    JLabel gainLabel;
    JSlider midi;
    JLabel midiLabel;
    JSlider sounds;
    JLabel soundsLabel;
    JSlider note;
    JLabel noteLabel;
    JLabel allocatedLabel;
    JLabel summary;
    PushButton swap; 
    int group;
    
    public static final int STRUT_WIDTH = 4;
        
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
            gain.setValue((int)(out.getGroup(group).getGain() * GAIN_RESOLUTION * Out.MAX_GAIN));

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
                note.setValue(min + 1);
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
    /** Builds the JComponent displayed in the main body (all but the title bar) of the
        ModulePanel.  Override this to customize as you see fit, by creating UnitInputs,
        UnitOutputs, ModulationInputs, ModulationOutputs, OptionsChoosers, and ConstraintsChoosers. */
    protected JComponent buildPanel()
        {        
        JPanel pane = new JPanel();
        pane.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel sacrificialLabel = new JLabel("8.88");
        sacrificialLabel.setFont(Style.SMALL_FONT());
        final int titleWidth = (int)sacrificialLabel.getPreferredSize().getWidth();
        final int titleHeight = (int)sacrificialLabel.getPreferredSize().getHeight();


        JLabel allocatedTitle = new JLabel("   Allocated: ");
        allocatedTitle.setFont(Style.SMALL_FONT());
        allocatedLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        allocatedLabel.setFont(Style.SMALL_FONT());

        JLabel soundsTitle = new JLabel("Max Voices: ", SwingConstants.RIGHT);
        soundsTitle.setFont(Style.SMALL_FONT());
        soundsLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        soundsLabel.setFont(Style.SMALL_FONT());
        sounds = new JSlider(0, rack.getOutput().getNumSounds() - 1);		// we need to leave one sound for the primary
        sounds.setSnapToTicks(true);
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
                
                buildSummary();
                }
            });
        Dimension d = sounds.getPreferredSize();
        d.width /= 2;
        sounds.setPreferredSize(d);
        
        c.gridx = 0;
        c.gridy = 0;
        pane.add(soundsTitle, c);
    
        c.gridx = 1;
        pane.add(soundsLabel, c);
    
        c.gridx = 2;
        pane.add(sounds, c);
        
        c.gridx = 3;
        pane.add(allocatedTitle, c);
    
        c.gridx = 4;
        pane.add(allocatedLabel, c);
        
        
        
        JLabel gainTitle = new JLabel("   Gain: ");
        gainTitle.setFont(Style.SMALL_FONT());
        gainLabel = new JLabel("")
            {
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        gainLabel.setFont(Style.SMALL_FONT());
        double g = rack.getOutput().getGroup(group).getGain();
        
        gain = new JSlider(0, (int)(Out.MAX_GAIN * GAIN_RESOLUTION), 0);
        gain.setSnapToTicks(true);
        gain.setBorder(null);
        gain.putClientProperty( "JComponent.sizeVariant", "small" );
        gain.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                double val = gain.getValue() / (double) GAIN_RESOLUTION;
                gainLabel.setText(String.format("%1.2f", val));

                // this one is real-time
                rack.getOutput().lock();
                try
                	{
	                rack.getOutput().getGroup(group).setGain(val / Out.MAX_GAIN);		// so we go from 0...1
	                rack.getOutput().redistributeGains();
	                }
	            finally
	            	{
	            	rack.getOutput().unlock();
	            	}

                buildSummary();
                }
            });

        c.gridx = 5;
        pane.add(gainTitle, c);
    
        c.gridx = 6;
        pane.add(gainLabel, c);
    
        c.gridx = 7;
        pane.add(gain, c);
        
        PushButton rename = new PushButton("Rename")
        	{
        	public void perform()
        		{
                rack.getOutput().lock();
                try
                	{
                	String result = AppMenu.showSimpleInput("Rename", "Provide a name for this patch.", 
                		rack.getOutput().getGroup(group).getPatchName(), rack);
                	if (result != null)
                		{
						rack.getOutput().getGroup(group).setPatchName(result);
						titleLabel.setText(" " + result);
						// I don't think we need to distribute this to the sounds
                		}
	                }
	            finally
	            	{
	            	rack.getOutput().unlock();
	            	}
                }
        	};
        c.gridx = 8;
        pane.add(rename, c);
        


        c.gridx = 9;
        c.weightx = 1.0;
        pane.add(Stretch.makeHorizontalStretch(), c);
        c.weightx = 0.0;
        




        JLabel midiTitle = new JLabel("MIDI Channel: ", SwingConstants.RIGHT);
        midiTitle.setFont(Style.SMALL_FONT());
        midiLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
        midiLabel.setFont(Style.SMALL_FONT());
        midi = new JSlider(0, Input.NUM_MIDI_CHANNELS);
        midi.setSnapToTicks(true);
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
                buildSummary();
                }
            });
        d = midi.getPreferredSize();
        d.width /= 2;
        midi.setPreferredSize(d);
        
        c.gridx = 0;
        c.gridy = 1;
        pane.add(midiTitle, c);
        
        c.gridx = 1;
        pane.add(midiLabel, c);
        
        c.gridx = 2;
        pane.add(midi, c);
        
                
        JLabel noteTitle = new JLabel("   MIDI Note: ");
        noteTitle.setFont(Style.SMALL_FONT());
     	noteLabel = new JLabel("")
            {
            public Dimension getMinimumSize() { return new Dimension(titleWidth, titleHeight); }
            public Dimension getPreferredSize() { return new Dimension(titleWidth, titleHeight); }
            };
            
        noteLabel.setFont(Style.SMALL_FONT());
        note = new JSlider(0, 128);
        note.setSnapToTicks(true);
        note.setBorder(null);
        note.putClientProperty( "JComponent.sizeVariant", "small" );
        note.addChangeListener(new ChangeListener()
            {
            public void stateChanged(ChangeEvent e) 
                {
                int v = note.getValue() - 1;
                if (v < 0)
                    {
                    noteLabel.setText("Any");
                    }
                else
                    {
                    noteLabel.setText("" + notes[v % 12] + (v / 12));
                    }

                if (!note.getValueIsAdjusting())
                    {
                    Output out = rack.getOutput();
                    out.lock();
                    try
                        {
                        if (v < 0)
                            {
                            out.getGroup(group).setBothNotes(0, 127);
                            }
                        else
                            {
                            out.getGroup(group).setBothNotes(v);
                            }
                        }
                    finally 
                        {
                        out.unlock();
                        }
                    }
                buildSummary();
                }
            });
        d = note.getPreferredSize();
        d.width *= 1.5;
        note.setPreferredSize(d);

        c.gridx = 3;
        pane.add(noteTitle, c);

        c.gridx = 4;
        pane.add(noteLabel, c);

        c.gridx = 5;
        c.gridwidth = 3;
        pane.add(note, c);
        c.gridwidth = 0;

        c.gridx = 8;
        c.weightx = 1.0;
        pane.add(Stretch.makeHorizontalStretch(), c);
        c.weightx = 0.0;

        summary = new JLabel();
        summary.setFont(Style.SMALL_FONT());
        buildSummary();

        return new DisclosurePanel(summary, pane);
        }
        
    public void buildSummary()
    	{
    	summary.setText("Voices: " + allocatedLabel.getText() + " / " + soundsLabel.getText() +
    		"     Midi: " + midiLabel.getText() +
    		"     Note: " + noteLabel.getText() +
    		"     Gain: " + gainLabel.getText());
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
        buildSummary();
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

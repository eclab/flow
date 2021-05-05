// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import org.json.*;


/**
   A module which accepts partials and outputs them as audio.
   Additionally, Out has three additional Unit inputs and four
   additional modulation inputs.  When a patch is converted
   into a macro, these inputs get routed to the outputs of the patch.
   all eight unit and modulation inputs can be custom named by clicking
   on their labels.
   
   <p>The first two Unit inputs (Audio and Aux) are also routed
   to the left and right partials displays.  And the first two
   Modulation inputs (Scope and Aux) are likewise routed to the
   left and right oscilloscopes.
   
   <p>Finally there is a GAIN which amplifies the audio output volume.
*/

public class Out extends Unit implements Miscellaneous
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MOD_OUTPUTS = 4;
    public static final int NUM_UNIT_OUTPUTS = 4;
    public static final String[] UNIT_NAMES = new String[]  { "A", "B", "C", "D" };
    public static final String[] MOD_NAMES = new String[] { "1", "2", "3", "4", "Gain", "Wet", "Damp", "Size", "Pan" }; //  "C", "R" };

    public static final int MOD_OSC_1 = 0;
    public static final int MOD_OSC_2 = 1;
    public static final int MOD_GAIN = NUM_MOD_OUTPUTS;
    public static final int MOD_REVERB_WET = NUM_MOD_OUTPUTS + 1;
    public static final int MOD_REVERB_DAMP = NUM_MOD_OUTPUTS + 2;
    public static final int MOD_REVERB_ROOM_SIZE = NUM_MOD_OUTPUTS + 3;
    public static final int MOD_PAN = NUM_MOD_OUTPUTS + 4;

    public static final int UNIT_DISPLAY_1 = 0;
    public static final int UNIT_DISPLAY_2 = 1;
        
    public static final int WAVE_SIZE = 96;
    public static final double MAX_GAIN = 4.0;

    double gain;
    public double getGain() { return gain; }

    double pan;
    public double getPan() { return pan; }
 
    boolean dephase;
    public boolean getDephase() { return dephase; }
    public void setDephase(boolean val) 
        {
        if (dephase == val) return;
        dephase = val; 

        if (val == false)  // we have to reset the sound so the positions are back to zero
            {
            getSound().resetPartialPhases();
            }
        }
    
    double[][] modWave = new double[2][WAVE_SIZE];
    int[] wavePos = new int[] { 0, 0 };
    boolean waveTriggered[] = new boolean[] { false, false };
    
    public int getWavePos(int index) { return wavePos[index]; }
    public double[] getModWave(int index) { return modWave[index]; }
    public boolean getAndClearWaveTriggered(int index) { boolean v = waveTriggered[index]; waveTriggered[index] = false; return v; }
    
    // Out
    
    public Object clone()
        {
        Out obj = (Out)(super.clone());
        obj.modWave = (double[][])(obj.modWave.clone());
        for(int i = 0; i < obj.modWave.length; i++)
            obj.modWave[i] = (double[])(obj.modWave[i].clone());
        obj.wavePos = (int[])(obj.wavePos.clone());
        obj.waveTriggered = (boolean[])(obj.waveTriggered.clone());
        return obj;
        }


    public Out(Sound sound)
        {
        super(sound);
        // we clone so we can keep the original names around
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, (String[]) UNIT_NAMES.clone());
        defineOutputs( new String[] { "A", "B" } );
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.QUARTER, Constant.ZERO, Constant.HALF, Constant.HALF, Constant.HALF }, (String[]) MOD_NAMES.clone());
        defineOptions(new String[] { "Dephase" }, new String[][] { { "Dephase" } } );
        if (sound != null) sound.setEmits(this);
        }



    public static final int OPTION_DEPHASE = 0;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_DEPHASE: return getDephase() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_DEPHASE: setDephase(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }       
        


    public boolean showsOutputs() { return false; }

    transient int targetNextTick = 0;
    public static final int TIME_INTERVAL = (int)(Output.SAMPLING_RATE / 1000);         // the amount of time before we add into the modulation buffer
    public void go()
        {
        super.go();
        
        gain = modulate(MOD_GAIN) * MAX_GAIN;
        pan = modulate(MOD_PAN);

        // extract the gain and the output
        if (macro == null)  // going to the output
            {
            pushFrequencies(0);
            //pushOrders(0);                      // already pushed in super.go()

            copyAmplitudes(0);
            double[] amplitudes = getAmplitudes(0);
            for(int i = 0; i < amplitudes.length; i++)
                amplitudes[i] *= gain;
                                
            // provide for auxillary display
            pushFrequencies(1, 1);
            pushAmplitudes(1, 1);
            pushOrders(1, 1);
            }
        else
            {
            // Macros will extract the amplitudes, frequencies, and all modulation
            // on their own.  And they also handle the gain.
            }

        int tick = sound.getOutput().getTick();
        if (macro == null)
            {
            double mod0 = modulate(MOD_OSC_1);
            double mod1 = modulate(MOD_OSC_2);
            waveTriggered[0] = waveTriggered[0] || isTriggered(MOD_OSC_1);
            waveTriggered[1] = waveTriggered[1] || isTriggered(MOD_OSC_2);

            while (tick >= targetNextTick)  // we're top-level, gather the wave
                {
                targetNextTick += TIME_INTERVAL;
                int wp = wavePos[0];
                modWave[0][wp] = mod0;
                wp++;
                if (wp >= WAVE_SIZE) wp = 0;
                wavePos[0] = wp;
        
                wp = wavePos[1];
                modWave[1][wp] = mod1;
                wp++;
                if (wp >= WAVE_SIZE) wp = 0;
                wavePos[1] = wp;
                }
            }
        }
                
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation == MOD_GAIN && isConstant)
            return String.format("%.4f", value * MAX_GAIN);
        else if (modulation == MOD_PAN && isConstant)
            {
            double val = (value * 200);
            return (val == 100 ? "--" : (val < 100 ? ("< " + (int)(100 - val)) : ("" + (int)(val - 100) + " >")));
            }
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }

    public boolean isConstrainable() { return false; }


    public String askForNewName(JComponent parent, String title, String oldName)
        {
        Box box = new Box(BoxLayout.X_AXIS);
        box.add(new JLabel("Enter a new name"));
        JTextField text = new JTextField(30);
        text.setText(oldName);
                
        // The following hack is inspired by https://tips4java.wordpress.com/2010/03/14/dialog-focus/
        // and results in the text field being selected (which is what should have happened in the first place) 
                
        text.addAncestorListener(new javax.swing.event.AncestorListener()
            {
            public void ancestorAdded(javax.swing.event.AncestorEvent e)    
                { 
                JComponent component = e.getComponent();
                component.requestFocusInWindow();
                text.selectAll(); 
                }
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {}
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {}
            });
        box.add(text);
                
        int opt = JOptionPane.showOptionDialog(parent, box, title,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[] { "OK",  "Cancel" }, "OK");

        if (opt == JOptionPane.CANCEL_OPTION)
            {
            return oldName;
            }
        else // (opt == JOptionPane.OK_OPTION)
            { 
            return text.getText().trim(); 
            }
        }

    public static final int LABEL_MAX_LENGTH = 24;


    public static abstract class OutModulePanel extends ModulePanel
        {
        public abstract void updatePatchInfo();
        public OutModulePanel(Modulation mod) { super(mod); }
        };

    public ModulePanel getPanel()
        {
        final JLabel _name = new JLabel(" ", SwingConstants.LEFT);
        final JLabel _author = new JLabel(" ", SwingConstants.LEFT);
        final JLabel _date = new JLabel(" ", SwingConstants.LEFT);
        final JLabel _version = new JLabel(" ", SwingConstants.LEFT);
        final JTextArea _info = new JTextArea(5, LABEL_MAX_LENGTH / 2);
        
        _name.setFont(Style.SMALL_FONT());
        _author.setFont(Style.SMALL_FONT());
        _date.setFont(Style.SMALL_FONT());
        _version.setFont(Style.SMALL_FONT());
        _info.setFont(Style.SMALL_FONT());
        _info.setLineWrap(true);
        _info.setWrapStyleWord(true);
        _info.setBorder(null);
        _info.setBackground(_name.getBackground());
        _info.setRows(10);
        _info.setText(" ");
        _info.setHighlighter(null);
        _info.setEditable(false);
        _info.setCaretPosition(0);  // scrolls to top
                
        final JScrollPane pane = new JScrollPane(_info);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(null);
        pane.getViewport().setBackground(_name.getBackground());
        pane.getVerticalScrollBar().setBackground(_name.getBackground());
        pane.getVerticalScrollBar().setOpaque(false);

        // I hate Java's broken closure rules...
        final javax.swing.Timer[] timer = new javax.swing.Timer[1];
        final ModulePanel[] panel = new ModulePanel[1];
        final boolean[] oldClipped = new boolean[] { false };        
        final boolean[] oldGlitched = new boolean[] { false };        
        final boolean[] oldPlayFirst = new boolean[] { false };        
        final int[] oldNumVoices = new int[] { -1 };            // -1 will force an update the first time
        final boolean[] oldBlockedMIDI = new boolean[] { false };
        final int[] oldChannel = new int[] { -100 };
        final int[] oldRestrict = new int[] { -100 };
        final int[] oldNumGroups = new int[] { -100 };
        
        final JLabel example = new JLabel("  (Audio)");
        example.setFont(Style.SMALL_FONT());

        panel[0] = new OutModulePanel(this)
            {
            public JLabel getAuxUnitInputTitle(int number)
                {
                JLabel label = null;
                if (number > UNIT_DISPLAY_2) return null;
                else if (number == UNIT_DISPLAY_1) label = new JLabel("  (Audio)");
                else if (number == UNIT_DISPLAY_2) label = new JLabel("  (Aux)");
                label.setPreferredSize(example.getPreferredSize());
                label.setFont(Style.SMALL_FONT());
                return label;
                }

            public JLabel getAuxModulationInputTitle(int number)
                {
                JLabel label = null;
                if (number > MOD_OSC_2) return null;
                else if (number == MOD_OSC_1) label = new JLabel("  (Scope)");
                else if (number == MOD_OSC_2) label = new JLabel("  (Aux)");
                label.setPreferredSize(example.getPreferredSize());
                label.setFont(Style.SMALL_FONT());
                return label;
                }
                                
            public void close()
                {
                Rack r = getRack();
                timer[0].stop();
                super.close();
                r.resetEmits();
                }
               
            public void updatePatchInfo()
                {
                String t = getRack().getPatchName();
                if (t == null || t.trim().equals("")) t = Sound.UNTITLED_PATCH_NAME;
                else t = t.trim();
                _name.setText(t);

                t = getRack().getPatchAuthor();
                if (t == null || t.trim().equals("")) t = "--";
                else t = t.trim();
                _author.setText(t);

                t = getRack().getPatchVersion();
                if (t == null || t.trim().equals("")) t = "--";
                else t = t.trim();
                _version.setText(t);

                t = getRack().getPatchDate();
                if (t == null || t.trim().equals("")) t = "--";
                else t = t.trim();
                _date.setText(t);

                t = getRack().getPatchInfo();
                if (t == null || t.trim().equals("")) t = "--";
                else t = t.trim();
                _info.setText(t);
                _info.setCaretPosition(0);  // scrolls to top
                                
                repaint();
                }
            
            public JComponent buildPanel()
                {
                JComponent left = super.buildPanel();
                
                Box right = new Box(BoxLayout.Y_AXIS);
                
                JLabel label = new JLabel("<html><b>Name</b></html>");
                label.setFont(Style.SMALL_FONT());
                right.add(label);
                right.add(_name);
                right.add(Strut.makeVerticalStrut(3));

                label = new JLabel("<html><b>Version</b></html>");
                label.setFont(Style.SMALL_FONT());              
                right.add(label);
                right.add(_version);
                right.add(Strut.makeVerticalStrut(3));

                label = new JLabel("<html><b>Author</b></html>");
                label.setFont(Style.SMALL_FONT());              
                right.add(label);
                right.add(_author);
                right.add(Strut.makeVerticalStrut(3));

                label = new JLabel("<html><b>Date</b></html>");
                label.setFont(Style.SMALL_FONT());              
                right.add(label);
                right.add(_date);
                right.add(Strut.makeVerticalStrut(3));

                label = new JLabel("<html><b>Info</b></html>");
                label.setFont(Style.SMALL_FONT());              
                right.add(label);
                
                JPanel p = new JPanel();
                p.setLayout(new BorderLayout());
                p.add(right, BorderLayout.NORTH);
                p.add(pane, BorderLayout.CENTER);

                JPanel pushPanel = new JPanel();
                pushPanel.setLayout(new BorderLayout());
                        
                final PushButton _update = new PushButton("Edit")
                    {
                    public void perform()
                        {
                        getRack().doPatchDialog("Edit Patch Info");
                        updatePatchInfo();
                        }
                    };
                pushPanel.add(_update, BorderLayout.WEST);
                JComponent comp = flow.gui.Stretch.makeHorizontalStretch();
                pushPanel.add(comp, BorderLayout.CENTER);
                        
                p.add(pushPanel, BorderLayout.SOUTH);
                
                Box box = new Box(BoxLayout.X_AXIS);
                box.add(left);
                box.add(Strut.makeHorizontalStrut(10));
                box.add(p);

                return box;
                }
                                
            public void setRack(Rack rack)
                {
                super.setRack(rack);
                updatePatchInfo();
                }
            };
            
        panel[0].getModulationInputs()[MOD_PAN].setOptionsAndConversions(
            new String[] { "< 100", "< 50", "< 25", "--", "25 >", "50 >", "100 >" },
            new double[] { 0, 0.25, 0.5 * 0.75, 0.5, 1.0 - 0.5 * 0.75, 0.75, 1.0 } );
        
        panel[0].getModulationInputs()[MOD_GAIN].setOptionsAndConversions(
            new String[] { "0", "1", "2", "3", "4" },
            new double[] { 0, 0.25, 0.5, 0.75, 1.0 } );
        
        ModulationInput[] a = panel[0].getModulationInputs();
        for(int i = 0; i < NUM_MOD_OUTPUTS; i++)
            a[i].setTitleCanChange(true);

        UnitInput[] b = panel[0].getUnitInputs();
        for(int i = 0; i < b.length; i++)
            {
            b[i].setTitleCanChange(true);
            }

        timer[0] = new javax.swing.Timer(250, new ActionListener()
            {
            public void actionPerformed ( ActionEvent e )
                {
                Output out = panel[0].getRack().getOutput();
                boolean clipped;
                boolean glitched;
                boolean playFirst;
                int numVoices;
                boolean blockedMIDI;
                int channel;
                int restrict = -1;
                int numGroups = 1;

                out.lock();
                try
                    {
                    clipped = out.getAndResetClipped();
                    glitched = out.getAndResetGlitched();
                    playFirst = out.getOnlyPlayFirstSound();
                    numVoices = out.getNumSounds(Output.PRIMARY_GROUP);
                    blockedMIDI = (out.getGroupOverridingPrimaryGroupInMIDI() != Output.PRIMARY_GROUP);
                    channel = out.getGroup(Output.PRIMARY_GROUP).getChannel();
                    numGroups = out.getNumGroups();
                    int min = out.getGroup(Output.PRIMARY_GROUP).getMinNote();
                    int max = out.getGroup(Output.PRIMARY_GROUP).getMaxNote();
                    if (min == max) restrict = min;
                    }
                finally
                    {
                    out.unlock();
                    }
                
                if (numVoices != oldNumVoices[0] || 
                    clipped != oldClipped[0] || 
                    glitched != oldGlitched[0] || 
                    playFirst != oldPlayFirst[0] || 
                    blockedMIDI != oldBlockedMIDI[0] || 
                    channel != oldChannel[0] || 
                    restrict != oldRestrict[0] ||
                    numGroups != oldNumGroups[0])               // anything to avoid changing the text
                    {
                    oldClipped[0] = clipped;
                    oldGlitched[0] = glitched;
                    oldPlayFirst[0] = playFirst;
                    oldNumVoices[0] = numVoices;
                    oldBlockedMIDI[0] = blockedMIDI;
                    oldChannel[0] = channel;
                    oldRestrict[0] = restrict;
                    oldNumGroups[0] = numGroups;

                    String labeladdendum = "";
                    if (playFirst)
                        {
                        labeladdendum = "  (Mono)";
                        }
                    else if (numVoices < Output.getNumVoices())  // we don't have a fully complement
                        {
                        if (numVoices == 1)
                            {
                            labeladdendum = "  (1 Voice)";
                            }                               
                        else
                            {
                            labeladdendum = "  (" + numVoices + " Voices)";
                            }
                        } 
                    
                    if (restrict >= 0)
                        {
                        labeladdendum = labeladdendum + "  " + Rack.notes[restrict % 12] + (restrict / 12);
                        }
                    
                    if (blockedMIDI)
                        {
                        labeladdendum = labeladdendum + "  M" + (out.getGroup(Output.PRIMARY_GROUP).getChannel() + 1) + "?";
                        }
                    else if (numGroups > 1)  // we've got subgroups
                        {
                        labeladdendum = labeladdendum + "  M" + (out.getGroup(Output.PRIMARY_GROUP).getChannel() + 1);
                        }

                    panel[0].getTitlePanel().setBackground(glitched ? Color.RED : (clipped ? Color.YELLOW : Color.BLACK));
                    panel[0].getTitleLabel().setForeground(glitched ? Color.WHITE : (clipped ? Color.BLACK : Color.WHITE));
                    String label = glitched ? " Glitch" : (clipped ? " Clip" : " Out");
                    label = label + labeladdendum;
                    panel[0].getTitleLabel().setText(label);
                    panel[0].getRack().repaint();
                    }
                }
            });
                        
        timer[0].start();

        return panel[0];
        }
        
        
    //// SERIALIZATION STUFF
    public String getKeyForModulation(int input)
        {
        if (input < NUM_MOD_OUTPUTS) return MOD_NAMES[input];
        else return super.getKeyForModulation(input);
        }
   
    public String getKeyForInput(int input)
        {
        if (input < NUM_UNIT_OUTPUTS) return UNIT_NAMES[input];
        else return super.getKeyForInput(input);
        }

    public void setData(JSONObject data, int moduleVersion, int patchVersion) 
        {
        if (data == null)
            warn("flow/modules/Out.java", "Empty Data for Out.  That can't be right.  Old patch?");
        else
            {
            JSONArray array = data.getJSONArray("mod");
            int num = getNumModulations();
            if (num != array.length())
                {
                warn("flow/modules/Out.java", "Number of modulations in Out (" + num + ") does not match those in the patch (" + array.length() + ")");
                if (array.length() < num) num = array.length();
                }
            for(int i = 0; i < num; i++)
                {
                setModulationName(i, array.getString(i));
                }
                        
            array = data.getJSONArray("unit");              
            num = getNumInputs();
            if (num != array.length())
                {
                warn("flow/modules/Out.java", "Number of unit inputs in Out (" + num + ") does not match those in the patch (" + array.length() + ")");
                if (array.length() < num) num = array.length();
                }
            for(int i = 0; i < num; i++)
                {
                setInputName(i, array.getString(i));
                }
            }
        }
    
    public JSONObject getData() 
        { 
        JSONObject obj = new JSONObject();
        
        JSONArray array = new JSONArray();

        for(int i = 0; i < getNumModulations(); i++)
            array.put(getModulationName(i));
                
        obj.put("mod", array);

        array = new JSONArray();

        for(int i = 0; i < getNumInputs(); i++)
            array.put(getInputName(i));
                
        obj.put("unit", array);
        return obj;
        }
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
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

public class Out extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MOD_OUTPUTS = 4;
    public static final int NUM_UNIT_OUTPUTS = 4;
    public static final String[] UNIT_NAMES = new String[]  { "A", "B", "C", "D" };
    public static final String[] MOD_NAMES = new String[] { "1", "2", "3", "4", "Gain", "Wet", "Damp", "Size" };

    public static final int MOD_OSC_1 = 0;
    public static final int MOD_OSC_2 = 1;
    public static final int MOD_GAIN = NUM_MOD_OUTPUTS;
    public static final int MOD_REVERB_WET = NUM_MOD_OUTPUTS + 1;
    public static final int MOD_REVERB_DAMP = NUM_MOD_OUTPUTS + 2;
    public static final int MOD_REVERB_ROOM_SIZE = NUM_MOD_OUTPUTS + 3;

    public static final int UNIT_DISPLAY_1 = 0;
    public static final int UNIT_DISPLAY_2 = 1;
        
    public static final int WAVE_SIZE = 100;
    public static final double MAX_GAIN = 4.0;

    double gain;
    public double getGain() { return gain; }

    Macro macro = null;
    
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
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, (String[])UNIT_NAMES.clone());
        defineOutputs( new String[] { "A", "B" } );
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.QUARTER, Constant.ZERO, Constant.HALF, Constant.HALF }, (String[]) MOD_NAMES.clone());
        if (sound != null) sound.setEmits(this);
        }

    void setMacro(Macro macro) { this.macro = macro; }

    public boolean showsOutputs() { return false; }

    transient int targetNextTick = 0;
    public static final int TIME_INTERVAL = (int)(Output.SAMPLING_RATE / 1000);         // the amount of time before we add into the modulation buffer
    public void go()
        {
        super.go();
        
        gain = modulate(MOD_GAIN) * MAX_GAIN;

        // extract the gain and the output
        if (macro == null)  // going to the output
            {
            pushFrequencies(0);
            pushOrders(0);                      // already pushed in super.go()

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
            // on their own.  And they're also handle the gain.
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
        if (modulation == 4 && isConstant)
            return String.format("%.4f", value * MAX_GAIN);
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

        
    public ModulePanel getPanel()
        {
        // I hate Java's broken closure rules...
        final javax.swing.Timer[] timer = new javax.swing.Timer[1];
        final ModulePanel[] panel = new ModulePanel[1];
        final boolean[] oldClipped = new boolean[] { false };        
        final boolean[] oldGlitched = new boolean[] { false };        
        
        final JLabel example = new JLabel("  (Audio)");
        example.setFont(Style.SMALL_FONT());

        panel[0] = new ModulePanel(this)
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
            };
            
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
                boolean clipped = panel[0].getRack().getOutput().getAndResetClipped();
                boolean glitched = panel[0].getRack().getOutput().getAndResetGlitched();
                
                if (clipped != oldClipped[0] || glitched != oldGlitched[0])
                    {
                    oldClipped[0] = clipped;
                    oldGlitched[0] = glitched;
                    

                    panel[0].getTitlePanel().setBackground(glitched ? Color.RED : (clipped ? Color.YELLOW : Color.BLACK));
                    panel[0].getTitleLabel().setForeground(glitched ? Color.WHITE : (clipped ? Color.BLACK : Color.WHITE));
                    panel[0].getTitleLabel().setText(glitched ? "  Glitch" : (clipped ? "  Clip" : "  Out"));
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
    		System.err.println("WARNING(flow/modules/Out.java): Empty Data for Out.  That can't be right.  Old patch?");
    	else
    		{
    		JSONArray array = data.getJSONArray("mod");
    		for(int i = 0; i < array.length(); i++)
    			{
    			setModulationName(i, array.getString(i));
    			}
    		array = data.getJSONArray("unit");
    		for(int i = 0; i < array.length(); i++)
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

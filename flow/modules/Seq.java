// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
   A Modulation which provides a simple step sequencer of up to 32 steps.  You specify
   the number of steps with the "steps" knob.  Each step changes the output modulation
   to a specified value.
   
   <p>If "free" is true, then steps are updated every time Seq receives a trigger.
   If "free" is false, then steps are updated every time Seq receives a trigger, but only
   when a note is pressed; as soon as it is released, Seq stops updating.
   
   <p>If "sample" is false, then you can change any of the step modulations in real
   time: for example, if Seq is in step 5, and you change the knob of step 5, then Seq's
   modulation will change as well.  You may or may not want this.  If "sample" is true,
   then Seq samples the next step's modulation when the trigger arrives, then only outputs
   that modulation value during the step.
   
   <p>Seq is designed to output modulation values: but you can set it up to effectively
   output notes.  To do this, attach Seq's output as the "shift" modulation for a Shift,
   and have Shift change the pitch of your sound.  Make sure Shift's type is set to "Pitch"
   and Bound = 1.0.  To make setting notes easier, if you
   double-click on a Seq dial, a keyboard will pop up which maps notes to modulation
   values: Middle C is equivalent to no shift.
*/

public class Seq extends Modulation
    {       
    private static final long serialVersionUID = 1;

    public static final int MOD_STEPS = 32;
    public static final int MOD_TRIGGER = 33;

    // Pitch equivalence mappings.  24 = centered (0.5), 0 = 2 octaves down (0.0), 48 = two octaves up (1.0)
    public static final double[] PITCHES = new double[49];
    
    static
        {
        for(int i = 0; i < PITCHES.length; i++)
            {
            PITCHES[i] = (1.0 - (i - 24)/12.0) / 4.0 + 0.25;
            }
        };


    public static final int NUM_STATES = 32;
        
    int state = 0;
    boolean free = false;
    boolean sample = true;
    boolean gated = false;
    boolean guided = false;

    public boolean getFree() { return free; }
    public void setFree(boolean val) { free = val; }
    public boolean getSample() { return sample; }
    public void setSample(boolean val) { sample = val; }
    public boolean getGuided() { return guided; }
    public void setGuided(boolean val) { guided = val; }
        
    public static final int OPTION_FREE = 0;
    public static final int OPTION_SAMPLE = 1;
    public static final int OPTION_GUIDED = 2;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return (getFree() ? 1 : 0);
            case OPTION_SAMPLE: return (getSample() ? 1 : 0);
            case OPTION_GUIDED: return (getGuided() ? 1 : 0);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            case OPTION_SAMPLE: setSample(value != 0); return;
            case OPTION_GUIDED: setGuided(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public Seq(Sound sound)
        {
        super(sound);
        defineOptions(new String[] { "Free", "Sample", "Guided" }, new String[][] { { "Free" }, { "Sample" }, { "Guided" } });
        defineModulations(new Constant[] 
            { Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF, 
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
              Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF, 
              Constant.ONE, Constant.ZERO, },
            new String[] {
                "1", "2", "3", "4", 
                "5", "6", "7", "8",
                "9", "10", "11", "12", 
                "13", "14", "15", "16",
                "17", "18", "19", "20",
                "21", "22", "23", "24",
                "25", "26", "27", "28",
                "29", "30", "31", "32",
                "Steps", "Trigger"
                });
        }

    // go() automatically clears the trigger so we need to set it a different way
    boolean didTrigger;
        
    public void reset()
        {
        super.reset();
        state = 0;
        setModulationOutput(0, modulate(state));
//        didTrigger = true;            // this creates beeps when resetting, bad
        gated = false;
        }
                
    public void gate()
        {
        super.gate();
        gated = true;
        if (!free) 
            {
            state = 0;
            setModulationOutput(0, modulate(state));
            didTrigger = true;
            }
        }
    
    public void release()
        {
        gated = false;
        }
        
    public void go()
        {
        super.go();
        if (didTrigger) updateTrigger(0);
        didTrigger = false;

        if (!gated && !free)  
            return;      
        
        if (isTriggered(MOD_TRIGGER))
            {
	        int maxState = (int)(modulate(MOD_STEPS) * (NUM_STATES - 1) + 1);
            if (guided)
            	{
            	state = (int)(modulate(MOD_TRIGGER) * maxState);
            	if (state == maxState) state--;
            	}
            else
            	{
            	state++;
	            if (state >= maxState) state = 0;
	            }
            setModulationOutput(0, modulate(state));
            updateTrigger(0);
            }
        else if (!sample)
            {
            setModulationOutput(0, modulate(state));
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_STEPS)
                {
                return "" + (int)(modulate(MOD_STEPS) * (NUM_STATES - 1) + 1);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    public ModulePanel getPanel()
        {
        return new ModulePanel(Seq.this)
            {
            public JComponent buildPanel()
                {
                JLabel example = new JLabel("0.0000 ");
                example.setFont(Style.SMALL_FONT());
                
                Modulation mod = getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new ModulationOutput(mod, 0, this));

                for(int i = 0; i < NUM_STATES; i+=4)
                    {
                    Box box2 = new Box(BoxLayout.X_AXIS);
                    for(int j = i; j < i + 4; j++)
                        {

                        // You'll notice a lot of code here is the same as in Shift
                        final ModulationInput[] m = new ModulationInput[1];
                        m[0] = new ModulationInput(mod, j, this)
                            {
                            public JPopupMenu getPopupMenu()
                                {
                                final JPopupMenu pop = new JPopupMenu();
                                KeyDisplay display = new KeyDisplay(null, Color.RED, 36, 84, 60, 0)
                                    {
                                    public void setState(int state)
                                        {
                                        pop.setVisible(false);
                                        m[0].setState(PITCHES[state - 60 + 24]);
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
//                        m[0].getData().setPreferredSize(example.getPreferredSize());
                        m[0].getData().setMinimumSize(example.getPreferredSize());
                        box2.add(m[0]);
                        }
                    box.add(box2);
                    }
                box.add(new ModulationInput(mod, MOD_STEPS, this));
                box.add(new ModulationInput(mod, MOD_TRIGGER, this));

                for(int i = 0; i < mod.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(mod, i));
                    }
                    
                return box;
                }
            };
        }

    }

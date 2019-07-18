// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.ref.*;

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
            PITCHES[i] = ((i - 24)/12.0) / Shift.MAX_PITCH_BOUND + 0.5;
            }
        };

    public static final int CURVE_LINEAR = 0;
    public static final int CURVE_X_2 = 1;
    public static final int CURVE_X_4 = 2;
    public static final int CURVE_X_8 = 3;
    public static final int CURVE_X_16 = 4;
    public static final int CURVE_X_32 = 5;
    public static final int CURVE_STEP = 6;

    public static final int NUM_STATES = 32;
        
    int state = 0;
    boolean free = true;
    boolean release = false;
    boolean sample = true;
    boolean guided = false;
    boolean display = false;
    boolean playing = false;
    int curve = CURVE_STEP;

    public int getCurve() { return curve; }
    public void setCurve(int val) { curve = val; }
    public boolean getFree() { return free; }
    public void setFree(boolean val) { free = val; }
    public boolean getRelease() { return release; }
    public void setRelease(boolean val) { release = val; }
    public boolean getSample() { return sample; }
    public void setSample(boolean val) { sample = val; }
    public boolean getGuided() { return guided; }
    public void setGuided(boolean val) { guided = val; }
    public boolean getDisplay() { return display; }
    public void setDisplay(boolean val) { display = val; }
        
    public static final int OPTION_CURVE = 0;
    public static final int OPTION_FREE = 1;
    public static final int OPTION_RELEASE = 2;
    public static final int OPTION_SAMPLE = 3;
    public static final int OPTION_GUIDED = 4;
    public static final int OPTION_DISPLAY = 5;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_CURVE: return getCurve();
            case OPTION_FREE: return (getFree() ? 1 : 0);
            case OPTION_RELEASE: return (getRelease() ? 1 : 0);
            case OPTION_SAMPLE: return (getSample() ? 1 : 0);
            case OPTION_GUIDED: return (getGuided() ? 1 : 0);
            case OPTION_DISPLAY: return (getDisplay() ? 1 : 0);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_CURVE: setCurve(value); return;
            case OPTION_FREE: setFree(value != 0); return;
            case OPTION_RELEASE: setRelease(value != 0); return;
            case OPTION_SAMPLE: setSample(value != 0); return;
            case OPTION_GUIDED: setGuided(value != 0); return;
            case OPTION_DISPLAY: setDisplay(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public Seq(Sound sound)
        {
        super(sound);
        defineOptions(new String[] { "Change", "Free", "Stop on Release", "Sample", "Guided", "Display" }, new String[][] { { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step" }, { "Free" }, { "Stop on Release" }, { "Sample" }, { "Guided" }, { "Display" } });
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
        
    void resetSequencerPosition()
        {
        int oldstate = state;
        state = 0;
        updateModulation();
        }
        
    public void reset()
        {
        super.reset();
        resetSequencerPosition();
        if (free)
            playing = true;
        }
                
    public void gate()
        {
        super.gate();
        if (!free)
            {
            resetSequencerPosition();
            playing = true;
            }
        }
    
    public void release()
        {
        if (!free && release)
            playing = false;
        }
        
    volatile int _state = 0;
    void updateModulation()
        {
        if (curve == CURVE_STEP)
            {
            setModulationOutput(0, modulate(state));
            }
        else
            {
            double alpha = modulate(MOD_TRIGGER);
                
            switch(curve)
                {
                case CURVE_STEP:
                    {
                    // should never happen
                    }
                break;
                case CURVE_X_2:
                    {
                    alpha = alpha * alpha;
                    }
                break;
                case CURVE_X_4:
                    {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    }
                break;
                case CURVE_X_8:
                    {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    }
                break;
                case CURVE_X_16:
                    {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    }
                break;
                case CURVE_X_32:
                    {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    }
                break;
                default:
                    {
                    // should never happen
                    }
                }
                        
            double currentVal = modulate(state);
            int maxState = (int)(modulate(MOD_STEPS) * (NUM_STATES - 1) + 1);
            int nextState = state + 1;
            if (nextState >= maxState) nextState = 0;
            double nextVal = modulate(nextState);
            setModulationOutput(0, (1 - alpha) * currentVal + alpha * nextVal);
            }

        _state = state;
        }
        
    public void go()
        {
        super.go();

        if (!playing && !free) 
            return;

        if (isTriggered(MOD_TRIGGER))
            {
            int oldstate = state;
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
            updateModulation();
            updateTrigger(0);
            }
        else if (!sample)
            {
            updateModulation();
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

    static ArrayList panels = new ArrayList();
    static javax.swing.Timer timer = null;
    static void startTimer()
        {
        if (timer == null)
            {
            timer = new javax.swing.Timer(25, new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    Iterator iterator = panels.iterator();
                    while(iterator.hasNext())
                        {
                        WeakReference ref = (WeakReference)(iterator.next());
                        Object obj = ref.get();
                        if (obj != null)
                            {
                            ((SeqModulePanel) obj).doRepaint();
                            }
                        else
                            {
                            iterator.remove();
                            }
                        }
                    }
                });
            timer.start();
            }
        }

    // we need a custom module so we can make up a method in it called doRepaint(),
    // which repaints when appropriate.
        
    class SeqModulePanel extends ModulePanel
        {
        boolean repaintOnce = true;                     // this allows us to clear things if the user turns off getDisplay
        public void doRepaint()
            {
            if (Seq.this.getDisplay())
                { repaint(); repaintOnce = true; }
            else if (repaintOnce)
                { repaint(); repaintOnce = false; }
            }
                
        public SeqModulePanel(Seq mod)
            {
            super(mod);
            }
                                
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
                    final int _state = j;

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
                            
                        public boolean getDrawsStateDot()
                            {
                            if (!Seq.this.getDisplay()) return false;
                                
                            int m = sound.findRegistered(Seq.this);
                            Sound soundlast = sound.getOutput().getInput().getLastPlayedSound();
                            if (soundlast == null)
                                soundlast = sound.getOutput().getSoundUnsafe(0);
                            Seq seq0 = (Seq)(soundlast.getRegistered(m));

                            return (seq0._state == _state);
                            }
                        };
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
        }

    public ModulePanel getPanel()
        {
        startTimer();
        SeqModulePanel mp = new SeqModulePanel(Seq.this);
        
        // I think this should be okay -- we're doing stuff here in the swing event thread
        panels.add(new WeakReference(mp));
        return mp;
        }

    }

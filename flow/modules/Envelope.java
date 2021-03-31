// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;

/**
   A Modulation which provides multistate envelope with 8 segments.
        
   <ol>
   <li>A pre-sustain state with 3 segments
   <li>A sustain state with 3 segments
   <li>A release state with 2 segments
   </ol>
                
   <p>Each state has an time (not rate) and a level.  If a segment has a zero length, it is 
   simply ignored.  The envelope can be free or reset on gate.
*/


public class Envelope extends Modulation implements ModSource
{
    private static final long serialVersionUID = 1;
        
    public static final int NUM_STATES = 8;
    public static final int NOT_STARTED = -1;
    public static final int START = 0;
    public static final int PRE_1 = 0;
    public static final int PRE_2 = 1;
    public static final int PRE_3 = 2;
    public static final int SUSTAIN_START_STATE = 3;
    public static final int SUS_1 = 3;
    public static final int SUS_2 = 4;
    public static final int SUSTAIN_END_STATE = 5;
    public static final int SUS_3 = 5;
    public static final int REL_1 = 6;
    public static final int REL_2 = 7;
    public static final int DONE = 8;
        
    public static final int MOD_GATE_TR = NUM_STATES * 2;
    public static final int MOD_REL_TR = NUM_STATES * 2 + 1;
    public static final int MOD_START_LEVEL = NUM_STATES * 2 + 2;

    public static final int SUSTAIN = 0;
    public static final int SUSTAIN_LOOPING = 1;
    public static final int ONE_SHOT = 2;
    public static final int PLAY_THROUGH = 3;
    public static final int FULL_LOOPING_WITH_RELEASE = 4;
    public static final int FULL_LOOPING = 5;
        
    public static final String[] TYPE_NAMES = new String[] { "Sustain", "Sustain Loop", "One Shot", "Play Through", "Loop w/Rel", "Loop" };
        
    public static final int CURVE_LINEAR = 0;
    public static final int CURVE_X_2 = 1;
    public static final int CURVE_X_4 = 2;
    public static final int CURVE_X_8 = 3;
    public static final int CURVE_X_16 = 4;
    public static final int CURVE_X_32 = 5;
    public static final int CURVE_STEP = 6;
    public static final int CURVE_X_2_X_8 = 7;
    public static final int CURVE_X_4_X_16 = 8;
    public static final int CURVE_X_8_X_32 = 9;
    public static final int CURVE_1_MINUS_X_2 = 10;
    public static final int CURVE_1_MINUS_X_4 = 11;
    public static final int CURVE_1_MINUS_X_8 = 12;
        
    public static final int OPTION_CURVE = 0;
    public static final int OPTION_TYPE = 1;
    public static final int OPTION_GATE_RESET = 2;
    public static final int OPTION_SYNC = 3;
    
    public static final int OUT_MOD = 0;
    public static final int OUT_PRE_1 = 1;
    public static final int OUT_PRE_2 = 2;
    public static final int OUT_PRE_3 = 3;
    public static final int OUT_SUS_1 = 4;
    public static final int OUT_SUS_2 = 5;
    public static final int OUT_SUS_3 = 6;
    public static final int OUT_REL_1 = 7;
    public static final int OUT_REL_2 = 8;
    public static final int OUT_DONE = 9;
    
    public int getOptionValue(int option) 
    { 
        switch(option)
            {
            case OPTION_CURVE: return getCurve();
            case OPTION_TYPE: return getType();
            case OPTION_GATE_RESET: return getGateReset() ? 1 : 0;
            case OPTION_SYNC: return getSync() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
    }
                
    public void setOptionValue(int option, int value)
    { 
        switch(option)
            {
            case OPTION_CURVE: setCurve(value); return;
            case OPTION_TYPE: setType(value); return;
            case OPTION_GATE_RESET: setGateReset(value != 0); return;
            case OPTION_SYNC: setSync(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
    }
        
    int curve;
    public int getCurve() { return curve; }
    public void setCurve(int val) { curve = val; }

    int type;
    public int getType() { return type; }
    public void setType(int val) { type = val; }
        
    boolean sync;
    public boolean getSync() { return sync; }
    public void setSync(boolean val) { sync = val; }

    boolean gateReset = false;
    public boolean getGateReset() { return gateReset; }
    public void setGateReset(boolean val) { gateReset = val; }

    double[] level = new double[NUM_STATES];
    double[] time = new double[NUM_STATES];
    
    public Object clone()
    {
        Envelope obj = (Envelope)(super.clone());
        obj.level = (double[])(obj.level.clone());
        obj.time = (double[])(obj.time.clone());
        return obj;
    }

    int state;
    double start;
    double interval;
    boolean reset = true;
    double firstLevel;
    boolean released;

    public Envelope(Sound sound)
    {
        super(sound);
        defineOptions(
                      new String[] { "Curve", "Type", "Gate Reset", "MIDI Sync" }, 
                      new String[][] { { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step", "x^2, 8", "x^4, 16", "x^8, 32", "Inv x^2", "Inv x^4", "Inv x^8"   },
                                       TYPE_NAMES, { "Gate Reset" }, { "MIDI Sync" } } );        

        Constant[] mods = new Constant[NUM_STATES * 2 + 3];
        for(int i = 0; i < mods.length; i++)
            mods[i] = Constant.ZERO;
        String[] names = new String[]
            {
                "Pre 1", "Level",
                "Pre 2", "Level",
                "Pre 3", "Level", 
                "Sus 1", "Level", 
                "Sus 2", "Level", 
                "Sus 3", "Level",
                "Rel 1", "Level",
                "Rel 2", "Level",
                "On Tr", "Off Tr",
                "Start Lvl"
            };
        defineModulations(mods, names);
        defineModulationOutputs(new String[] { "Mod", "Pre 1", "Pre 2", "Pre 3", "Sus 1", "Sus 2", "Sus 3", "Rel 1", "Rel 2", "E" }); 
        setModulationOutput(0, 0);  
        this.type = SUSTAIN;
        firstLevel = 0;
        state = NOT_STARTED;
    }

    public void reset() 
    {
        super.reset();
        state = NOT_STARTED;
        reset = true;
    }
        
    public void restart()
    {
        super.restart();
        if (sync)
            {
                state = NOT_STARTED;
            }
    }
        
    public void gate()
    {
        super.gate();
        if (isModulationConstant(MOD_GATE_TR))
            doGate();
    }

    void doGate()
    {
        released = false;
        for(int i = 0 ; i < NUM_STATES; i++)
            {
                time[i] = modulate(i * 2);
                level[i] = modulate(i * 2 + 1);
            }
                
        if (gateReset)
            {
                firstLevel = modulate(MOD_START_LEVEL);
            }
        else
            {
                firstLevel = getModulationOutput(0);            // so we change from there when we start
            }
                
        interval = 0;
        start = getSyncTick(sync);

        state = NOT_STARTED;  // so we start at the very beginning with findNextState() next
        state = findNextState();
        if (state != DONE)
            {
                interval = toTicks(time[state]);
            }
        scheduleTrigger(state);
    }
        
    public void release()
    {
        super.release();
        if (isModulationConstant(MOD_REL_TR))
            doRelease();
    }
    
    void doRelease()
    {
        if (type == ONE_SHOT) return;  // we ignore release on one shot
        
        released = true;
        start = getSyncTick(sync);
        firstLevel = getModulationOutput(0);            // so we change from there when we start
        int oldState = state;
        state = findNextState();
        if (state != DONE)
            {
                interval = toTicks(time[state]);
            }
        if (oldState != state)
            scheduleTrigger(state);
    }
        
    public static final int T_PRE_1 = 0;
    public static final int T_PRE_2 = 1;
    public static final int T_PRE_3 = 2;
    public static final int T_SUS_1 = 3;
    public static final int T_SUS_2 = 4;
    public static final int T_SUS_3 = 5;
    public static final int T_REL_1 = 6;
    public static final int T_REL_2 = 7;
    public static final int T_DONE = 8;
    
    int scheduledTriggers = 0;
    void scheduleTrigger(int val)
    {
        if (val == T_PRE_1)
            {
                scheduledTriggers |= 1;
            }
        else if (val == T_PRE_2)
            {
                scheduledTriggers |= 2;
            }
        else if (val == T_PRE_3)
            {
                scheduledTriggers |= 4;
            }
        else if (val == T_SUS_1)
            {
                scheduledTriggers |= 8;
            }
        else if (val == T_SUS_2)
            {
                scheduledTriggers |= 16;
            }
        else if (val == T_SUS_3)
            {
                scheduledTriggers |= 32;
            }
        else if (val == T_REL_1)
            {
                scheduledTriggers |= 64;
            }
        else if (val == T_REL_2)
            {
                scheduledTriggers |= 128;
            }
        else if (val == T_DONE)
            {
                scheduledTriggers |= 256;
            }
    }


    public int findNextState()
    {
        if (type == FULL_LOOPING)
            {
                // Go through all N states, looping around, and return the next one which doesn't skip.
                // If you couldn't find one, return DONE
                int s = state;  
                for(int i = 0; i < NUM_STATES; i++)
                    {
                        s++;
                        if (s >= DONE) s = START;
                        if (modulate(s * 2) > 0) return s;
                    }
                // if we've gotten here, we couldn't find a valid sustain state, so we're DONE
            
                return DONE;
            }
        else if (type == FULL_LOOPING_WITH_RELEASE)
            {
                if (released)
                    return DONE;
                
                // Go through all N states, looping around, and return the next one which doesn't skip.
                // If you couldn't find one, return DONE
                int s = state;  
                for(int i = 0; i < NUM_STATES; i++)
                    {
                        s++;
                        if (s >= DONE) s = START;
                        if (modulate(s * 2) > 0) return s;
                    }
                // if we've gotten here, we couldn't find a valid sustain state, so we're DONE
            
                return DONE;
            }
        else if (type == SUSTAIN_LOOPING && state <= SUSTAIN_END_STATE && !released)
            {
                // Go through all later states, looping back to SUSTAIN_START_STATE, and return the next one which doesn't skip.
                // If you couldn't find one, return the original state (which might be -1)
                int s = state;  
                for(int i = 0; i < SUSTAIN_END_STATE + 1; i++)
                    {
                        s++;
                        if (s > SUSTAIN_END_STATE) s = SUSTAIN_START_STATE; 
                        if (modulate(s * 2) > 0) return s;
                    }
                // if we've gotten here, we couldn't find a valid sustain state, so we're going to continue just like one shot

                // Go through all later states, ending at DONE, and return the next one which doesn't skip.
                // If you couldn't find one, return the original state (which might be -1)
                s = SUSTAIN_END_STATE;
                for(int i = 0; i < NUM_STATES; i++)
                    {
                        s++;
                        if (s >= DONE) return DONE;
                        if (modulate(s * 2) > 0) return s;
                    }
            
                // If absolutely nothing is turned on....
                return DONE;
            }
        else if (type == SUSTAIN && state <= SUSTAIN_END_STATE && !released)
            {
                // Go through all later states, stoppping at SUSTAIN_END_STATE, and return the next one which doesn't skip.
                int s = state;  
                for(int i = 0; i <= SUSTAIN_END_STATE; i++)
                    {
                        s++;
                        if (s > SUSTAIN_END_STATE) break; 
                        if (modulate(s * 2) > 0) return s;
                    }
                // if we've gotten here, we couldn't find a valid sustain state, so we're going to continue just like one shot

                // try SUSTAIN_END_STATE to see if we hold there
                s = SUSTAIN_END_STATE;
                if (modulate(s * 2) > 0) return s;

                // Go through all later states, ending at DONE, and return the next one which doesn't skip.
                // If you couldn't find one, DONE
                s = SUSTAIN_END_STATE;
                for(int i = 0; i < NUM_STATES; i++)
                    {
                        s++;
                        if (s >= DONE) return DONE;
                        if (modulate(s * 2) > 0) return s;
                    }
            
                // If absolutely nothing is turned on....
                return DONE;
            }
        else // SUSTAIN release, SUSTAIN LOOPING release, ONE_SHOT, etc.
            {
                // Go through all later states, ending at DONE, and return the next one which doesn't skip.
                // If you couldn't find one, return the original state (which might be -1)
                int s = state;  
                for(int i = 0; i < NUM_STATES; i++)
                    {
                        s++;
                        if (s >= DONE) return DONE;
                        if (modulate(s * 2) > 0) return s;
                    }
                return DONE;
            }
    }
        
    public double toTicks(double mod)
    {
        return modToLongRate(mod) * Output.SAMPLING_RATE;
    }
    
    public void go()
    {
        super.go();

        if (isTriggered(MOD_GATE_TR))
            {
                doGate();
            }
        else if (isTriggered(MOD_REL_TR))
            {
                doRelease();
            }
                
        if (scheduledTriggers != 0)
            {
                if ((scheduledTriggers & 1) == 1)
                    {
                        updateTrigger(OUT_PRE_1);
                    }
                if ((scheduledTriggers & 2) == 2)
                    {
                        updateTrigger(OUT_PRE_2);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 4) == 4)
                    {
                        updateTrigger(OUT_PRE_3);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 8) == 8)
                    {
                        updateTrigger(OUT_SUS_1);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 16) == 16)
                    {
                        updateTrigger(OUT_SUS_2);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 32) == 32)
                    {
                        updateTrigger(OUT_SUS_3);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 64) == 64)
                    {
                        updateTrigger(OUT_REL_1);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 128) == 128)
                    {
                        updateTrigger(OUT_REL_2);
                        updateTrigger(OUT_MOD);
                    }
                if ((scheduledTriggers & 256) == 256)
                    {
                        updateTrigger(OUT_DONE);
                        updateTrigger(OUT_MOD);
                    }
                scheduledTriggers = 0;
            }

        if (state == NOT_STARTED)               // haven't started the envelope yet
            return;
        
        if (state == DONE)  // we're done
            return;
        
        long tick = getSyncTick(sync);

        if (tick < start) // uh oh, probably switched to MIDI Sync
            {
                start = tick;
            }
                
        // Do we need to transition to a new state?
        boolean transitioned = false;
        int lastState = state;
        while (tick >= start + interval)
            {
                state = findNextState();
                if (lastState != state)
                    {
                        if (state == PRE_1)                     // shouldn't happen
                            {
                                updateTrigger(OUT_PRE_1);
                            }
                        else if (state == PRE_2)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_PRE_2);
                            }
                        else if (state == PRE_3)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_PRE_3);
                            }
                        else if (state == SUS_1)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_SUS_1);
                            }
                        else if (state == SUS_2)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_SUS_2);
                            }
                        else if (state == SUS_3)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_SUS_3);
                            }
                        else if (state == REL_1)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_REL_1);
                            }
                        else if (state == REL_2)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_REL_2);
                            }
                        else if (state == DONE)
                            {
                                updateTrigger(OUT_MOD);
                                updateTrigger(OUT_DONE);
                            }
                    }
                 
                if (state == DONE)      // we're done again
                    return;

                // update the state
                start = start + interval;
                interval = toTicks(time[state]);
                firstLevel = level[lastState];
            }
      
        // Where are we in the state? 
        
        double alpha = (tick - start) / interval;               
        switch(curve)
            {
            case CURVE_LINEAR:
                {
                    // do nothing
                }
                break;
            case CURVE_X_2:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = 1 - alpha;
                }
                break;
            case CURVE_X_4:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    alpha = 1 - alpha;
                }
                break;
            case CURVE_X_8:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = 1 - alpha;
                }
                break;
            case CURVE_X_16:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = 1 - alpha;
                }
                break;
            case CURVE_X_32:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = 1 - alpha;
                }
                break;
            case CURVE_STEP:
                {
                    alpha = 1.0;
                }
                break;
            case CURVE_X_2_X_8:
                {
                    alpha = (1-alpha) * (1-alpha);
                    double beta = alpha;            // x^2
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;          // x^8
                    alpha = 1 - (alpha + beta) * 0.5;
                }
                break;
            case CURVE_X_4_X_16:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    double beta = alpha;            // x^4
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;          // x^16
                    alpha = 1 - (alpha + beta) * 0.5;
                }
                break;
            case CURVE_X_8_X_32:
                {
                    alpha = (1-alpha) * (1-alpha);
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    double beta = alpha;            // x^8
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;          // x^32
                    alpha = 1 - (alpha + beta) * 0.5;
                }
                break;
            case CURVE_1_MINUS_X_2:
                {
                    alpha = alpha * alpha;
                }
                break;
            case CURVE_1_MINUS_X_4:
                {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                }
                break;
            case CURVE_1_MINUS_X_8:
                {
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                    alpha = alpha * alpha;
                }
                break;
            default:
                {
                    // shouldn't happen
                }
            }

        double levels = (1 - alpha) * firstLevel + alpha * level[state];  
        setModulationOutput(0, levels);
    }        
        
        
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (isConstant)
            {
                if (modulation < NUM_STATES * 2 && modulation % 2 == 0)  // it's a time
                    {
                        if (value == 0) return "Off";
                        String str = String.format("%.2f" , modToLongRate(value)) + " S";
                        if (str.length() > 6)
                            str = String.format("%.1f" , modToLongRate(value)) + " S";
                        return str;
                    }
                else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
    }


    public ModulePanel getPanel()
    {
        return new ModulePanel(Envelope.this)
            {
                public JComponent buildPanel()
                {               
                    JLabel example = new JLabel("0.00 S ");
                    example.setFont(Style.SMALL_FONT());
                
                    Box box = new Box(BoxLayout.Y_AXIS);
                    Modulation mod = getModulation();
                
                    Box box1 = new Box(BoxLayout.X_AXIS);
                    box1.add(new ModulationInput(mod, MOD_START_LEVEL, this));
                    box1.add(box1.createGlue());
                    box1.add(new ModulationOutput(mod, 0, this));
                    box.add(box1);
                                
                    for(int i = 0; i < NUM_STATES; i++)
                        {
                            Box box2 = new Box(BoxLayout.X_AXIS);
                            ModulationInput t = new ModulationInput(mod, i * 2, this)
                                {
                                    public String[] getOptions() { return MidiClock.CLOCK_NAMES; }
                                    public double convert(int elt) 
                                    {
                                        return MIDI_CLOCK_LONG_MOD_RATES[elt];
                                    }
                                };
                            t.getData().setPreferredSize(example.getPreferredSize());
                            box2.add(t);
                    
                            t = new ModulationInput(mod, i * 2 + 1, this);
                            box2.add(t);
                            box.add(box2);
                        }
                
                    // Add Release and Reset
                    Box box2 = new Box(BoxLayout.X_AXIS);
                    box2.add(new ModulationInput(mod, MOD_GATE_TR, this));
                    box2.add(new ModulationInput(mod, MOD_REL_TR, this));
                    box.add(box2);
                                
                    for(int i = 0; i < mod.getNumOptions(); i++)
                        {
                            box.add(new OptionsChooser(mod, i));
                        }

 
                    box2 = new Box(BoxLayout.X_AXIS);
                    box2.add(Box.createGlue());
                
                    JPanel pan = new JPanel();
                    pan.setLayout(new BorderLayout());
                    pan.add(box2, BorderLayout.EAST);
                    box.add(pan);

                    box.add(Strut.makeVerticalStrut(5));

                    box2 = new Box(BoxLayout.X_AXIS);
                    box2.add(Box.createGlue());

                    Box box3 = new Box(BoxLayout.Y_AXIS);

                    ModulationOutput mo = new ModulationOutput(mod, OUT_PRE_1, this);
                    mo.setTitleText(" Pre 1", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_SUS_1, this);
                    mo.setTitleText(" Sus 1", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_REL_1, this);
                    mo.setTitleText(" Rel 1", false);
                    box3.add(mo);


                    box2.add(box3);
                    box3 = new Box(BoxLayout.Y_AXIS);

                    mo = new ModulationOutput(mod, OUT_PRE_2, this);
                    mo.setTitleText(" 2", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_SUS_2, this);
                    mo.setTitleText(" 2", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_REL_2, this);
                    mo.setTitleText(" 2", false);
                    box3.add(mo);

                    box2.add(box3);
                    box3 = new Box(BoxLayout.Y_AXIS);

                    mo = new ModulationOutput(mod, OUT_PRE_3, this);
                    mo.setTitleText(" 3", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_SUS_3, this);
                    mo.setTitleText(" 3", false);
                    box3.add(mo);
                    box3.add(Strut.makeVerticalStrut(5));

                    mo = new ModulationOutput(mod, OUT_DONE, this);
                    mo.setTitleText(" E", false);
                    box3.add(mo);

                    box2.add(box3);

                    pan = new JPanel();
                    pan.setLayout(new BorderLayout());
                    pan.add(box2, BorderLayout.EAST);
                    box.add(pan);

                    return box;
                }
        };
    }



    public static final String[] MOD_NAMES = new String[]
        {
            "Pre 1", "Level 1",
            "Pre 2", "Level 2",
            "Pre 3", "Level 3", 
            "Sus 1", "Level 4", 
            "Sus 2", "Level 5", 
            "Sus 3", "Level 6",
            "Rel 1", "Level 7",
            "Rel 2", "Level 8",
            "On Tr", "Off Tr", "Start"
        };

    //// SERIALIZATION STUFF
    public String getKeyForModulation(int input)
    {
        return MOD_NAMES[input];
    }
}

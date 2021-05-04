// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;

/**
   A Modulation which implements a simple AHR (Attack Hold Release) envelope.
   AHR is an elaboration of the AR (or AD) envelope common on many synths.   You get to specify
   the attack time and attack level, the hold time, and the decay/release time.  The envelope always
   starts from and ends at zero.  There is no sustain: this envelope goes up, holds, and goes down.
   However there still is a one-shot option, which causes it to do this even if you have released.
   You can specify the attack and decay/release curves independently of one another.  You can also
   specify MIDI sync.
*/


public class AHR extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    //
    // ATTCK  HOLD REL  DONE
    // +-----+---+-----+----+
    // |     |   |     |    |
    // |     a---a     |    |
    // |    /|   |\    |    |
    // |   / |   | \   |    |
    // |  /  |   |  \  |    |
    // | /   |   |   \ |    |
    // |/    |   |    \|    |
    // l     |   |     r----r
    // +-----+---+-----+----+
    // |         |
    // |         HOLD DONE / RELEASE EVENT       
    // |                     
    // GATE/RESET EVENT 
    //
    // l, a, d, r: target levels for initial, delay, attack, decay [sustain], release.
    // L, A, D, R: rates for delay, attack, decay, release.
    //
    // On RELEASE, the modulation level starts at the CURRENT value.
    // On GATE, the modulation level starts at the CURRENT value.
    // On RESET, this current value is reset to the RELEASE LEVEL.
    //
    // Modulation values for time map to actual time as follows.
    //
    // 0.0  = 0.000 seconds
    // 0.1  = 0.011 seconds
    // 0.2  = 0.025 seconds
    // 0.3  = 0.042 seconds 
    // 0.4  = 0.066 seconds
    // 0.5  = 0.098 seconds
    // 0.6  = 0.146 seconds
    // 0.7  = 0.226 seconds
    // 0.8  = 0.381 seconds
    // 0.9  = 0.817 seconds
    // 0.92 = 1.021 seconds
    // 0.94 = 1.341 seconds
    // 0.96 = 1.916 seconds
    // 0.98 = 3.256 seconds
    // 1.00 = 9.900 seconds
    //
    // We also provide three more time curves: x^2, x^4, and x^8, plus JUMP
        
    // states
        
    static final int ATTACK = 0;
    static final int HOLD = 1;
    static final int RELEASE = 2;
    static final int DONE = 3;
        
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
        
    public static final int MOD_RELEASE_LEVEL = 0;
    public static final int MOD_ATTACK_TIME = 1;
    public static final int MOD_ATTACK_LEVEL = 2;
    public static final int MOD_HOLD_TIME = 3;
    public static final int MOD_RELEASE_TIME = 4;
    public static final int MOD_GATE_TR = 5;
    public static final int MOD_REL_TR = 6;
        
    public static final int OUT_MOD = 0;
    public static final int OUT_ATTACK = 1;
    public static final int OUT_HOLD = 2;
    public static final int OUT_RELEASE = 3;
    public static final int OUT_DONE = 4;
    
    double[] level = new double[4];
    double[] time = new double[4];          // not all of these slots will be used
    double start;
    double interval;
    int state = DONE;

    int attackCurve;
    int releaseCurve;
    boolean oneshot;        
    boolean sync;
    boolean ramp;
        
    public boolean getSync() { return sync; }
    public void setSync(boolean val) { sync = val; }
    public boolean getOneShot() { return oneshot; }
    public void setOneShot(boolean val) { oneshot = val; }
    public int getAttackCurve() { return attackCurve; }
    public void setAttackCurve(int val) { attackCurve = val; }
    public int getReleaseCurve() { return releaseCurve; }
    public void setReleaseCurve(int val) { releaseCurve = val; }
    public boolean getRamp() { return ramp; }
    public void setRamp(boolean val) { ramp = val; }

    public static final int OPTION_ATTACK_CURVE = 0;
    public static final int OPTION_RELEASE_CURVE = 1;
    public static final int OPTION_ONE_SHOT = 2;
    public static final int OPTION_SYNC = 3;
    public static final int OPTION_RAMP = 4;

    public Object clone()
        {
        AHR obj = (AHR)(super.clone());
        obj.level = (double[])(obj.level.clone());
        obj.time = (double[])(obj.time.clone());
        return obj;
        }
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_ATTACK_CURVE: return getAttackCurve();
            case OPTION_RELEASE_CURVE: return getReleaseCurve();
            case OPTION_ONE_SHOT: return getOneShot() ? 1 : 0;
            case OPTION_SYNC: return getSync() ? 1 : 0;
            case OPTION_RAMP: return getRamp() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_ATTACK_CURVE: setAttackCurve(value); return;
            case OPTION_RELEASE_CURVE: setReleaseCurve(value); return;
            case OPTION_ONE_SHOT: setOneShot(value != 0); return;
            case OPTION_SYNC: setSync(value != 0); return;
            case OPTION_RAMP: setRamp(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public AHR(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.HALF, Constant.ONE, Constant.ZERO, Constant.HALF, Constant.ZERO, Constant.ZERO }, 
            new String[] { "Start Level", "Attack Time", "Attack Level", "Hold Time",  "Release Time", "On Tr", "Off Tr" });
        defineOptions(new String[] { "Attack Curve", "Release Curve", "One Shot",  "MIDI Sync", "Ramp" }, 
            new String[][] { { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step", "x^2, 8", "x^4, 16", "x^8, 32", "Inv x^2", "Inv x^4", "Inv x^8"  }, 
                { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step", "x^2, 8", "x^4, 16", "x^8, 32", "Inv x^2", "Inv x^4", "Inv x^8" },
                { "One Shot" }, { "Gate Reset" }, { "MIDI Sync" } } );
        defineModulationOutputs(new String[] { "Mod", "A", "H", "R", "E" }); 
        this.oneshot = false;
        setModulationOutput(0, 0);  
        }
        
    public void gate()
        {
        super.gate();
        if (isModulationConstant(MOD_GATE_TR))
            doGate();
        }
        
    void doGate()
        {
        time[ATTACK] = modulate(MOD_ATTACK_TIME);
        level[ATTACK] = modulate(MOD_ATTACK_LEVEL);
        time[HOLD] = modulate(MOD_HOLD_TIME);
        level[HOLD] = level[ATTACK];
        time[RELEASE] = modulate(MOD_RELEASE_TIME);
        level[RELEASE] = modulate(MOD_RELEASE_LEVEL);
        time[DONE] = (0); // doesn't matter
        level[DONE] = level[RELEASE];

        state = ATTACK;
        start = getSyncTick(sync);
        interval = toTicks(time[ATTACK]);

        scheduleTrigger(T_ATTACK);
        }
        
    public void release()
        {
        super.release();
        if (isModulationConstant(MOD_REL_TR))
            doRelease();
        }
    
    public static final int T_ATTACK = 1;
    public static final int T_HOLD = 2;
    public static final int T_RELEASE = 3;
    public static final int T_DONE = 4;
    
    int scheduledTriggers = 0;
    void scheduleTrigger(int val)
        {
        if (val == T_ATTACK)
            {
            scheduledTriggers |= 1;
            }
        if (val == T_HOLD)
            {
            scheduledTriggers |= 2;
            }
        else if (val == T_RELEASE)
            {
            scheduledTriggers |= 4;
            }
        else if (val == T_DONE)
            {
            scheduledTriggers |= 8;
            }
        }
    
    void doRelease()
        {
        if (oneshot) return;
        
        if (state != RELEASE && state != DONE)
            {
            scheduleTrigger(T_RELEASE);
            }
        
        if (state == DONE)
            {
            // do nothing
            }
        else if (ramp)
            {
            state = HOLD;
            start = getSyncTick(sync);
            interval = toTicks(time[HOLD]);
            level[ATTACK] = level[HOLD] = getModulationOutput(0);  // so we decrease from there during release
            }
        else
            {
            state = RELEASE;
            start = getSyncTick(sync);
            interval = toTicks(time[RELEASE]);
            level[HOLD] = getModulationOutput(0);  // so we decrease from there during release
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
                updateTrigger(OUT_ATTACK);
                }
            if ((scheduledTriggers & 2) == 2)
                {
                updateTrigger(OUT_HOLD);
                updateTrigger(OUT_MOD);
                }
            if ((scheduledTriggers & 4) == 4)
                {
                updateTrigger(OUT_RELEASE);
                updateTrigger(OUT_MOD);
                }
            if ((scheduledTriggers & 8) == 8)
                {
                updateTrigger(OUT_DONE);
                updateTrigger(OUT_MOD);
                }
            scheduledTriggers = 0;
            }

        long tick = getSyncTick(sync);

        // What state are we in?
                        
        if (tick < start) // uh oh, probably switched to MIDI Sync
            {
            start = tick;
            }
    
        // if we're in a sticky state, just return the level
        if (state == DONE)
            {
            setModulationOutput(0, level[DONE]);
            return;
            }
    
        // Do we need to transition to a new state?
        while (tick >= start + interval)
            {
            if (ramp && state == HOLD)
                {
                // don't transition
                setModulationOutput(0, level[HOLD]);
                return;
                }
                           
            state++;
            if (state == ATTACK)                                // this can't happen
                {
                updateTrigger(OUT_ATTACK);
                }
            else if (state == HOLD)
                {
                updateTrigger(OUT_MOD);
                updateTrigger(OUT_HOLD);
                }
            else if (state == RELEASE)
                {
                updateTrigger(OUT_MOD);
                updateTrigger(OUT_RELEASE);
                }
            else if (state == DONE)
                {
                updateTrigger(OUT_MOD);
                updateTrigger(OUT_DONE);
                }
                 
            // try sticky again
            if (state == DONE)
                {
                setModulationOutput(0, level[DONE]);
                return;
                }

            // update the state
            start = start + interval;
            interval = toTicks(time[state]);
            }
  
        // Where are we in the state?  We compute only for delay, attack, decay, and release
        
        double firstLevel = level[DONE];                // initially, for state = delay
        if (state > ATTACK)
            firstLevel = level[state - 1];  

        double alpha = (tick - start) / interval;
                                
        int c = CURVE_LINEAR;
                
        if (state == ATTACK)
            c = attackCurve;
        else if (state == RELEASE)
            c = releaseCurve;

        switch(c)
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
            if (modulation == MOD_ATTACK_TIME || modulation == MOD_HOLD_TIME || modulation == MOD_RELEASE_TIME)  // it's a time
                {
                return String.format("%.4f" , modToLongRate(value)) + " Sec";
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
        
        
    public ModulePanel getPanel()
        {
        return new ModulePanel(AHR.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new ModulationOutput(mod, 0, this));

                for(int i = 0; i < mod.getNumModulations(); i++)
                    {
                    ModulationInput t;
                    if (i == MOD_ATTACK_TIME || i == MOD_HOLD_TIME || i == MOD_RELEASE_TIME)
                        {
                        t = new ModulationInput(mod, i, this)
                            {
                            public String[] getOptions() { return MidiClock.CLOCK_NAMES; }
                            public double convert(int elt) 
                                {
                                return MIDI_CLOCK_LONG_MOD_RATES[elt];
                                }
                            };
                        }
                    else 
                        {
                        t = new ModulationInput(mod, i, this);
                        }
                    box.add(t);
                    }

                for(int i = 0; i < mod.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(mod, i));
                    }
                    
                box.add(Strut.makeVerticalStrut(5));
                Box box2 = new Box(BoxLayout.X_AXIS);
                
                Box box3 = new Box(BoxLayout.Y_AXIS);
                ModulationOutput mo = new ModulationOutput(mod, OUT_ATTACK, this);
                mo.setTitleText(" A", false);
                box3.add(mo);

                box3.add(Strut.makeVerticalStrut(5));
                
                mo = new ModulationOutput(mod, OUT_RELEASE, this);
                mo.setTitleText(" R", false);
                box3.add(mo);

                box2.add(box3);
                box3 = new Box(BoxLayout.Y_AXIS);
                                
                mo = new ModulationOutput(mod, OUT_HOLD, this);
                mo.setTitleText(" H", false);
                box3.add(mo);
                box3.add(Strut.makeVerticalStrut(5));
                
                mo = new ModulationOutput(mod, OUT_DONE, this);
                mo.setTitleText(" E", false);
                box3.add(mo);

                box2.add(box3);
                
                JPanel pan = new JPanel();
                pan.setLayout(new BorderLayout());
                pan.add(box2, BorderLayout.EAST);

                box.add(pan);
                return box;
                }
            };
        }
    }

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
        
    public static final int MOD_RELEASE_LEVEL = 0;
    public static final int MOD_ATTACK_TIME = 1;
    public static final int MOD_ATTACK_LEVEL = 2;
    public static final int MOD_HOLD_TIME = 3;
    public static final int MOD_RELEASE_TIME = 4;
    public static final int MOD_GATE_TR = 5;
    public static final int MOD_REL_TR = 6;
        
    double[] level = new double[4];
    double[] time = new double[4];          // not all of these slots will be used
    double start;
    double interval;
    int state;

    int attackCurve;
    int releaseCurve;
    boolean oneshot;        
    boolean sync;
    boolean ramp;
        
    public boolean getSync() { return sync; }
    public void setSync(boolean val) { sync = val; }
    public void setOneShot(boolean val) { oneshot = val; }
    public boolean getOneShot() { return oneshot; }
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
            new String[][] { { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step" } , 
                { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step" }, 
                { "One Shot" }, { "Gate Reset" }, { "MIDI Sync" } } );
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
        resetTrigger(0);
        }
        
    public void release()
        {
        super.release();
        if (isModulationConstant(MOD_REL_TR))
            doRelease();
        }
    
    void doRelease()
        {
        if (oneshot) return;
        
        if (ramp)
            {
            state = HOLD;
            start = getSyncTick(sync);
            interval = toTicks(time[HOLD]);
            level[ATTACK] = level[HOLD] = getModulationOutput(0);  // so we decrease from there during release
            updateTrigger(0);
            }
        else
            {
            state = RELEASE;
            start = getSyncTick(sync);
            interval = toTicks(time[RELEASE]);
            level[HOLD] = getModulationOutput(0);  // so we decrease from there during release
            updateTrigger(0);
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
        else 
            {
            if (isTriggered(MOD_REL_TR))
                {
                doRelease();
                }
            }
            
        long tick = getSyncTick(sync);

        // What state are we in?
                        
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
            updateTrigger(0);
                
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
                    
                return box;
                }
            };
        }

    public String[] getOptionHelp() 
        { 
        return new String[]
            { 
            "Function for rate of attack, ranging from a straight line (linear) to very extreme (x^32).  x^4 is close to exponential tradition.  Step is no change at all until the very end, then a sudden jump.",
            "Function for rate of release, ranging from a straight line (linear) to very extreme (x^32).  x^4 is close to exponential tradition.  Step is no change at all until the very end, then a sudden jump.",
            "Envelope ignores note-off, and continues on its own.",
            "Envelope time is synced to MIDI clock.  Double-click on a time dial and MIDI note speeds will appear.",
            "Envelope ignores hold and release time: it just goes from start level to attack level, then holds there.",
            };
        }

    public String[] getModulationHelp() 
        { 
        return new String[]
            { 
            "Level for starting or ending the envelope.  If Ramping, then this is just the level for starting the envelope.",
            "Time to reach the attack level",
            "Attack level value.  If a release occurs early, this value may never be reached.",
            "Time to hold at the attack level after reaching it",
            "Time to release to back to the start level",
            "Alternative trigger for (re)starting the envelope.  If nothing is connected here, the envelope is triggered by a NOTE ON.  If connected, then a NOTE ON does nothing.",
            "Alternative trigger for releasing the envelope.  If nothing is connected here, the release is triggered by a NOTE OFF.  If connected, then a NOTE OFF does nothing.",
            };
        }

    public String[] getModulationOutputHelp() 
        { 
        return new String[]
            { 
            "Current AHR value.",
            };
        }

    public String[] getUnitInputHelp() 
        {
        return null; 
        }

    public String[] getUnitOutputHelp() 
        { 
        return null;
        }
    }

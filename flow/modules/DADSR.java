// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;

/**
   A Modulation which implements a classic ADSR envelope with an additional Delay at the front.
   DADSR is common in many synths, such as the Oberheim Matrix series.  You get to specify the
   delay level and time (this is a time-based envelope, not a rate-based envelope), the attack
   level and time, the decay time and level (which is also the sustain level), and finally the
   release time and level.  There are four curves offered linear (x) through x^8, none of which
   is exponential :-) but several are reasonable approximations.  The envelope can be ONE SHOT,
   which basically means that the sustain stage is ignored -- once we reach the sustain level
   we immediately begin the release stage.  We can also specify whether the envelope should 
   reset on gate, or continue regardless of a gate event.
*/


public class DADSR extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    //
    // DELAY ATTK DCY   SUS  REL DONE
    // +---+-----a---+-------+-+------+
    // |   |    /|\  |       | |      |
    // |   |   / | \ |       | r------r ...
    // |   |  /  |  \|       |/|      |
    // |   | /   |   d-------d |      |
    // |   |/    |   |       | |      |
    // l---l     |   |       | |      |
    // |   |     |   |       | |      |
    // +---+-----+---+-------+-+------+
    // |  L   A    D         |R
    // |                     |
    // |                     |
    // GATE/RESET EVENT      RELEASE EVENT
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
    // We also provide three more time curves: x^2, x^4, and x^8 to a
        
    // states
        
    static final int DELAY = 0;
    static final int ATTACK = 1;
    static final int DECAY = 2;
    static final int SUSTAIN = 3;
    static final int RELEASE = 4;
    static final int DONE = 5;
        
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

    public static final int MOD_DELAY_TIME = 0;
    public static final int MOD_DELAY_LEVEL = 1;
    public static final int MOD_ATTACK_TIME = 2;
    public static final int MOD_ATTACK_LEVEL = 3;
    public static final int MOD_DECAY_TIME = 4;
    public static final int MOD_DECAY_LEVEL = 5;
    public static final int MOD_RELEASE_TIME = 6;
    public static final int MOD_RELEASE_LEVEL = 7;
    public static final int MOD_GATE_TR = 8;
    public static final int MOD_REL_TR = 9;
        
    double[] level = new double[6];         // not all of these slots will be used
    double[] time = new double[6];          // not all of these slots will be used
    double start;
    double interval;
    int state;
    boolean released;

    int curve;
    boolean oneshot = false;        
    boolean sync;
    boolean gateReset = false;
    boolean quickRelease = false;
        
    public boolean getSync() { return sync; }
    public void setSync(boolean val) { sync = val; }
    public void setOneShot(boolean val) { oneshot = val; }
    public boolean getOneShot() { return oneshot; }
    public int getCurve() { return curve; }
    public void setCurve(int val) { curve = val; }
    public boolean getGateReset() { return gateReset; }
    public void setGateReset(boolean val) { gateReset = val; }
    public void setQuickRelease(boolean val) { quickRelease = val; }
    public boolean getQuickRelease() { return quickRelease; }

    public static final int OPTION_CURVE = 0;
    public static final int OPTION_ONE_SHOT = 1;
    public static final int OPTION_GATE_RESET = 2;
    public static final int OPTION_SYNC = 3;
    public static final int OPTION_QUICK_RELEASE = 4;

    public Object clone()
        {
        DADSR obj = (DADSR)(super.clone());
        obj.level = (double[])(obj.level.clone());
        obj.time = (double[])(obj.time.clone());
        return obj;
        }
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_CURVE: return getCurve();
            case OPTION_ONE_SHOT: return getOneShot() ? 1 : 0;
            case OPTION_GATE_RESET: return getGateReset() ? 1 : 0;
            case OPTION_SYNC: return getSync() ? 1 : 0;
            case OPTION_QUICK_RELEASE: return getQuickRelease() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_CURVE: setCurve(value); return;
            case OPTION_ONE_SHOT: setOneShot(value != 0); return;
            case OPTION_GATE_RESET: setGateReset(value != 0); return;
            case OPTION_SYNC: setSync(value != 0); return;
            case OPTION_QUICK_RELEASE: setQuickRelease(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public DADSR(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.HALF, Constant.ONE, Constant.ZERO, Constant.ONE, Constant.HALF, Constant.ZERO, Constant.ZERO, Constant.ZERO }, 
            new String[] {  "Delay Time", "Delay Level", "Attack Time", "Attack Level", "Decay Time", "Sustain Level", "Release Time", "Release Level", "On Tr", "Off Tr" });
        defineOptions(new String[] { "Curve", "One Shot", "Gate Reset", "MIDI Sync", "Fast Release" }, 
            new String[][] { { "Linear", "x^2", "x^4", "x^8", "x^16", "x^32", "Step", "x^2, 8", "x^4, 16", "x^8, 32", "Inv x^2", "Inv x^4", "Inv x^8"  }, 
                { "One Shot" }, { "Gate Reset" }, { "MIDI Sync" }, { "No Release" } } );
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
        time[DELAY] = (modulate(MOD_DELAY_TIME));
        level[DELAY] = modulate(MOD_DELAY_LEVEL);
        time[ATTACK] = (modulate(MOD_ATTACK_TIME));
        level[ATTACK] = modulate(MOD_ATTACK_LEVEL);
        time[DECAY] = (modulate(MOD_DECAY_TIME));
        level[DECAY] = modulate(MOD_DECAY_LEVEL);
        time[SUSTAIN] = (0);  // doesn't matter
        level[SUSTAIN] = level[DECAY];  // really doesn't matter actually, release modifies it
        time[RELEASE] = (modulate(MOD_RELEASE_TIME));
        level[RELEASE] = modulate(MOD_RELEASE_LEVEL);
        time[DONE] = (0); // doesn't matter

        if (gateReset)
            level[DONE] = level[RELEASE];  // so we reset to the canonocal default
        else
            level[DONE] = getModulationOutput(0); // so we change from there when we starting
                        
        state = DELAY;
        start = getSyncTick(sync);
        interval = toTicks(time[DELAY]);
        resetTrigger(0);
        released = false;
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
        
        if (state == DECAY && !quickRelease)
            {
            released = true;
            return;
            }
                
        state = RELEASE;
        start = getSyncTick(sync);
        interval = toTicks(time[RELEASE]);
        level[SUSTAIN] = getModulationOutput(0);  // so we decrease from there during release
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

        // need to reset level[DONE]        
        if (state > DELAY)
            {
            level[DONE] = level[RELEASE];
            }
                
        // What state are we in?
                        
        // if we're in a sticky state, just return the level
        if (state == DONE)
            {
            setModulationOutput(0, level[DONE]);
            return;
            }
            
        if (!oneshot && !released && state == SUSTAIN)
            {
            setModulationOutput(0, level[SUSTAIN]);
            return;
            }
        
        
        // Do we need to transition to a new state?
        while (tick >= start + interval)
            {
            state++;
            updateTrigger(0);
                 
            // try sticky again
            if (state == DONE)
                {
                setModulationOutput(0, level[DONE]);
                return;
                }

            if (!oneshot && !released && state == SUSTAIN)
                {
                setModulationOutput(0, level[SUSTAIN]);
                return;
                }

            // update the state
            start = start + interval;
            interval = toTicks(time[state]);
            }
        
      
        // Where are we in the state?  We compute only for delay, attack, decay, and release
        
        double firstLevel = level[DONE];                // initially, for state = delay
        if (state > DELAY)
            firstLevel = level[state - 1];  
                

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
            if (modulation % 2 == 0)  // it's a time
                {
                return String.format("%.4f" , modToLongRate(value)) + " Sec";
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
        
        
    public ModulePanel getPanel()
        {
        return new ModulePanel(DADSR.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new ModulationOutput(mod, 0, this));

                for(int i = 0; i < mod.getNumModulations(); i++)
                    {
                    ModulationInput t;
                    if (i == MOD_DELAY_TIME || i == MOD_ATTACK_TIME || i == MOD_DECAY_TIME || i == MOD_RELEASE_TIME)
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
    }

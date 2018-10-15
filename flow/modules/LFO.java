// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;
import flow.gui.*;
import javax.swing.*;

/**
   A Modulation which provides a standard low frequency oscillator.   The LFO can be one of six different waveforms:
   sine, triangle, square, saw (up), random, and random sample and hold.  The LFO can be set to free-running or to reset
   on gate.  You can also invert the wave.
   
   <p>There are several incoming modulations which affect the LFO.  The RATE defines the LFO rate.  The INITIAL STATE
   <p>Defines where in the wave we start: for example, if the initial state is 0.25, then on gate down in a non-free
   LFO we're essentially doing a cosine rather than a sine.  You can also define the SCALE: the amplitude of the LFO
   wave, and also SHIFT it vertically. 
   
   <P>The scale affects random and random sample and hold differently than the others.  Random repeatedly picks a new
   target Y position at each wave start, and slowly interpolates to that position until it is reached at wave end.
   Random sample and hold simply goes to the new position and holds there until wave end.  The scale effects the degree
   of randomness in the Y position -- the variance from the previous Y position.  Additionally, the SEED modulation 
   determines the random pattern -- this pattern is repeated each gate in a non-free random or random sample and hold wave
   so your sounds can be consistent.  Alternatively, if SEED is 0, then the pattern is truly random each gate.
   SEED has no effect on non-random waves.
*/


public class LFO extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    public static final int SIN = 0;
    public static final int TRIANGLE = 1;
    public static final int SQUARE = 2;
    public static final int SAW_UP = 3;
    public static final int RANDOM = 4;
    public static final int RANDOM_SAMPLE_AND_HOLD = 5;

    public static final String[] TYPE_NAMES = { "Sine", "Triangle", "Square", "Saw Up", "Random", "Rnd S&H" };
        
    public Random random = null;
    int type;
    transient double randomPos;
    transient double oldRandomPos;
    transient int firstTick = 0;
    transient int lastTick = 0;
    transient double lastRate = Double.NaN;
    transient double bias = 0;
    transient double state;
    double lastState;
    boolean free;
    boolean invert;
    boolean halfTrigger;
    boolean sync;
    boolean linear;
        
    public boolean getSync() { return sync; }
    public void setSync(boolean val) { sync = val; }
    public boolean getLinear() { return linear; }
    public void setLinear(boolean val) { linear = val; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public boolean getFree() { return free; }
    public void setFree(boolean free) { this.free = free; }
    public boolean getInvert() { return invert; }
    public void setInvert(boolean invert) { this.invert = invert; }
    public boolean getHalfTrigger() { return halfTrigger; }
    public void setHalfTrigger(boolean val) { this.halfTrigger = val; }

    public static final int OPTION_TYPE = 0;
    public static final int OPTION_FREE = 1;
    public static final int OPTION_INVERT= 2;
    public static final int OPTION_HALF_TRIGGER = 3;
    public static final int OPTION_LINEAR = 4;
    public static final int OPTION_SYNC = 5;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            case OPTION_FREE: return getFree() ? 1 : 0;
            case OPTION_INVERT: return getInvert() ? 1 : 0;
            case OPTION_HALF_TRIGGER: return getHalfTrigger() ? 1 : 0;
            case OPTION_LINEAR: return getLinear() ? 1 : 0;
            case OPTION_SYNC: return getSync() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            case OPTION_FREE: setFree(value != 0); return;
            case OPTION_INVERT: setInvert(value != 0); return;
            case OPTION_HALF_TRIGGER: setHalfTrigger(value != 0); return;
            case OPTION_LINEAR: setLinear(value != 0); return;
            case OPTION_SYNC: setSync(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }       
        
    void initializeRandom()     
        {
        // first reseed
        double mod = modulate(4);
                
        if (mod == 0)
            {
            random = null;
            }
        else
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }

        // next determine the first oldRandomPos and randomPos          
        oldRandomPos = 0;
        double scale = modulate(2);
        double shift = modulate(3);
        modulateRandom(scale, shift, true, false);  // set a new random pos
        }
        
    void resetLFO()
        {
        initializeRandom();
        firstTick = getSyncTick(sync);
        lastTick = firstTick;
        lastRate = Double.NaN;
        lastState = 0;
        }
        
    public void gate() 
        { 
        super.gate();
                
        if (!free)
            {
            resetLFO();
            }
        }

    public void restart()
        {
        super.restart();
        if (sync)
            {
            resetLFO();
            }
        } 

    public void reset() 
        { 
        super.reset();
        
        resetLFO();
        }

                
    public LFO(Sound sound)
        {
        super(sound);
        // Modulation 0: Rate.  0 = Sampling rate / 2.  0.5 = one second?  1 = 1/(sampling rate / 2)?
        // Modulation 1: Initial State
        // Modulation 2: Scale
        // Modulation 3: Shift
        // Modulation 4: Random Seed
        defineModulations(new Constant[] { Constant.HALF, Constant.ZERO, Constant.ONE, Constant.HALF, Constant.ONE, Constant.ZERO }, new String[] { "Rate", "Phase", "Scale", "Shift", "Seed", "Reset" });
        defineOptions(new String[] { "Type", "Free", "Invert", "Half Trigger", "Linear Rate", "MIDI Sync"}, new String[][] { TYPE_NAMES, { "Free" }, { "Invert" }, { "Half Trigger" }, { "Linear Rate" }, { "MIDI Sync" } } );

        this.type = TRIANGLE;
        free = true;
        }
        
    static final int MAX_TRIES = 20;
    double modulateRandom(double scale, double shift, boolean wrapped, boolean sampleAndHold)
        {
        // do I update?
        if (wrapped)
            {
            oldRandomPos = randomPos;
            
            Random rand = (random == null ? getSound().getRandom() : random);
            double newPos = 0;
            int i = 0;
            for(i = 0; i < MAX_TRIES; i++)
                {
                newPos = (rand.nextDouble() * 2.0 - 1.0) * scale + randomPos;
                if (newPos + shift - 0.5 <= 1.0 && newPos + shift - 0.5 >= 0.0) break; 
                // note we could go into an infinite loop here if the user changes the scale
                // and shift before we have created a new valid randomPos!
                }
            if (i == MAX_TRIES)   // we failed, bound to a legal value and that's that
                {
                newPos = Math.max(Math.min(newPos + shift - 0.5, 1.0), 0.0);
                }
            randomPos = newPos;
            return oldRandomPos;
            }
        else if (sampleAndHold)
            {
            return oldRandomPos;
            }
        else
            {
            return state * randomPos + (1 - state) * oldRandomPos;
            }
        }
        
    double modulateSquare(double scale)
        {
        if (state < 0.5)
            {
            return 0.0
                * scale + 0.5 - (scale * 0.5);
            }
        else
            {
            return 1.0
                * scale + 0.5 - (scale * 0.5);
            }
        }
        
        
    double modulateTriangle(double scale)
        {
        if (state < 0.5)
            {
            return (state * 2) 
                * scale + 0.5 - (scale * 0.5);
            }
        else
            {
            return (1.0 - ((state - 0.5) * 2)) 
                * scale + 0.5 - (scale * 0.5);
            }
        }
        
    double modulateSaw(double scale)
        {
        return state
            * scale + 0.5 - (scale * 0.5);
        }

    double modulateSin(double scale)
        {
        // Yes, it says Cos, not Sin
        return (1.0 - ((1.0 + Utility.fastCos(state * Math.PI * 2)) * 0.5))
            * scale + 0.5 - (scale * 0.5);
        }


//int count = 0;
//int countTick = 0;

/** Our logic is:

    STATE = 0...1   initially 0
    BIAS = +- 

    If we started
    POS = 0....             wave position at current rate
    BIAS = 0
    STATE = POS + BIAS, MOD 1

    If rate is unchanged
    POS = 0....             wave position at current rate
    STATE = POS + BIAS, MOD 1

    But if new rate is different from old rate:

    POS = 0....             wave position at the new rate
    PREVPOS = 0.... previous wave position at the new rate

    What I want is: STATE = OLDSTATE + (POS - PREVPOS)                      [MOD 1]
    Now STATE = POS + BIAS                                                                  [MOD 1]
    So POS + BIAS = OLDSTATE + (POS - PREVPOS)                              [MOD 1]
    So BIAS = OLDSTATE + (POS - PREVPOS) - POS                              [MOD 1]
    BIAS = OLDSTATE - PREVPOS                                                                       [MOD 1]
                
    STATE = POS + BIAS, MOD 1       
*/

    public void go()
        {
        super.go();
                
        if (isTriggered(5))
            {
            resetLFO();
            }
                
        double phase = modulate(1);  // get initial state
        double rate = modulate(0);
        if (!linear) rate = makeSensitive(rate);
        double scale = modulate(2);
        double shift = modulate(3);

        // Update the current state.  This is complicated if we changed the rate;  jumps in
        // the rate will cause eccentricities in the waveform because we're basing this on
        // the position of the wave from the start, not the relative change.
                
        int currentTick = getSyncTick(sync);
        
        // our time interval, stretched by [new] rate
        double pos = (modToRate(rate) * Output.INV_SAMPLING_RATE * (currentTick - firstTick));
        
        // Revise if we changed the rate
        if (rate != lastRate && (lastRate == lastRate))         // that is, lastRate has been computed, it's not NaN
            {
            // our time interval, stretched by new rate
            double lastPos = (modToRate(rate) * Output.INV_SAMPLING_RATE * (lastTick - firstTick));
            bias = state - lastPos;
            // bias could be negative I think, so we have to use floor
            bias = bias - Math.floor(bias);
            }
                                
        state = pos + bias + phase;
        state = state - (int) state;            // mod 1
        lastTick = currentTick;
        lastRate = rate;
                        
                                
        // Now we have the state.  Do we trigger?
        boolean wrapped = false;
        if (lastState > state)
            {
            updateTrigger(0);
            wrapped = true;
            }
        else if (halfTrigger && (lastState < 0.5 && state >= 0.5))
            {
            updateTrigger(0);
            }
        lastState = state;

        
        /*
          count++;
          Output o = sound.getOutput();
          if (count > 1000 && sound == o.getSoundUnsafe(0))
          {        
          System.err.println(sound + " " + (currentTick - countTick));
          countTick = currentTick;
          count = 0;      
          }
        */
                        
        // Okay here we go
        double output = 0;
        switch (type)
            {
            case SIN: output = modulateSin(scale); break;
            case TRIANGLE: output = modulateTriangle(scale); break;
            case SQUARE: output = modulateSquare(scale); break;
            case SAW_UP: output = modulateSaw(scale); break;
            case RANDOM: output = modulateRandom(scale, shift, wrapped, false); break;
            case RANDOM_SAMPLE_AND_HOLD: output = modulateRandom(scale, shift, wrapped, true); break;
            }
                
        output = output + shift - 0.5;
        
        if (output > 1) output = 1;
        if (output < 0) output = 0;
        if (invert) output = 1.0 - output;
        
        setModulationOutput(0, output);
        }
        
    public ModulePanel getPanel()
        {
        return new ModulePanel(LFO.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new ModulationOutput(mod, 0, this));

                ModulationInput rate = new ModulationInput(mod, 0, this)
                    {
                    public String[] getOptions() { return MidiClock.CLOCK_NAMES; }
                    public double convert(int elt) 
                        {
                        if (linear)
                            return MIDI_CLOCK_MOD_RATES[elt];
                        else
                            return Math.sqrt(MIDI_CLOCK_MOD_RATES[elt]);
                        }
                    };
                                        
                box.add(rate);
                for(int i = 1; i < mod.getNumModulations(); i++)
                    {
                    box.add(new ModulationInput(mod, i, this));
                    }

                for(int i = 0; i < mod.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(mod, i));
                    }
                    
                return box;
                }
            };
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 0)  // rate
                {
                if (!linear)
                    value = makeSensitive(value);
                double d =  1.0 / modToRate(value);
                if (d >= 10)
                    return String.format("%.4f", d) + " Sec";
                return String.format("%.5f", d) + " Sec";
                }
            else if (modulation == 3)  // shift
                {
                return String.format("%.4f" , value - 0.5);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
        
    }

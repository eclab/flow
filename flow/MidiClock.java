// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;


/**
   MidiClock is a class which provides a sync clock for modulations
   which otherwise rely on standard clocks.  The idea is to provide
   a different clock which ticks at a rate determined by the current
   MIDI clock.
        
   <p>When you allocate a MidiClock, it starts at the state
   WAITING_FOR_FIRST_PULSE, which means that it's not ticking yet.
   When, after repeated calls go go(), the MidiClock determines that
   MIDI CLOCK START has been called, it transitions to WAITING_FOR_FIRST_PULSE
   (and is still not running yet).
*/
        
public class MidiClock
    {
    Input input;
    
    public MidiClock(Input input)
        {
        this.input = input;
        clockState = CLOCK_STATE_INITIAL;
        pulses = 0;
        state = WAITING_FOR_START;
        }
        
    boolean syncing;
    /** Sets whether or not modules should sync to MIDI Clock (if they've been individually set to) */
    public void setSyncing(boolean val) { syncing = val; }
    /** Returns whether or not modules should sync to MIDI Clock (if they've been individually set to) */
    public boolean isSyncing() { return syncing; }  
    
    ////// MIDI CLOCK STATE MACHINE
    
    // The clockState is set to this to indicate that we never started the clock
    static final int CLOCK_STATE_INITIAL = 0;
    // The clockState is set to this to indicate that the clock is stopped/paused
    static final int CLOCK_STATE_PAUSED = 1;
    // The clockState is set to this to indicate that the clock is running
    static final int CLOCK_STATE_RUNNING = 2;

    int clockState = CLOCK_STATE_INITIAL;

    // The number of clock ticks which have occurred (24 ticks per quarter note)
    int pulses;

    // Set to 1 when the clock had a START.  go() will then increment it once before voices see it, so when it's incremented TWICE, we reset it.
    int clockStartTrigger;
    // Set to 1 when the clock had a PULSE.  go() will then increment it once before voices see it, so when it's incremented TWICE, we reset it.
    int clockPulseTrigger;

    /** Returns true if this tick the MIDI clock registered a START.
        This is reset on go(). */
    public boolean getClockStartTrigger() { return clockStartTrigger > 0; }

    /** Returns true if this tick the MIDI clock registered a PULSE.
        This is reset on go(). */
    public boolean getClockPulseTrigger() { return clockPulseTrigger > 0; }

    /** Returns the number of pulses since the last MIDI Clock Start. */
    public synchronized int getPulses()
        {
        return pulses; 
        }
        
    public synchronized int getClockState()
        {
        return clockState;
        }






    //// CLOCK TICKS ESTIMATE

    
    // We're going to imagine that "time" is 1024 ticks per pulse.
    // At 44100 ticks/sec / 918.75 ticks/pulse / 24 pulse/beat * 60 sec/min
    // This comes to exactly 120 BPM.
    public static final double TICKS_PER_PULSE = 918.75;
        
    public static final String[] CLOCK_NAMES = new String[]
    { "Triplet 1/64th", "Triplet 1/32th", "1/32th", "Triplet 1/16th", "1/16th", "Triplet Eighth", "Eighth",
      "Triplet Quarter", "Dotted Eighth", "Quarter", "Triplet Half", "Dotted Quarter", "Half", "Triplet Whole",
      "Dotted Half", "Whole", "Dotted Whole", "Double Whole" };
                        
    public static final int [] CLOCK_PULSES = new int[]
    { 1, 2, 3, 4, 6, 8, 12, 16, 18, 24, 32, 36, 48, 64, 72, 96, 144, 192 };
                
    
    public static final int WAITING_FOR_START = 0;
    public static final int WAITING_FOR_FIRST_PULSE = 1;
    public static final int WAITING_FOR_SECOND_PULSE = 2;
    public static final int GOING = 3;
    public static final int PAUSED = 4;
    public static final int UNPAUSED = 5;
    int state;


    double smoothedDiff;
    double currentPulseRealTick;
    double smoothedMidiTickTarget;
    

    static final double DIFF_ALPHA = 0.5;
    static final double MIDI_TICK_ALPHA = 0.1;
        
    volatile int syncTick = 0;
    int tick = 0;
        
    void syncTick()
        {
        tick = syncTick;
        }
        
    public int getTick()
        {
        return tick;
        }
        
    ///// CLOCK

    // Starts the clock
    synchronized void startClock()
        {
        pulses = 0;
        clockStartTrigger = 1;
        clockState = CLOCK_STATE_RUNNING;

        smoothedMidiTickTarget = 0;
        syncTick = (int)smoothedMidiTickTarget;
        state = WAITING_FOR_FIRST_PULSE;
        }

    // stops the clock
    synchronized void stopClock()
        {
        if (clockState == CLOCK_STATE_RUNNING)
            clockState = CLOCK_STATE_PAUSED;

        state = PAUSED;
        }

    // continues the clock
    synchronized void continueClock()
        {
        if (clockState == CLOCK_STATE_PAUSED)
            clockState = CLOCK_STATE_RUNNING;
                        
        // what to do here?
        state = UNPAUSED;
        }

    // pulses the clock
    synchronized void pulseClock()
        {
        int currentRealTick = input.getOutput().getTick();
                
        if (clockState == CLOCK_STATE_RUNNING)
            {
            pulses++;
            clockPulseTrigger = 1;
            }

        if (state == WAITING_FOR_FIRST_PULSE)
            {
            // we got our first pulse.  So we record the tick as the current pulse and also our start 
            state = WAITING_FOR_SECOND_PULSE;
            currentPulseRealTick = currentRealTick;
            }
        else if (state == WAITING_FOR_SECOND_PULSE)
            {
            // we got our second pulse.  So now we have a first delta
            smoothedDiff = (currentRealTick - currentPulseRealTick);
            currentPulseRealTick = currentRealTick;
            state = GOING;
            }
        else if (state == GOING)
            {
            smoothedDiff = (1 - DIFF_ALPHA) * smoothedDiff + DIFF_ALPHA * (currentRealTick - currentPulseRealTick);
            currentPulseRealTick = currentRealTick;
            }
        else if (state == UNPAUSED)
            {
            // we need to revise the ticks to reflect the jump to the new current tick value
                        
            currentPulseRealTick = currentRealTick;
            state = GOING;
            }
        //update();
        }


    public void go()
        {
        if (clockPulseTrigger > 0) clockPulseTrigger++;
        if (clockStartTrigger > 0) clockStartTrigger++;
        if (clockPulseTrigger > 2) clockPulseTrigger = 0;
        if (clockStartTrigger > 2) clockStartTrigger = 0;
        update();
        }
        
        
    public static final double BIG_CHANGE = 1500;
    // this is called either from go() via Input.java, or on receiving pulses.
    synchronized void update()
        {        
        if (state == GOING)
            {
            int currentRealTick = input.getOutput().getTick();

            double tickDelta = (currentRealTick - currentPulseRealTick) / smoothedDiff;
                        
            // This is our final target estimate of where we should be (in Midi ticks)
            double midiTickTarget = (getPulses() + tickDelta) * TICKS_PER_PULSE;
            if (midiTickTarget > smoothedMidiTickTarget) 
                smoothedMidiTickTarget = (1 - MIDI_TICK_ALPHA) * smoothedMidiTickTarget + MIDI_TICK_ALPHA * midiTickTarget;
            else if (midiTickTarget < smoothedMidiTickTarget - BIG_CHANGE) // big drop in value
                {
                System.err.println("WARNING(MidiClock.java): Clock locked to " + (smoothedMidiTickTarget - midiTickTarget));
                // lock it
                smoothedMidiTickTarget = midiTickTarget;
                }
            syncTick = (int)smoothedMidiTickTarget;
            }
        else
            {
            return; // even for WAITING_FOR_SECOND_PULSE for now
            }
        }
        
    }

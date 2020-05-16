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
        //System.err.println("STATE: " + state);
        }
        
    boolean syncing = false;
    /** Sets whether or not modules should sync to MIDI Clock (if they've been individually set to) */
    public void setSyncing(boolean val) { syncing = val; }
    /** Returns whether or not modules should sync to MIDI Clock (if they've been individually set to) */
    public boolean isSyncing() { return syncing; }  
    
    ////// MIDI CLOCK STATE MACHINE
    
    // The state is set to this to indicate that we never started the clock
    static final int STATE_STOPPED = 0; 
    static final int STATE_WAITING_FOR_FIRST_PULSE = 1;
    static final int STATE_WAITING_FOR_SECOND_PULSE = 2;
    static final int STATE_RUNNING = 3;
    int state = STATE_STOPPED;

    // The number of clock ticks which have occurred (24 ticks per quarter note)
    int pulses = 0;

    // Set to 1 when the clock had a START.  go() will then increment it once before voices see it, so when it's incremented TWICE, we reset it.
    volatile int clockStartTrigger = 0;
    // Set to 1 when the clock had a PULSE.  go() will then increment it once before voices see it, so when it's incremented TWICE, we reset it.
    volatile int clockPulseTrigger = 0;

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

    //// CLOCK TICKS ESTIMATE

    
    // We're going to imagine that "time" is 1024 ticks per pulse.
    // At 44100 ticks/sec / 918.75 ticks/pulse / 24 pulse/beat * 60 sec/min
    // This comes to exactly 120 BPM.
    public static final double TICKS_PER_PULSE = 918.75;
        
    public static final String[] CLOCK_NAMES = new String[]
    { "Triplet 1/64th", "Triplet 1/32th", "1/32th", "Triplet 1/16th", "1/16th", "Triplet Eighth", "Eighth",
      "Triplet Quarter", "Dotted Eighth", "Quarter", "Triplet Half", "Dotted Quarter", "Half", "Triplet Whole",
      "Dotted Half", "Whole", "Dotted Whole", "2 Whole", "3 Whole", "4 Whole", "6 Whole", "8 Whole", 
      "12 Whole", "16 Whole", "24 Whole", "32 Whole" };
                        
    public static final int [] CLOCK_PULSES = new int[]
    { 1, 2, 3, 4, 6, 8, 12, 16, 18, 24, 32, 36, 48, 64, 72, 96, 144, 192, 96 * 3, 96 * 4, 96 * 6, 96 * 8, 96 * 12, 96 * 16, 96 * 24, 96 * 32 };
             
             
    // Our interpolation rate for updating our estimate of rate
    static final double RATE_ESTIMATE_ALPHA = 0.5;

    // Our interpolation rate for updating our estimate of the next target tick
    static final double MIDI_TICK_ESTIMATE_ALPHA = 0.25;

    // If we're this far ahead or behind in ticks, there's no saving us for catching up.  Just lock to the current value.
    static final double BIG_CHANGE = 3000;
    
    // If we've been waiting for pulses and the clock source appears to be behind by about 4 pulses
    // it has probably died.  We hang until it resumes.
    static final double BIG_PULSES_BEHIND = 4;
        

    // our estimated rate in TICKS PER PULSE
    double rateEstimate = 0;
    // The tick at last pulse.
    int lastPulseRealTick = 0;
    // The tick that we believe we currently are at.  This is a smoothed estimate. 
    double midiTickEstimate = 0;
    // Stores the same data as midiTickEstimate, and is updated when appropriate.
    // Volatile so that we can load it atomically as getTick()
    volatile int syncTick = 0;
    // Stores the same data as syncTick, and is updated during syncTick()
    int tick = 0;
    
    public void syncTick()
        {
        tick = syncTick;
        }
        
    public int getTick()
        {
        return tick;
        }
        
    public boolean isRunning()
        {
        return state > STATE_STOPPED && state <= STATE_RUNNING;
        }
        
    ///// CLOCK

    // Starts the clock
    synchronized void startClock()
        {
        pulses = 0;
        clockStartTrigger = 1;
        midiTickEstimate = 0;
        syncTick = (int)midiTickEstimate;
        state = STATE_WAITING_FOR_FIRST_PULSE;
//        System.err.println("start");
        }

    // stops the clock
    synchronized void stopClock()
        {
        state = STATE_STOPPED;
//        System.err.println("stopped");
        }

    // continues the clock
    synchronized void continueClock()
        {
        int currentRealTick = input.getOutput().getTick();
        midiTickEstimate = getPulses() * TICKS_PER_PULSE + (currentRealTick - lastPulseRealTick) / rateEstimate * TICKS_PER_PULSE;
        syncTick = (int)midiTickEstimate;
        state = STATE_WAITING_FOR_FIRST_PULSE;
        }

    // pulses the clock
    synchronized void pulseClock()
        {
        int currentRealTick = input.getOutput().getTick();
        
        if (isRunning())
            {
            pulses++;
            clockPulseTrigger = 1;
            }
                
        if (state == STATE_WAITING_FOR_FIRST_PULSE)
            {
            // we have just now gotten our first pulse.  So we record the tick as the last pulse tick 
            lastPulseRealTick = currentRealTick;
            state = STATE_WAITING_FOR_SECOND_PULSE;
//        System.err.println("first pulse " + pulses);
            }
        else if (state == STATE_WAITING_FOR_SECOND_PULSE)
            {
            // we have just now gotten our second pulse and we can make a first estimate of our rate
            rateEstimate = (currentRealTick - lastPulseRealTick);
            lastPulseRealTick = currentRealTick;
            state = STATE_RUNNING;
//        System.err.println("second pulse " + pulses);
            }
        else if (state == STATE_RUNNING)
            {
            // we update our rate estimate
            rateEstimate = (1 - RATE_ESTIMATE_ALPHA) * rateEstimate + RATE_ESTIMATE_ALPHA * (currentRealTick - lastPulseRealTick);
            lastPulseRealTick = currentRealTick;
//        System.err.println("running " + pulses);
            }
        }


    public void go()
        {
        clockStartTrigger++;
        if (clockStartTrigger > 2) clockStartTrigger = 0;
        
        clockPulseTrigger++;
        if (clockPulseTrigger > 2) clockPulseTrigger = 0;
        
        update();
        }
        
            
    // this is called either from go() via Input.java, or on receiving pulses.
    synchronized void update()
        {        
        if (state == STATE_RUNNING)
            {
            int currentRealTick = input.getOutput().getTick();
            double newPulses = (currentRealTick - lastPulseRealTick) / rateEstimate;
            if (newPulses >= BIG_PULSES_BEHIND)
                return;  // we haven't been getting many pulses lately
                
            double midiTickTarget = (getPulses() + newPulses) * TICKS_PER_PULSE;
            
            // We are far ahead.  Lock it.
            if (midiTickTarget > midiTickEstimate + BIG_CHANGE)
                {
                System.err.println("WARNING(MidiClock.java): Clock locked forward to " + (midiTickEstimate - midiTickTarget));
                // lock it
                midiTickEstimate = midiTickTarget;
                }
            // We roll in our computed target from the difference only if we're ahead.  If we're behind, we're not there yet.
            // Therefore we can't go backwards
            else if (midiTickTarget > midiTickEstimate)
                midiTickEstimate = (1 - MIDI_TICK_ESTIMATE_ALPHA) * midiTickEstimate + MIDI_TICK_ESTIMATE_ALPHA * midiTickTarget;
            // We are far behind.  Lock it.
            else if (midiTickTarget < midiTickEstimate - BIG_CHANGE) // we have to bail
                {
                System.err.println("WARNING(MidiClock.java): Clock locked backward to " + (midiTickEstimate - midiTickTarget));
                // lock it
                midiTickEstimate = midiTickTarget;
                }
            syncTick = (int)midiTickEstimate;
            }
        }
        
    }

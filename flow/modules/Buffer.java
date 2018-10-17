// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/** 
    A Unit which does a kind of sample-and-hold on an incoming set of partials.  Specifically
    when it receives a TRIGGER, it samples the partials from its input source and only
    outputs those partials.  If FREE is FALSE, then it also samples on gate.  It always samples
    on reset.  You can kind of think of Buffer as the counterpart to Smooth: it's an unsmoother.
*/

public class Buffer extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_TRIGGER = 0;

    boolean free;
    boolean start;
    boolean reset;
    double[] bufferedAmplitudes = new double[NUM_PARTIALS];
    double[] bufferedFrequencies = new double[NUM_PARTIALS];
        
    public static final int OPTION_FREE = 0;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return getFree() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
        
    public boolean getFree() { return free; }
    public void setFree(boolean free) { this.free = free; } 
        
    public Buffer(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ONE }, new String[] { "Trigger" });
        defineOptions(new String[] { "Free" }, new String[][] { { "Free" } } );
        setFree(false);
        start = true;
        }
        
    public void reset()
        {
        super.reset();
        start = true;
        }
                
    public void gate()
        {
        super.gate();
        if (!free) start = true;
        }
                
    public void go()
        {
        super.go();
                
        double[] amplitudesIn = getAmplitudesIn(0);
        double[] frequenciesIn = getFrequenciesIn(0);
                
        modulate(MOD_TRIGGER);
        if (start || isTriggered(MOD_TRIGGER))
            {
            System.arraycopy(amplitudesIn, 0, bufferedAmplitudes, 0, amplitudesIn.length);
            System.arraycopy(frequenciesIn, 0, bufferedFrequencies, 0, frequenciesIn.length);
            start = false;
            reset = true;
            }
            
        if (reset)
            {
            System.arraycopy(bufferedAmplitudes, 0, getAmplitudes(0), 0, bufferedAmplitudes.length);
            System.arraycopy(bufferedFrequencies, 0, getFrequencies(0), 0, bufferedFrequencies.length);
            }
                
        reset = constrain();
        if (reset) simpleSort(0, false);
        }       
    }

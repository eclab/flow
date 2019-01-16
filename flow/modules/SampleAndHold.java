// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Modulation which performs a classic Sample and Hold on its incoming modulation
   Signal.  When it receives a Trigger, it performs another sample, then only outputs
   that sample until the next Trigger.
*/

public class SampleAndHold extends Modulation
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_SIGNAL = 0;
    public static final int MOD_TRIGGER = 1;

	public static final int TYPE_SAMPLE_AND_HOLD = 0;
	public static final int TYPE_TRACK_AND_HOLD = 1;
	public static final int TYPE_TRACK_AND_HOLD_ALT = 2;
    
    int type;
    public int getType() { return type; }
    public void setType(int val) { type = val; }
    
    public static final int OPTION_TYPE = 0;
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public static String getName() { return "S & H"; }

    boolean sampling = false;
        
    public SampleAndHold(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO }, new String[] { "Signal", "Trigger", });
        defineOptions(new String[] { "Type" }, new String[][] { { "S & H", "T & H", "T & H 2" } });
        }

    public void reset()
        {
        super.reset();
        setModulationOutput(0, modulate(MOD_SIGNAL));
        if (getType() == TYPE_TRACK_AND_HOLD)
        	{
        	sampling = true;
        	}
        else
        	{
        	sampling = false;
        	}
        }

    public void gate()
        {
        super.gate();
        setModulationOutput(0, modulate(MOD_SIGNAL));
        if (getType() == TYPE_TRACK_AND_HOLD)
        	{
        	sampling = true;
        	}
        else
        	{
        	sampling = false;
        	}
        }

    public void go()
        {
        super.go();
        
        if (getType() == TYPE_SAMPLE_AND_HOLD)
        	{
        	if (isTriggered(MOD_TRIGGER))
        		{
    		    setModulationOutput(0, modulate(MOD_SIGNAL));
        		}
        	}
        else
        	{
        	if (isTriggered(MOD_TRIGGER))
        		{
        		if (sampling)
        			{
	    		    setModulationOutput(0, modulate(MOD_SIGNAL));
	    		    }
	    		sampling = !sampling;
        		}
        	else if (!sampling)
        		{
	    		setModulationOutput(0, modulate(MOD_SIGNAL));
        		}
        	}
        }
    }

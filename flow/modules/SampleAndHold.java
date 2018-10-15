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

    public static String getName() { return "S & H"; }

    boolean started;
        
    public SampleAndHold(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO }, new String[] { "Signal", "Trigger" });
        }

    public void gate()
        {
        super.gate();
        started = false;
        }

    public void go()
        {
        super.go();
        modulate(1);
        if (!started || isTriggered(1))
            {
            setModulationOutput(0, modulate(0));
            started = true;
            }
        }
    }

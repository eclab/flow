// Copyright 2024 by Sean Luke
// Licensed under the Apache 2.0 License


package flow.modules;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

import flow.*;

public class Latch extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_TRIGGER = 0;
    public static final int MOD_SELECT_INITIAL = 1;
    public static final int MOD_SELECT_IN = 2;
    public static final int NUM_INITIALS = 4;
    public static final int NUM_INS = 4;
        
    boolean trigger = false;
        
    public Latch(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Initial 1", "Initial 2", "Initial 3", "Initial 4", "Later 1", "Later 2", "Later 3", "Later 4" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO }, new String[] { "Trigger", "Sel Initial", "Sel Later" });
        }
       
    public void gate()
        {
        super.gate();
        if (isModulationConstant(MOD_TRIGGER))
            {
            trigger = true;
            }
        }

    public void go()
        {
        super.go();
        
        if (trigger || isTriggered(MOD_TRIGGER))
            {
            int val = (int)(modulate(MOD_SELECT_INITIAL) * NUM_INITIALS);           // 0...4
            if (val == NUM_INITIALS) val = NUM_INITIALS - 1;                                        // 0...3
            copyFrequencies(val);
            copyAmplitudes(val);
            trigger = false;
            }
        else
            {
            int val = (int)(modulate(MOD_SELECT_IN) * NUM_INS + NUM_INITIALS);              // 4...8
            if (val == NUM_INITIALS + NUM_INS) val = NUM_INITIALS + NUM_INS - 1;    // 4...7
            copyFrequencies(val);
            copyAmplitudes(val);
            }
        constrain();    // is this useful?
        }       

    public static String getName() { return "Latch"; }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_TRIGGER)
                {
                return super.getModulationValueDescription(modulation, value, isConstant);
                }
            else
                {
                int d = (int)(value * NUM_INS);
                if (d == NUM_INS) d = NUM_INS - 1;
                d += 1;
                return String.valueOf(d);
                }
            }
        else return "";
        }
    }

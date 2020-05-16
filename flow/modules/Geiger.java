// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
   A source of randomness in triggering. Geiger follows a negative binomial distribution
   to determine whether to trigger when it receives a trigger.  A negative binomial distribution
   is defined by two variables, *p* (the probability of not incrementing a counter) and *r*
   (the number of counter increments before a trigger is outputted).  Geiger defines
   *1-p* to be its MOD_PROBABILITY ("Prob"), and *r* to be its MOD_RATE ("Trials").  Whenever
   Geiger triggers, it also toggles from 0.0 to 1.0 or vice versa.  When Trials is set to 0
   then this reduces to a simple geometric distribution: Geiger is just outputting a trigger
   with the given probability when it receives a trigger.
*/

public class Geiger extends Modulation
    {       
    private static final long serialVersionUID = 1;

    public static final int MOD_RATE = 0;
    public static final int MOD_PROBABILITY = 1;
    public static final int MOD_TRIGGER = 2;
    public static final int MOD_SEED = 3;
    
    public static final int OPTION_PULSING = 0;
    
    boolean pulsing = false;
    public boolean isPulsing() { return pulsing; }
    public void setPulsing(boolean val) { pulsing = val; }
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_PULSING: return isPulsing() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_PULSING: setPulsing(value == 1); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public Random random = null;
        
    void initializeRandom()     
        {
        // first reseed
        double mod = modulate(MOD_SEED);
                
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
        }
        
    public Geiger(Sound sound)
        {
        super(sound);
        defineOptions(new String[] { "Pulsing" }, new String[][] { { "Pulsing" }});
        defineModulations(new Constant[] { Constant.ZERO, Constant.HALF, Constant.ZERO, Constant.ONE }, new String[] { "Trials", "Prob", "Trigger", "Seed" });
        }

    public void reset()
        {
        super.reset();
        initializeRandom(); 
        down = false; 
        setModulationOutput(0, 0);
        }
                
    public void gate()
        {
        super.gate();
        initializeRandom(); 
        }
    
    public static final double MAX_RATE = 10.0;
    
    boolean down = false;
    int count = 0;
    public void go()
        {
        super.go();

        boolean pulseDown = !down && isPulsing();
        if (isTriggered(MOD_TRIGGER))
            {
            Random rand = (random == null ? getSound().getRandom() : random);
            if (rand.nextFloat() < modulate(MOD_PROBABILITY) || pulseDown)
                {
                count++;
                if (count > modulate(MOD_RATE) * MAX_RATE)
                    {
                    down = !down;
                    setModulationOutput(0, down ? 0 : 1);
                    if (!(down && pulseDown))
                        updateTrigger(0);
                    count = 0;
                    }
                }
            }
        }


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_RATE)
                {
                return "" + ((int)(modulate(MOD_RATE) * MAX_RATE) + 1);
                }
            else if (modulation == MOD_SEED)
                {
                return (value == 0.0 ? "Free" : String.format("%.4f" , value));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    }

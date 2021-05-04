// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.util.*;

public class RandMod extends Modulation implements ModSource
    {       
    private static final long serialVersionUID = 1;

    public static String getName() { return "Random"; }

    public static final int MOD_HIGH = 0;
    public static final int MOD_LOW = 1;
    public static final int MOD_TRIGGER = 2;
    public static final int MOD_SEED = 3;

    double current = 0;
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
        
    boolean onGate = true;
    public boolean isOnGate() { return onGate; }
    public void setOnGate(boolean val) { onGate = val; }
        
    public static final int OPTION_GATE = 0;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_GATE: return isOnGate() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_GATE: setOnGate(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }



    public RandMod(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ONE, Constant.ZERO, Constant.ZERO, Constant.ZERO }, new String[] { "High", "Low", "Trigger", "Seed" });
        defineOptions(new String[] { "Note On" }, new String[][] { { "Note On" } } );
        }
    
    public void randomize()
        {
        Random rand = (random == null ? getSound().getRandom() : random);
        double val = rand.nextDouble();
        double high = modulate(MOD_HIGH);
        double low = modulate(MOD_LOW);
        if (high < low)         // swap if the boundaries are bad
            {
            double tmp = high;
            high = low;
            low = tmp;
            }
        current = (val * (high - low)) + low;
        }
    
    public void gate()
        {
        super.gate();
        if (onGate)
            randomize();
        }

    public void reset()
        {
        super.reset();
        initializeRandom(); 
        randomize();
        }

    public void go()
        {
        super.go();
        if (isTriggered(MOD_TRIGGER))
            randomize();
        setModulationOutput(0, current);
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_SEED)
                {
                return (value == 0.0 ? "Free" : String.format("%.4f" , value));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
    }

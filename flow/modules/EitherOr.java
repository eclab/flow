// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;

public class EitherOr extends Modulation
{       
    private static final long serialVersionUID = 1;

    public static String getName() { return "Either/Or"; }

    public static final int MAX_OPTIONS = 4;

    public static final int MOD_INPUT = 0;
    public static final int MOD_YES = 1;
    public static final int MOD_NO = 2;
    public static final int MOD_NUM_OPTIONS = 3;
        
    int choice = -1;
    int numOptions = -1;

    public EitherOr(Sound sound)
    {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE, Constant.ZERO, Constant.ZERO }, new String[] { "Choice", "Yes", "No", "Options" });
        defineModulationOutputs(new String[] { "1", "2", "3", "4" });
    }

    int discretize(double val, int num)
    {
        int v = (int)(val * num);
        if (val == 1.0) return v - 1;
        else return v; 
    }

    public void go()
    {
        super.go();
        
        int _numOptions = discretize(modulate(MOD_NUM_OPTIONS), MAX_OPTIONS - 1) + 2;
        if (_numOptions != numOptions)
            {
                numOptions = _numOptions;
                double noMod = modulate(MOD_NO);
                for(int i = 0; i < getNumModulationOutputs(); i++)
                    {
                        setModulationOutput(i, noMod);
                    }
                choice = -1;  // need to reset it all
            }
        
        int _choice = discretize(modulate(MOD_INPUT), numOptions);
                
        if (_choice != choice)
            {
                choice = _choice;
                double yesMod = modulate(MOD_YES);
                double noMod = modulate(MOD_NO);

                for(int i = 0; i < getNumModulationOutputs(); i++)
                    {
                        setModulationOutput(i, noMod);
                    }
                setModulationOutput(_choice, yesMod);
            }
                
    }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (isConstant)
            {
                if (modulation == MOD_NUM_OPTIONS)
                    {
                        return "" + (discretize(value, MAX_OPTIONS - 1) + 2);
                    }
                else if (modulation == MOD_INPUT)
                    {
                        return "" + (discretize(value, numOptions) + 1);
                    }
                else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
    }
}

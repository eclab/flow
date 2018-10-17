// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   An unusual Modulation which outputs a modulation signal based on the values
   of incoming partials and an incoming modulation.  Specifically the incoming
   modulation specifies the partial to be chosen from among the incoming partials;
   and the output signal is the current amplitude of that partial.  If INTERPOLATE
   is true, then the output signal smoothly changes from amplitude to amplitude,
   else it is abrupt.
*/

public class PartialMod extends Unit implements ModSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_PARTIAL = 0;

    public static String getName() { return "Partial Mod"; }

    public int oldIndex;
        
    boolean interpolate = true;
    public boolean getInterpolate() { return interpolate; }
    public void setInterpolate(boolean val) { interpolate = val; }
        
    public void gate() { super.gate(); oldIndex = -1; }

    public void go()
        {
        super.go();
        
        double mod = (modulate(MOD_PARTIAL) * (Unit.NUM_PARTIALS - 1));
        int index = (int) mod;
        if (oldIndex != index)
            updateTrigger(0);
        oldIndex = index;   
        
        double alpha = mod - index;
        double[] inputs0Amplitudes = getAmplitudesIn(0);
        
        double val = 0;
        if (!interpolate || index == Unit.NUM_PARTIALS - 1)
            val = Math.abs(inputs0Amplitudes[index]);
        else
            val = Math.abs((1.0 - alpha) * inputs0Amplitudes[index] + alpha * inputs0Amplitudes[index + 1]);

        if (val > 1.0)
            val = 1.0;
                        
        setModulationOutput(0, val);
        }
                
    public boolean isConstrainable() { return false; }

    public PartialMod(Sound sound) 
        { 
        super(sound);   
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Partial" });
        defineModulationOutputs(new String[] { "Mod" });
        defineOutputs(new String[] { });
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] { "Interpolate" }, new String[][] { { "Interpolate"} } );
        interpolate = true;
        resetAllTriggers();
        }
                
    public static final int OPTION_INTERPOLATE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_INTERPOLATE: return getInterpolate() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_INTERPOLATE: setInterpolate(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    }

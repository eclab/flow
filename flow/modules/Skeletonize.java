// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which strips out partials based on their distance from the nearest
   highest-amplitude partial.  The further a partial is from the the nearest
   peak partial, the more its amplitude will be cut down, in an exponential
   distance fashion.  CUT determines the strength of the cut-down: at its 
   extreme, all partials other than the local peaks are eliminated entirely.
   
   <p>If you're wondering where this term came from, see
   <a href="https://en.wikipedia.org/wiki/Topological_skeleton">here</a>.
*/

public class Skeletonize extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_CUT = 0;

    public static final int DIRECTION_LEFT = 0;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_BOTH = 2;
     
    int direction = DIRECTION_BOTH;
    public int getDirection() { return direction; }
    public void setDirection(int val) { direction = val; }
     
    public static final int OPTION_DIRECTION = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_DIRECTION: return getDirection();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_DIRECTION: setDirection(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }


    public Skeletonize(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO }, new String[] { "Cut" });
        defineOptions(new String[] { "Direction" }, new String[][] { { "Lower", "Higher", "Both" } });
        }
                
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);
        
        double cut = 1.0 - modulate(MOD_CUT);

        double[] upcuts = new double[amplitudes.length];
        double[] downcuts = new double[amplitudes.length];  
        double c = 1.0;
        
        if (direction == DIRECTION_BOTH)
            {
            upcuts[0] = c;
            for(int i = 1; i < amplitudes.length; i++)
                {
                if (amplitudes[i] <= amplitudes[i - 1])
                    {  c *= cut; }
                else
                    { c = 1.0; }
                upcuts[i] = c;
                }
                                
            c = 1.0;
            downcuts[amplitudes.length - 1] = c;
            for(int i = amplitudes.length - 2; i >= 0; i--)
                {
                if (amplitudes[i] <= amplitudes[i + 1])
                    {  c *= cut; }
                else
                    { c = 1.0; }
                downcuts[i] = c;
                }
                                
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = amplitudes[i] * Math.min(upcuts[i], downcuts[i]);
                }
            }
        else if (direction == DIRECTION_RIGHT)
            {
            upcuts[0] = c;
            for(int i = 1; i < amplitudes.length; i++)
                {
                if (amplitudes[i] <= amplitudes[i - 1])
                    {  c *= cut; }
                else
                    { c = 1.0; }
                upcuts[i] = c;
                }
                                
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = amplitudes[i] * upcuts[i];
                }
            }
        else            // direction == DIRECTION_LEFT
            {
            downcuts[amplitudes.length - 1] = c;
            for(int i = amplitudes.length - 2; i >= 0; i--)
                {
                if (amplitudes[i] <= amplitudes[i + 1])
                    {  c *= cut; }
                else
                    { c = 1.0; }
                downcuts[i] = c;
                }
                                
            for(int i = 0; i < amplitudes.length; i++)
                {
                amplitudes[i] = amplitudes[i] * downcuts[i];
                }
            }
                
        constrain(); 
        }       
    }

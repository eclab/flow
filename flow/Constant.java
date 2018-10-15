// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

/**
   Constant is a Modulation which provides a single value.  The class name is actually 
   a misnomer, as you can change the value of a Constant via its setValue(...) method.
   Constants are the typical default elements which fill the incoming Modulation slots 
   of Modulation and Unit objects.

   <p>Constants do not register themselves with Sounds.  You will never see a
   ModPanel which represents a Constant.
        
   <p>Constants do not provide triggers.
        
   <p>There are three useful Constant objects: Modulation.ZERO, Modulation.HALF, and
   Modulation.ONE.
**/

public class Constant extends Modulation
    {
    private static final long serialVersionUID = 1;

    public static final Constant ZERO = new Constant(0);
    public static final Constant QUARTER = new Constant(0.25);
    public static final Constant HALF = new Constant(0.5);
    public static final Constant ONE = new Constant(1);
    
    public Constant(double val)
        {
        super(null);  // don't register me
        setValue(val);
        }
    
    /** Returns the Constant's value, between 0...1.  This method is the same as modulate(). */
    public double getValue()
        {
        return getModulationOutput(0);
        }
                
    /** Sets the Constant's value, bounded to between 0...1. */
    public void setValue(double val)
        {
        if (val < 0) val = 0;
        else if (val > 1) val = 1;
        setModulationOutput(0, val);
        }
                
    public String toString() { return "<" + getValue() + ">"; }
    }

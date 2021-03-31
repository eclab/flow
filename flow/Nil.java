// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

/**
   Nil is a Unit which does nothing.  Its frequencies and amplitudes are all zero.
   The sole purpose of Nil objects is to indicate that the input to a Unit has not
   been filled by another Unit (Nil basically serves the purpose of null).
        
   <p>Nil instances do not register themselves with Sounds.  You will never see a
   ModPanel which represents Nil.
        
   <p>There is a canonical Nil object available: Unit.NIL.  However other Nil
   instances can and will be created, and they should all be treated as equivalent.
**/

public class Nil extends Unit
{
    private static final long serialVersionUID = 1;

    public Nil()
    {
        super(null);
    }
        
    public String toString() { return "<NIL>"; }
}

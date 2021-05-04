// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

/**
   Indicates a widget which can rebuild itself to reflect underlying changes
   in its Modulation or Unit.
*/ 

public interface Rebuildable
    {    
    /** Resets the component to reflect the underlying of the synthesizer. */
    public void rebuild();
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

/** Implement this interface to enable presets in your module. */

public interface Presetable
    {    
    public String[] getPresets();
    public void setPreset(int preset);
    }

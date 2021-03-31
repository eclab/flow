// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import org.json.*;

/**
   A module which works with the Macro facility to route
   higher-level partials and modulations to your patch as 
   unit and modulation outputs. All eight unit and modulation
   outputs can be custom named by clicking on their labels.
*/


public class Fix extends Modulation implements Miscellaneous
{
    private static final long serialVersionUID = 1;

    public static final int MOD_NOTE = 0;
    public static final int MOD_VELOCTY = 1;

    boolean started = false;
        
    public Fix(Sound sound)
    {
        super(sound);
        defineModulationOutputs(new String[] { });
        defineModulations(new Constant[] { Constant.HALF, Constant.ONE }, 
                          new String[] {  "Note", "Velocity" });
    }

    int lastNoteCounter = -1;
    int lastNote = -1;
    public void go()
    {
        super.go();
        int note = (int)(modulate(MOD_NOTE) * 128) - 1;
        if (note > -1)  // off
            {
                int nc = sound.getNoteCounter();
                if (lastNote != note || lastNoteCounter != nc)
                    {
                        sound.setNote(Math.pow(2.0, (double) (note - 69) / 12.0) * 440.0);
                        lastNote = note;
                        lastNoteCounter = nc;
                    }
            }

        double vel = modulate(MOD_VELOCTY);
        if (vel > 0)
            {               
                sound.setVelocity(vel);
            }
    }

    public static final String[] notes = new String[] { "C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B" };
    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (isConstant)
            {
                if (modulation == MOD_NOTE)  // rate
                    {
                        int note = (int)(value * 128) - 1;
                        if (note == -1)
                            return "Free";
                        else 
                            {
                                return notes[note % 12] + (note / 12);
                            }
                    }
                else if (modulation == MOD_VELOCTY)  // rate
                    {
                        if (value == 0.0)
                            return "Free";
                        else
                            return String.format("%.4f", value);
                    }
            }
        return "";
    }
}

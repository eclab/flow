package flow.tests;

import flow.*;
import flow.modules.*;

/** Add in one harmonic-shifted version
 */

public class Exp10
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Rectified rect = new Rectified(sound);
                
        Shift shift = new Shift(sound);
        shift.setType(Shift.TYPE_PARTIALS);
        shift.setInput(rect);
        shift.setModulation(v.getModulation(1, "shift", 1.0), 1);
                        
        Combine add = new Combine(sound);
        add.setInputs(rect, shift);
        add.setHalf(true);
                
        Smooth smooth = new Smooth(sound);
        smooth.setInputs(add);
        smooth.setModulation(new Constant(0.4), 0);
                
        v.setInputs(smooth);
        sound.setEmits(v);
                
                
        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

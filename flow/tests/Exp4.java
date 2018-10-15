package flow.tests;

import flow.*;
import flow.modules.*;

/** Add in two pitch-shifted lower octaves
 */

public class Exp4
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Rectified rect = new Rectified(sound);
                
        Scale scale = new Scale(sound);
        scale.setInput(rect);
        scale.setModulation(new Constant(0.25 / 2), 0);

        Scale scale2 = new Scale(sound);
        scale2.setInput(scale);
        scale2.setModulation(new Constant(0.25 / 2), 0);
                
        Shift shift = new Shift(sound);
        shift.setInput(scale);
        shift.setType(Shift.TYPE_PITCH);
        shift.setModulation(new Constant(0.25), 1);

        Shift shift2 = new Shift(sound);
        shift2.setType(Shift.TYPE_PITCH);
        shift2.setInput(scale2);
        shift2.setModulation(new Constant(0.1 / 2), 1);
                
        Combine add = new Combine(sound);
        add.setInputs(shift, shift2);
        add.setMerge(true);

        Combine add2 = new Combine(sound);
        add2.setInputs(rect, add);
        add2.setMerge(true);
                        
        Viewer v = new Viewer(sound);
        v.setInputs(add2);
        sound.setEmits(v);
                
        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

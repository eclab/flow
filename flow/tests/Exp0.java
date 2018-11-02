package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

/** Modulate the cutoff of the BPF of a sawtooth.
    Tests: Sound, Sawtooth, Constant, BPF */

public class Exp0
    {
    public static int LENGTH = 2;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        //Viewer v = new Viewer(sound);
                
        Constant c = new Constant(0);
                
        Unit saw = new Sawtooth(sound);

        Filter filter = new Filter(sound);
        filter.setInput(saw);
        filter.setModulation(c, 0);
        //filter.setModulations(c, v.getModulation(0, "drop", 1.0));
        filter.setType(Filter.TYPE_LP);

        //v.setInput(filter);
        //sound.setEmits(v);
        sound.setEmits(filter);

        sound.reset();
        
        Output output = sound.getOutput();
        while(true)
            {
            sound.gate();
            int t = output.getTick();
            
            while(true)
                {
                int t2 = output.getTick() - t;
                if (t2 > samples) break;
                c.setValue(1.0 - t2 * increment);
                sound.getOutput().go();
                }
            }
        }
    }

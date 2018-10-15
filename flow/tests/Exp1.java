package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

/** Modulate the cutoff of the LPF of a sawtooth.
    Tests: Sound, Sawtooth, Constant, LPF */

public class Exp1
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
                
        Constant c = new Constant(0);
                
        Unit saw = new Sawtooth(sound);

        Filter lpf = new Filter(sound);
        lpf.setInput(saw);
        lpf.setModulations(c, v.getModulation(0, "drop", 1.0));
        lpf.setType(Filter.TYPE_LP);

        v.setInput(lpf);
        sound.setEmits(v);

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

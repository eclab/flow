package flow.tests;

import flow.*;
import flow.modules.*;

/** Morph between a standard triangle having its LPF modulated and
    a sawtooth having its LPF modulated in the opposite direction.
    Test: Triangle, Sawtooth, ModMath, LPF, Morph */

public class Exp2
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Constant c = new Constant(0);
        ModMath d = new ModMath(sound);
        d.setOperation(ModMath.MULTIPLY);
        d.setModulation(c, 0);

        Triangle tri = new Triangle(sound);
        Sawtooth tri2 = new Sawtooth(sound);
                
        Filter lpf = new Filter(sound);
        lpf.setInput(tri);
        lpf.setModulations(v.getModulation(0, "lpf1 drop", 1.0), c);
        lpf.setType(Filter.TYPE_LP);

        Filter lpf2 = new Filter(sound);
        lpf2.setInput(tri2);
        lpf2.setModulations(v.getModulation(1, "lpf2 drop", 1.0), d);
        lpf2.setType(Filter.TYPE_LP);
                
        Morph morph = new Morph(sound);
        morph.setInputs(lpf2, lpf);
        morph.setModulation(c);

        v.setInput(morph);
        sound.setEmits(morph);

        sound.reset();
        while(true)
            {
            sound.gate();
                        
            int tick = sound.getOutput().getTick();
            int i = 0;
            while(true)
                {
                i = sound.getOutput().getTick() - tick;
                if (i >= samples) break;
                c.setValue(1.0 - i * increment);
                sound.getOutput().go();
                }
            tick = sound.getOutput().getTick();
            i = 0;
            while(true)
                {
                i = sound.getOutput().getTick() - tick;
                if (i >= samples) break;
                c.setValue(i * increment);
                sound.getOutput().go();
                }
            }
        }
    }

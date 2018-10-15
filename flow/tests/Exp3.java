package flow.tests;

import flow.*;
import flow.modules.*;

/** Random-Morph through an infinite series of random partials.  We're normalizing too.
    Test: RandomMorph, Triangle, Sawtooth, ModMath, Random, Normalize */


public class Exp3
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
                
        Constant a = new Constant(0.5);
                
        Constant c = new Constant(0);
        ModMath d = new ModMath(sound);
        d.setOperation(ModMath.SUBTRACT);
        d.setModulation(c, 0);

        Rand rand1 = new Rand(sound);
        Rand rand2 = new Rand(sound);
                
        Morph morph = new Morph(sound);
        morph.setMorph(Morph.MORPH_ALL_RANDOM);
        morph.setInput(rand1,0);
        morph.setInput(rand2,1);
        morph.setModulations(new Modulation[] { c, v.getModulation(1, "variance", 0)});
        morph.setIncludesFundamental(true);
                
        Filter lpf1 = new Filter(sound);
        lpf1.setInput(morph);
        lpf1.setModulations(a, v.getModulation(0, "drop", 0.25));
        lpf1.setType(Filter.TYPE_LP);

        Normalize norm1 = new Normalize(sound);
        norm1.setInput(lpf1);
                
        Smooth smooth = new Smooth(sound);
        smooth.setModulation(new Constant(0.8), 0);
        smooth.setInput(norm1); 

        v.setInputs(smooth);
        sound.setEmits(smooth);
                
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
                c.setValue(i * increment);
                sound.getOutput().go();
                }
            }
        }
    }

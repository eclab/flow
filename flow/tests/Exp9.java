package flow.tests;

import flow.*;
import flow.modules.*;

/** Squish a mostly saw, partly sine wave.
    Tests: Squish, Sine
*/
        


public class Exp9
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Constant c = new Constant(0);

        Sine sine = new Sine(sound);
        Sawtooth saw = new Sawtooth(sound);
        Squish squish = new Squish(sound);

        Constant d = new Constant(0.1);

        Morph morph = new Morph(sound);
        morph.setModulation(d);
        morph.setInput(saw, 0);
        morph.setInput(sine, 1);
                
        squish.setInput(morph, 0);
        squish.setModulation(c);
                
        Viewer v = new Viewer(sound);
        v.setInputs(squish);
        sound.setEmits(v);

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

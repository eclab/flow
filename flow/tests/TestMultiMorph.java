package flow.tests;

import flow.*;
import flow.modules.*;

/** Morph between a standard triangle having its LPF modulated and
    a non-standardized triangle having its LPF modulated in the opposite direction.
    Test: Triangle, Sawtooth, ModMath, LPF, Morph 
*/

public class TestMultiMorph
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        LFO lfo = new LFO(sound);
        lfo.setModulation(new Constant(0.5), 0);
        lfo.setType(LFO.TRIANGLE);

        Triangle tri = new Triangle(sound);
        Square sq = new Square(sound);
        Sawtooth saw = new Sawtooth(sound);
        Rectified rect = new Rectified(sound);
                
        Morph morph = new Morph(sound);
        morph.setInputs(new Unit[] { tri, sq, saw, rect } );
        morph.setModulation(lfo);
        morph.setFree(true);
                
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
                sound.getOutput().go();
                }
            }
        }
    }

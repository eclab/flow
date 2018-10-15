package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestLFO
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        LFO lfo = new LFO(sound);
        Constant c = new Constant(0.5);
        lfo.setModulation(c, 0);
        
        Square saw = new Square(sound);
        Triangle tri = new Triangle(sound);
        Morph morph = new Morph(sound);
        morph.setInputs(saw, tri);
        morph.setModulation(lfo, 0);

        Viewer v = new Viewer(sound);
        v.setInput(morph, 0);
        sound.setEmits(v);

        sound.reset();          
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

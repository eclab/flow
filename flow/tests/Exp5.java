package flow.tests;

import flow.*;
import flow.modules.*;

/** Moprh between a square and saw using a COS LFO.  The LFO rate
    is itself modulated with a triangle LFO. 
    Tests: SQUARE, LFO */

public class Exp5
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        LFO lfo2 = new LFO(sound);
        lfo2.setType(LFO.TRIANGLE);
        lfo2.setModulation(new Constant(0.01), 0);
                
        LFO lfo = new LFO(sound);
        lfo.setType(LFO.SIN);
        lfo.setModulation(lfo2, 0);
                
        Square square = new Square(sound);
        Sawtooth saw = new Sawtooth(sound);
        Morph morph = new Morph(sound);
        morph.setInputs(square, saw);
        morph.setModulation(lfo);
                
        Viewer v = new Viewer(sound);
        v.setInputs(morph);
        sound.setEmits(v);

        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

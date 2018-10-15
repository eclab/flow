package flow.tests;

import flow.*;
import flow.modules.*;


/** Square a triangle wave, then subtract it from a square wave.  Double it.
    Then morph betwee that and a triangle. 
*/

public class Exp8
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Constant c = new Constant(0);

        Triangle tri = new Triangle(sound);
        AmpMath abs = new AmpMath(sound);
        abs.setOperation(AmpMath.MULTIPLY);
        abs.setInput(tri, 0);
        abs.setInput(tri, 1);

        Square square = new Square(sound);

        AmpMath abs2 = new AmpMath(sound);
        abs2.setOperation(AmpMath.SUBTRACT);
        abs2.setInput(abs, 0);
        abs2.setInput(square, 1);
                
        VCA vca = new VCA(sound);
        vca.setInput(tri);
        vca.setModulation(new Constant(1), 0);
        vca.setModulation(new Constant(.6428571428), 1);                // I think this is roughly 2.0
                
        Morph morph = new Morph(sound);
        morph.setInput(vca,1);
        morph.setInput(tri,0);
        morph.setModulation(c);
                
        Viewer v = new Viewer(sound);
        v.setInputs(morph);
        sound.setEmits(v);

        sound.reset();
        sound.gate();
        while(true)
            {
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
                c.setValue(1.0 - i * increment);
                sound.getOutput().go();
                }
            }
        }
    }

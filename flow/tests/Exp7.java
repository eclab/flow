package flow.tests;

import flow.*;
import flow.modules.*;

/** Swap partials within a square.
    Tests: Swap */
        
public class Exp7
    {
    public static int LENGTH = 5;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
                
        Constant c = new Constant(0);

        Square square = new Square(sound);
        Swap swap = new Swap(sound);
        swap.setSwapFundamental(true);
                
        swap.setInputs(square);
        swap.setModulation(c);
                
        v.setInputs(swap);
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

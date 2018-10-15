package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestPWM
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Square square = new Square(sound);
        Constant c1 = new Constant(0);
        square.setModulation(c1);

        Viewer v = new Viewer(sound);
        v.setInput(square, 0);
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
                c1.setValue(i * increment);
                sound.getOutput().go();
                }
            }
        }
    }

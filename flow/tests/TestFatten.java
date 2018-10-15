package flow.tests;

import flow.*;
import flow.modules.*;

/** Fatten (?) a sawtooth.  Obviously sound is not doing what is expected
    and needs to be fixed
    Tests: Fatten
*/
        

public class TestFatten
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Constant c = new Constant(0);

        Sawtooth saw = new Sawtooth(sound);
        Fatten fatten = new Fatten(sound);

        fatten.setInput(saw);
        fatten.setModulation(c);
                
        v.setInputs(fatten);
        sound.setEmits(v);

        sound.reset();
        int lastTick = 0;
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

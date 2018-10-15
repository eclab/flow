package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestJitter
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Jitter jitter = new Jitter(sound);
        Constant c1 = new Constant(0);
        Constant c2 = new Constant(0);
        LFO c3 = new LFO(sound);
        c3.setType(LFO.SQUARE);
        c3.setModulation(v.getModulation(2, "rate", 0.3), 0);
        jitter.setModulations(new Modulation[] { v.getModulation(0, "a", 0),
                                                 v.getModulation(1, "b", 0),
                                                 c3 });

        Square saw = new Square(sound);
        jitter.setInputs(saw);

        Smooth smooth = new Smooth(sound);
        smooth.setInputs(jitter);
        smooth.setModulations(v.getModulation(3, "smooth", 0.01));
        //new Constant(0.01));  // 0.001));  // 0.01));
                
        /*
          Buffer buffer = new Buffer(sound, true);
          buffer.setInputs(smooth);
          buffer.setModulations(new LFO(sound, LFO.SQUARE, 0.2));
        */

        v.setInput(smooth, 0);
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
                c2.setValue(i * increment);
                sound.getOutput().go();
                }
            }
        }
    }

package flow.tests;

import flow.*;
import flow.modules.*;
        

public class TestHarmonics
    {
    public static int LENGTH = 100;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Harmonics k5 = new Harmonics(sound);
                
        Smooth smooth = new Smooth(sound);
        smooth.setInput(k5);
        smooth.setModulations(new Constant(0.5));
                
        Viewer v = new Viewer(sound);
        v.setInputs(smooth);
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
                k5.setOptionValue(0, (int)(Harmonics.NUM_PARTIALS * (i * increment)));
                sound.getOutput().go();
                }
            }
        }
    }

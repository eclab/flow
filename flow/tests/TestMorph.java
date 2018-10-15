package flow.tests;

import flow.*;
import flow.modules.*;

/** Fatten (?) a sawtooth.  Obviously sound is not doing what is expected
    and needs to be fixed
    Tests: Fatten
*/
        

public class TestMorph
    {
    public static int LENGTH = 2;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Constant c = new Constant(0);

        Sawtooth saw = new Sawtooth(sound);
        Rectified rect = new Rectified(sound);

        Morph morph = new Morph(sound);
        morph.setInputs(new Unit[] { saw, rect });
        morph.setModulations(new Modulation[] { c, new Constant(0.5)});

        v.setInputs(morph);
        sound.setEmits(morph);

        sound.reset();
        sound.gate();
        while(true)
            {
            morph.setIncludesFundamental(true);
            for(int j = 0; j < morph.getNumMorphs(); j++)
                {
                morph.setMorph(j);
                System.err.println(morph.getMorphName(j));
                morph.gate();
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
            morph.setIncludesFundamental(false);                    
            for(int j = 0; j < morph.getNumMorphs(); j++)
                {
                morph.setMorph(j);
                System.err.println("FUNDAMENTAL " + morph.getMorphName(j));
                morph.gate();
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
    }

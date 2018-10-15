package flow.tests;

import flow.*;
import flow.modules.*;

/** Add a Saw with a scaled Saw and a pitch-shifted saw.
    Tests: Scale, PitchShift, Combine
*/

public class Exp6
    {
    public static int LENGTH = 1;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        LFO lfo = new LFO(sound);
        lfo.setModulation(new Constant(0.25), 0);

        Sawtooth saw = new Sawtooth(sound);
        Scale scale = new Scale(sound);
        scale.setInput(saw);
        scale.setModulation(lfo, 0);
                
        Shift shift = new Shift(sound);
        shift.setType(Shift.TYPE_PITCH);
        shift.setInput(saw);
        shift.setModulation(lfo, 0);
                
        Combine add2 = new Combine(sound);
        add2.setInputs(saw, scale);
        Combine add = new Combine(sound);
        add.setInputs(add2, shift);
                        
        Viewer v = new Viewer(sound);
        v.setInputs(add);
        sound.setEmits(v);
                
        //sound.getOutput().setSkip(1);

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

package flow.tests;
import flow.*;
import flow.modules.*;

import javax.sound.sampled.*;

public class TestTinkle
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
                
        LFO lfo = new LFO(sound);
        lfo.setModulation(v.getModulation(0, "rate", 0.4), 0);
                
        Tinkle tinkle = new Tinkle(sound);
        Constant c1 = new Constant(0.4);
        Constant c2 = new Constant(1.0);
        tinkle.setModulations(new Modulation[] { lfo,
                                                 v.getModulation(1, "alpha", 0.5),
                                                 v.getModulation(2, "volume", 0.5),
                                                 v.getModulation(3, "number", 0.5),
                                                 v.getModulation(4, "probability", 1.0) });
//              tinkle.setConstraint(Tinkle.CONSTRAINT_MINOR_THIRDS);
//              tinkle.setInvertConstraints(false);
                
        Filter lpf = new Filter(sound);
        lpf.setInput(tinkle);
        lpf.setModulations(v.getModulation(5, "Cutoff", 1.0), new Constant(0.5));
        lpf.setType(Filter.TYPE_LP);

        Smooth smooth = new Smooth(sound);
        smooth.setModulation(v.getModulation(6, "Smooth", 1.0), 0);
        smooth.setInput(lpf, 0);

        v.setInput(smooth, 0);
        sound.setEmits(smooth);
                
        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

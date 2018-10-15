package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class StressTest
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Output output = new Output();
        Sound sound = null;
        for(int q = 0; q < 8; q++)
            {
            sound = new Sound(output);
            sound.setNote(440);

            double samples = Output.SAMPLING_RATE * LENGTH;
            double increment = 1.0 / samples;

            Viewer v = new Viewer(sound);
            Modulation f0 = v.getModulation(0, "v2", 0.5);
            Modulation f4 = v.getModulation(4, "v3", 0.5);
            Modulation f1 = v.getModulation(1, "f1", 0.2);
            Modulation f2 = v.getModulation(2, "f2", 0.7);
            Modulation f3 = v.getModulation(3, "f3", 0.9);
            Modulation q1 = v.getModulation(5, "q1", 0.7);
            Modulation q2 = v.getModulation(6, "q2", 0.7);
            Modulation q3 = v.getModulation(7, "q3", 0.7);
                
            Unit saw = new Sawtooth(sound);

            Filter bpf1 = new Filter(sound);
            bpf1.setInput(saw);
            bpf1.setModulations(f1, q1);
            bpf1.setType(Filter.TYPE_BP);

            Filter bpf2 = new Filter(sound);
            bpf2.setInput(saw);
            bpf2.setModulations(f2, q2);
            bpf2.setType(Filter.TYPE_BP);

            Filter bpf3 = new Filter(sound);
            bpf3.setInput(saw);
            bpf3.setModulations(f3, q3);
            bpf3.setType(Filter.TYPE_BP);

            for(int w = 0; w < 10000; w++)
                {
                bpf1 = new Filter(sound);
                bpf1.setInput(saw);
                bpf1.setModulations(f1, q1);
                bpf1.setType(Filter.TYPE_BP);

                bpf2 = new Filter(sound);
                bpf2.setInput(saw);
                bpf2.setModulations(f2, q2);
                bpf2.setType(Filter.TYPE_BP);

                bpf3 = new Filter(sound);
                bpf3.setInput(saw);
                bpf3.setModulations(f3, q3);
                bpf3.setType(Filter.TYPE_BP);
                }

            AmpMath math = new AmpMath(sound);
            math.setOperation(AmpMath.MAX);
            math.setModulation(f0);
            math.setInputs(bpf1, bpf2);
                
            AmpMath math2 = new AmpMath(sound);
            math.setOperation(AmpMath.MAX);
            math.setNormalize(true);
            math2.setModulation(f4);
            math2.setInputs(math, bpf3);

            v.setInput(math2);
            sound.setEmits(v);

            sound.reset();
            sound.gate();
            }

        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class FormantTest
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Output output = new Output();
        Sound sound = null;

        sound = new Sound(output);

        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
        Modulation f1 = v.getModulation(1, "f1", 0.52);
        Modulation f2 = v.getModulation(2, "f2", 0.68);
        Modulation f3 = v.getModulation(3, "f3", 0.68);
        Modulation q1 = v.getModulation(5, "q1", 0.51);
        Modulation q2 = v.getModulation(6, "q2", 0.13);
        Modulation q3 = v.getModulation(7, "q3", 0.19);
                
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

        AmpMath math = new AmpMath(sound);
        math.setOperation(AmpMath.MAX);
        math.setInputs(bpf1, bpf2);
                
        AmpMath math2 = new AmpMath(sound);
        math.setOperation(AmpMath.MAX);
        math2.setInputs(math, bpf3);
                
        Normalize norm = new Normalize(sound);
        norm.setInputs(math2);

        v.setInput(norm);
        sound.setEmits(norm);

        sound.reset();
        sound.gate();

        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

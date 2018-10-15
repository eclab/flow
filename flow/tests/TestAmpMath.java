package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestAmpMath
    {
    public static int LENGTH = 2;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Square square = new Square(sound);
        Sawtooth saw = new Sawtooth(sound);
        AmpMath math = new AmpMath(sound);
        math.setOperation(AmpMath.MULTIPLY);
        math.setNormalize(true);
        math.setInputs(square, saw);

        Viewer v = new Viewer(sound);
        v.setInput(math, 0);
        sound.setEmits(v);
                
        int op = AmpMath.ADD;
        sound.reset();
        while(true)
            {
            math.setOperation(op);
            sound.gate();
            int tick = sound.getOutput().getTick();
            int i = 0;
            while(true)
                {
                i = sound.getOutput().getTick() - tick;
                if (i >= samples) break;
                sound.getOutput().go();
                }
            op++;
            if (op > AmpMath.MAX)
                op = AmpMath.ADD;
            }
        }
    }

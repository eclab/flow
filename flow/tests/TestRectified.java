package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestRectified
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Rectified rect = new Rectified(sound);

        Viewer v = new Viewer(sound);
        v.setInput(rect, 0);
        sound.setEmits(v);
                
        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

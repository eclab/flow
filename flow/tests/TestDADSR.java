package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestDADSR
    {
    public static int LENGTH = 5;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Square square = new Square(sound);
                
        DADSR a_dadsr = new DADSR(sound);
        a_dadsr.setModulations(new Modulation[]
            {
            v.getModulation(0, "Delay Time", 0.5),
            v.getModulation(1, "Delay Level", 0.0),
            v.getModulation(2, "Attack Time", 0.2),
            v.getModulation(3, "Attack Level", 1.0),
            v.getModulation(4, "Decay Time", 0.5),
            v.getModulation(5, "Decay Level", 0.5),
            v.getModulation(6, "Release Time", 0.9),
            v.getModulation(7, "Release Level", 0.0),
            });
                
        VCA vca = new VCA(sound);
        vca.setInput(square);
        vca.setModulation(a_dadsr);

        v.setInput(vca, 0);
        sound.setEmits(vca);
                
        sound.reset();
        boolean released;
        while(true)
            {
            released = false;
            sound.gate();
            int tick = sound.getOutput().getTick();
            int i = 0;
            while(true)
                {
                i = sound.getOutput().getTick() - tick;
                if (!released && i >= samples / 2)
                    { 
                    sound.release();
                    released = true; 
                    System.err.println("Release");
                    }
                if (i >= samples) break;
                sound.getOutput().go();
                }
            }
        }
    }

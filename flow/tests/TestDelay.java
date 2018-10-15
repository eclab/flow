package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestDelay
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
                
        Viewer v = new Viewer(sound);

        Delay delay = new Delay(sound);
        
        LFO lfo = new LFO(sound);
        lfo.setModulation(v.getModulation(0, "rate", 0.4), 0);
                

        Tinkle tinkle = new Tinkle(sound);
        Constant c1 = new Constant(0.4);
        Constant c2 = new Constant(1.0);
        tinkle.setModulations(new Modulation[] { lfo,
                                                 v.getModulation(1, "alpha", 0.5),
                                                 new Constant(0.5),
                                                 v.getModulation(2, "number", 0.5), 
                                                 new Constant(1.0) });


        /*Square saw = new Square(sound);
                
          DADSR a_dadsr = new DADSR(sound);
          a_dadsr.setModulations(new Modulation[]
          {
          new Constant(0.0),               // Delay Time
          new Constant(0.0),               // Delay Level
          new Constant(0.0),               // Attack Time
          new Constant(1.0),               // Attack Level
          new Constant(0.5),               // Decay Time
          new Constant(0.0),               // Decay Level
          new Constant(0.0),               // Release Time
          new Constant(0.5)                // Release Level
          }
          );
                
          VCA vca = new VCA(sound);
          vca.setInput(saw);
          vca.setModulation(a_dadsr);
        */

        delay.setInput(tinkle); 
        delay.setModulations(new Modulation[] { v.getModulation(3, "Wet", 0.4),
                                                v.getModulation(4, "Init Len", 1.0),
                                                v.getModulation(5, "Later Len", 0.01),
                                                v.getModulation(6, "Initial", 0.5),
                                                v.getModulation(7, "Later", 0.1)});
                
        v.setInput(delay, 0);
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
                sound.getOutput().go();
                }
            }
        }
    }

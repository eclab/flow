package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.midi.*;
import java.util.*;


public class Synth
    {
    public static int LENGTH = 1;

    public static void main(String[] args)
        {
        Output output = new Output();
                

        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;
        Sound[] sounds = new Sound[Output.getNumVoices()];

        for(int i = 0; i < sounds.length; i++)
            {
            sounds[i] = new Sound(output);
            Sound sound = sounds[i];
                        
            Unit tinkle = new Tinkle(sound);
            tinkle.setModulations(new Modulation[]
                {
                new Constant(0.4),               // Rate
                new Constant(0.1),               // Decay
                new Constant(0.1),               // Volume
                new Constant(0.6),               // Number
                new Constant(0.0),               // Drift
                }
                );
                                
            Unit saw = new Sawtooth(sound);
            Fatten fatten = new Fatten(sound);
            fatten.setInput(saw);
            fatten.setModulation (new Constant(0.25), 0);
                        
            Filter lpf = new Filter(sound);
            lpf.setInput(fatten);
            lpf.setType(Filter.TYPE_LP);

            DADSR f_dadsr = new DADSR(sound);
            f_dadsr.setModulations(new Modulation[]
                {
                new Constant(0.9),               // Delay Time
                new Constant(0.0),               // Delay Level
                new Constant(0.3),               // Attack Time
                new Constant(0.8),               // Attack Level
                new Constant(0.9),               // Decay Time
                new Constant(0.5),               // Decay Level
                new Constant(0.95),              // Release Time
                new Constant(0.0)                // Release Level
                }
                );

            lpf.setModulations(new Constant(0.75),
                new Constant(0.75));
            //f_dadsr, 0);

            Morph morph = new Morph(sound);
            morph.setInputs(lpf, tinkle);
            morph.setModulation(new Constant(0.5), 0);


            DADSR a_dadsr = new DADSR(sound);
            a_dadsr.setModulations(new Modulation[]
                {
                new Constant(0.0),               // Delay Time
                new Constant(0.0),               // Delay Level
                new Constant(0.5),               // Attack Time
                new Constant(1.0),               // Attack Level
                new Constant(0.9),               // Decay Time
                new Constant(1.0),               // Decay Level
                new Constant(0.95),              // Release Time
                new Constant(0.0)                        // Release Level
                }
                );
                
            VCA vca = new VCA(sound);
            vca.setInput(morph);
            vca.setModulation(a_dadsr);

            sound.setEmits(vca);
            sound.reset();
            }


        while(true)
            {
            output.go();
            }
        }
    }

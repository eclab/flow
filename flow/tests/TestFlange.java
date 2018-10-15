package flow.tests;

import flow.*;
import flow.modules.*;
import javax.sound.sampled.*;

public class TestFlange
    {
    public static int LENGTH = 10;
        
    public static void main(String[] args)
        {
        Sound sound = new Sound(new Output());
        double samples = Output.SAMPLING_RATE * LENGTH;
        double increment = 1.0 / samples;

        Viewer v = new Viewer(sound);
        Modulation f0 = v.getModulation(0, "Start", 0.);
        Modulation f1 = v.getModulation(1, "OffsetModScale", 1.0); // 0.95);
        Modulation f2 = v.getModulation(2, "Amount", 0.4);
        Modulation f3 = v.getModulation(3, "Stretch", 0.09); // 0.06);
        Modulation f4 = v.getModulation(4, "StrechModScale", 0.0);
        Modulation f5 = v.getModulation(5, "Mod LFO", 0.13); // 0.15);
        Modulation f6 = v.getModulation(6, "Stretch LFO", 0.0);
                
        Square all = new Square(sound);
                
        LFO lfo = new LFO(sound);
        lfo.setModulation(f5, 0);
        lfo.setType(LFO.SIN);

        LFO lfo2 = new LFO(sound);
        lfo2.setModulation(f6, 0);

        FlangeFilter flange = new FlangeFilter(sound);
        flange.setInput(all);
        flange.setFixed(true);
        flange.setStyle(FlangeFilter.STYLE_SPIKE_DOWN);
        flange.setModulations(new Modulation[] { f0, lfo, f1, f2, f3 , lfo2, f4});

        v.setInput(flange);
                
//              Smooth smooth = new Smooth(sound, true);
//              smooth.setInput(v);
//              smooth.setModulation(f6);
                
        //Normalize n = new Normalize(sound);
        //n.setInput(v);
                
        sound.setEmits(v);

        sound.reset();
        sound.gate();
        while(true)
            {
            sound.getOutput().go();
            }
        }
    }

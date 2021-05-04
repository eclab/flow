// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import flow.modules.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;


public class Test
    {
    public static int LENGTH = 1;

    public static void main(String[] args)
        {
        System.setProperty("apple.awt.graphics.EnableQ2DX", "true");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        Output output = new Output();

        Sound[] sounds = new Sound[Output.getNumVoices()];


        for(int i = 0; i < sounds.length; i++)
            {
            sounds[i] = new Sound(output);
            Sound sound = sounds[i];
            
            LFO lfo = new LFO(sound);
            lfo.setModulation(new Constant(0.63), 0);
            lfo.setFree(false);
            
            Unit tinkle = new Tinkle(sound);
            tinkle.setModulations(new Modulation[]
                {
                lfo,
                new Constant(0.5),               // Decay
                new Constant(0.5),               // Volume
                new Constant(0.6),               // Number
                new Constant(1.0),               // Drift
                }
                );
                                
            Unit saw = new Sawtooth(sound);
                        
            DADSR f_dadsr = new DADSR(sound);
            f_dadsr.setModulations(new Modulation[]
                {
                new Constant(0.0),               // Delay Time
                new Constant(0.9),               // Delay Level
                new Constant(0.25),               // Attack Time
                new Constant(0.8),               // Attack Level
                new Constant(0.5),               // Decay Time
                new Constant(0.3),               // Decay Level
                new Constant(0.95),              // Release Time
                new Constant(0.0)                // Release Level
                }
                );
                
            f_dadsr.setCurve(DADSR.CURVE_X_2);

            Filter lpf = new Filter(sound);
            lpf.setInput(saw);
            //            lpf.setType(Filter.TYPE_LP);

            lpf.setModulations(
                f_dadsr,
                new Constant(0.75));
                
            Normalize normalize = new Normalize(sound);
            normalize.setInput(lpf);
                
            LFO lfo2 = new LFO(sound);
            lfo2.setModulation(new Constant(0.63), 0);
 
            Morph morph = new Morph(sound);
            morph.setInputs(normalize, tinkle);
            morph.setModulation(lfo2, 0);


            DADSR a_dadsr = new DADSR(sound);
            a_dadsr.setModulations(new Modulation[]
                {
                new Constant(0.0),               // Delay Time
                new Constant(0.0),               // Delay Level
                new Constant(0.1),               // Attack Time
                new Constant(1.0),               // Attack Level
                new Constant(0.2),               // Decay Time
                new Constant(0.5),               // Decay Level
                new Constant(0.5),              // Release Time
                new Constant(0.0)                        // Release Level
                }
                );
                
            Fatten fatten = new Fatten(sound);
            fatten.setInput(morph);
            fatten.setModulation (new Constant(0.55), 0);

            VCA vca = new VCA(sound);
            vca.setInput(fatten);
            vca.setModulation(a_dadsr);

            Out out = new Out(sound);
            out.setInput(vca);

            sound.reset();
            }

        Rack rack = new Rack(output);

        rack.smallerOkay = true;
        for (Modulation mod : sounds[0].getRegistered())
            {
            if (!(mod instanceof Constant) && !(mod instanceof Nil))
                rack.addModulation(mod);
            }
        rack.smallerOkay = false;
        rack.rebuild();

        rack.sprout();
                
        output.startPrimaryVoiceThread();       
            
        /*        
        
                        
                  Rack rack = new Rack();
                  rack.add(new In(sound).getPanel());
                  rack.add(new Triangle(sound).getPanel());
                  rack.add(new LFO(sound).getPanel());
                  rack.add(new Morph(sound).getPanel());
                  rack.add(new NotchFilter(sound).getPanel());
                  rack.add(new Jitter(sound).getPanel());
                  rack.add(new Harmonics(sound).getPanel());
                  rack.add(new flow.modules.Map(sound).getPanel());
                  rack.add(new Bell(sound).getPanel());
                  rack.add(new DADSR(sound).getPanel());
                  rack.add(new Fatten(sound).getPanel());
                  rack.add(new Smooth(sound).getPanel());
                  rack.add(new Out(sound).getPanel());
        */
                
        }
                
    }

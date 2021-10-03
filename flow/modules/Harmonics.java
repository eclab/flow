// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;
import java.util.*;

/**
   A Unit which allows you to set the amplitudes of
   up to the first 16 harmonics, but not their frequency.
*/

public class Harmonics extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;
        
    public static final int NUM_HARMONICS = 32;

    public Harmonics(Sound sound) 
        {
        super(sound);
        Constant[] con = new Constant[NUM_HARMONICS];
        String[] str = new String[NUM_HARMONICS];
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
            con[i] = new Constant(0);  // we need different Constants so we can update them programmatically, see below
            str[i] = "Amp " + (i + 1);
            }
        defineModulations(con, str);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Source" });
        }

    public void go()
        {
        super.go();
 
        double[] amplitudes = getAmplitudes(0);        
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
            amplitudes[i] = modulate(i);
            }
        }

    // Only mutate Constants, avoiding other modulations
    public void mutate(double[] noise)
        {
        for(int i = 0; i < NUM_HARMONICS; i++)
            {
            if (getModulation(i) instanceof Constant)
                {
                Constant val = (Constant)getModulation(i);
                double v = val.getValue();
                if (noise[i] < 0)
                    {
                    if (v + noise[i] < 0)
                        {
                        v = v - noise[i];
                        }
                    else
                        {
                        v = v + noise[i];
                        }
                    }
                else // noise[i] >= 0
                    {
                    if (v + noise[i] > 1)
                        {
                        v = v - noise[i];
                        }
                    else
                        {
                        v = v + noise[i];
                        }
                    }
                val.setValue(v);
                }
            }
        }

    public ModulePanel getPanel()
        {
        final ModulationInput[] mi = new ModulationInput[NUM_HARMONICS * 2];
        final ModulePanel[] mp = new ModulePanel[1];
        mp[0] = new ModulePanel(Harmonics.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                
                box.add(new UnitOutput(unit, 0, this));
                
                Box box1 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_HARMONICS/2; i++)
                    {
                    mi[i] = new ModulationInput(unit, i, this);
                    box1.add(mi[i]);
                    }
                                
                Box box2 = new Box(BoxLayout.Y_AXIS);
                for(int i = NUM_HARMONICS/2; i < NUM_HARMONICS; i++)
                    {
                    mi[i] = new ModulationInput(unit, i, this);
                    box2.add(mi[i]);
                    }
                                
                Box box3 = new Box(BoxLayout.X_AXIS);
                box3.add(box1);
                box3.add(Strut.makeHorizontalStrut(3));
                box3.add(box2);
                box.add(box3);

                PushButton sample = new PushButton("Get")
                    {
                    public void perform()
                        {
                        Harmonics harmonics = (Harmonics)(getModulation());
                        int index = harmonics.getSound().findRegistered(harmonics);
                        Output output = getRack().getOutput();
                        int numSounds = output.getNumSounds();
                        output.lock();
                        try
                            {
                            double[] amplitudesIn = harmonics.getAmplitudesIn(0);        
                            for(int i = 0; i < NUM_HARMONICS; i++)
                                {
                                double a = amplitudesIn[i];
                                if (a < 0) a = 0;
                                if (a > 1) a = 1;
                                                                
                                // distribute modulation to all the sounds
                                for(int j = 0; j < numSounds; j++)
                                    {
                                    Sound s = output.getSound(j);
                                    if (s.getGroup() == Output.PRIMARY_GROUP)
                                        {
                                        Harmonics d = (Harmonics)(s.getRegistered(index));

                                        if (d.getModulation(i) instanceof Constant)
                                            ((Constant)(d.getModulation(i))).setValue(a);
                                        }
                                    }
                                }
                            }
                        finally 
                            {
                            output.unlock();
                            }
                        for(int i = 0; i < NUM_HARMONICS; i++)
                            {
                            mi[i].updateText();
                            }
                        mp[0].repaint();
                        }
                    };
                
                Box box5 = new Box(BoxLayout.X_AXIS);
                box5.add(new UnitInput(unit, 0, this));
                box5.add(sample);
                box5.add(Box.createGlue());
                box.add(box5);


                PushButton mutate = new PushButton("Mutate", new String[] { "0.1%", "0.3%", "1%", "3%", "10%", "30%", "100%" } )
                    {
                    public void perform(int result)
                        {
                        Harmonics harmonics = (Harmonics)(getModulation());
                        int index = harmonics.getSound().findRegistered(harmonics);
                        Output output = getRack().getOutput();
                        int numSounds = output.getNumSounds();
                        output.lock();
                        try
                            {
                            // build noise array
                            double[] amts = new double[] { 0.001, 0.003, 0.01, 0.03, 0.10, 0.3, 1.0};
                            double amt = amts[result];
                            double[] noise = new double[NUM_HARMONICS];
                            Random random = output.getSound(0).getRandom();
                            for(int i = 0; i < noise.length; i++)
                                {
                                noise[i] = random.nextDouble() * amt - amt / 2.0;
                                }
                                
                            // randomize
                            for(int j = 0; j < numSounds; j++)
                                {
                                Sound s = output.getSound(j);
                                if (s.getGroup() == Output.PRIMARY_GROUP)
                                    {
                                    Harmonics d = (Harmonics)(s.getRegistered(index));
                                    d.mutate(noise);
                                    }
                                }
                            }
                        finally 
                            {
                            output.unlock();
                            }
                        for(int i = 0; i < NUM_HARMONICS; i++)
                            {
                            mi[i].updateText();
                            }
                        mp[0].repaint();
                        }
                    };
                box.add(mutate);
                
                return box;
                }
            };
        return mp[0];
        }

    }

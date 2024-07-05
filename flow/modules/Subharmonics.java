// Copyright 2023 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;
import java.util.*;

/**
   A Unit which allows you to set the amplitudes of
   up to the first 32 "subharmonics", but not their frequency.
*/

public class Subharmonics extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;
        
    public static final int MAX_SUBHARMONICS = 32;

    public Subharmonics(Sound sound) 
        {
        super(sound);
        Constant[] con = new Constant[MAX_SUBHARMONICS];
        String[] str = new String[MAX_SUBHARMONICS];
        for(int i = 0; i < MAX_SUBHARMONICS; i++)
            {
            con[i] = new Constant(0);  // we need different Constants so we can update them programmatically, see below
            str[i] = (i == 0 ? "Amp 1" : ("Amp 1/" + (i + 1)));
            }
        defineModulations(con, str);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Source" });
        }

    public void go()
        {
        super.go();
 
 		copyAmplitudes(0);
 		copyFrequencies(0);
 		
        double[] amplitudes = getAmplitudes(0);        
        double[] frequencies = getFrequencies(0);        
        for(int i = 0; i < MAX_SUBHARMONICS; i++)
            {
            amplitudes[amplitudes.length - i - 1] = modulate(i);
            frequencies[frequencies.length - i - 1] = 1.0 / (i + 1.0);
            }
        }

    // Only mutate Constants, avoiding other modulations
    public void mutate(double[] noise)
        {
        for(int i = 0; i < MAX_SUBHARMONICS; i++)
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
        final ModulationInput[] mi = new ModulationInput[MAX_SUBHARMONICS];
        final ModulePanel[] mp = new ModulePanel[1];
        mp[0] = new ModulePanel(Subharmonics.this)
            {
            public JComponent buildPanel()
                {
                Unit unit = (Unit) getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new UnitOutput(unit, 0, this));
                
                
                Box box1 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < MAX_SUBHARMONICS/2; i++)
                    {
                    mi[i] = new ModulationInput(unit, i, this);
                    box1.add(mi[i]);
                    }

                Box box2 = new Box(BoxLayout.Y_AXIS);
                for(int i = MAX_SUBHARMONICS/2; i < MAX_SUBHARMONICS; i++)
                    {
                    mi[i] = new ModulationInput(unit, i, this);
                    box2.add(mi[i]);
                    }

                Box box3 = new Box(BoxLayout.X_AXIS);
                box3.add(box1);
                box3.add(Strut.makeHorizontalStrut(3));
                box3.add(box2);
                box.add(box3);
                
                PushButton mutate = new PushButton("Mutate", new String[] { "0.1%", "0.3%", "1%", "3%", "10%", "30%", "100%" } )
                    {
                    public void perform(int result)
                        {
                        Subharmonics subharmonics = (Subharmonics)(getModulation());
                        int index = subharmonics.getSound().findRegistered(subharmonics);
                        Output output = getRack().getOutput();
                        int numSounds = output.getNumSounds();
                        output.lock();
                        try
                            {
                            // build noise array
                            double[] amts = new double[] { 0.001, 0.003, 0.01, 0.03, 0.10, 0.3, 1.0 };
                            double amt = amts[result];
                            double[] noise = new double[MAX_SUBHARMONICS];
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
                                    Subharmonics d = (Subharmonics)(s.getRegistered(index));
                                    d.mutate(noise);
                                    }
                                }
                            }
                        finally 
                            {
                            output.unlock();
                            }
                        for(int i = 0; i < MAX_SUBHARMONICS; i++)
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

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;

/**
   A Unit which allows you to set the amplitudes of
   up to the first 16 harmonics, but not their frequency.
*/

public class Harmonics extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;
        
    public static final int NUM_HARMONICS = 16;

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
                
                for(int i = 0; i < NUM_HARMONICS; i++)
                    {
                    mi[i] = new ModulationInput(unit, i, this);
                    box.add(mi[i]);
                    }

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
                                    Harmonics d = (Harmonics)(output.getSound(j).getRegistered(index));

                                    if (d.getModulation(i) instanceof Constant)
                                        ((Constant)(d.getModulation(i))).setValue(a);
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
                
                box.add(sample);
                box.add(new UnitInput(unit, 0, this));
                return box;
                }
            };
        return mp[0];
        }

    }

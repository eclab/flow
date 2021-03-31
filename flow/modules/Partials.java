// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;

/**
   A Unit which allows you to set the amplitudes and
   frequency of up to 8 partials.
*/


public class Partials extends Unit implements UnitSource
{
    private static final long serialVersionUID = 1;
        
    public static final int NUM_PARTIALS = 16;
    
    public static final double FREQUENCY_RANGE = (NUM_PARTIALS * 2);

    public Partials(Sound sound) 
    {
        super(sound);
        Constant[] con = new Constant[NUM_PARTIALS * 2];
        String[] str = new String[NUM_PARTIALS * 2];
        for(int i = 0; i < NUM_PARTIALS; i++)
            {
                con[i] = new Constant((i + 1) / FREQUENCY_RANGE);
                con[i + NUM_PARTIALS] = new Constant(0);  // we do this rather than Constant.ZERO so we can change them programmatically via Capture later
                str[i] = "Freq " + (i + 1);
                str[i + NUM_PARTIALS] = "Amp " + (i + 1);
            }
        defineModulations(con, str);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Source" });
    }
        
    public void go()
    {
        super.go();
 
        double[] amplitudes = getAmplitudes(0);        
        double[] frequencies = getFrequencies(0);  
              
        for(int i = 0; i < NUM_PARTIALS; i++)
            {
                frequencies[i] = modulate(i) * FREQUENCY_RANGE;
                amplitudes[i] = modulate(i + NUM_PARTIALS);
            }
    }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
    {
        if (modulation < NUM_PARTIALS && isConstant)
            return String.format("%.2f", value * FREQUENCY_RANGE);
        else return super.getModulationValueDescription(modulation, value, isConstant);
    }

    public ModulePanel getPanel()
    {
        final ModulationInput[] mi = new ModulationInput[NUM_PARTIALS * 2];
        final ModulePanel[] mp = new ModulePanel[1];
        mp[0] = new ModulePanel(Partials.this)
            {
                public JComponent buildPanel()
                {               
                    JLabel example = new JLabel("" + FREQUENCY_RANGE + ".00");
                    Box box = new Box(BoxLayout.Y_AXIS);
                    Unit unit = (Unit) getModulation();
                    box.add(new UnitOutput(unit, 0, this));
                
                    /*                
                                      Box box2 = new Box(BoxLayout.X_AXIS);
                                      Box box3 = new Box(BoxLayout.Y_AXIS);
                                      for(int i = 0; i < NUM_PARTIALS; i++)
                                      {
                                      mi[i] = new ModulationInput(unit, i, this);
                                      mi[i].getData().setMinimumSize(example.getMinimumSize());
                                      box3.add(mi[i]);
                                      }
                                      box2.add(box3);

                                      Box box4 = new Box(BoxLayout.Y_AXIS);
                                      for(int i = NUM_PARTIALS; i < NUM_PARTIALS * 2; i++)
                                      {
                                      box4.add(mi[i] = new ModulationInput(unit, i, this));
                                      }
                                      box2.add(box4);
                                      box.add(box2);
                    */

                    Box box1 = new Box(BoxLayout.Y_AXIS);
                    for(int i = 0; i < NUM_PARTIALS; i++)
                        {
                            mi[i] = new ModulationInput(unit, i, this);
                            box1.add(mi[i]);
                        }
                                
                    Box box2 = new Box(BoxLayout.Y_AXIS);
                    for(int i = NUM_PARTIALS; i < NUM_PARTIALS * 2; i++)
                        {
                            mi[i] = new ModulationInput(unit, i , this);
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
                                Partials partials = (Partials)(getModulation());
                                int index = partials.getSound().findRegistered(partials);
                                Output output = getRack().getOutput();
                                int numSounds = output.getNumSounds();
                                output.lock();
                                try
                                    {
                                        double[] amplitudesIn = partials.getAmplitudesIn(0);        
                                        double[] frequenciesIn = partials.getFrequenciesIn(0);
                                        for(int i = 0; i < NUM_PARTIALS; i++)
                                            {
                                                // compute the frequency and amplitude
                                                double m = frequenciesIn[i] / FREQUENCY_RANGE;
                                                if (m < 0) m = 0;
                                                if (m > 1) m = 1;
                                                                
                                                double a = amplitudesIn[i];
                                                if (a < 0) a = 0;
                                                if (a > 1) a = 1;
                                                                
                                                // distribute modulation to all the sounds
                                                for(int j = 0; j < numSounds; j++)
                                                    {
                                                        Sound s = output.getSound(j);
                                                        if (s.getGroup() == Output.PRIMARY_GROUP)
                                                            {
                                                                Partials d = (Partials)(s.getRegistered(index));

                                                                if (d.getModulation(i) instanceof Constant)
                                                                    ((Constant)(d.getModulation(i))).setValue(m);
                                                                if (d.getModulation(i + NUM_PARTIALS) instanceof Constant)
                                                                    ((Constant)(d.getModulation(i + NUM_PARTIALS))).setValue(a);
                                                            }
                                                    }
                                            }
                                    }
                                finally 
                                    {
                                        output.unlock();
                                    }
                                for(int i = 0; i < NUM_PARTIALS * 2; i++)
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
                    return box;
                }
            };
        return mp[0];
    }

}

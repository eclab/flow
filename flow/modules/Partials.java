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
        
    public static final int NUM_PARTIALS = 8;

    public Partials(Sound sound) 
        {
        super(sound);
        Constant[] con = new Constant[NUM_PARTIALS * 2];
        String[] str = new String[NUM_PARTIALS * 2];
        for(int i = 0; i < NUM_PARTIALS; i++)
            {
            con[i] = new Constant((i + 1) / (NUM_PARTIALS - 1));
            con[i + NUM_PARTIALS] = Constant.ZERO;
            str[i] = "Freq " + (i + 1);
            str[i + NUM_PARTIALS] = "Amp";
            }
        defineModulations(con, str);
        }

    public void go()
        {
        super.go();
 
        double[] amplitudes = getAmplitudes(0);        
        double[] frequencies = getFrequencies(0);  
              
        for(int i = 0; i < NUM_PARTIALS; i++)
            {
            frequencies[i] = modulate(i) * (NUM_PARTIALS - 1);
            amplitudes[i] = modulate(i + NUM_PARTIALS);
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation < NUM_PARTIALS && isConstant)
            return String.format("%.2f", value * (NUM_PARTIALS - 1));
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Partials.this)
            {
            public JComponent buildPanel()
                {               
                JLabel example = new JLabel("" + (NUM_PARTIALS - 1) + ".00");
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                                
                Box box2 = new Box(BoxLayout.X_AXIS);
                Box box3 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_PARTIALS; i++)
                    {
                    ModulationInput m = new ModulationInput(unit, i, this);
                    m.getData().setMinimumSize(example.getMinimumSize());
                    box3.add(m);
                    }
                box2.add(box3);

                Box box4 = new Box(BoxLayout.Y_AXIS);
                for(int i = NUM_PARTIALS; i < NUM_PARTIALS * 2; i++)
                    {
                    box4.add(new ModulationInput(unit, i, this));
                    }
                box2.add(box4);
                                
                box.add(box2);
                return box;
                }
            };
        }

    }

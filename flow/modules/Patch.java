// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**
 */

public class Patch extends Unit implements Miscellaneous
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MODULATIONS = 8;
    public static final int NUM_UNITS = 8;
        
    public Patch(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "A", "B", "C", "D", "E", "F", "G", "H" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO }, new String[] { "1", "2", "3", "4", "5", "6", "7", "8" });
        defineOutputs(new String[] { "A", "B", "C", "D", "E", "F", "G", "H" });
        defineModulationOutputs(new String[] { "1", "2", "3", "4", "5", "6", "7", "8" });
        }
                
    public void go()
        {
        super.go();

        for(int i = 0; i < NUM_UNITS; i++)
            {
            pushFrequencies(i, i);
            pushAmplitudes(i, i);
            }
                
        for(int i = 0; i < NUM_MODULATIONS; i++)
            {
            setModulationOutput(i, modulate(i));
            }
        }       
    
    public boolean isConstrainable() { return false; }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Patch.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);

                for(int i = 0; i < NUM_UNITS; i++)
                    {
                    final int _i = i;

                    Box hbox = new Box(BoxLayout.X_AXIS);
                    UnitOutput output = new UnitOutput((Unit)mod, i, this);
                    output.setTitleText("", false);
                    UnitInput input = new UnitInput((Unit)mod, i, this);
                    hbox.add(input);
                    hbox.add(output);
                    box.add(hbox);
                    }

                for(int i = 0; i < NUM_MODULATIONS; i++)
                    {
                    final int _i = i;

                    Box hbox = new Box(BoxLayout.X_AXIS);
                    ModulationOutput output = new ModulationOutput(mod, i, this);
                    output.setTitleText("", false);
                    ModulationInput input = new ModulationInput(mod, i, this, false);
                    hbox.add(input);
                    hbox.add(output);
                    box.add(hbox);
                    }
                return box;
                }
            };
        }



    }

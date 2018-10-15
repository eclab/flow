// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;

/**
   A module which works with the Macro facility to route
   higher-level partials and modulations to your patch as 
   unit and modulation outputs. All eight unit and modulation
   outputs can be custom named by clicking on their labels.
*/


public class In extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MOD_INPUTS = 4;
    public static final int NUM_UNIT_INPUTS = 4;
    Macro macro = null;
        
    public In(Sound sound)
        {
        super(sound);
        defineModulationOutputs( new String[] { "1", "2", "3", "4" });
        defineOutputs(new String[] { "A", "B", "C", "D" });
        defineInputs(new Unit[0], new String[0]);
        standardizeFrequencies();
        setOrders();
        }

    void setMacro(Macro macro) { this.macro = macro; }
                
    public void go()
        {
        super.go();
        
        for(int i = 0; i < NUM_MOD_INPUTS; i++)
            {
            if (macro != null)
                setModulationOutput(i, macro.modulate(i));
            else 
                setModulationOutput(i, 0.0);  // probably don't need to do this, but...
            }

        for(int i = 0; i < NUM_UNIT_INPUTS; i++)
            {
            if (macro != null)
                {
                double[] amplitudes = getAmplitudes(i);
                double[] frequencies = getFrequencies(i);
                byte[] orders = getOrders(i);

                System.arraycopy(macro.getAmplitudesIn(i), 0, amplitudes, 0, amplitudes.length);
                System.arraycopy(macro.getFrequencies(i), 0, frequencies, 0, frequencies.length);
                System.arraycopy(macro.getOrdersIn(i), 0, orders, 0, orders.length);
                }
            else
                {
                // do nothing
                }
            }
        }


    public ModulePanel getPanel()
        {
        ModulePanel panel = new ModulePanel(this)
            {
            public void updateTitleChange(InputOutput inout, int number, String newTitle)
                {
                // Here we're going to redistribute the title to all the Ins in the patch
                Rack rack = getRack();
                if (!inout.isInput())
                    {
                    if (inout.isUnit())
                        {
                        ModulePanel[] modpanels = rack.getAllModulePanels();
                        for(int i = 0; i < modpanels.length; i++)
                            {
                            ModulePanel modpanel = modpanels[i];
                            if (modpanel.getModulation() instanceof In)
                                {
                                modpanel.getUnitOutputs()[number].setTitleText(newTitle);
                                }
                            }
                        }
                    else
                        {
                        ModulePanel[] modpanels = rack.getAllModulePanels();
                        for(int i = 0; i < modpanels.length; i++)
                            {
                            ModulePanel modpanel = modpanels[i];
                            if (modpanel.getModulation() instanceof In)
                                {
                                modpanel.getModulationOutputs()[number].setTitleText(newTitle);
                                }
                            }
                        }
                    }
                }
            };
        
        ModulationOutput[] a = panel.getModulationOutputs();
        for(int i = 0; i < a.length; i++)
            a[i].setTitleCanChange(true);

        UnitOutput[] b = panel.getUnitOutputs();
        for(int i = 0; i < b.length; i++)
            b[i].setTitleCanChange(true);

        return panel;
        }
    }

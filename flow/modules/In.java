// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import org.json.*;
import java.awt.*;
import javax.swing.*;

/**
   A module which works with the Macro facility to route
   higher-level partials and modulations to your patch as 
   unit and modulation outputs. All eight unit and modulation
   outputs can be custom named by clicking on their labels.
*/


public class In extends Unit implements Miscellaneous
{
    private static final long serialVersionUID = 1;

    public static final int NUM_MOD_INPUTS = 8;
    public static final int NUM_UNIT_INPUTS = 8;
    public static final String[] UNIT_NAMES = new String[]  { "A", "B", "C", "D", "E", "F", "G", "H" };
    public static final String[] MOD_NAMES = new String[] { "1", "2", "3", "4", "5", "6", "7", "8" };
       
    public In(Sound sound)
    {
        super(sound);
        // we want the original names around so we can refer to them later
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO }, (String[])(MOD_NAMES.clone()));
        // notice that these are cloned.  This is so MOD_NAMES and UNIT_NAMES can't be changed via setModulationOutput() etc.
        // See getKeyForModulation() below for a hint as to why
        defineModulationOutputs((String[])(MOD_NAMES.clone()));
        defineOutputs((String[])(UNIT_NAMES.clone()));
        defineInputs(new Unit[0], new String[0]);
        standardizeFrequencies();
        setOrders();
    }

    public void go()
    {
        super.go();
                
        for(int i = 0; i < NUM_MOD_INPUTS; i++)
            {
                if (macro != null)
                    {
                        setModulationOutput(i, macro.modulate(i));
                    }
                else 
                    {
                        setModulationOutput(i, modulate(i));    // set outputs to the defaults the user set
                    }
            }

        for(int i = 0; i < NUM_UNIT_INPUTS; i++)
            {
                if (macro != null)
                    {
                        double[] amplitudes = getAmplitudes(i);
                        double[] frequencies = getFrequencies(i);
                        byte[] orders = getOrders(i);

                        System.arraycopy(macro.getAmplitudesIn(i), 0, amplitudes, 0, amplitudes.length);
                        System.arraycopy(macro.getFrequenciesIn(i), 0, frequencies, 0, frequencies.length);
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
        final ModulePanel[] modpanel = new ModulePanel[1];
        final ModulationInput[] modIn = new ModulationInput[NUM_MOD_INPUTS];
        final ModulationOutput[] modOut = new ModulationOutput[NUM_MOD_INPUTS];

        modpanel[0] = new ModulePanel(this)
            {
                public JComponent buildPanel()
                {
                    Unit unit =  (Unit) getModulation();
                    Box outer = new Box(BoxLayout.Y_AXIS);
                
                    for(int i = 0; i < NUM_UNIT_INPUTS; i++)
                        {
                            outer.add(new UnitOutput(unit, i, this));
                        }
                        
                    for(int i = 0; i < NUM_MOD_INPUTS; i++)
                        {
                            outer.add(modOut[i] = new ModulationOutput(unit, i, this));
                        }

                    for(int i = 0; i < NUM_MOD_INPUTS; i++)
                        {
                            outer.add(modIn[i] = new ModulationInput(unit, i, this));
                        }

                    return outer;
                }

                public void setRack(Rack rack)
                {
                    super.setRack(rack);
                    ModulePanel[] all = rack.getAllModulePanels();
                
                    // are there any other Ins?  Find the first one
                    for(int i = 0; i < all.length; i++)
                        {
                            if (all[i].getModulation() instanceof In && all[i] != this)
                                {
                                    // set me to the same values as the first one
                                    ModulationOutput[] a = getModulationOutputs();
                                    ModulationOutput[] aa = all[i].getModulationOutputs();
                                    for(int j = 0; j < a.length; j++)
                                        a[j].setTitleText(aa[j].getTitleText().trim());

                                    UnitOutput[] b = getUnitOutputs();
                                    UnitOutput[] bb = all[i].getUnitOutputs();
                                    for(int j = 0; j < b.length; j++)
                                        b[j].setTitleText(bb[j].getTitleText().trim());

                                    break;  // we're done, no more changing
                                }
                        }
                }
                
                public void updateTitleChange(InputOutput inout, int number, String newTitle)
                {
                    // update the assocated Input
                    for(int i = 0; i < NUM_MOD_INPUTS; i++)
                        {
                            modIn[i].setTitleText(modOut[i].getTitleText().trim());
                        }
                
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

        ModulationInput[] a = modpanel[0].getModulationInputs();
        for(int i = 0; i < a.length; i++)
            a[i].setTitleText("", false);

        ModulationOutput[] c = modpanel[0].getModulationOutputs();
        for(int i = 0; i < c.length; i++)
            c[i].setTitleCanChange(true);

        UnitOutput[] b = modpanel[0].getUnitOutputs();
        for(int i = 0; i < b.length; i++)
            b[i].setTitleCanChange(true);

        // update the assocated Input
        for(int i = 0; i < NUM_MOD_INPUTS; i++)
            {
                modIn[i].setTitleText(modOut[i].getTitleText().trim());
            }
                                
        return modpanel[0];
    }


    //// SERIALIZATION STUFF
    
    ////// Why are we overriding these three methods?  Because we want to use the standard "A B C D"
    ////// as the KEYS for connection regardless of what the user sets the actual names to.  That way
    ////// he can name the all "foo" if he likes, and they're still unique names.  
    
    public String getKeyForModulation(int input)
    {
        // notice we're reusing a lot of stuff from getKeyForModulationOutput below
        if (input < NUM_MOD_INPUTS) return MOD_NAMES[input];
        else return super.getKeyForModulation(input);
    }
   
    public String getKeyForModulationOutput(int output)
    {
        if (output < NUM_MOD_INPUTS) return MOD_NAMES[output];
        else return super.getKeyForModulationOutput(output);
    }
   
    public String getKeyForOutput(int output)
    {
        if (output < NUM_UNIT_INPUTS) return UNIT_NAMES[output];
        else return super.getKeyForOutput(output);
    }

    public void setData(JSONObject data, int moduleVersion, int patchVersion) 
    {
        if (data == null)
            warn("flow/modules/In.java", "Empty Data for In.  That can't be right.");
        else
            {
                JSONArray array = data.getJSONArray("mod");
                int num = getNumModulationOutputs();
                if (num != array.length())
                    {
                        warn("flow/modules/In.java", "Number of modulation outputs in In (" + num + ") does not match those in the patch (" + array.length() + ")");
                        if (array.length() < num) num = array.length();
                    }
                for(int i = 0; i < num; i++)
                    {
                        setModulationOutputName(i, array.getString(i));
                    }

                array = data.getJSONArray("unit");
                num = getNumOutputs();
                if (num != array.length())
                    {
                        warn("flow/modules/Out.java", "Number of unit outputs in Out (" + num + ") does not match those in the patch (" + array.length() + ")");
                        if (array.length() < num) num = array.length();
                    }
                for(int i = 0; i < num; i++)
                    {
                        setOutputName(i, array.getString(i));
                    }
            }
    }
    
    public JSONObject getData() 
    { 
        JSONObject obj = new JSONObject();
        
        JSONArray array = new JSONArray();

        for(int i = 0; i < getNumModulationOutputs(); i++)
            array.put(getModulationOutputName(i));
                
        obj.put("mod", array);

        array = new JSONArray();

        for(int i = 0; i < getNumOutputs(); i++)
            array.put(getOutputName(i));
                
        obj.put("unit", array);
        return obj;
    }
}

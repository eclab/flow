// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import org.json.*;

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
    public static final String[] UNIT_NAMES = new String[]  { "A", "B", "C", "D" };
    public static final String[] MOD_NAMES = new String[] { "1", "2", "3", "4" };
    Macro macro = null;
        
    public In(Sound sound)
        {
        super(sound);
        // we want the original names around so we can refer to them later
        defineModulationOutputs((String[])(MOD_NAMES.clone()));
        defineOutputs((String[])(UNIT_NAMES.clone()));
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
        final ModulePanel[] panel = new ModulePanel[1];
        panel[0] = new ModulePanel(this)
            {
            public void setRack(Rack rack)
            	{
            	super.setRack(rack);
            	ModulePanel[] all = rack.getAllModulePanels();
            	
            	// are there any other Ins?  Find the first one
            	for(int i = 0; i < all.length; i++)
            		{
            		if (all[i].getModulation() instanceof In && all[i] != panel[0])
            			{
            			// set me to the same values as the first one
						ModulationOutput[] a = panel[0].getModulationOutputs();
						ModulationOutput[] aa = all[i].getModulationOutputs();
						for(int j = 0; j < a.length; j++)
							a[j].setTitleText(aa[j].getTitleText());

						UnitOutput[] b = panel[0].getUnitOutputs();
						UnitOutput[] bb = all[i].getUnitOutputs();
						for(int j = 0; j < b.length; j++)
							b[j].setTitleText(bb[j].getTitleText());

            			break;  // we're done, no more changing
            			}
            		}
            	}
            	
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
            
        ModulationOutput[] a = panel[0].getModulationOutputs();
        for(int i = 0; i < a.length; i++)
            a[i].setTitleCanChange(true);

        UnitOutput[] b = panel[0].getUnitOutputs();
        for(int i = 0; i < b.length; i++)
            b[i].setTitleCanChange(true);

        return panel[0];
        }


    //// SERIALIZATION STUFF
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
    		for(int i = 0; i < array.length(); i++)
    			{
    			setModulationOutputName(i, array.getString(i));
    			}
    		array = data.getJSONArray("unit");
    		for(int i = 0; i < array.length(); i++)
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

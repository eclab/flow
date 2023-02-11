// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 */

public class Dissolve extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_DISSOLVE = 0;
    public static final int MOD_SEED = 1;

    public static final int TYPE_DISSOLVE = 0;
    public static final int TYPE_WIPE_DOWN = 1;
    public static final int TYPE_WIPE_UP = 2;
     
    int type = TYPE_DISSOLVE;
    public int getType() { return type; }
    public void setType(int val) { type = val; }
     
    public static final int OPTION_TYPE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE: return getType();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE: setType(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }


    public static String getName() { return "Dissolve"; }

    public Dissolve(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "A", "B" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE }, new String[] { "Dissolve", "Seed" });
        defineOptions(new String[] { "Type" }, new String[][] { { "Dissolve", "Wipe Down", "Wipe Up" } });
        }
        
    double lastMod = -1;
    byte[] dissolveMap = new byte[Unit.NUM_PARTIALS];
    
    public void buildDissolveMap()
        {
        Random random = getSound().getRandom();

        // first build Random from seed
        double mod = modulate(MOD_SEED);
        if (mod > 0)
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }

        // load the dissolveMap
        for(int i = 0; i < dissolveMap.length; i++)
            {
            dissolveMap[i] = (byte) i;
            }

        // Next shuffle dissolveMap using Fisher-Yates
        for (int i = 0; i < dissolveMap.length; i++) 
            {
            int randomValue = i + random.nextInt(dissolveMap.length - i);
            byte temp = dissolveMap[randomValue];
            dissolveMap[randomValue] = dissolveMap[i];
            dissolveMap[i] = temp;
            }
            
        // set lastMod
        lastMod = mod;
        }
        
    public void gate()
        {
        if (lastMod == 0) lastMod = -1; // force it to reset
        }

    public void reset()
        {
        if (lastMod == 0) lastMod = -1; // force it to reset
        }

	boolean rebuildDissolveMap = true;
	
    public void go()
        {
        super.go();
            
        
        int type = getType();
    	if (type == TYPE_DISSOLVE)
    		{
	        double mod = modulate(MOD_SEED);
	        if (mod != lastMod || rebuildDissolveMap)             // gotta rebuild the map
				{
				buildDissolveMap();
				rebuildDissolveMap = false;
				}
				
			// Perform interpolation
			copyFrequencies(0);
			copyAmplitudes(0);
			double[] amplitudes = getAmplitudes(0);
			double[] a0 = getAmplitudesIn(0);
			double[] a1 = getAmplitudesIn(1);
			double[] frequencies = getFrequencies(0);
			double[] f0 = getFrequenciesIn(0);
			double[] f1 = getFrequenciesIn(1);
			mod = modulate(MOD_DISSOLVE);
		
			double div = (dissolveMap.length + 1) * mod;
			int pivotpartial = (int)(div);
			if (pivotpartial == dissolveMap.length + 1) pivotpartial = dissolveMap.length;  // deal with 1.0
		
			double alpha = div - pivotpartial;
		
			for(int i = 0; i < dissolveMap.length; i++)
				{
				if (i < pivotpartial)
					{
					int p = dissolveMap[i] & 0xFF;
					amplitudes[p] = a1[p];
					frequencies[p] = f1[p];
					}
				else if (i > pivotpartial)
					{
					int p = dissolveMap[i] & 0xFF;
					amplitudes[p] = a0[p];
					frequencies[p] = f0[p];
					}
				else    // i == pivotpartial, use alpha to cross-fade
					{
					int p = dissolveMap[i] & 0xFF;
					amplitudes[p] = alpha * a1[p] + (1-alpha) * a0[p];
					frequencies[p] = alpha * f1[p] + (1-alpha) * f0[p];
					}
				}

			constrain();
			simpleSort(0, true);
			}
		else
			{
			rebuildDissolveMap = true;		// just in case
			
			// Perform interpolation
			copyFrequencies(0);
			copyAmplitudes(0);
			double[] amplitudes = getAmplitudes(0);
			double[] a0 = getAmplitudesIn(0);
			double[] a1 = getAmplitudesIn(1);
			double[] frequencies = getFrequencies(0);
			double[] f0 = getFrequenciesIn(0);
			double[] f1 = getFrequenciesIn(1);
			
			double mod = modulate(MOD_DISSOLVE);
			double val = mod * amplitudes.length;
			int partial = (int)val;
			if (partial == amplitudes.length) 
				{
				partial = (amplitudes.length - 1);
				}
			double alpha = val - partial;
			
			if (type == TYPE_WIPE_DOWN)
				{
				// no need for this part
				/*
				for(int i = 0; i < partial; i++)
					{
					amplitudes[i] = a0[i];
					frequencies[i] = f0[i];
					}
				*/
				for(int i = partial + 1; i < amplitudes.length; i++)
					{
					amplitudes[i] = a1[i];
					frequencies[i] = f1[i];
					}
				amplitudes[partial] = alpha *  a0[partial] + (1.0 - alpha) * a1[partial];
				frequencies[partial] = alpha *  f0[partial] + (1.0 - alpha) * f1[partial];
				}
			else // if (type == TYPE_WIPE_UP)
				{
				// no need for this part
				/*
				for(int i = amplitudes.length - 1; i > partial; i--)
					{
					amplitudes[i] = a0[i];
					frequencies[i] = f0[i];
					}
				*/
				for(int i = partial - 1; i >= 0; i--)
					{
					amplitudes[i] = a1[i];
					frequencies[i] = f1[i];
					}
				amplitudes[partial] = alpha *  a1[partial] + (1.0 - alpha) * a0[partial];
				frequencies[partial] = alpha *  f1[partial] + (1.0 - alpha) * f0[partial];
				}					
			}
        }               


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_SEED)
                {
                return (value == 0.0 ? "Free" : String.format("%.4f" , value));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
    
    }

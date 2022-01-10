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

    public static String getName() { return "Dissolve"; }

    public Dissolve(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "A", "B" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE }, new String[] { "Dissolve", "Seed" });
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

    public void go()
        {
        super.go();
                
        double mod = modulate(MOD_SEED);
        if (mod != lastMod)             // gotta rebuild the map
            {
            buildDissolveMap();
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

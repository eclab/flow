// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import javax.swing.*;
import java.awt.*;
import flow.gui.*;

/**
   A Unit which provides a filter based on up to eight peaks or valleys whose amplitude and frequency is specified by the user.
   The filter interpolates linearly between these peaks and valleys, much in the same way that PartialsFilter does.  The big
   difference between the two being that these peaks and valleys are modulatable.  LinearFilter also provides a BASE FREQUENCY
   which is added to all the other frequencies so as to shift the filter up and down.  Partials lower than Frequency 1 * base are filtered
   using Gain 1.  Partials higher than Frequency[NumNodes - 1] * base are filtered using Gain[NumNodes - 1].
*/


public class LinearFilter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_NODES = 0;
    public static final int MOD_BASE = 1;

    public static String getName() { return "Linear Filter"; }

    public int MAX_NODES = 8;
    public double MAX_BASE_FREQUENCY = Output.SAMPLING_RATE / 2; //20.0;
    public static final double MIDDLE_C_FREQUENCY = 261.6256;    
        
    double[] nodeGain = new double[MAX_NODES];
    double[] nodeFreq = new double[MAX_NODES];
        
    public Object clone()
        {
        LinearFilter obj = (LinearFilter)(super.clone());
        obj.nodeGain = (double[])(obj.nodeGain.clone());
        obj.nodeFreq = (double[])(obj.nodeFreq.clone());
        return obj;
        }

    public LinearFilter(Sound sound) 
        { 
        super(sound);
                
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineOptions(new String[] {"Relative" }, new String[][] {{"Relative"}});
        defineModulations( new Constant[] { Constant.ONE, Constant.HALF,
                                            Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO,
                                            Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE }, 
            new String[] { "Nodes", 
                           "Base",
                           "Freq 1 ", "Freq 2 ", "Freq 3 ", "Freq 4 ", "Freq 5 ", "Freq 6 ", "Freq 7 ", "Freq 8 ",  
                           "Gain 1", "Gain 2", "Gain 3", "Gain 4", "Gain 5", "Gain 6", "Gain 7", "Gain 8" });
        }
    
    // probably small enough to be inlined (33 bytes)
    void swapNode(int i, int j, double[] f, double[] g)
        {
        double d = f[i];
        f[i] = f[j];
        f[j] = d;
        d = g[i];
        g[i] = g[j];
        g[j] = d;
        }

    void insertionSortNode(double[] freq, double[] gain, int len) 
        {
        for (int i=1; i < len; i++) // Insert i'th record
            for (int j=i; (j > 0) && (freq[j] < freq[j - 1]); j--)
                {
                swapNode(j, j - 1, freq, gain);
                }
        }

    boolean relative = false;
    public boolean getRelative() { return relative; }
    public void setRelative(boolean val) { relative = val; }
        
    public static final int OPTION_RELATIVE = 0;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_RELATIVE: return getRelative() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_RELATIVE: setRelative(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public void go()
        {
        super.go();
 
        int numNodes = (int)(modulate(MOD_NODES) * MAX_NODES);
       
        if (numNodes == 0)                      // no nodes at all
            {
            pushFrequencies(0);
            pushAmplitudes(0);
            return;
            }
        
        pushFrequencies(0);
        copyAmplitudes(0);

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        double pitch = sound.getPitch();

        if (relative)
            {
            pitch = MIDDLE_C_FREQUENCY;
            }
                
        int baseFreq = (int)((modulate(MOD_BASE) * 2 - 1.0) * MAX_BASE_FREQUENCY);
        
        for(int i = 0; i < numNodes; i++)
            nodeFreq[i] = modToInsensitiveFrequency(modulate(i + 2)) + baseFreq;
        for(int i = 0; i < numNodes; i++)
            nodeGain[i] = modulate(i + 2 + MAX_NODES);
        
        // now sort nodes by frequency
        insertionSortNode(nodeFreq, nodeGain, numNodes);
        
        int node = 0;
        for(int i = 0; i < amplitudes.length; i++)
            {
            double freq = frequencies[i] * pitch;
            // First consider the situation where the frequency is lower than the minimum node
            if (freq <= nodeFreq[0])  // (node == 0 && freq <= nodeFreq[0])
                {
                amplitudes[i] *= nodeGain[0];
                }
            // Next consider the situation where the frequency is higher than the maximum node
            else if (freq >= nodeFreq[numNodes-1])
                {
                amplitudes[i] *= nodeGain[numNodes-1];
                }
            else 
                {
                // Find the pair.  This corresponds to the first higher node which is > freq,
                // while the existing node is <= freq
                while (node < (numNodes - 2) && nodeFreq[node + 1] <= freq)
                    {
                    node++;
                    }
                
                // don't want to divide by zero...
                if (nodeFreq[node] == nodeFreq[node + 1])
                    {
                    amplitudes[i] *= nodeGain[node];
                    }
                        
                // finally interpolate between the node and the next node
                else
                    {
                    double alpha = (freq - nodeFreq[node]) / (nodeFreq[node + 1] - nodeFreq[node]);
                    double gain = (1 - alpha) * nodeGain[node] + alpha * nodeGain[node + 1];
                    amplitudes[i] *= gain;
                    }
                }
            }

        constrain();
        }       

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 0)  // Num Nodes
                {
                int numNodes = (int)(value * MAX_NODES);
                return "" + numNodes;
                }
            else if (modulation == 1)  // Base Frequency
                {
                return "" + (int)((modulate(MOD_BASE) * 2 - 1.0) * MAX_BASE_FREQUENCY);
                }
            else if (modulation < MAX_NODES + 2)
                {
                return "" + (int)modToInsensitiveFrequency(value);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    public ModulePanel getPanel()
        {
        return new ModulePanel(LinearFilter.this)
            {
            public JComponent buildPanel()
                {               
                JLabel example = new JLabel("22888");
                example.setFont(Style.SMALL_FONT());
                Box box = new Box(BoxLayout.Y_AXIS);
                Box box1 = new Box(BoxLayout.X_AXIS);
                Unit unit = (Unit) getModulation();
                box1.add(new UnitInput(unit, 0, this));
                box1.add(new UnitOutput(unit, 0, this));
                box.add(box1);
                box1 = new Box(BoxLayout.X_AXIS);
                ModulationInput m = new ModulationInput(unit, MOD_NODES, this);
                m.getData().setMinimumSize(example.getMinimumSize());
                box1.add(m);
                m = new ModulationInput(unit, MOD_BASE, this);
                m.getData().setMinimumSize(example.getMinimumSize());
                m.getData().setPreferredSize(example.getPreferredSize());
                box1.add(m);
                box.add(box1);

                for(int i = 0; i < MAX_NODES; i++)
                    {
                    Box box2 = new Box(BoxLayout.X_AXIS);
                    ModulationInput in = new ModulationInput(unit, i + 2, this);
                    in.getData().setMinimumSize(example.getMinimumSize());
                    box2.add(in);
                    box2.add(new ModulationInput(unit, i + 2 + MAX_NODES, this));
                    box.add(box2);
                    }

                box.add(new OptionsChooser(unit, OPTION_RELATIVE));

                box.add(new ConstraintsChooser(unit, this));

                return box;
                }
            };
        }
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;
import java.util.*;

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
        
        boolean changed = false;
        for(int i = 0; i < NUM_PARTIALS; i++)
            {
            double f = modulate(i) * FREQUENCY_RANGE;
                
            if (f != frequencies[i])
                {
                frequencies[i] = f;
                changed = true;
                }
            
            double a = modulate(i + NUM_PARTIALS);
            if (a != amplitudes[i])
                {
                amplitudes[i] = a;
                changed = true;
                }
            }
        
        int harmonic = 1;
        if (changed)
            {
            for(int i = NUM_PARTIALS; i < amplitudes.length; i++)
                {
                frequencies[i] = FREQUENCY_RANGE + 1 + (harmonic++);
                }
            simpleSort(0, false);
            }
                        
        }

    // Only compresses Constants, avoiding other modulations
    public void compress()
        {
        for(int i = 0; i < NUM_PARTIALS - 1; i++)                               // no need to do last one
            {
            if (getModulation(i) instanceof Constant &&                 // frequency
                getModulation(i + NUM_PARTIALS) instanceof Constant &&          // amplitude
                modulate(i + NUM_PARTIALS) == 0)
                {
                for(int j = i + 1; j < NUM_PARTIALS; j++)
                    {
                    if (getModulation(j) instanceof Constant &&                     // frequency
                        getModulation(j + NUM_PARTIALS) instanceof Constant &&          // amplitude
                        modulate(j + NUM_PARTIALS) != 0)
                        {
                        // swap frequencies
                        Constant from = (Constant)getModulation(i);
                        Constant to = (Constant)getModulation(j);
                        double val = from.getValue();
                        from.setValue(to.getValue());
                        to.setValue(val);

                        // swap amplitudes
                        from = (Constant)getModulation(i + NUM_PARTIALS);
                        to = (Constant)getModulation(j + NUM_PARTIALS);
                        val = from.getValue();
                        from.setValue(to.getValue());
                        to.setValue(val);
                        break;
                        }
                    }
                }
            }

        for(int i = 0; i < NUM_PARTIALS; i++)
            {
            if (getModulation(i) instanceof Constant &&                 // frequency
                getModulation(i + NUM_PARTIALS) instanceof Constant &&          // amplitude
                modulate(i + NUM_PARTIALS) == 0)
                {
                Constant freq = (Constant)getModulation(i);
                freq.setValue(FREQUENCY_RANGE);
                }
            }
        }


    // Only mutate Constants, avoiding other modulations
    public void mutate(double[] noise, boolean freq)
        {
        for(int _i = 0; _i < NUM_PARTIALS; _i++)
            {
            int i = _i;
            if (!freq) i += NUM_PARTIALS;
            
            if (getModulation(i) instanceof Constant)
                {
                Constant val = (Constant)getModulation(i);
                double v = val.getValue();
                if (noise[_i] < 0)
                    {
                    if (v + noise[_i] < 0)
                        {
                        v = v - noise[_i];
                        }
                    else
                        {
                        v = v + noise[_i];
                        }
                    }
                else // noise[_i] >= 0
                    {
                    if (v + noise[_i] > 1)
                        {
                        v = v - noise[_i];
                        }
                    else
                        {
                        v = v + noise[_i];
                        }
                    }
                val.setValue(v);
                }
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

                PushButton compress = new PushButton("Compress")
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
                            // distribute compression to all the sounds
                            for(int j = 0; j < numSounds; j++)
                                {
                                Sound s = output.getSound(j);
                                if (s.getGroup() == Output.PRIMARY_GROUP)
                                    {
                                    Partials d = (Partials)(s.getRegistered(index));
                                    d.compress();
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
                box.add(compress);

                PushButton mutate = new PushButton("Mutate", new String[] { "Freq 0.1%", "Freq 0.3%", "Freq 1%", "Freq 3%", "Freq 10%", "Freq 30%", "Freq 100%", "Amp 0.1%", "Amp 0.3%", "Amp 1%", "Amp 3%", "Amp 10%", "Amp 30%", "Amp 100%" } )
                    {
                    public void perform(int result)
                        {
                        Partials partials = (Partials)(getModulation());
                        int index = partials.getSound().findRegistered(partials);
                        Output output = getRack().getOutput();
                        int numSounds = output.getNumSounds();
                        output.lock();
                        try
                            {
                            // build noise array
                            double[] amts = new double[] { 0.001, 0.003, 0.01, 0.03, 0.10, 0.3, 1.0, 0.001, 0.003, 0.01, 0.03, 0.10, 0.3, 1.0};
                            double amt = amts[result];
                            double[] noise = new double[NUM_PARTIALS];
                            Random random = output.getSound(0).getRandom();
                            for(int i = 0; i < noise.length; i++)
                                {
                                noise[i] = random.nextDouble() * amt - amt / 2.0;
                                }
                                
                            // randomize
                            for(int j = 0; j < numSounds; j++)
                                {
                                Sound s = output.getSound(j);
                                if (s.getGroup() == Output.PRIMARY_GROUP)
                                    {
                                    Partials d = (Partials)(s.getRegistered(index));
                                    d.mutate(noise, result < amts.length / 2);
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
                box.add(mutate);
                
                return box;
                }
            };
        return mp[0];
        }

    }

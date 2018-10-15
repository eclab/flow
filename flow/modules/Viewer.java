// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
   A Unit which displays its values in a JComponent.  This is similar to flow.gui.Display,
   and in fact is an earlier version of the same.  The purpose of Viewer is for testing purposes
   in the testing suite, and that's all.
   
   <p>Viewer also sports eight JSliders, and can create Modulations based on those JSliders.
   The testing suite uses these Modulations to allow you to tweak parameters to see response.
*/




public class Viewer extends Unit
    {
    private static final long serialVersionUID = 1;

    int q = 0;
        
    JLabel[] label = new JLabel[8];
    JSlider[] slider = new JSlider[8];
    Modulation[] mods = new Modulation[8];
        
    public void setupMods()
        {
        for(int i = 0; i < 8; i++)
            {
            final int _i = i;
            mods[i] = new Modulation(getSound())
                {
                public void go() { setModulationOutput(0, slider[_i].getValue() / 100.0); }
                };
            }
        }
                
    public Modulation getModulation(final int i, String text, double initialValue)
        {
        label[i].setText(text);
        label[i].revalidate();
        slider[i].setValue((int)(initialValue * 100));
        return mods[i];
        }
        
    public Viewer(Sound sound)
        {
        super(sound);
                
        defineInputs(new Unit[] { Unit.NIL }, new String[] { "Input" });
                
        final ViewerPanel panel = new ViewerPanel();
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(panel, BorderLayout.CENTER);
                
        Box buttons = new Box(BoxLayout.X_AXIS);
        for(int i = 0; i < 4; i++)
            {
            label[i] = new JLabel("FooBarBaz");
            buttons.add(label[i]);
            slider[i] = new JSlider(0, 100, 0);
            slider[i].addChangeListener(new ChangeListener()
                {
                public void stateChanged(ChangeEvent e) { for(int j = 0; j < 8; j++) System.err.println(label[j].getText() + " = " + (slider[j].getValue() / 100.0)); }
                });
            buttons.add(slider[i]);
            }
        buttons.add(Box.createGlue());
        p.add(buttons, BorderLayout.SOUTH);

        buttons = new Box(BoxLayout.X_AXIS);
        for(int i = 4; i < 8; i++)
            {
            label[i] = new JLabel("FooBarBaz");
            buttons.add(label[i]);
            slider[i] = new JSlider(0, 100, 0);
            final int _i = i;
            slider[i].addChangeListener(new ChangeListener()
                {
                public void stateChanged(ChangeEvent e) { for(int j = 0; j < 8; j++) System.err.println(label[j].getText() + " = " + (slider[j].getValue() / 100.0)); }
                });
            buttons.add(slider[i]);
            }
        buttons.add(Box.createGlue());
        p.add(buttons, BorderLayout.NORTH);
                
        setupMods();
                
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(p, BorderLayout.CENTER);
        frame.setSize(1024, 300);
        frame.setVisible(true);
                
        Timer timer = new Timer(10, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                panel.repaint();
                SwingUtilities.invokeLater(new Runnable() { public void run() { } });
                }
            });
        timer.start();
                
                
        }
                
    public void go()
        {
        super.go();

        synchronized(this)
            {
            copyFrequencies(0);
            copyAmplitudes(0);
            }
        }
        
    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 180);
        
    class ViewerPanel extends JComponent
        {
        public static final double MAX_FREQUENCY = 150;
        public static final double MAX_AMPLITUDE = 1.0;
        public static final int BORDER = 8;
        
        public void paintComponent(Graphics graphics)
            {
            double[] amplitudes;
            double[] frequencies;

            synchronized(Viewer.this)
                {
                amplitudes = (double[])(getAmplitudes(0).clone());
                frequencies = (double[])(getFrequencies(0).clone());
                }

            Graphics2D g = (Graphics2D) graphics;
            int height = getHeight();
            int width = getWidth();

            g.setColor(Color.BLACK);
            g.fill(new Rectangle2D.Double(0, 0, width, height));
                        
            g.setColor(DARK_BLUE);
            double xx = (width - BORDER * 2) / MAX_FREQUENCY * (Output.SAMPLING_RATE / 2.0) / getSound().getPitch() + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));
                
            g.setColor(DARK_GREEN);
            xx = (width - BORDER * 2) / MAX_FREQUENCY * (1) + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));
                
            for(int i = 0; i < frequencies.length; i++)
                {
                double x = (width - BORDER * 2) / MAX_FREQUENCY * frequencies[i] + BORDER;
                double y = (height - BORDER * 2) / MAX_AMPLITUDE * amplitudes[i];
                        
                if (amplitudes[i] > 1 || amplitudes[i] == 0)
                    {
                    g.setColor(Color.RED);
                    }
                else
                    {
                    g.setColor(Color.WHITE);
                    }
                g.draw(new Line2D.Double(x, height - BORDER, x, height - BORDER - y));
                }
                
            }
        }
    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import flow.modules.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
   A display for Modulation signals.
*/

public class Oscilloscope extends JComponent
    {
    public static final int WAVE_SIZE = 96;
    public static final int WAVE_HEIGHT = 96;
    public static final int BORDER = 8;
    public static final float STROKE_WIDTH = 1.25f;
    
    public static final int FUNCTION_MAIN_MOD = 0;
    public static final int FUNCTION_AUX_MOD = 1;
    public static final int FUNCTION_OUTPUT = 2;
    
    public static final int INTERVAL_MS = 20;
    
    public static final Color CLIP_COLOR = Color.RED;
    
    Output output;
    int function;
    Color axisColor = DARK_GREEN;
    Stroke stroke;

    // are we currently triggered?
    boolean trig = false;
    
    // are we clipping
    boolean clip = false;
    
    public void setClipping(boolean val) { clip = val; }
    public boolean isClipping() { return clip; }
    
    // wave is a circular buffer of the current wave information; and wavePos is our position in it
    double[] wave = new double[WAVE_SIZE];
    int wavePos = 0;

    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 180);
        
    public Oscilloscope(Output output, int function)
        {
        this.function = function;
        this.output = output;
        stroke = new BasicStroke(STROKE_WIDTH);

        Timer timer = new Timer(INTERVAL_MS, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                repaint();
                SwingUtilities.invokeLater(new Runnable() { public void run() { } });
                }
            });
        timer.start();
        }
                
    public Dimension getMinimumSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    public Dimension getPreferredSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    public Dimension getMaximumSize() { return new Dimension(WAVE_SIZE + BORDER * 2, WAVE_HEIGHT + BORDER * 2); }
    
    public void paintComponent(Graphics graphics)
        {
        int p = 0;
        output.lock();
        try
            {
            Sound sound = output.getInput().getLastPlayedSound();
            try 
                {
                if (sound == null) 
                    sound = output.getSound(0);
                }
            catch (Exception e)
                {
                sound = null;
                }
                        
            if (sound != null)
                {
                Unit emit = sound.getEmits();

                if (emit != null && emit instanceof Out)
                    {
                    if (function <= FUNCTION_AUX_MOD)
                        {
                        Out out = (Out)emit;
                        System.arraycopy(out.getModWave(function), 0, wave, 0, wave.length);
                        p = out.getWavePos(function);
                        if (out.getAndClearWaveTriggered(function))
                            trig = !trig;
                        }
                    else
                        {
                        synchronized(output.leftSamplesOut)
                            {
                            System.arraycopy(output.leftSamplesOut, 0, wave, 0, wave.length);
                            }
                        for(int i = 0; i < wave.length; i++)
                            {
                            wave[i] *= (0.5 / 32768);
                            wave[i] += 0.5;
                            }
                        }
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        
    RenderingHints rh = new RenderingHints(
             RenderingHints.KEY_ANTIALIASING,
             RenderingHints.VALUE_ANTIALIAS_ON);
             
             
        // fill with black regardless
        Graphics2D g = (Graphics2D) graphics;
        g.setRenderingHints(rh);
        int height = getHeight();
        int width = getWidth();
        g.setColor(Color.BLACK);
        g.fill(new Rectangle2D.Double(0, 0, width, height));

        // draw axis                                        
        g.setColor(trig ? DARK_BLUE : DARK_GREEN);
        g.draw(new Line2D.Double(BORDER, BORDER + WAVE_HEIGHT / 2.0, WAVE_SIZE + BORDER, BORDER + WAVE_HEIGHT / 2.0));
        g.draw(new Line2D.Double(BORDER + WAVE_SIZE / 2.0, BORDER, BORDER + WAVE_SIZE / 2.0, WAVE_HEIGHT + BORDER));

        // Draw the wave
        g.setColor(Color.WHITE);
        g.setStroke(stroke);
        int q = 0;
        for(int i = 0; i < WAVE_SIZE - 1; i++)
            {
            q = p + 1;
            if (q >= WAVE_SIZE) q = 0;
            double x = i + BORDER;
            double y = WAVE_HEIGHT * (1 - wave[p]) + BORDER;
            double xx = x + 1;
            double yy = WAVE_HEIGHT * (1 - wave[q]) + BORDER;
            g.draw(new Line2D.Double(x, y, xx, yy));
            if (clip && (wave[p] >= (1.0 / 65536) * 65535 || wave[p] <= 0.0))
                {
                g.setColor(CLIP_COLOR);
                g.draw(new Line2D.Double(x, y, x, y));
                g.setColor(Color.WHITE);
                }

            p++;
            if (p >= WAVE_SIZE) p = 0;
            }
        }
    }

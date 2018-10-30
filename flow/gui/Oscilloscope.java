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
    public static final int WAVE_SIZE = 100;
    public static final int WAVE_HEIGHT = 100;
    public static final int BORDER = 8;
    public static final float STROKE_WIDTH = 1f;
    
    Output output;
    boolean aux;
    Color axisColor = DARK_GREEN;
    Stroke stroke;

    // are we currently triggered?
    boolean trig = false;
    
    // wave is a circular buffer of the current wave information; and wavePos is our position in it
    double[] wave = new double[WAVE_SIZE];
    int wavePos = 0;

    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 180);
        
    public Oscilloscope(Output output, boolean aux)
        {
        this.aux = aux;
        this.output = output;
        stroke = new BasicStroke(STROKE_WIDTH);

        Timer timer = new Timer(25, new ActionListener()
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
                int incoming = (aux ? 1 : 0);

                if (emit != null && emit instanceof Out)
                    {
                    Out out = (Out)emit;
                    System.arraycopy(out.getModWave(incoming), 0, wave, 0, wave.length);
                    p = out.getWavePos(incoming);
                    if (out.getAndClearWaveTriggered(incoming))
                        trig = !trig;
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        
        // fill with black regardless
        Graphics2D g = (Graphics2D) graphics;
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
            p++;
            if (p >= WAVE_SIZE) p = 0;
            }
        }
    }

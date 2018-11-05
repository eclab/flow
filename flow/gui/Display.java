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
   Draws a set of partials in a pleasing manner.  Can also be used to edit partials.
*/

public class Display extends JComponent
    {
    Output output;
    ModulePanel modPanel;
    
    // Am I the "auxiliary" display?
    boolean aux;
    
    public void setModulePanel(ModulePanel panel) { modPanel = panel; }
    
    public Display(Output output, boolean aux)
        {
        this(output, aux, false);
        }
    
    void updatePitch(MouseEvent e)
        {
        lastX = e.getX();
        lastY = e.getY();
        output.lock();    
        try
            {
            Sound sound = output.getInput().getLastPlayedSound();
            if (sound == null) 
                sound = output.getSound(0);
            if (sound == null)
                lastPitch = -1;
            else lastPitch = sound.getPitch();
            }
        finally 
            {
            output.unlock();
            }
        }
        
    public Display(Output output, boolean aux, boolean allowMouse)
        {
        this.aux = aux;
        this.output = output;

        Timer timer = new Timer(25, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                repaint();
                SwingUtilities.invokeLater(new Runnable() { public void run() { } });
                }
            });
        timer.start();

        MouseAdapter ma = new MouseAdapter()
            {
            public void mouseDragged(MouseEvent e)
                {
                if (allowMouse) updateFromMouse(e, mouseToFrequency(e.getX()), mouseToAmplitude(e.getY()), true);
                else updatePitch(e); 
                repaint();
                }
                                
            public void mousePressed(MouseEvent e)
                {
                if (allowMouse) updateFromMouse(e, mouseToFrequency(e.getX()), mouseToAmplitude(e.getY()), false);
                else updatePitch(e); 
                }
                    
            public void mouseMoved(MouseEvent e)
                {
                updatePitch(e); 
                repaint();
                }
                
            public void mouseEntered(MouseEvent e)
                {
                updatePitch(e); 
                repaint();
                }
 
            public void mouseExited(MouseEvent e)
                {
                lastPitch = -1;
                repaint();
                }
            };
                
        addMouseListener(ma);
        addMouseMotionListener(ma);
        }
                
    public static final double MAX_FREQUENCY = 150;
    public static final double MAX_AMPLITUDE = 1.0;
    
    double lastPitch = -1;
    double lastX = -1;
    double lastY = -1;
    
    static final int BORDER = 8;
    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 180);
        
    public Dimension getPreferredSize() { return new Dimension(600, 100); }
    
    // Returns the underlying Unit whose partials we're displaying 
    Unit getUnit(Sound sound)
        {
        if (modPanel == null)
            return null;
        Modulation mod = modPanel.getModulation();
        int index = mod.getSound().findRegistered(mod);
        Modulation newmod = sound.getRegistered(index);
        if (newmod instanceof Unit)
            {
            return (Unit) newmod;
            }
        else return null;
        }
    
    public void paintComponent(Graphics graphics)
        {
        double[] amplitudes = null;
        double[] frequencies = null;
        double pitch = 0;
        Sound sound = null;
        
        output.lock();            
        try
            {
            sound = output.getInput().getLastPlayedSound();
            try 
                {
                if (sound == null) sound = output.getSound(0);
                }
            catch (Exception e)
                {
                sound = null;
                }
            if (sound != null)
                {
                pitch = sound.getPitch();
                
                if (modPanel == null)
                    {
                    Unit emit = sound.getEmits();
                    int incoming = (aux ? 1 : 0);

                    if (emit != null && emit instanceof Out && !(emit.getInput(incoming) instanceof Nil))
                        {
                        amplitudes = (double[])(emit.getAmplitudes(incoming).clone());
                        frequencies = (double[])(emit.getFrequencies(incoming).clone());
                        }
                    }
                else
                    {
                    Unit unit = getUnit(sound);
                    if (unit != null)
                        {
                        amplitudes = (double[])(unit.getAmplitudes(0).clone());
                        frequencies = (double[])(unit.getFrequencies(0).clone());
                        }
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

        // now display only if the sound isn't null etc.
        if (sound == null) return;
        if (amplitudes == null) return;
                        
        g.setColor(DARK_BLUE);
        double xx = (width - BORDER * 2) / MAX_FREQUENCY * (Output.SAMPLING_RATE / 2.0) / pitch + BORDER;
        g.draw(new Line2D.Double(xx, height, xx, 0));
                
        g.setColor(DARK_GREEN);
        xx = (width - BORDER * 2) / MAX_FREQUENCY * (1) + BORDER;
        g.draw(new Line2D.Double(xx, height, xx, 0));
        
        int closestPartial = -1;
        double mtf = mouseToFrequency(lastX);
        double closestDiff = Double.POSITIVE_INFINITY;
        if (lastPitch != -1)
            {
            for(int i = 0; i < frequencies.length; i++)
                {
                double diff = Math.abs(mtf - frequencies[i]);
                if (diff < closestDiff)
                    {
                    closestDiff = diff;
                    closestPartial = i;
                    }
                }
            }

        prepareColorsForPartials();
        for(int i = 0; i < frequencies.length; i++)
            {
            double x = (width - BORDER * 2) / MAX_FREQUENCY * frequencies[i] + BORDER;
            double y = (height - BORDER * 2) / MAX_AMPLITUDE * amplitudes[i];
                        
            g.setColor(closestPartial == i ? Color.ORANGE : getColorForPartial(i, amplitudes[i]));
            g.draw(new Line2D.Double(x, height - BORDER, x, height - BORDER - y));
            }
        
        String text = null;
        if (lastPitch != -1)
            {
            g.setFont(Style.SMALL_FONT());
            g.setColor(Color.GRAY);
            FontMetrics fm = g.getFontMetrics();
            int hh = fm.getHeight();

            text = "partial " + closestPartial + " amp: " + String.format("%.2f", amplitudes[closestPartial]) +
                " freq: " + String.format("%.2f", frequencies[closestPartial] * lastPitch) + " (" + String.format("%.2f", frequencies[closestPartial]) + ")";
            Rectangle2D strbounds = fm.getStringBounds(text, g);
            g.drawString(text, (float)(getBounds().getWidth() - strbounds.getWidth() - 10), (float)(hh + 1));


            double amp = mouseToAmplitude(lastY);
            text = "mouse   amp: " + String.format("%.2f", amp < 0 ? 0 : amp) +
                " freq: " + String.format("%.2f", mtf * lastPitch < 0 ? 0 : mtf * lastPitch) + " (" + String.format("%.2f", mtf < 0 ? 0 : mtf) + ")";

            strbounds = fm.getStringBounds(text, g);
            g.drawString(text, (float)(getBounds().getWidth() - strbounds.getWidth() - 10), (float)(hh + 1) * 2);
                
            }
        }
    
    /** A hook called before getColorForPartial(...) is called many times.
        Normally does nothing. */ 
    public void prepareColorsForPartials()
        {
        }
    
    /** Called to extract the color for a given partial by its index, given a current amplitude for that partial.
        By default this returns RED if the partial is over 1.0 in amplitude or is 0.0 in amplitude, else returns white. */
    public Color getColorForPartial(int partial, double amp)
        {
        if (amp > 1.0 || amp == 0.0)
            {
            return Color.RED;
            }
        else
            {
            return Color.WHITE;
            }
        }
    
    /** Called to revise a partial to a given amplitude.  If this is is the result of a drag rather than an
        immediate Mouse Down, continuation will be TRUE.*/ 
    public void updatePartial(int index, double amp, boolean continuation)
        {
        }
        
    // Called to update in response to a click or drag (continuation is true if a drag)
    void updateFromMouse(MouseEvent e, double x, double y, boolean continuation)
        {
        double[] amplitudes = null;
        double[] frequencies = null;
        output.lock();            
        try
            {
            Sound sound = output.getInput().getLastPlayedSound();
            try 
                {
                if (sound == null) 
                    sound = output.getSound(0);
                }
            catch (Exception ex)
                {
                sound = null;
                }

            if (sound != null)
                {
                Unit unit = getUnit(sound);
                if (unit != null)
                    {
                    amplitudes = unit.getAmplitudes(0);
                    frequencies = unit.getFrequencies(0);
                    }
                                                        
                // what's the closest partial?  Not sure if binary search is useful here
                // since we only have 256 or so partials, so we just do an O(n) scan
                int partial = frequencies.length - 1;
                for(int i = 0; i < frequencies.length - 1; i++)
                    {
                    if (x >= frequencies[i] && x <= frequencies[i + 1])
                        {
                        if (Math.abs(x - frequencies[i]) < Math.abs(x - frequencies[i + 1]))
                            { updatePartial(i, y, continuation); break; }
                        else { updatePartial(i + 1, y, continuation); break; }
                        }
                    }
                }
            updatePitch(e); 
            }
        finally 
            {
            output.unlock();
            }
        }

    // Returns the frequency associated with an x coordinate
    double mouseToFrequency(double x)
        {
        int width = getWidth();
        return (x - BORDER) * MAX_FREQUENCY  / (width - BORDER * 2);
        }
        
    // Returns the amplitude associated with a y coordinate
    double mouseToAmplitude(double y)
        {
        int height = getHeight();        
        return 0 - (y - height + BORDER) * MAX_AMPLITUDE / (height - BORDER * 2);
        }
    }

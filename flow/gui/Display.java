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
import java.awt.image.*;

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
    
    boolean timesup = false;
    
    public Display(Output output, boolean aux, boolean allowMouse)
        {
        this.aux = aux;
        this.output = output;

        Timer timer = new Timer(50, new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                timesup = true;
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

    BufferedImage buffer = null;
    boolean waterfall = false;
    public void setWaterfall(boolean val) { waterfall = val; }
    public boolean isWaterfall() { return waterfall; }
        
    public static final Color COLOR_BEYOND_NYQUIST = new Color(175, 175, 175);
    public static final double DEFAULT_MAX_FREQUENCY = 150;
    public static final double DEFAULT_MIN_FREQUENCY = 1.0 / 16.0;
    public static final double DEFAULT_MAX_AMPLITUDE = 1.0;
    
    boolean logFrequency = false;
    boolean boundPartials = false;
    double maxFrequency = DEFAULT_MAX_FREQUENCY;
    double minFrequency = DEFAULT_MIN_FREQUENCY;
    double maxAmplitude = DEFAULT_MAX_AMPLITUDE;
    public double getMaxFrequency() { return maxFrequency; }
    public void setMaxFrequency(double v) { maxFrequency = v; }
    public double getMinFrequency() { return minFrequency; }
    public void setMinFrequency(double v) { minFrequency = v; }
    public double getMaxAmplitude() { return maxAmplitude; }
    public void setMaxAmplitude(double v) { maxAmplitude = v; }
    public boolean getLogFrequency() { return logFrequency; }
    public void setLogFrequency(boolean v) { logFrequency = v; }
    public boolean getBoundPartials() { return boundPartials; }
    public void setBoundPartials(boolean v) { boundPartials = v; }
    
    double lastPitch = -1;
    double lastX = -1;
    double lastY = -1;
    
    double boundMax = 1;
    double boundMin = 0;
    
    static final int BORDER = 8;
    static final Color DARK_GREEN = new Color(0, 127, 0);
    static final Color DARK_BLUE = new Color(0, 0, 255);
        
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
        
        
    double invNormalizedFrequency(double nFrequency, double fundamental)
        {
        if (logFrequency)
            {
            return Math.exp(nFrequency - fundamental);
            }
        else
            {
            return (nFrequency - fundamental) + 1;
            }
        }

    double normalizedFrequency(double frequency, double fundamental)
        {
        return fundamental + (logFrequency ? Math.log(frequency) : frequency - 1.0);
        }
    
    public Graphics2D prepareBuffer()
        {
        if (buffer == null || buffer.getWidth() != getWidth() || buffer.getHeight() != getHeight())
            {
            // prep a new buffer
            buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D)(buffer.getGraphics());
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
            }
        
        BufferedImage old = buffer;
        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D)(buffer.getGraphics());
        g.drawImage(old, 0, -1, null);
        
        return g;
        }
    
    public static Color[] colors;
    public static final int NUM_COLORS = 256;
        
    public Color getColorForAmplitude(double amplitude)
        {
        if (colors == null)
            {
            colors = new Color[NUM_COLORS];
            for(int i = 0; i < NUM_COLORS; i++)
                colors[i] = new Color(i, i, i);
            }
        amplitude = Math.log(1.0 + amplitude) * (1.0 / 0.6931471805599453);  // so amplitude = 2.0 goes to 1.0

        if (amplitude > 1.0) amplitude = 1.0;
        if (amplitude < 0.0) amplitude = 0.0;
                
        amplitude = 1 - amplitude;
        amplitude = amplitude * amplitude * amplitude * amplitude * amplitude;
        amplitude = 1 - amplitude;
                
        return colors[(int) (Math.min(amplitude * NUM_COLORS, 255))];
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
        
        double max = maxFrequency;
        double min = minFrequency;
        if (boundPartials)
            {
            min = frequencies[0];
            max = frequencies[frequencies.length - 1];
            boundMin = min;
            boundMax = max;
            }
        if (max < min) { double temp = max; max = min; min = temp; }
        if (max == min) max = max + 1.0;
        double fundamental = 1.0 / max;
        double range = max - min;
        if (logFrequency)
            {
            double m = Math.abs(Math.log(min));
            range = Math.abs(Math.log(max)) + m;
            fundamental = m;  // this is the zero point
            } 
                    
        double nyquist = (Output.SAMPLING_RATE / 2.0) / pitch;
        
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
            
        if (!waterfall)
            {
            buffer = null;
                
            g.setColor(DARK_BLUE);
            double xx = (width - BORDER * 2) / range * normalizedFrequency(nyquist, fundamental) + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));
                                
            g.setColor(DARK_GREEN);
            xx = (width - BORDER * 2) / range * normalizedFrequency(1.0, fundamental) + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));

            prepareColorsForPartials();
            for(int i = 0; i < frequencies.length; i++)
                {
                double x = (width - BORDER * 2) / range * normalizedFrequency(frequencies[i], fundamental) + BORDER;
                double y = (height - BORDER * 2) / maxAmplitude * amplitudes[i];
                                                
                if (x >= 0 && x <= width)
                    {
                    g.setColor(closestPartial == i ? Color.ORANGE : 
                            (frequencies[i] > nyquist ? COLOR_BEYOND_NYQUIST : 
                            getColorForPartial(i, amplitudes[i])));
                    g.draw(new Line2D.Double(x, height - BORDER, x, height - BORDER - y));
                    }
                }
            }
        else
            {
            Graphics2D gb = null;
            if (timesup)
                {
                gb = prepareBuffer();
                }

            double closestX = -1;
            for(int i = 0; i < frequencies.length; i++)
                {
                double x = (width - BORDER * 2) / range * normalizedFrequency(frequencies[i], fundamental) + BORDER;
                double y = (height - BORDER * 2) / maxAmplitude * amplitudes[i];
                                        
                if (x >= 0 && x <= width)
                    {
                    if (closestPartial == i)
                        {
                        closestX = x;
                        }
                                        
                    if (timesup)
                        {
                        gb.setColor(frequencies[i] > nyquist ? Color.BLACK : getColorForAmplitude(amplitudes[i]));
                        gb.draw(new Line2D.Double(x, height - 1, x, height - 2));
                        }
                    }
                }
                                
            timesup = false;
                                
            if (buffer != null)
                g.drawImage(buffer, 0, 0, width, height, null);

            if (closestX != -1)
                {
                g.setColor(Color.ORANGE);
                g.draw(new Line2D.Double(closestX, height, closestX, 0));
                }
                                                        
            g.setColor(DARK_BLUE);
            double xx = (width - BORDER * 2) / range * normalizedFrequency(nyquist, fundamental) + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));
                                
            g.setColor(DARK_GREEN);
            xx = (width - BORDER * 2) / range * normalizedFrequency(1.0, fundamental) + BORDER;
            g.draw(new Line2D.Double(xx, height, xx, 0));

            }
        
        String text = null;
        if (lastPitch != -1 && closestPartial != -1)
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
 
                    frequencies = unit.getFrequencies(0);
                                                        
                    // what's the closest partial?  Not sure if binary search is useful here
                    // since we only have 256 or so partials, so we just do an O(n) scan

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
        double d = (x - BORDER) / (getWidth() - BORDER * 2);

        double max = maxFrequency;
        double min = minFrequency;

        if (boundPartials)
            {
            min = boundMin;
            max = boundMax;
            }

        if (max < min) { double temp = max; max = min; min = temp; }
        if (max == min) max = max + 1.0;
        double fundamental = 1.0 / max;
        double range = max - min;
        if (logFrequency)
            {
            double m = Math.abs(Math.log(min));
            range = Math.abs(Math.log(max)) + m;
            fundamental = m;  // this is the zero point
            } 

        return invNormalizedFrequency(d * range, fundamental);
        }
        
    // Returns the amplitude associated with a y coordinate
    double mouseToAmplitude(double y)
        {
        int height = getHeight();        
        return 0 - (y - height + BORDER) * maxAmplitude / (height - BORDER * 2);
        }
    }

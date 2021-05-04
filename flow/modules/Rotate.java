// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
   A unit which rotates all the partials, by frequency, within
   the some range.  You can attach this unit to a sawtooth lfo
   to cause them to continuously rotate within this range.
   You might also wish to <b>window</b> the amplitude of the partials so that
   the ones at the far ends aren't immediately wiped out.  There
   are a number of window options; you can also change how the partials
   are stretched during rotation from linear to a pseudo-exponential.
   The windowing is normally based on the pre-stretched frequencies,
   so stretching them will also stretch the window.  You can change this
   to have the windowing based on the post-stretched frequencies instead
   by selecting CENTER.
**/


public class Rotate extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_ROTATE = 0;
    public static final int MOD_UPPER_BOUND = 1;
    public static final int MOD_LOWER_BOUND = 2;
    public static final int MOD_SPACE = 3;

    public static final int WINDOW_NONE = 0;
    public static final int WINDOW_TRIANGLE = 1;
    public static final int WINDOW_X_2 = 2;
    public static final int WINDOW_X_4 = 3;
    public static final int WINDOW_X_8 = 4;
    public static final int WINDOW_X_16 = 5;
    public static final int WINDOW_X_32 = 6;
    public static final int WINDOW_X_64 = 7;
    public static final int WINDOW_X_128 = 8;

    public static final int STRETCH_NONE = 0;
    public static final int STRETCH_X_2 = 1;
    public static final int STRETCH_X_4 = 2;
    public static final int STRETCH_X_8 = 3;
    public static final int STRETCH_X_16 = 4;
    
    public static final int MAX_SPACING = 31;
    
    int stretch = STRETCH_NONE;
    int window = WINDOW_NONE;
    boolean center = false;
    boolean solo = true;
    
    public void setWindow(int val) { window = val; }
    public int getWindow() { return window; }

    public void setStretch(int val) { stretch = val; }
    public int getStretch() { return stretch; }

    public void setCenter(boolean val) { center = val; }
    public boolean getCenter() { return center; }

    public void setSolo(boolean val) { solo = val; }
    public boolean getSolo() { return solo; }

    public static final int OPTION_STRETCH = 0;
    public static final int OPTION_WINDOW = 1;
    public static final int OPTION_CENTER = 2;
    public static final int OPTION_SOLO = 3;

    public static final double FREQUENCY_SCALE = 256;
    
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_WINDOW: return getWindow();
            case OPTION_STRETCH: return getStretch();
            case OPTION_CENTER: return getCenter() ? 1 : 0;
            case OPTION_SOLO: return getSolo() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_WINDOW: setWindow(value); return;
            case OPTION_STRETCH: setStretch(value); return;
            case OPTION_CENTER: setCenter(value != 0); return;
            case OPTION_SOLO: setSolo(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

        
    public Rotate(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE, Constant.ZERO, Constant.ZERO }, new String[] { "Rotate", "Upper", "Lower", "Thin" });
        defineOptions(new String[] { "Stretch", "Window", "Center", "Solo" }, new String[][] { { "None", "x^2", "x^4", "x^8", "x^16"}, { "None", "Tri", "x^2", "x^4", "x^8", "x^16", "x^32", "x^64", "x^128" }, { "Center" }, { "Solo" } } );
        }
    
    
    public void go()
        {
        super.go();
                
        double rotate = modulate(MOD_ROTATE);
        double upper = modulate(MOD_UPPER_BOUND);
        double lower = modulate(MOD_LOWER_BOUND);

        copyFrequencies(0);
        if (window == WINDOW_NONE && !solo)
            pushAmplitudes(0);
        else
            copyAmplitudes(0);
                
        if (upper == lower) return;
                
        if (upper < lower)
            {
            double swap = upper;
            upper = lower;
            lower = swap;
            }
                        
        upper *= FREQUENCY_SCALE;
        lower *= FREQUENCY_SCALE;
                
        double[] frequencies = getFrequencies(0);
        double[] amplitudes = getAmplitudes(0);

        double delta = rotate * (upper - lower);
                
        double scale = 0.5;
        for(int w = WINDOW_X_2 ; w <= window ; w++)
            {
            scale = scale * scale;
            }
                
        int x = 0;
                
        int remove = (int)Math.floor(modulate(MOD_SPACE) * MAX_SPACING);
        for(int i = 0; i < frequencies.length; i++)
            {
            if (frequencies[i] >= lower && frequencies[i] <= upper)
                {
                // space
                if (x > 0)
                    {
                    amplitudes[i] = 0;
                    }
                x++;
                if (x > remove) x = 0;
                        
                // map
                frequencies[i] = frequencies[i] + delta;
                        
                if (frequencies[i] < lower) 
                    frequencies[i] += (upper - lower);
                else if (frequencies[i] > upper) 
                    {
                    frequencies[i] -= (upper - lower);
                    }
                                        
                double f = (frequencies[i] - lower) / (upper - lower);
                double a = f;
                for(int s = 0; s < stretch; s++)
                    f = f * f;
                                
                if (center) a = f;
                                
                f = f * (upper - lower) + lower;
                frequencies[i] = f;


                // window
                if (window == WINDOW_NONE)
                    {
                    // do nothing
                    a = 1.0;
                    }
                else if (window == WINDOW_TRIANGLE)
                    {
                    a = 1 - 2 * Math.abs(a - 0.5);
                    }
                else
                    {
                    a = a - 0.5;
                    for(int w = WINDOW_X_2 ; w <= window ; w++)
                        {
                        a = a * a;
                        }
                    a /= scale;
                    a = 1 - a;
                    }
                amplitudes[i] *= a;
                }
            else if (solo)
                {
                amplitudes[i] = 0;
                }
            }

        if (constrain())
            {
            simpleSort(0, true);
            }
        else
            {
            // In the future, we could do better than this since we know exactly
            // which partials got moved, so we could very easily shift.
            simpleSort(0, true);
            }
        }       
    }

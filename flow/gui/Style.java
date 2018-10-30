// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;

/**
   A collection of GUI style constants.
*/

public class Style
    {
    public final static Color UNIT_COLOR = Color.BLUE;
    public final static Color MOD_COLOR = Color.BLACK;
    
    /////// GLOBAL CONSTANTS
    
    /** Background color */
    public final static Color DEFAULT_BACKGROUND_COLOR = Color.BLACK;
    static Color BACKGROUND_COLOR = DEFAULT_BACKGROUND_COLOR;
    public static Color BACKGROUND_COLOR() { return BACKGROUND_COLOR; }
    /** Text color */
    public final static Color DEFAULT_TEXT_COLOR = Color.WHITE;
    static Color TEXT_COLOR = DEFAULT_TEXT_COLOR;
    public static Color TEXT_COLOR() { return TEXT_COLOR; }
    /** Small font, primarily for labels, button and combo box text. */
    public static Font SMALL_FONT() { return new Font(Font.SANS_SERIF, Font.PLAIN, isUnix() ? 9 : 10); }
    public static Color DEFAULT_DYNAMIC_COLOR = Color.RED;
    static Color DYNAMIC_COLOR = DEFAULT_DYNAMIC_COLOR;
    public static Color DYNAMIC_COLOR() { return DYNAMIC_COLOR; }
    
    /** Width of the set region in Dials etc.  Should be a multiple of 2, ideally 4*/
    public static float DIAL_STROKE_WIDTH() { return 4.0f; }
    /** Width of the dial **/
    public static int LABELLED_DIAL_WIDTH() { return 20; }
    /** Color of the set region in Dials etc. when being updated. */
    public static Color DIAL_DYNAMIC_COLOR() { return DYNAMIC_COLOR(); }
    /** The stroke for the set region in Dials etc. */
    public static BasicStroke DIAL_THIN_STROKE() { return new BasicStroke(DIAL_STROKE_WIDTH() / 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL); }
    /** The stroke for the unset region in Dials etc. */
    public static BasicStroke DIAL_THICK_STROKE() { return new BasicStroke(DIAL_STROKE_WIDTH(), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL); }

    /////// KEYBOARD CONSTANTS
    public static Color KEYBOARD_WHITE_COLOR() { return Color.WHITE; }
    public static Color KEYBOARD_BLACK_COLOR() { return Color.BLACK; }
    public static Color KEYBOARD_DYNAMIC_COLOR() { return DYNAMIC_COLOR(); }
    public static int KEYBOARD_DEFAULT_WHITE_KEY_WIDTH() { return 12; }
    public static int KEYBOARD_DEFAULT_WHITE_KEY_HEIGHT() { return 48; }
    

    /////// GRAPHICS PREPARATION
        
    /** Updates the graphics rendering hints before drawing.  Called by a few widgets.  */
    public static void prepareGraphics(Graphics g)
        {
        Graphics2D graphics = (Graphics2D) g;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        }
        
         
    /////// OS DISTINGUISHING PROCEDURES

    private static String OS() { return System.getProperty("os.name").toLowerCase(); }

    public static boolean isWindows() 
        {
        return (OS().indexOf("win") >= 0);
        }

    public static boolean isMac() 
        {
        return (OS().indexOf("mac") >= 0 || System.getProperty("mrj.version") != null);
        }

    public static boolean isUnix() 
        {
        return (OS().indexOf("nix") >= 0 || OS().indexOf("nux") >= 0 || OS().indexOf("aix") > 0 );
        }
    }

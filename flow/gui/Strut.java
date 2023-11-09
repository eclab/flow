/***
    Copyright 2023 by Sean Luke
    Licensed under the Apache License version 2.0
*/

package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;


/**
   A utility class for making simple fixed-height or fixed-width blobs
        
   @author Sean Luke
*/

public class Strut
    {
    public static JComponent makeHorizontalStrut(final int space)
        {
        return makeStrut(space, 0);
        } 
                
    public static JComponent makeVerticalStrut(final int space)
        {
        return makeStrut(0, space);
        }

    public static JComponent makeHorizontalStrut(final int space, boolean setBackground)
        {
        return makeStrut(space, 0, setBackground);
        } 
                
    public static JComponent makeVerticalStrut(final int space, boolean setBackground)
        {
        return makeStrut(0, space, setBackground);
        }

    public static JComponent makeStrut(final int width, final int height, boolean setBackground)
        {
        JPanel panel = new JPanel()
            {
            public Dimension getMinimumSize() { return new Dimension(width, height); }
            public Dimension getPreferredSize() { return new Dimension(width, height); }
            public Dimension getMaximumSize() { return new Dimension(width, height); }
            };
        if (setBackground) panel.setBackground(Style.BACKGROUND_COLOR());
        return panel;
        } 
    
    public static JComponent makeStrut(final int width, final int height)
        {
        return makeStrut(width, height, true);
        } 
    
    public static JComponent makeStrut(Component[] components)
        {
        return makeStrut(components, false, false);
        }

    public static JComponent makeStrut(Component[] components, boolean zeroWidth, boolean zeroHeight)
        {
        int maxWidth = 0;
        int maxHeight = 0;
        for(int i = 0; i < components.length; i++)
            {
            components[i].validate();
            Dimension size = components[i].getPreferredSize();
            if (maxWidth < size.width)
                maxWidth = size.width;
            if (maxHeight < size.height)
                maxHeight = size.height;
            }
        return makeStrut((zeroWidth ? 0 : maxWidth), (zeroHeight ? 0 : maxHeight));
        }
        
    public static JComponent makeStrut(Component[] components, boolean zeroWidth)
        {
        return makeStrut(components, zeroWidth, !zeroWidth);
        }

    public static JComponent makeStrut(Component component, boolean zeroWidth, boolean zeroHeight)
        {
        return makeStrut(new Component[] { component }, zeroWidth, zeroHeight);
        }

    public static JComponent makeStrut(Component component, boolean zeroWidth)
        {
        return makeStrut(new Component[] { component }, zeroWidth, !zeroWidth);
        }

    public static JComponent makeStrut(Component component)
        {
        return makeStrut(new Component[] { component });
        }

    public static JComponent makeHorizontalStrut(Component component)
        {
        return makeStrut(new Component[] { component }, false, true);
        }

    public static JComponent makeVerticalStrut(Component component)
        {
        return makeStrut(new Component[] { component }, true, false);
        }

    }

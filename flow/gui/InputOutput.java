// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.awt.font.*;

/**
   Abstract superclass of ModulationInput, ModulationOutput, UnitInput, UnitOutput, and ConstraintsChooser
   which gathers together several items they have in common:  (1) a JLabel title, (2) a ModPanel,
   (3) the index of the incoming input or outgoing output modulation (4) The Jack class, which draws
   jacks as circles.
*/ 

public abstract class InputOutput extends JPanel
    {
    JLabel title;
    int number;         // this is the index for the input/output unit/modulation
    ModulePanel modPanel;
    boolean titleCanChange = false;

    /** Returns true if the underlying object is an input module, false if it is an output module. */    
    public abstract boolean isInput();

    /** Returns true if the underlyingobject is a unit, false if it is a modulation. */
    public abstract boolean isUnit();
    
    /** Returns the owner ModulePanel. */
    public ModulePanel getModulePanel() { return modPanel; }
    
    /** Returns the actual title JLabel */
    public JLabel getTitle() { return title; }
    
    /** Returns the text of the title JLabel */
    public String getTitleText() { return title.getText(); }
    
    /** Returns the text of the title JLabel */
    public void setTitleText(String val)
        {
        val = val.trim();
        title.setText(" " + val); 

        // distribute
        Modulation mod = modPanel.getModulation();
        Output output = mod.getSound().getOutput();
        output.lock();
        try
            {
            int index = mod.getSound().findRegistered(mod);
            if (index == Sound.NOT_FOUND)
                System.err.println("InputOutput.distributetitle: mod/unit " + mod + " not found!");
            else
                {
                int numSounds = output.getNumSounds();
                for(int i = 0; i < numSounds; i++)
                    {
                    if (isUnit())
                        {
                        Unit a = (Unit)(output.getSound(i).getRegistered(index));
                        if (isInput())
                            {
                            a.setInputName(number, val);
                            }
                        else
                            {
                            a.setOutputName(number, val);
                            }
                        }
                    else
                        {
                        Modulation a = (Modulation)(output.getSound(i).getRegistered(index));
                        if (isInput())
                            {
                            a.setModulationName(number, val);
                            }
                        else
                            {
                            a.setModulationOutputName(number, val);
                            }
                        }
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        }

    /** Returns true if the title is permitted to be edited by the user.  */
    public boolean getTitleCanChange() { return titleCanChange; }
    
    /** Sets whether the title can be edited by the user. */
    public void setTitleCanChange(boolean val) 
        { 
        titleCanChange = val; 
        Font font = title.getFont();
        Map attributes = font.getAttributes();
        if (val)
            attributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED);
        else
            attributes.remove(TextAttribute.INPUT_METHOD_UNDERLINE);
        title.setFont(font.deriveFont(attributes));
        }
    
    // Handles clicks on editable title JLabels
    class LabelMouseAdapter extends MouseAdapter
        {
        public void mouseClicked(MouseEvent e)
            {
            if (!getTitleCanChange()) return;
                                
            String result = Rack.showTextDialog(modPanel.rack, InputOutput.this, "New Label", "Enter a Label:", title.getText().trim());
                                
            if (result != null)
                {
                setTitleText(result);
                modPanel.updateTitleChange(InputOutput.this, number, result);
                }
            }
        }


    // Defines jacks for input/output modules
    class Jack extends JPanel
        {
        public Dimension getPreferredSize() { return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH()); }
        public Dimension getMinimumSize() { return new Dimension(Style.LABELLED_DIAL_WIDTH(), Style.LABELLED_DIAL_WIDTH()); }
                
        boolean isUnit;
        
        public Jack(boolean isUnit) { this.isUnit = isUnit; }
        
        /** Returns the InputOutput which owns this Jack. */
        public InputOutput getParent() { return InputOutput.this; }
        
        /** Returns the actual square within which the Jack's circle is drawn. */
        public Rectangle getDrawSquare()
            {
            Insets insets = getInsets();
            Dimension size = getSize();
            int width = size.width - insets.left - insets.right;
            int height = size.height - insets.top - insets.bottom;
                
            // How big do we draw our circle?
            if (width > height)
                {
                // base it on height
                int h = height;
                int w = h;
                int y = insets.top;
                int x = insets.left + (width - w) / 2;
                return new Rectangle(x, y, w, h);
                }
            else
                {
                // base it on width
                int w = width;
                int h = w;
                int x = insets.left;
                int y = insets.top + (height - h) / 2;
                return new Rectangle(x, y, w, h);
                }
            }
    
        /** Returns the standard fill color. */
        public Color getFillColor()
            {
            Color c = getDrawColor();
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), 64);
            }
     
        /** Returns the standard draw color. */
        public Color getDrawColor()
            {
            return Color.BLACK;
            }
     
        public void paintComponent(Graphics g)
            {
            super.paintComponent(g);

            Style.prepareGraphics(g);
                
            Graphics2D graphics = (Graphics2D) g;
                
            Rectangle rect = getBounds();
            rect.x = 0;
            rect.y = 0;
            graphics.setPaint(title.getBackground());
            graphics.fill(rect);
            rect = getDrawSquare();
            
            Ellipse2D.Double e = new Ellipse2D.Double(rect.getX() + Style.DIAL_STROKE_WIDTH() / 2, rect.getY() + Style.DIAL_STROKE_WIDTH()/2, rect.getWidth() - Style.DIAL_STROKE_WIDTH(), rect.getHeight() - Style.DIAL_STROKE_WIDTH());
            graphics.setPaint(getFillColor());
            graphics.fill(e);

            graphics.setPaint(getDrawColor());
            graphics.setStroke(Style.DIAL_THIN_STROKE());
            graphics.draw(e);
            
            if (isUnit)
                {
                graphics.setPaint(getDrawColor());
                e.x += e.width / 2.5;
                e.y += e.height / 2.5;
                e.width -= e.width * 2.0 / 2.5;
                e.height -= e.height * 2.0 / 2.5;
                graphics.fill(e);
                }
            }
        }


    }

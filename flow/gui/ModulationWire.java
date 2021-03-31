// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;
import java.util.*;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;

/**
   Draws the lines which represent wires attaching ModulationOutput jacks to ModulationInput jacks.
*/

public class ModulationWire
{
    ModulationOutput start = null;
    ModulationInput end = null;
    Rack rack;
        
    Stroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[] {2, 2}, 0);
    Color color;
        
    public void setStart(ModulationOutput p) { start = p; }
    public ModulationOutput getStart() { return start; }
    public void setEnd(ModulationInput p) { end = p; }
    public ModulationInput getEnd() { return end; }
        
    public void chooseColor() 
    {
        color = new Color((int)(Math.random() * 220), (int)(Math.random() * 220), (int)(Math.random() * 220), 175);
    }
        
    public ModulationWire(Rack rack) 
    { 
        this.rack = rack; 
        chooseColor();
    }
        
    public void draw(Graphics2D g)
    {
        if (start == null) {System.err.println("ModulationWire: Null Start"); return; }
        if (start.jack == null) {System.err.println("ModulationWire: Null StartJack"); return; }
                
        Rectangle bounds = start.jack.getBounds();
        Point start_p = new Point(bounds.width/2, bounds.height/2);
        start_p = SwingUtilities.convertPoint(start.jack, start_p, rack.getWirePaintComponent());                

        Point end_p = null;
        if (end == null)
            {
                end_p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(end_p, start.jack);
            }
        else
            {
                bounds = end.dial.getBounds();
                end_p = new Point(bounds.width/2, bounds.height/2);
                end_p = SwingUtilities.convertPoint(end.dial, end_p, start.jack);
            }
            
        end_p = SwingUtilities.convertPoint(start.jack, end_p, rack.getWirePaintComponent());                
                                
        g.setStroke(stroke);
        g.setColor(color);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(start_p.getX(), start_p.getY());
        double diff = Math.abs(start_p.getX() - end_p.getX());
        double midx = (start_p.getX() + end_p.getX()) / 2;
        double midY = (start_p.getY() + end_p.getY()) / 2;
        path.curveTo(midx, midY + diff / 4,
                     midx,midY + diff / 4, end_p.getX(), end_p.getY());
        g.draw(path);
        
    }

    public String toString() { return "ModWire [from=" + start + ", to=" + end + "]"; }
}

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;

/** 
    An InputOutput which represents an underlying input Unit.
    Contains a single Jack, the Unit in question, and at most one incoming wire.
*/

public class UnitInput extends InputOutput implements Rebuildable
    {
    Jack jack;
    Unit unit;
    
    // True if we're (temporarily) highlighting ourselves
    boolean highlight;
    
    // The incoming UnitWire attached to us, if any
    UnitWire incoming;
     
    public void rebuild()
        {
        Output output = unit.getSound().getOutput();
        Rack rack = modPanel.getRack();
        output.lock();
        try
            {
            Unit connection;
                        
            // What connection do we have?
            if (number == ConstraintsChooser.INDEX)
                {
                connection = unit.getConstraintIn();
                }
            else
                {
                connection = unit.getInput(number);
                }
                                
            disconnect();
                        
            // now reconnect
                        
            if (connection instanceof Nil)
                {
                return;  // we're done
                }
                                
            // find output
            int index = unit.getSound().findRegistered(connection);
            if (index == Sound.NOT_FOUND)
                {
                System.err.println("UnitInput: Can't find connection!");
                }
            else
                {
                ModulePanel connectionPanel = rack.getModulePanel(index);
                UnitWire wire = new UnitWire(rack);

                int outUnitNumber = unit.getInputIndex(number);
                UnitOutput outputUnit = connectionPanel.findUnitOutputForIndex(outUnitNumber);
                wire.setStart(outputUnit);
                wire.setEnd(this);
                outputUnit.attach(this, wire);
                }
            }
        finally 
            {
            output.unlock();
            }
        }
    
    /** Removes the wire attached to this UnitInput. */
    public void disconnect()
        {
        if (incoming != null)
            {
            // delete wire from arraylists
            
            incoming.getStart().outgoing.remove(incoming);
            modPanel.getRack().removeUnitWire(incoming);
            Output output = unit.getSound().getOutput();
            
            // disconnect from all sounds
            output.lock();
            try
                {
                int index = unit.getSound().findRegistered(unit);
                if (index == Sound.NOT_FOUND)
                    System.err.println("UnitInput.disconnect: unit " + unit + " not found!");
                else
                    {
                    int numSounds = output.getNumSounds();
                    for(int i = 0; i < numSounds; i++)
                        {
                        Unit a = (Unit)(output.getSound(i).getRegistered(index));
                        if (number == ConstraintsChooser.INDEX)
                            a.setConstraintIn(Unit.NIL, 0);
                        else
                            a.clearInput(number);
                        }
                    }
                }
            finally 
                {
                output.unlock();
                }                       
            // eliminate
            incoming = null;
            modPanel.getRack().repaint();
            }            
        }

    /** Constructor, given an owning unit and ModPanel, plus which unit input number we are in our owner. 
        If number is ConstraintsChooser.INDEX, then we draw somewhat differently.  */
    public UnitInput(Unit unit, int number, ModulePanel modPanel)
        {
        this.unit = unit;
        this.number = number;
        this.modPanel = modPanel;
        
        jack = new Jack(true)
            {
            public Color getFillColor()
                {
                if (highlight)
                    {
                    return Color.RED;
                    }
                else if ((number == ConstraintsChooser.INDEX && !(unit.getConstraintIn() instanceof Nil)) ||
                    (number != ConstraintsChooser.INDEX && !unit.isDefaultInput(number)))
                    {
                    return Color.YELLOW;
                    }
                else
                    {
                    return title.getBackground();
                    }
                }
            };

        if (number != ConstraintsChooser.INDEX)
            title = new JLabel(" " + unit.getInputName(number));
        else
            title = new JLabel();
        title.setFont(Style.SMALL_FONT());
        title.addMouseListener(new LabelMouseAdapter());
        
        setLayout(new BorderLayout());
        add(title, BorderLayout.CENTER);
        
        JLabel aux = modPanel.getAuxUnitInputTitle(number);
        if (aux != null)
            {
            add(aux, BorderLayout.EAST);
            }
        add(jack, BorderLayout.WEST);

        MouseAdapter mouseAdapter = new MouseAdapter()
            {
            public void mouseClicked(MouseEvent e)
                {
                disconnect();
                }
            };
            
        addMouseListener(mouseAdapter);
        }

    public String toString()
        {
        return "UnitInput[name=" + title.getText() + ", number=" + number + ", panel=" + modPanel + "]"; 
        }

    public boolean isInput() { return true; }
    public boolean isUnit() { return true; }
    
    }

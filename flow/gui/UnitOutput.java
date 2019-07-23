// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.font.*;
import java.util.*;

/** 
    An InputOutput which represents an underlying output Unit.
    Contains a single Jack, the Unit in question, and a list of outgoing wires.
*/

public class UnitOutput extends InputOutput
    {
    Jack jack;
    Unit unit;
    
    // All outgoing wires
    ArrayList<UnitWire> outgoing = new ArrayList<>();
    
    // Holds the temporary wire being drawn by the user
    UnitWire temp;
    
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    
    // The previous UnitInput which was highlighted as we passed over it during drawing, so we can unhighlight it
    UnitInput lastUnitInput = null;
    
    /** Removes all wires attached to the UnitOutput */
    public void disconnect()
        {
        // I'm not sure if disconnecting will mess up the arraylist, so I'm not sure if I can iterate over it here.
        // so instead I'll just copy first.
        ArrayList<UnitWire> w = new ArrayList<>(outgoing);
        for(UnitWire wire : w)
            wire.end.disconnect();
        }
    
    /** Constructor, given an owning unit and ModPanel, plus which unit output number we are in our owner. */
    public UnitOutput(Unit unit, int number, final ModulePanel modPanel)
        {
        this.unit = unit;
        this.number = number;
        this.modPanel = modPanel;
        
        jack = new Jack(true);

        title = new JLabel(unit.getOutputName(number) + " ", SwingConstants.RIGHT);
        title.setFont(Style.SMALL_FONT());
        title.addMouseListener(new LabelMouseAdapter());
        
        setLayout(new BorderLayout());
        add(title, BorderLayout.CENTER);
        add(jack, BorderLayout.EAST);
        
        MouseAdapter mouseAdapter = new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                Rack rack = modPanel.getRack();
                temp = new UnitWire(rack);
                temp.setStart(UnitOutput.this);
                rack.addUnitWire(temp);
                rack.repaint();

                if (releaseListener != null)
                    Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);

                // This gunk fixes a BAD MISFEATURE in Java: mouseReleased isn't sent to the
                // same component that received mouseClicked.  What the ... ? Asinine.
                // So we create a global event listener which checks for mouseReleased and
                // calls our own private function.  EVERYONE is going to do this.
                                
                Toolkit.getDefaultToolkit().addAWTEventListener( releaseListener = new AWTEventListener()
                    {
                    public void eventDispatched(AWTEvent e)
                        {
                        if (e instanceof MouseEvent && e.getID() == MouseEvent.MOUSE_RELEASED)
                            {
                            UnitOutput.this.mouseReleased((MouseEvent)e);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);

                mouseDown = true;
                }
                                        
            public void mouseDragged(MouseEvent e)
                {
                if (lastUnitInput != null)
                    lastUnitInput.highlight = false;
                UnitInput input = modPanel.getRack().findUnitInputFor(e.getPoint(), jack);
                if (input != null)
                    {
                    lastUnitInput = input;
                    input.highlight = true; 
                    }
                modPanel.getRack().repaint();
                }
                                        
            public void mouseReleased(MouseEvent e)
                {
                UnitOutput.this.mouseReleased(e);
                }
                
            public void mouseClicked(MouseEvent e)
                {
                for(UnitWire wire : outgoing)
                    {
                    wire.chooseColor();
                    }
                }
            };
            
        jack.addMouseMotionListener(mouseAdapter);
        jack.addMouseListener(mouseAdapter);

        String[] help = unit.wrapHelp(unit.getUnitOutputHelp());
        if (help != null && help.length > number && help[number] != null)
            {
            jack.setToolTipText(help[number]);
            title.setToolTipText(help[number]);
            }
        }


    /** Attaches a UnitWire connected to the given UnitInput. */
    public void attach(UnitInput input, UnitWire wire)
        {
        Output output = input.unit.getSound().getOutput();
        output.lock();
        try
            {
            input.disconnect();
            wire.setEnd(input);
            outgoing.add(wire);
            modPanel.getRack().addUnitWire(wire);
                                    
            int index = input.unit.getSound().findRegistered(input.unit);
            if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                {
                return;
                }

            int outIndex = input.unit.getSound().findRegistered(unit);
            if (outIndex == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                {
                return;
                }
                
            int numSounds = output.getNumSounds();

            if (input.number == ConstraintsChooser.INDEX)
                {
                // distribute to all sounds
                for(int i = 0; i < numSounds; i++)
                    {
                    Sound s = output.getSound(i);
                    if (s.getGroup() == Output.PRIMARY_GROUP)
                        {
                        ((Unit)(s.getRegistered(index))).setConstraintIn(
                            ((Unit)(s.getRegistered(outIndex))), number);
                        }
                    }
                }
            else
                {
                // distribute to all sounds
                for(int i = 0; i < numSounds; i++)
                    {
                    Sound s = output.getSound(i);
                    if (s.getGroup() == Output.PRIMARY_GROUP)
                        {
                        ((Unit)(s.getRegistered(index))).setInput(
                            ((Unit)(s.getRegistered(outIndex))), input.number, number);
                        }
                    }
                }
            input.incoming = wire;
            }
        finally 
            {
            output.unlock();
            }
        }
                
    public void mouseReleased(MouseEvent e)
        {
        if (!mouseDown) return;
                
        if (releaseListener != null)
            Toolkit.getDefaultToolkit().removeAWTEventListener(releaseListener);
        mouseDown = false;

        // we do a conversion because sometimes it's another component due to AWT, stupid Mac.
        Point p = e.getPoint();
        p = SwingUtilities.convertPoint(e.getComponent(), p, jack);

        Rack rack = modPanel.getRack();
        UnitInput input = rack.findUnitInputFor(p, jack);
        rack.removeUnitWire(temp);

        if (input != null)
            {
            // does the wire already exist?
            boolean exists = false;
            for(UnitWire wire: outgoing)
                {
                if (wire.getEnd() == input)
                    { exists = true; break; }
                }
                                                
            if (!exists)
                {
                attach(input, temp);
                }
            }
        temp = null;
        if (lastUnitInput != null)
            lastUnitInput.highlight = false;
        lastUnitInput = null;
                                                
        rack.repaint();
        }
        
    public String toString()
        {
        return "UnitOutput[name=" + title.getText() + ", number=" + number + ", panel=" + modPanel + "]"; 
        }

    public boolean isInput() { return false; }
    public boolean isUnit() { return true; }
    }

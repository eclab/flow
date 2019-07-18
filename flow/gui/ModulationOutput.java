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

/** 
    An InputOutput which represents an underlying output Modulation.
    Contains a single Jack, the Modulation in question, and a list of outgoing wires.
*/

public class ModulationOutput extends InputOutput
    {
    Jack jack;
    Modulation modulation;

    // All outgoing wires
    ArrayList<ModulationWire> outgoing = new ArrayList<>();
    
    // Holds the temporary wire being drawn by the user
    ModulationWire temp;
    
    // Handles mouse global released messages (because they don't always go to the right place)
    AWTEventListener releaseListener = null;
    
    // True if we're expecting a release, so the releaseListener's messages are valid 
    boolean mouseDown = false;
    
    // The previous ModulationInput which was highlighted as we passed over it during drawing, so we can unhighlight it
    ModulationInput lastModulationInput = null;
    
    /** Removes all wires attached to the ModulationOutput */
    public void disconnect()
        {
        // I'm not sure if disconnecting will mess up the arraylist, so I'm not sure if I can iterate over it here.
        // so instead I'll just copy first.
        ArrayList<ModulationWire> w = new ArrayList<>(outgoing);
        for(ModulationWire wire : w)
            wire.end.disconnect();
        }
    
    /** Constructor, given an owning modulation and ModPanel, plus which moduation output number we are in our owner. */
    public ModulationOutput(Modulation modulation, int number, final ModulePanel modPanel)
        {
        this.modulation = modulation;
        this.number = number;
        this.modPanel = modPanel;
        
        jack = new Jack(false);      

        title = new JLabel(modulation.getModulationOutputName(number) + " ", SwingConstants.RIGHT);
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
                temp = new ModulationWire(rack);
                temp.setStart(ModulationOutput.this);
                rack.addModulationWire(temp);
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
                            ModulationOutput.this.mouseReleased((MouseEvent)e);
                            }
                        }
                    }, AWTEvent.MOUSE_EVENT_MASK);

                mouseDown = true;
                }
                                        
            public void mouseDragged(MouseEvent e)
                {
                if (lastModulationInput != null)
                    lastModulationInput.highlight = false;
                ModulationInput input = modPanel.getRack().findModulationInputFor(e.getPoint(), jack);
                if (input != null)
                    {
                    lastModulationInput = input;
                    input.highlight = true; 
                    }
                modPanel.getRack().repaint();
                }
                                        
            public void mouseReleased(MouseEvent e)
                {
                ModulationOutput.this.mouseReleased(e);
                }

            public void mouseClicked(MouseEvent e)
                {
                for(ModulationWire wire : outgoing)
                    {
                    wire.chooseColor();
                    }
                }
            };
            
        jack.addMouseMotionListener(mouseAdapter);
        jack.addMouseListener(mouseAdapter);

        String[] help = modulation.wrapHelp(modulation.getModulationOutputHelp());
        if (help != null && help.length > number && help[number] != null)
            {
            jack.setToolTipText(help[number]);
            title.setToolTipText(help[number]);
            }
        }

    /** Attaches a ModulationWire connected to the given ModulationInput. */
    public void attach(ModulationInput input, ModulationWire wire)
        {
        Output output = input.modulation.getSound().getOutput();
        output.lock();
        try
            {
            input.disconnect();
                                                                                                
            input.modulation.setModulation(modulation, input.number, number);

            // distribute to all sounds
            int index = input.modulation.getSound().findRegistered(input.modulation);
            if (index == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                {
                return;
                }

            int outIndex = input.modulation.getSound().findRegistered(modulation);
            if (outIndex == Sound.NOT_FOUND)  // stray mouse event, probably just closed
                {
                return;
                }
                
            int numSounds = output.getNumSounds();
            for(int i = 0; i < numSounds; i++)
                {
                output.getSound(i).getRegistered(index).setModulation(
                    output.getSound(i).getRegistered(outIndex), input.number, this.number);
                }

            wire.setEnd(input);
            outgoing.add(wire);
            modPanel.getRack().addModulationWire(wire);
            }
        finally 
            {
            output.unlock();
            }

        input.incoming = wire;
        input.updateText();
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
        ModulationInput input = rack.findModulationInputFor(p, jack);
        rack.removeModulationWire(temp);

        if (input != null)
            {
            // does the wire already exist?
            boolean exists = false;
            for(ModulationWire wire: outgoing)
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
        if (lastModulationInput != null)
            lastModulationInput.highlight = false;
        lastModulationInput = null;
                                                
        rack.repaint();
        }
        
    public boolean isInput() { return false; }
    public boolean isUnit() { return false; }
    }

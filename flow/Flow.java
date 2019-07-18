// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import flow.gui.*;
import flow.modules.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;


public class Flow
    {
    // Flow Version
    public static int VERSION = 2;
    
    public static void main(String[] args)
        {
        Output output = new Output();

        Sound sound0 = null;
        for(int i = 0; i < Output.getNumVoices(); i++)
            {
            Sound sound = new Sound(output);
            if (i == 0) sound0 = sound;
            Out out = new Out(sound);               // findBugs thinks this is a dead store.  It isn't.
            sound.reset();
            }

        Rack rack = new Rack(output);
        rack.smallerOkay = true;
        for (Modulation mod : sound0.getRegistered())
            {
            // there will be only one: Out
            if (!(mod instanceof Constant) && !(mod instanceof Nil))
                rack.addModulation(mod);
            }
        rack.smallerOkay = false;
        rack.rebuild();
        
        Dimension d = rack.getScrollPane().getPreferredSize();
        d.height *= 1.6;
        rack.getScrollPane().setPreferredSize(d);

        rack.sprout();
                
        output.startPrimaryVoiceThread();       
        }
    }

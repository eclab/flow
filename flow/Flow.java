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
import org.json.*;
import java.io.*;
import java.util.zip.*;


public class Flow
    {
    // Flow Version
    public static int VERSION = 12;
    
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

        final Rack rack = new Rack(output);
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
        
        if (args.length == 1)
            {
            SwingUtilities.invokeLater(new Runnable() 
                {
                public void run()
                    {
                    try
                        {
                        File f = new File(args[0]);
                        AppMenu.doLoad(rack, new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(f)))), true);
                        AppMenu.setLastFile(f);
                        rack.setPatchFile(f);
                        rack.setPatchName(rack.getPatchName());
                        }
                    catch(Exception ex) { System.err.println("Couldn't load file " + args[0]);  System.err.println(ex); }
                    }
                });
            }
        }
    }

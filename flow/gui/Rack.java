// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.gui;

import flow.*;
import flow.modules.*;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

import javax.sound.sampled.*;

import org.json.*;

/**
   Rack is the container which holds ModulePanels and allows you to move them, delete them,
   and add new ones.  Rack also is responsible for actually managing the wires layer.  Most
   GUI objects access Rack because it holds the global output object, which they often need
   to obtain the Sound Lock.
*/

public class Rack extends JPanel
    {
    JScrollPane pane;
    Box box;
    Output output;
    String patchName = null;
    String patchAuthor = null;
    String patchDate = null;
    String patchVersion = null;
    String patchInfo = null;
    boolean addModulesAfter;
    
    // A list of all current module panels.  Note that this isn't all the
    // *Modulations* in use -- Constants and NILs don't get panels.  But they're
    // also not registered with Sounds.
    ArrayList<ModulePanel> allModulePanels = new ArrayList<> ();

    // Lists of all wires currently in use in the Rack
    ArrayList<UnitWire> unitWires = new ArrayList<>();
    ArrayList<ModulationWire> modulationWires = new ArrayList<>();
    
    public JScrollPane getScrollPane() { return pane; }
    
    public Rack(Output output)
        {
        super();
        this.output = output;

        box = new Box(BoxLayout.X_AXIS)
            {
            public void paint(Graphics g)
                {
                super.paint(g);
                for(UnitWire wire: unitWires)
                    {
                    wire.draw((Graphics2D)g);
                    }
                for(ModulationWire wire: modulationWires)
                    {
                    wire.draw((Graphics2D)g);
                    }
                }
            };
        JPanel pane1 = new JPanel();
        pane1.setLayout(new BorderLayout());
        pane1.add(box, BorderLayout.WEST);
        pane = new JScrollPane(pane1, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(pane, BorderLayout.CENTER);
        
        Box pane2 = new Box(BoxLayout.X_AXIS);
        Display display = new Display(output, false);
        display.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, new JLabel().getBackground()));
        pane2.add(display);
        Oscilloscope display2 = new Oscilloscope(output, false);
        display2.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, new JLabel().getBackground()));
        pane2.add(display2);
        display2 = new Oscilloscope(output, true);
        display2.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 2, new JLabel().getBackground()));
        pane2.add(display2);
        display = new Display(output, true);
        display.setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new JLabel().getBackground()));
        pane2.add(display);
        
        add(pane2, BorderLayout.NORTH);

        setTransferHandler(new ModulePanelTransferHandler());
        this.setDropTarget(new DropTarget(this, new ModulePanelDropTargetListener())
            {
            public int getDefaultActions() { return TransferHandler.MOVE | TransferHandler.COPY; }
            });
        setPatchName(getPatchName());
        setAddModulesAfter(Prefs.getLastAddModulesAfter());

        if (Style.isMac())
            Mac.setup(this);
        }
    
    public JFrame sprout()
        {
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        
        JMenuBar menubar = new JMenuBar();
        menubar.add(AppMenu.provideFileMenu(this));
        menubar.add(AppMenu.providePlayMenu(this));
        menubar.add(AppMenu.provideModuleMenu(this));
        if (Style.isWindows() || Style.isUnix())
            {
            menubar.add(AppMenu.provideWindowsAboutMenu(this));
            }
        frame.setJMenuBar(menubar);

        frame.addWindowListener(new java.awt.event.WindowAdapter() 
            {
            public void windowClosing(java.awt.event.WindowEvent windowEvent) 
                {
                doCloseWindow();
                }
            });

        frame.pack();
        frame.show();
        
        return frame;
        }

    public void doQuit()
        {
        System.exit(0);
        }
        
    public void doCloseWindow()
        {
        doQuit();
        }
        
    public boolean getAddModulesAfter() { return addModulesAfter; }
    public void setAddModulesAfter(boolean val) { addModulesAfter = val; }
        
    public String getPatchName() { return patchName; }
    public void setPatchName(String val) 
        { 
        patchName = val; 
        String p = patchName;
        if (p == null) p = "Untitled";
        Object frame = ((JFrame)(SwingUtilities.getWindowAncestor(this)));
        if (frame != null && frame instanceof JFrame)
            {
            ((JFrame) frame).setTitle(p);
            }
        }

    public String getPatchAuthor() { return patchAuthor; }
    public void setPatchAuthor(String val) { patchAuthor = val; }

    public String getPatchDate() { return patchDate; }
    public void setPatchDate(String val) { patchDate = val; }

    public String getPatchVersion() { return patchVersion; }
    public void setPatchVersion(String val) { patchVersion = val; }

    public String getPatchInfo() { return patchInfo; }
    public void setPatchInfo(String val) { patchInfo = val; }
    
    /** Adds a Unit Wire to the rack */
    public void addUnitWire(UnitWire wire) { unitWires.add(wire); }
    /** Removes a Unit Wire from the rack */
    public void removeUnitWire(UnitWire wire) { unitWires.remove(wire); }
    /** Adds a Modulation Wire to the rack */
    public void addModulationWire(ModulationWire wire) { modulationWires.add(wire); }
    /** Removes a Unit Wire from the rack */
    public void removeModulationWire(ModulationWire wire) { modulationWires.remove(wire); }
    
    /** Returns the global Output */
    public Output getOutput() { return output; }
    
    /** Returns the component which, on repainting, draws the wires.  */
    public Component getWirePaintComponent() { return box; }
    
    /** Returned by getIndex if it can't find the given panel */
    public static final int NOT_FOUND = -1;
    /** Returns the index of the given ModulePanel in the Rack. */ 
    public int getIndex(ModulePanel panel)
        {
        for(int i = 0; i < allModulePanels.size(); i++)
            {
            if (allModulePanels.get(i) == panel)
                return i;
            }
        return NOT_FOUND;
        }
    
    /** Returns all ModulePanels in the Rack */
    public ModulePanel[] getAllModulePanels() { return allModulePanels.toArray(new ModulePanel[allModulePanels.size()]); }
    /** Returns the ModulePanel associated with the given index in the Rack. */
    public ModulePanel getModulePanel(int index) { return allModulePanels.get(index); }
    
    public void scrollToRight()
        {
        box.revalidate();
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                JScrollBar b = pane.getHorizontalScrollBar();
                if (b != null) b.setValue( b.getMaximum() );
                }
            });
        }
    
    public void scrollToLeft()
        {
        box.revalidate();
        SwingUtilities.invokeLater(new Runnable()
            {
            public void run()
                {
                JScrollBar b = pane.getHorizontalScrollBar();
                if (b != null) b.setValue( b.getMinimum() );
                }
            });
        }
    
    public void reset()
        {
        output.lock();
        try
            {
            int len = output.getNumSounds();
            
            // Clear all notes
            for(int i = 0; i < len; i++)
                {
                output.getSound(i).release();
                }
            
            // Perform reset
            for(int i = 0; i < len; i++)
                {
                output.getSound(i).reset();
                }
            }
        finally 
            {
            output.unlock();
            }
        }

    /** Loads a Macro from the given file and adds it to the Rack. */
    public void addMacro(File file)
        {
        try
            {
            Modulation firstModulation = null;
            output.lock();
            try
                {
                int num = output.getNumSounds();
                for(int i = 0; i < num; i++)
                    {
                    Sound sound = output.getSound(i);
                    Macro macro = Macro.loadMacro(sound, file);  // Macro.deserializeAsMacro(sound, file);
                    if (firstModulation == null)
                        firstModulation = macro;
                    }
                }
            finally 
                {
                output.unlock();
                }
            ModulePanel pan = firstModulation.getPanel();
            addModulePanel(pan);
            reset();
            repaint();
 
            // now move to front.  Very inefficient
            move(pan, 0);
            }
        catch (Exception ex)
            {
            output.unlock();
            ex.printStackTrace();
            }
        }
  
    /** Adds a modulation to the end of the rack.  This should not be a Constant or Nil.  
        We assume that the modulation, and copies of it, have
        been registered with all of the Sounds already. */ 
    public void addModulation(Modulation modulation)
        {
        addModulePanel(modulation.getPanel());
        reset();
        repaint();
        }
  
    /** Builds a ModulePanel for a Modulation of the class moduleClass and adds it to the BEGINNING the Rack. */
    public void add(Class moduleClass)
        {
        try
            {
            Modulation firstModulation = null;
            output.lock();
            try
                {
                int num = output.getNumSounds();
                for(int i = 0; i < num; i++)
                    {
                    Sound sound = output.getSound(i);
                    Modulation modulation = (Modulation)(moduleClass.getConstructor(Sound.class).newInstance(sound));
                    modulation.reset();
                    if (firstModulation == null)
                        firstModulation = modulation;
                    }
                }
            finally 
                {
                output.unlock();
                }
            ModulePanel pan = firstModulation.getPanel();
            addModulePanel(pan);
            reset();
            repaint();
                
            // now move to front.  Very inefficient
            move(pan, 0);
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            }
        }
        
    // Adds a ModulePanel to the Rack. Does not wire it up or associate it with
    //    a Modulation or distribute Modulations to the various Sounds -- you are
    //    responsible for doing this.
    void addModulePanel(ModulePanel panel)
        {
        box.add(panel);
        allModulePanels.add(panel);
        panel.setRack(this);
        box.revalidate();
        checkOrder();
        resetEmits();
        }
    
    // Removes a ModulePanel from the Rack. Does not disconnect it or deassociate it with
    //    a Modulation or distribute Modulations to the various Sounds -- you are
    //    responsible for doing this.  */
    void remove(ModulePanel panel) 
        {
        box.remove(panel);
        allModulePanels.remove(panel);
        }
        
    public void move(ModulePanel droppedPanel, int position)
        {
        // move the panel
        
        int removed = allModulePanels.indexOf(droppedPanel);
        allModulePanels.remove(droppedPanel);
        allModulePanels.add(position, droppedPanel);
                        
        // reorganize sounds
        output.lock();
        try
            {
            int len = getOutput().getNumSounds();
            for(int i = 0; i < len; i++)
                {
                Modulation mod = getOutput().getSound(i).removeRegistered(removed);
                getOutput().getSound(i).addRegistered(position, mod);
                }
            }
        finally 
            {
            output.unlock();
            }

        // rebuild the box from scratch
        box.removeAll();
        for(int i = 0; i < allModulePanels.size(); i++)
            box.add(allModulePanels.get(i));
        box.revalidate();
        repaint();
        checkOrder();
        resetEmits();
        }
        

    /** Closes all Modules in the Rack (effectively deleting them).  */
    public void closeAll()
        {
        ModulePanel[] panels = (ModulePanel[])(allModulePanels.toArray(new ModulePanel[0]));
        for(int i = 0; i < panels.length; i++)
            panels[i].close();
        }
        
    /** Rebuilds the Rack.  Clears all the wires, then rebuilds all the ModulePanels (which rebuilds
        the wires).  */
    public void rebuild()
        {
        unitWires.clear();
        modulationWires.clear();
        for(ModulePanel modPanel : allModulePanels)
            modPanel.rebuild();
        checkOrder();
        }

    /** Set this temporariliy to allow checkOrder to ignore the fact that sounds might hold more modulations
        than there are modulationpanels because you bulk-added them before adding the panels. */
    public boolean smallerOkay;     
    
    /** Perform some verification for debugging. */
    public void checkOrder()
        {
        output.lock();
        try
            {
            int len = output.getNumSounds();
            for(int i = 0; i < len; i++)
                {                       
                if ((smallerOkay && allModulePanels.size() < output.getSound(i).getNumRegistered()))
                    {
                    return;
                    }
                else if (allModulePanels.size() != output.getSound(i).getNumRegistered())
                    {
                    System.err.println("Rack CHECK WARNING: Sound " + i + " differs in length from allModulePanels (" +
                        output.getSound(i).getNumRegistered() + " vs " + 
                        allModulePanels.size());
                    //new Throwable().printStackTrace();
                    return;
                    }
                }
                                
            for(int j = 0; j < output.getSound(0).getNumRegistered(); j++)
                {
                Modulation mod = output.getSound(0).getRegistered(j);
                if (mod != allModulePanels.get(j).getModulation())
                    {
                    System.err.println("Rack CHECK WARNING: Modulation " + j + " in Sound " + 0 + 
                        " is a " + mod + " but associated panel doesn't point to the same object.  It points to " + allModulePanels.get(j).getModulation());
                    //new Throwable().printStackTrace();
                    return;                                 
                    }
                }

            for(int i = 1; i < len; i++)
                {
                for(int j = 0; j < output.getSound(i).getNumRegistered(); j++)
                    {
                    Modulation mod = output.getSound(i).getRegistered(j);
                    if (mod.getClass() != allModulePanels.get(j).getModulation().getClass())
                        {
                        System.err.println("Rack CHECK WARNING: Modulation " + j + " in Sound " + i + 
                            " is " + mod + " but associated panel holds " + allModulePanels.get(j).getModulation());
                        //new Throwable().printStackTrace();
                        return;                                 
                        }
                    }
                }
            }
        finally 
            {
            output.unlock();
            }

        for(int j = 0; j < unitWires.size(); j++)
            {
            boolean found = false;
            UnitWire wire = unitWires.get(j);
            for(int i = 0; i < allModulePanels.size(); i++)
                {
                if (allModulePanels.get(i) == wire.getStart().getModulePanel())
                    { found = true; break; }
                }
            if (!found)
                {
                System.err.println("Rack CHECK WARNING: UnitWire  " + j + " has as start nonexistent ModPanel " +
                    wire.getStart().getModulePanel() + " " + wire.getStart().number);
                //new Throwable().printStackTrace();
                return;
                }
                        
            found = false;
            for(int i = 0; i < allModulePanels.size(); i++)
                {
                if (allModulePanels.get(i) == wire.getEnd().getModulePanel())
                    { found = true; break; }
                }
            if (!found)
                {
                System.err.println("Rack CHECK WARNING: UnitWire  " + j + " has as end nonexistent ModPanel " +
                    wire.getEnd().getModulePanel() + " " + wire.getStart().number);
                //new Throwable().printStackTrace();
                return;
                }
            }
                        
        for(int j = 0; j < modulationWires.size(); j++)
            {
            boolean found = false;
            ModulationWire wire = modulationWires.get(j);
            for(int i = 0; i < allModulePanels.size(); i++)
                {
                if (allModulePanels.get(i) == wire.getStart().getModulePanel())
                    { found = true; break; }
                }
            if (!found)
                {
                System.err.println("Rack CHECK WARNING: UnitWire  " + j + " has as start nonexistent ModPanel " +
                    wire.getStart().getModulePanel() + " " + wire.getStart().number);
                //new Throwable().printStackTrace();
                return;
                }

            found = false;
            for(int i = 0; i < allModulePanels.size(); i++)
                {
                if (allModulePanels.get(i) == wire.getEnd().getModulePanel())
                    { found = true; break; }
                }
            if (!found)
                {
                System.err.println("Rack CHECK WARNING: UnitWire  " + j + " has as end nonexistent ModPanel " +
                    wire.getEnd().getModulePanel() + " " + wire.getEnd().number);
                //new Throwable().printStackTrace();
                return;
                }
            }
        }


    /** Returns the UnitInput associated with a point in a given Component (ModulePanel),
        else null.  */

    public UnitInput findUnitInputFor(Point p, Component comp)
        {
        for(ModulePanel modPanel : allModulePanels)
            {
            Point p2 = new Point(p);
            p2 = SwingUtilities.convertPoint(comp, p2, modPanel);
            UnitInput unit = _findUnitInputFor(p2, modPanel);
            if (unit != null) return unit;
            }
        return null;
        }
    
    UnitInput _findUnitInputFor(Point p, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof InputOutput.Jack)
                {
                if (comp.getBounds().contains(p))  // got it
                    {
                    if (((InputOutput.Jack)comp).getParent() instanceof UnitInput)
                        {
                        return (UnitInput)(((InputOutput.Jack)comp).getParent());
                        }
                    }
                }
            else if (comp instanceof Container)
                {
                if (comp.getBounds().contains(p))
                    {
                    Point p2 = new Point(p);
                    p2 = SwingUtilities.convertPoint(c, p2, comp);
                    UnitInput unit = _findUnitInputFor(p2, (Container) comp);
                    if (unit != null) return unit;
                    }
                }
            }
        return null;
        }
        
    /** Returns the ModulationInput associated with a point in a given Component (ModulePanel),
        else null.  */

    public ModulationInput findModulationInputFor(Point p, Component comp)
        {
        for(ModulePanel modPanel : allModulePanels)
            {
            Point p2 = new Point(p);
            p2 = SwingUtilities.convertPoint(comp, p2, modPanel);
            ModulationInput unit = _findModulationInputFor(p2, modPanel);
            if (unit != null) return unit;
            }
        return null;
        }
    
    ModulationInput _findModulationInputFor(Point p, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof ModulationInput.Dial)
                {
                if (comp.getBounds().contains(p))  // got it
                    {
                    return ((ModulationInput.Dial)comp).modulationInput;
                    }
                }
            else if (comp instanceof Container)
                {
                if (comp.getBounds().contains(p))
                    {
                    Point p2 = new Point(p);
                    p2 = SwingUtilities.convertPoint(c, p2, comp);
                    ModulationInput unit = _findModulationInputFor(p2, (Container) comp);
                    if (unit != null) return unit;
                    }
                }
            }
        return null;
        }

    /** Determines and sets up the emitting Out Module, if any. */
    public void resetEmits()
        {
        Output output = getOutput();                        
        // Is there an Out?
        for(int i = allModulePanels.size() - 1; i >= 0 ; i--)
            {
            ModulePanel modPanel = allModulePanels.get(i);
            Modulation mod = modPanel.getModulation();
            if (mod instanceof Out)  // got it
                {
                int index = getIndex(modPanel);
                output.lock();
                try
                    {
                    int numSounds = output.getNumSounds();
                    for(int j = 0; j < numSounds; j++)
                        {
                        Sound sound = output.getSound(j);
                        Out o = (Out)(sound.getRegistered(index));
                        sound.setEmits(o);
                        }
                    }
                finally 
                    {
                    output.unlock();
                    }
                return;
                }
            }                       
                
        // if we've gotten here, there's nothing to emit
        output.lock();
        try
            {
            int numSounds = output.getNumSounds();
            for(int i = 0; i < numSounds; i++)
                {
                Sound sound = output.getSound(i);
                sound.setEmits(null);
                }
            }
        finally 
            {
            output.unlock();
            }
        }




    /** Prints statistical information about module panels for debugging purposes. */
    public void printStats()
        {
        for(ModulePanel panel : allModulePanels)
            panel.printStats();
        }


    public void chooseTuningParameters()
        {
        int[] partials = new int[] { 64, 128, 256 };
        String[] s_partials = new String[] { "64", "128", "256" };
        JComboBox partialsCombo = new JComboBox(s_partials);
        int partial = Prefs.getLastNumPartials();
        partialsCombo.setSelectedIndex(partial == 64 ? 0 : (partial == 128 ? 1 : 2));

        int[] voices = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
        String[] s_voices = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" };
        JComboBox voicesCombo = new JComboBox(s_voices);
        int voice = Prefs.getLastNumVoices();
        voicesCombo.setSelectedIndex(voice - 1);
        
        int[] bufferSize = new int[] { 128, 256, 512, 1024, 1536, 2048, 2560, 3072, 3584, 4096 };
        String[] s_bufferSize = new String[] { "128", "256", "512", "1024", "1536", "2048", "2560", "3072", "3584", "4096" };
        JComboBox bufferSizeCombo = new JComboBox(s_bufferSize);
        int bs = Prefs.getLastBufferSize();
        int index = 0;
        for(int i = bufferSize.length - 1; i > 0; i--)
            {
            if (bufferSize[i] <= bs)
                { index = i; break; }
            }
        bufferSizeCombo.setSelectedIndex(index);
        
        int result = showMultiOption(this, 
            new String[] { "Polyphony", "Audio Buffer Size", "Partials" }, 
            new JComponent[] { voicesCombo, bufferSizeCombo, partialsCombo }, 
            "Tuning Parameters", 
            "<html>Parameters don't take effect until<br>the synthesizer is restarted.",
            new String[] { "Okay", "Cancel", "Reset" });
        
        if (result == 0)  // OKAY
            {
            Prefs.setLastNumVoices(voices[voicesCombo.getSelectedIndex()]);
            Prefs.setLastBufferSize(bufferSize[bufferSizeCombo.getSelectedIndex()]);
            Prefs.setLastNumPartials(partials[partialsCombo.getSelectedIndex()]);
            }
        else if (result == 2) // RESET
            {
            Prefs.setLastNumVoices(Output.DEFAULT_NUM_VOICES);
            Prefs.setLastBufferSize(Output.DEFAULT_BUFFER_SIZE);
            Prefs.setLastNumPartials(Unit.DEFAULT_NUM_PARTIALS);
            }
        }


        
    public void chooseMIDIandAudio()
        {
        ArrayList<Midi.MidiDeviceWrapper> devices = output.getInput().getDevices();
        JComboBox devicesCombo = new JComboBox(devices.toArray());
        devicesCombo.setSelectedItem(output.getInput().getMidiDevice());
        String midiDevice = Prefs.getLastMidiDevice();
        for(Midi.MidiDeviceWrapper m : devices)
            {
            if (m.toString().equals(midiDevice))
                { devicesCombo.setSelectedItem(m); break; }
            }

        String[] mpeChannelNames = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" };
        final JComboBox mpeChannelsCombo = new JComboBox(mpeChannelNames);
        int numMPEChannels = Prefs.getLastNumMPEChannels();
        mpeChannelsCombo.setSelectedIndex(numMPEChannels - 1);


        String[] channelNames = new String[] { "MPE Lower Zone", "MPE Higher Zone", "Any Channel", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" };
        final JComboBox channelsCombo = new JComboBox(channelNames);
        channelsCombo.addItemListener(new ItemListener()
            {
            public void itemStateChanged(ItemEvent e)
                {
                mpeChannelsCombo.setEnabled(channelsCombo.getSelectedIndex() < 2);
                }
            });
        int channel = Prefs.getLastChannel();
        mpeChannelsCombo.setEnabled(channel < Input.CHANNEL_OMNI);
        channelsCombo.setSelectedIndex(channel + Input.NUM_SPECIAL_CHANNELS);
        
        
        Mixer.Info[] mixers = output.getSupportedMixers();
        String[] mixerNames = new String[mixers.length];
        Mixer.Info mixer = output.getMixer();
        for(int i = 0; i < mixers.length; i++)
            {
            mixerNames[i] = mixers[i].getName();
            }
        JComboBox mixersCombo = new JComboBox(mixerNames);
        mixersCombo.setSelectedItem(output.getMixer().toString());
        String mix = Prefs.getLastAudioDevice();
        for(String m : mixerNames)
            {
            if (m.equals(mix))
                { mixersCombo.setSelectedItem(m); break; }
            }
        
        boolean result = showMultiOption(this, 
            new String[] { "MIDI Device", "MIDI Channel", "MPE Channels", "Audio Device" }, 
            new JComponent[] { devicesCombo, channelsCombo, mpeChannelsCombo, mixersCombo }, 
            "MIDI and Audio Options", 
            "Select the MIDI device and channel, and the Audio device.");
                        
        if (result)
            {
            // set up
            output.setMixer(mixers[mixersCombo.getSelectedIndex()]);
            output.getInput().setupMIDI(
                channelsCombo.getSelectedIndex() - Input.NUM_SPECIAL_CHANNELS,
                mpeChannelsCombo.getSelectedIndex() + 1,
                devices.get(devicesCombo.getSelectedIndex()));
                
            Prefs.setLastMidiDevice(devicesCombo.getSelectedItem().toString());
            Prefs.setLastChannel(channelsCombo.getSelectedIndex() - Input.NUM_SPECIAL_CHANNELS);
            Prefs.setLastNumMPEChannels(mpeChannelsCombo.getSelectedIndex() + 1);
            Prefs.setLastAudioDevice(mixersCombo.getSelectedItem().toString());
            }
        }

    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   Returns TRUE if the user performed
        the operation, FALSE if cancelled. */
    public static boolean showMultiOption(JComponent root, String[] labels, JComponent[] widgets, String title, String message)
        {
        return showMultiOption(root, labels, widgets, title, message, null) == JOptionPane.OK_OPTION;
        }


    /** Perform a JOptionPane confirm dialog with MUTLIPLE widgets that the user can select.  The widgets are provided
        in the array WIDGETS, and each has an accompanying label in LABELS.   You can also optionally provide BUTTONS,
        which is an array of buttons (such as "Okay" or "Cancel" or "Thermonuclear War").  On the Mac, the buttons will
        appear right-to-left, and traditionally the default "Okay" button equivalent should be first (that is, button[0]).
        Returns the button pressed.  If buttons is null or is an empty array, then this reverts to a standard okay/cancel
        button array, returning either JOptionPane.OK_OPTION or JOptionPane.CANCEL_OPTION. */
    public static int showMultiOption(JComponent root, String[] labels, JComponent[] widgets, String title, String message, String[] buttons)
        {
        WidgetList list = new WidgetList(labels, widgets);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel p = new JPanel();
        p.setLayout(new BorderLayout());
        p.add(new JLabel("    "), BorderLayout.NORTH);
        if (message != null)
            {
            p.add(new JLabel(message), BorderLayout.CENTER);
            p.add(new JLabel("    "), BorderLayout.SOUTH);
            }
        panel.add(p, BorderLayout.NORTH);
        panel.add(list, BorderLayout.CENTER);
        if (buttons == null || buttons.length == 0)
            {
            return JOptionPane.showConfirmDialog(root, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
            }
        else
            {
            return JOptionPane.showOptionDialog(root, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, buttons, buttons[0]);
            }
        }
    
    ArrayList<JMenuItem> disabledMenus = null;
    int disableCount;
    public void disableMenuBar()
        {
        if (disabledMenus == null)
            {
            disabledMenus = new ArrayList<JMenuItem>();
            disableCount = 0;
            JMenuBar bar = ((JFrame)(SwingUtilities.getWindowAncestor(this))).getJMenuBar();
            for(int i = 0; i < bar.getMenuCount(); i++)
                {
                JMenu menu = bar.getMenu(i);
                if (menu != null)
                    {
                    for(int j = 0; j < menu.getItemCount(); j++)
                        {
                        JMenuItem item = menu.getItem(j);
                        if (item != null && item.isEnabled())           // apparently separators return null
                            {
                            disabledMenus.add(item);
                            item.setEnabled(false);
                            }
                        }
                    }
                }
            }
        else
            {
            disableCount++;
            return;
            }
        }       
        
    public void enableMenuBar()
        {
        if (disableCount == 0)
            {
            for(int i = 0; i < disabledMenus.size(); i++)
                {
                disabledMenus.get(i).setEnabled(true);
                }
            disabledMenus = null;
            }
        else
            {
            disableCount--;
            }
        }       


    public static final int LABEL_MAX_LENGTH = 32;
    public static String[] showPatchDialog(JComponent root, String name, String author, String date, String version, String info)
        {
        if (name == null) name = "";
        if (author == null) author = "";
        if (date == null) date = "";
        if (version == null) version = "";
        if (info == null) info = "";

        JTextField n = new JTextField(LABEL_MAX_LENGTH);
        n.setText(name);

        JTextField a = new JTextField(LABEL_MAX_LENGTH);
        a.setText(author);

        JTextField d = new JTextField(LABEL_MAX_LENGTH);
        d.setText(date);

        JTextField v = new JTextField(LABEL_MAX_LENGTH);
        v.setText(version);

        JTextArea i = new JTextArea(5, LABEL_MAX_LENGTH);
        i.setText(info);
        i.setLineWrap(true);
		i.setWrapStyleWord(true);
        JScrollPane pane = new JScrollPane(i);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.setBorder(v.getBorder());

        int result = showMultiOption(root, 
            new String[] { "Patch Name", "Author", "Date", "Version", "Patch Info" }, 
            new JComponent[] { n, a, d, v, pane }, 
            "Patch Info", null,
            new String[] { "Okay", "Cancel" });
            
        if (result == 1)  // cancel
            return new String[] { name, author, date, version, info };
        else
            return new String[] { n.getText(), a.getText(), d.getText(), v.getText(), i.getText() };
        }
                


    /// Drag-and-drop data flavor
    static DataFlavor flavor = null;
    
    static
        {
        try
            {
            flavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=flow.gui.ModulePanel");
            }
        catch (ClassNotFoundException ex)
            {
            ex.printStackTrace();
            }
        }

    }




////// DRAG AND DROP JUNK


class ModulePanelTransferHandler extends TransferHandler implements DragSourceMotionListener 
    {
    /*
      public boolean canImport(TransferHandler.TransferSupport support) 
      {
      if (!support.isDrop() || !support.isDataFlavorSupported(Rack.flavor)) return false;
        
      if ((support.getDropAction() & TransferHandler.COPY) == TransferHandler.COPY)
      {
      return true;
      }
      else if ((support.getDropAction() & TransferHandler.MOVE) == TransferHandler.MOVE)
      {
      return true;
      }
      else return false;
      }
    */

    public Transferable createTransferable(JComponent c) 
        {
        if (c instanceof ModulePanel) 
            {
            return (Transferable) c;
            }
        else return null;
        }

    public int getSourceActions(JComponent c) 
        {
        if (c instanceof ModulePanel) 
            {
            return TransferHandler.COPY | TransferHandler.MOVE;
            }
        else return TransferHandler.NONE;
        }

    public void dragMouseMoved(DragSourceDragEvent dsde) {}
    } 

class ModulePanelDropTargetListener extends DropTargetAdapter 
    {
    public void drop(DropTargetDropEvent dtde) 
        {
        Object transferableObj = null;
        
        try 
            {
            if (dtde.getTransferable().isDataFlavorSupported(Rack.flavor))
                {
                transferableObj = dtde.getTransferable().getTransferData(Rack.flavor);
                } 
            
            } 
        catch (Exception ex) {  }
        
        if (transferableObj != null)
            {
            ModulePanel droppedPanel = (ModulePanel)transferableObj;
            Rack rack = droppedPanel.getRack();
                
            Point p = dtde.getLocation();
            Component comp = dtde.getDropTargetContext().getComponent();
            
            int removed = 0;
            int added = -1;
            
            if (dtde.getDropAction() == DnDConstants.ACTION_MOVE)
                {
                if (comp instanceof ModulePanel)
                    {
                    if (comp == droppedPanel) return;  // no change
                    boolean before = (p.getX() < comp.getWidth() / 2);  // p is in a ModulePanel coordinate system
                    removed = rack.allModulePanels.indexOf(droppedPanel);
                    rack.allModulePanels.remove(droppedPanel);
                    for(int i = 0; i < rack.allModulePanels.size(); i++)
                        {
                        if (rack.allModulePanels.get(i) == comp)
                            {
                            added = (before ? i : i + 1);
                            rack.allModulePanels.add(added, (ModulePanel)droppedPanel);
                            break;
                            }
                        }
                    }
                else if (comp == rack)  // we dragged to the end
                    {
                    final int SCROLLBAR_SLOP = 20;
                    final int TOP_SLOP = 4;
                    Rectangle paneBounds = rack.pane.getBounds();
                    if (p.y <= paneBounds.y + TOP_SLOP || p.y >= paneBounds.y + paneBounds.height - SCROLLBAR_SLOP)
                        {
                        System.err.println("Drag failed");  // display region
                        return;
                        }
                    else
                        {       
                        removed = rack.allModulePanels.indexOf(droppedPanel);
                        rack.allModulePanels.remove(droppedPanel);
                        added = rack.allModulePanels.size();
                        rack.allModulePanels.add(added, (ModulePanel)droppedPanel);
                        }
                    }
                else
                    {
                    System.err.println("Drag failed");  // wasn't a mod panel I guess
                    return;
                    }
                                
                if (removed == -1)
                    {
                    System.err.println("ModulePanelDropTargetListener WARNING: no such removed panel " + droppedPanel);
                    }
                else if (added == -1)
                    {
                    System.err.println("ModulePanelDropTargetListener WARNING: no such added panel relative to " + comp);
                    }
                else
                    {
                    // reorganize sounds
                    rack.getOutput().lock();
                    try
                        {
                        int len = rack.getOutput().getNumSounds();
                        for(int i = 0; i < len; i++)
                            {
                            Modulation mod = rack.getOutput().getSound(i).removeRegistered(removed);
                            rack.getOutput().getSound(i).addRegistered(added, mod);
                            }
                        }
                    finally 
                        {
                        rack.getOutput().unlock();
                        }
                    }
                }
            else if (dtde.getDropAction() == DnDConstants.ACTION_COPY) /// COPYING
                {
                if (comp instanceof ModulePanel)
                    {
                    if (comp == droppedPanel) return;  // no change
                    boolean before = (p.getX() < comp.getWidth() / 2);  // p is in a ModulePanel coordinate system
                    removed = rack.allModulePanels.indexOf(droppedPanel);
                    for(int i = 0; i < rack.allModulePanels.size(); i++)
                        {
                        if (rack.allModulePanels.get(i) == comp)
                            {
                            added = (before ? i : i + 1);
                            break;
                            }
                        }
                    }
                else if (comp == rack)  // we dragged to the end
                    {
                    final int SCROLLBAR_SLOP = 20;
                    final int TOP_SLOP = 4;
                    Rectangle paneBounds = rack.pane.getBounds();
                    if (p.y <= paneBounds.y + TOP_SLOP || p.y >= paneBounds.y + paneBounds.height - SCROLLBAR_SLOP)
                        {
                        System.err.println("Drag failed");  // display region
                        return;
                        }
                    else
                        {       
                        removed = rack.allModulePanels.indexOf(droppedPanel);
                        added = rack.allModulePanels.size();
                        }
                    }
                else
                    {
                    System.err.println("Drag failed");  // wasn't a mod panel I guess
                    return;
                    }
                                
                if (removed == -1)
                    {
                    System.err.println("ModulePanelDropTargetListener WARNING: no such removed panel " + droppedPanel);
                    }
                else if (added == -1)
                    {
                    System.err.println("ModulePanelDropTargetListener WARNING: no such added panel relative to " + comp);
                    }
                else
                    {
                    // reorganize sounds
                    rack.getOutput().lock();
                    try
                        {
                        if (!(rack.getOutput().getSound(0).getRegistered(removed) instanceof Out))  // can't copy Out
                            {
                            int len = rack.getOutput().getNumSounds();
                            Modulation mod0 = null;
                            for(int i = 0; i < len; i++)
                                {
                                Modulation mod = rack.getOutput().getSound(i).getRegistered(removed);
                                Modulation newmod = (Modulation)(mod.clone());
                                if (mod0 == null) mod0 = newmod;
                                rack.getOutput().getSound(i).addRegistered(added, newmod);
                                }
                            ModulePanel mp = mod0.getPanel();
                            mp.setRack(rack);
                            rack.allModulePanels.add(added, mp);
                            }
                        }
                    finally 
                        {
                        rack.getOutput().unlock();
                        }
                                                        
                    }
                }       
                        
            // rebuild the box from scratch
            rack.box.removeAll();
            for(int i = 0; i < rack.allModulePanels.size(); i++)
                rack.box.add(rack.allModulePanels.get(i));
            rack.rebuild();
            rack.box.revalidate();
            rack.repaint();
                        
            rack.checkOrder();

            rack.resetEmits();
            }
        }
    }

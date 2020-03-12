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

/**
   ModulePanel is the GUI representation of a given Modulation or Unit (a module).
*/

public class ModulePanel extends JPanel implements Transferable
    {
    // This is the modulation associated with ModulePanel for Sound #0.
    Modulation modulation;
    Rack rack;
    JComponent title;
    
    JLabel titleLabel;
    JComponent titlePanel;
    
    ModulationInput[] modIn = new ModulationInput[0];
    ModulationOutput[] modOut = new ModulationOutput[0];
    public ModulationInput getModIn(int i) { return modIn[i]; }
    public ModulationOutput getModOut(int i) { return modOut[i]; }
    
    public boolean getFillPanel() { return false; }
        
    public ModulePanel(Modulation mod)
        {
        modulation = mod;
        modulation.setModulePanel(this);
        
        setLayout(new BorderLayout());
        title = buildTitle();
        add(title, BorderLayout.NORTH);
        title.addMouseListener(new MouseAdapter()
            {
            public void mousePressed(MouseEvent e)
                {
                getTransferHandler().exportAsDrag(ModulePanel.this, e, TransferHandler.COPY);
                }
            });
                
        JPanel pan2 = new JPanel();
        pan2.setLayout(new BorderLayout());
        if (getFillPanel())
            pan2.add(buildPanel(), BorderLayout.CENTER);
        else
            pan2.add(buildPanel(), BorderLayout.NORTH);
                
        add(pan2, BorderLayout.CENTER);
                
        Border border = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 2, 2, 2),
            BorderFactory.createLineBorder(Color.GRAY));
        border = BorderFactory.createCompoundBorder(
            border,
            BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setBorder(border);

        this.setTransferHandler(new ModulePanelTransferHandler());
        this.setDropTarget(new DropTarget(this, new ModulePanelDropTargetListener()));
        }
        

    // close box
    static final ImageIcon I_CLOSE = iconFor("BellyButton.png");
    static final ImageIcon I_CLOSE_PRESSED = iconFor("BellyButtonPressed.png");

    static ImageIcon iconFor(String name)
        {
        return new ImageIcon(ModulePanel.class.getResource(name));
        }

    /** Returns the associated Modulation.  Note a ModulePanel is associated with
        one Modulation from every Sound: the Modulation returned here is the "canonical"
        Modulation, that is, the Modulation for Sound #0. */
    public Modulation getModulation() { return modulation; }
        
    /** Sets the Rack of the ModulePanel. */
    public void setRack(Rack rack) { this.rack = rack; }

    /** Returns the Rack of the ModulePanel. */
    public Rack getRack() { return rack; }

    /** Returns the background color of the title bar.  Override this
        to customize the title bar to something different than the standards. */
    protected Color getTitleBackground()
        {
        if (modulation instanceof flow.Miscellaneous)
            return Color.BLACK;
        else if (modulation instanceof flow.modules.Macro)
            return new Color(32, 100, 32);
        else if (modulation instanceof flow.UnitSource)
            return new Color(32, 0, 128);
        else if (modulation instanceof flow.ModSource)
            return new Color(150, 0, 0);
        else if (modulation instanceof Unit)
            return new Color(100, 100, 150);
        else  // Module
            return new Color(180, 100, 100);
        }
        
    /** Returns the foreground color of the title bar.  Override this
        to customize the title bar to something different than the standards. */
    protected Color getTitleForeground()
        {
        return Color.WHITE;
        }

    /** Returns the ModPanel's outer title component. */
    public JComponent getTitle()
        {
        return title;
        }

    /** Returns the ModPanel's inner title component (which is colored). */
    public JComponent getTitlePanel()
        {
        return titlePanel;
        }

    /** Returns the ModPanel's title JLabel. */
    public JLabel getTitleLabel()
        {
        return titleLabel;
        }

    // Builds the title bar for the ModulePanel
    JComponent buildTitle()
        {
        JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
                
        titlePanel = new JPanel();
        titlePanel.setBackground(getTitleBackground());
        titlePanel.setLayout(new BorderLayout());
                
        titleLabel = new JLabel(" " + modulation.getNameForModulation() + " ");
        titleLabel.setForeground(getTitleForeground());
        titleLabel.setFont(Style.SMALL_FONT());
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        if (!(modulation instanceof Out))  // Out doesn't have a remove button
            {
            JButton removeButton = new JButton(I_CLOSE);
            removeButton.setPressedIcon(I_CLOSE_PRESSED);
            removeButton.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            removeButton.setBorderPainted(false);
            removeButton.setContentAreaFilled(false);
            removeButton.addActionListener(new ActionListener()
                {
                public void actionPerformed ( ActionEvent e )
                    {
                    close();
                    }
                });
            titlePanel.add(removeButton, BorderLayout.WEST);                 
            }

        outer.add(titlePanel, BorderLayout.NORTH);
        outer.add(Strut.makeVerticalStrut(2), BorderLayout.CENTER);
                
        return outer;
        }

    /** Called to inform the ModulePanel that a title has changed in a given
        InputOutput.  Override this as you see fit; the default does nothing. */
    public void updateTitleChange(InputOutput inout, int number, String newTitle) { }

    /** Builds the JComponent displayed in the main body (all but the title bar) of the
        ModulePanel.  Override this to customize as you see fit, by creating UnitInputs,
        UnitOutputs, ModulationInputs, ModulationOutputs, OptionsChoosers, and ConstraintsChoosers. */
    protected JComponent buildPanel()
        {               
        Box box = new Box(BoxLayout.Y_AXIS);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        box.add(panel);
                          
        if (modulation instanceof Unit)
            {
            Unit unit = (Unit) modulation;
            
            if (unit.showsOutputs())
                {
                for(int i = 0; i < unit.getNumOutputs(); i++)
                    {
                    UnitOutput uo = new UnitOutput(unit, i, this);
                    if (//modulation instanceof Parameterizable &&
                        i == 0 && 
                        unit.getNumOutputs() == 1 &&
                        unit.getOutputName(i).equals(Unit.DEFAULT_UNIT_OUT_NAME))
                        panel.add(uo, BorderLayout.EAST);
                    else box.add(uo);
                    }
                }
                                
            for(int i = 0; i < unit.getNumInputs(); i++)
                {
                box.add(new UnitInput(unit, i, this));
                }
            }

        modOut = new ModulationOutput[modulation.getNumModulationOutputs()];
        for(int i = 0; i < modulation.getNumModulationOutputs(); i++)
            {
            box.add(modOut[i] = new ModulationOutput(modulation, i, this));
            }

        modIn = new ModulationInput[modulation.getNumModulations()];
        for(int i = 0; i < modulation.getNumModulations(); i++)
            {
            box.add(modIn[i] = new ModulationInput(modulation, i, this));
            }
                        
        for(int i = 0; i < modulation.getNumOptions(); i++)
            {
            box.add(buildOptionsChooser(modulation, i));
            }
                        
        if (modulation instanceof Unit)
            {
            Unit unit = (Unit) modulation;
                        
            if (unit.isConstrainable())
                {
                box.add(new ConstraintsChooser(unit, this));
                }
            }

        if (modulation instanceof Presetable)
            {
            //panel.add(new Presets(this), BorderLayout.WEST);
            box.add(new Presets(this));
            }

        return box;
        }
    
    /** Builds an OptionsChooser for the given modulation and options number.
        Override this to customize your own. */
    public OptionsChooser buildOptionsChooser(Modulation modulation, int number)
        {
        return new OptionsChooser(modulation, number);
        }

    ArrayList<ModulationOutput> loadModulationOutputs(ArrayList<ModulationOutput> list, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof ModulationOutput)
                list.add((ModulationOutput)comp);
            else if (comp instanceof Container)
                loadModulationOutputs(list, (Container) comp);
            }
        return list;
        }
                
    /** Returns all ModulationOutputs in the ModulePanel.  These are not necessarily in the same order as they
        appear in the underlying Modulation. */
    public ModulationOutput[] getModulationOutputs()
        {
        return loadModulationOutputs(new ArrayList<ModulationOutput>(), this).toArray(new ModulationOutput[0]);
        }       

    ArrayList<ModulationInput>  loadModulationInputs(ArrayList<ModulationInput> list, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof ModulationInput)
                list.add((ModulationInput)comp);
            else if (comp instanceof Container)
                loadModulationInputs(list, (Container) comp);
            }
        return list;
        }
                
    /** Returns all ModulationInputs in the ModulePanel.  These are not necessarily in the same order as they
        appear in the underlying Modulation. */
    public ModulationInput[] getModulationInputs()
        {
        return loadModulationInputs(new ArrayList<ModulationInput>(), this).toArray(new ModulationInput[0]);
        }       

    ArrayList<UnitOutput> loadUnitOutputs(ArrayList<UnitOutput> list, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof UnitOutput)
                list.add((UnitOutput)comp);
            else if (comp instanceof Container)
                loadUnitOutputs(list, (Container) comp);
            }
        return list;
        }
           
    /** Returns all UnitOutputs in the ModulePanel.  These are not necessarily in the same order as they
        appear in the underlying Unit. */
    public UnitOutput[] getUnitOutputs()
        {
        return loadUnitOutputs(new ArrayList<UnitOutput>(), this).toArray(new UnitOutput[0]);
        }       

    ArrayList<UnitInput>  loadUnitInputs(ArrayList<UnitInput> list, Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof UnitInput)
                list.add((UnitInput)comp);
            else if (comp instanceof Container)
                loadUnitInputs(list, (Container) comp);
            }
        return list;
        }
                
    /** Returns all UnitInputs in the ModulePanel.  These are not necessarily in the same order as they
        appear in the underlying Unit. */
    public UnitInput[] getUnitInputs()
        {
        return loadUnitInputs(new ArrayList<UnitInput>(), this).toArray(new UnitInput[0]);
        }       


    /** Finds the ModulationOutput associated with a specific numbered modulation output in the underlying Modulation. 
        Note that this is O(n). */
    public ModulationOutput findModulationOutputForIndex(int index)
        {
        Output output = modulation.getSound().getOutput();
        output.lock();
        try
            {
            ModulationOutput[] modulationOutputs = getModulationOutputs();
            for(int i = 0; i < modulationOutputs.length; i++)
                {
                if (modulationOutputs[i].number == index)
                    {
                    return modulationOutputs[i];
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        return null;
        }

    /** Finds the ModulationInput associated with a specific numbered modulation input in the underlying Modulation. 
        Note that this is O(n). */
    public ModulationInput findModulationInputForIndex(int index)
        {
        Output output = modulation.getSound().getOutput();
        output.lock();
        try
            {
            ModulationInput[] modulationInputs = getModulationInputs();
            for(int i = 0; i < modulationInputs.length; i++)
                {
                if (modulationInputs[i].number == index)
                    {
                    return modulationInputs[i];
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        return null;
        }

    /** Finds the UnitOutput associated with a specific numbered unit output in the underlying Modulation. 
        Note that this is O(n). */
    public UnitOutput findUnitOutputForIndex(int index)
        {
        Output output = modulation.getSound().getOutput();
        output.lock();
        try
            {
            UnitOutput[] unitOutputs = getUnitOutputs();
            for(int i = 0; i < unitOutputs.length; i++)
                {
                if (unitOutputs[i].number == index)
                    {
                    return unitOutputs[i];
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        return null;
        }

    /** Finds the UnitInput associated with a specific numbered unit input in the underlying Modulation. 
        Note that this is O(n). */
    public UnitInput findUnitInputForIndex(int index)
        {
        Output output = modulation.getSound().getOutput();
        output.lock();
        try
            {
            UnitInput[] unitInputs = getUnitInputs();
            for(int i = 0; i < unitInputs.length; i++)
                {
                if (unitInputs[i].number == index)
                    {
                    return unitInputs[i];
                    }
                }
            }
        finally 
            {
            output.unlock();
            }
        return null;
        }


    void rebuild(Container c)
        {
        for(int i = 0; i < c.getComponentCount(); i++)
            {
            Component comp = c.getComponent(i);
            if (comp instanceof Rebuildable)
                ((Rebuildable)comp).rebuild();
            else if (comp instanceof Container)
                rebuild((Container)comp);
            }
        }
    
    boolean rebuilding = false;  // breaks infinite loops, such as in foo[0].rebuild in Drawbars.java
    public void rebuild()
        {
        if (!rebuilding)
            {
            rebuilding = true;
            rebuild(this);
            rebuilding = false;
            }
        }       


    public String toString() { return "Mod Panel " + modulation.getNameForModulation(); }
    
    /** Closes the ModulePanel.  Disposes of all the associated widgets, disconnects all wires,
        and removes the underlying Modulations from their Sounds. Then removes the ModulePanel from the Rack. */
    public void close()
        {
        Output output = rack.getOutput();                        
        output.lock();

        // This is an opportunity to cancel the audio input
        output.getAudioInput().stop();
                
        try
            {
            // disconnect all
                
            UnitInput[] inputU = getUnitInputs();
            for(int i = 0; i < inputU.length; i++)
                {
                inputU[i].disconnect();
                }
                        
            UnitOutput[] outputU = getUnitOutputs();
            for(int i = 0; i < outputU.length; i++)
                {
                outputU[i].disconnect();
                }

            ModulationInput[] inputM = getModulationInputs();
            for(int i = 0; i < inputM.length; i++)
                {
                inputM[i].disconnect();
                }
                        
            ModulationOutput[] outputM = getModulationOutputs();
            for(int i = 0; i < outputM.length; i++)
                {
                outputM[i].disconnect();
                }
                        
            // Remove from all sounds
            int numSounds = output.getNumSounds();
            int index = rack.getIndex(this);
            for(int i = 0; i < numSounds; i++)
                {
                Sound s = output.getSound(i);
                if (s.getGroup() == Output.PRIMARY_GROUP)
                    {
                    s.removeRegistered(index);
                    }
                }

            // Remove from the Rack
            rack.remove(this);
            rack.checkOrder();
            rack.revalidate();
            rack.repaint();
            rack = null;
            }
        finally 
            {
            output.unlock();
            }
        }

    /** Prints statistical information about the module panel for debugging purposes. */
    public void printStats()
        {
        modulation.printStats();
        }

    /** Unit Inputs can have TWO JLabels as titles: the second is to the right of the first
        (see Out.java).  This returns the rightmost JLabel.  By default this returns null.
        Override as you see fit. */
    public JLabel getAuxUnitInputTitle(int number)
        {
        return null;
        }

    /** Modulation Inputs can have TWO JLabels as titles: the second is to the right of the first
        (see Out.java).  This returns the rightmost JLabel.  By default this returns null.
        Override as you see fit. */
    public JLabel getAuxModulationInputTitle(int number)
        {
        return null;
        }
        
    /** Called prior to attempting to save the patch out to a file to give the
        ModulePanel a chance to revise the module first.  For example, if the ModulePanel
        has a TextArea that needs to be lazily written to a string.  By default this method
        does nothing. */ 
    public void updateForSave()
        {
        }

    /** Called by doLoad(...) when loading a file.  Override this however you see fit (see doLoad()) */
    public void loadFile(File file, Rack rack) { }

    /** A convenience method for loading a file in a ModulePanel. */
    public File doLoad(String title, final String filenameExtension)
        {
        return doLoad(title, new String[] { filenameExtension });
        }
        
    /** A convenience method for loading a file in a ModulePanel. */
    public File doLoad(String title, final String[] filenameExtensions)
        {
        return doLoad(title, filenameExtensions, true);
        }
        
    /** A convenience method for loading a file in a ModulePanel. */
    public File doLoad(String title, final String[] filenameExtensions, boolean callLoadFile)
        {
        Rack rack = getRack();
        FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), title, FileDialog.LOAD);
        fd.setFilenameFilter(new FilenameFilter()
            {
            public boolean accept(File dir, String name)
                {
                for(int i = 0; i < filenameExtensions.length; i++)
                    if (AppMenu.ensureFileEndsWith(name, filenameExtensions[i]).equals(name))
                        return true;
                return false;
                }
            });

        if (AppMenu.dirFile != null)
            fd.setDirectory(AppMenu.dirFile.getParentFile().getPath());

        rack.disableMenuBar();
        fd.setVisible(true);
        rack.enableMenuBar();
        File f = null; // make compiler happy
                
        if (fd.getFile() != null)
            {
            try
                {
                f = new File(fd.getDirectory(), fd.getFile());
                if (callLoadFile)
                    {
                    loadFile(f, rack);
                    }
                AppMenu.dirFile = f;
                }                       
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }
            
        return f;
        }


////// DRAG AND DROP JUNK

                
    public Object getTransferData(DataFlavor flavor) 
        {
        if (flavor.equals(Rack.moduleflavor))
            return this;
        else
            return null;
        }
                
    public DataFlavor[] getTransferDataFlavors() 
        {
        return new DataFlavor[] { Rack.moduleflavor };
        }

    public boolean isDataFlavorSupported(DataFlavor flavor) 
        {
        return (flavor.equals(Rack.moduleflavor));
        }
        
    }

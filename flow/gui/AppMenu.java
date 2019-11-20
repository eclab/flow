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
import java.util.zip.*;
import org.json.*;

/** 
    A collection of functions which build the application menu.
*/

public class AppMenu
    {
    public static final String PATCH_EXTENSION = ".flow";
    
    // Returns a menu for a given module class.
    static JMenuItem menuFor(Class moduleClass, Rack rack)
        {
        JMenuItem menu = new JMenuItem(Modulation.getNameForModulation(moduleClass));
        menu.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.add(moduleClass);
                if (rack.getAddModulesAfter())  // need to move it
                    {
                    rack.move(rack.getAllModulePanels()[0], rack.getAllModulePanels().length - 2);
                    rack.scrollToRight();
                    }
                else
                    {
                    rack.scrollToLeft();
                    }
                rack.checkOrder();
                }
            });
        return menu;
        }
    
    // Returns the MIDI and Audio Preferences menu
    static JMenuItem setupPatchMenu(final Rack rack)
        {
        JMenuItem setup = new JMenuItem("MIDI and Audio Options");
        setup.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.chooseMIDIandAudio();
                }
            });
        return setup;
        }       

    // Returns the MIDI and Audio Preferences menu
    static JMenuItem setupTuningMenu(final Rack rack)
        {
        JMenuItem setup = new JMenuItem("Tuning Parameters");
        setup.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.chooseTuningParameters();
                }
            });
        return setup;
        }       

    public static void setLastFile(File _file)
        {
        file = _file;
        dirFile = _file.getParentFile();
        }
                
    // last file selected by open/save/save as
    static File file = null;

    /** The last directory used to open or save a file. */
    public static File dirFile = null;
        
    /** Returns a string which guarantees that the given filename ends with the given ending. */   
    public static String ensureFileEndsWith(String filename, String ending)
        {
        // do we end with the string?
        if (filename.regionMatches(false,filename.length()-ending.length(),ending,0,ending.length()))
            return filename;
        else return filename + ending;
        }


/*
  static JMenuItem namePatchMenu(Rack rack)
  {
  JMenuItem name = new JMenuItem("Patch Info...");
  name.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

  name.addActionListener(new ActionListener()
  {
  public void actionPerformed(ActionEvent e)
  {
  rack.doPatchDialog("Patch Info");
  }
  });
  return name;
  }
*/

    static JMenuItem quitMenu(Rack rack)
        {
        JMenuItem quit = new JMenuItem("Exit");

        quit.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.doQuit();
                }
            });
        return quit;
        }


 
    // Produces the Save Patch menu
    static JMenuItem savePatchMenu(Rack rack)
        {
        JMenuItem save = new JMenuItem("Save Patch");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        save.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                /*
                  Modulation[] mods = new Modulation[rack.allModulePanels.size()];
                  for(int i = 0; i < mods.length; i++)
                  {
                  mods[i] = rack.allModulePanels.get(i).getModulation();
                  }
                */
                     
                File ff = rack.getPatchFile();
                
                if (ff != null)
                    {
                    JSONObject obj = new JSONObject();
                    
                    Output out = rack.getOutput();
                    Sound.saveGroups(out.getGroups(), out.getNumGroups(), obj);
                    Sound.savePatchInfo(rack.getPatchInfo(), obj);
                    Sound.savePatchDate(rack.getPatchDate(), obj);
                    Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
                    Sound.saveFlowVersion(obj);
                    Sound.savePatchVersion(rack.getPatchVersion(), obj);
                    Sound.saveName(rack.getPatchName(), obj);
                    
                    PrintWriter p = null;

                    rack.getOutput().lock();
                    try
                        {
                        int numModulePanels = rack.allModulePanels.size();
                        for(int i = 0; i < numModulePanels; i++)
                            {
                            rack.allModulePanels.get(i).updateForSave();
                            }
                        rack.getOutput().getSound(0).saveModules(obj);
                        p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(ff)));
                        System.out.println(obj);
                        p.println(obj);
                        p.close();
                        }
                    catch (Exception e2)
                        {
                        e2.printStackTrace();
                        try { if (p != null) p.close(); }
                        catch (Exception e3) { }
                        }
                    finally 
                        {
                        rack.getOutput().unlock();
                        }
                    }
                else
                    {
                    doSaveAs(rack, false);
                    }
                }
            });
        return save;
        }


    // Produces the Save Patch menu
    static JMenuItem saveAsPatchMenu(Rack rack)
        {
        JMenuItem save = new JMenuItem("Save Patch As...");
        save.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));
        save.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doSaveAs(rack, false);
                }
            });
        return save;
        }
    
        
    // Produces the Export Primary Patch menu
    static JMenuItem exportPrimaryPatchMenu(Rack rack)
        {
        JMenuItem export = new JMenuItem("Export Primary Patch...");
        export.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_MASK));
        export.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doSaveAs(rack, true);
                }
            });
        return export;
        }
    
    static void doSaveAs(Rack rack, boolean primaryOnly)
        {
        FileDialog fd = new FileDialog((Frame)(SwingUtilities.getRoot(rack)), 
                (primaryOnly ?
                "Export Primary Patch to File..." :
                "Save Patch to File..."), FileDialog.SAVE);
                
        File ff = file; // rare occurrence
        if (rack.getPatchFile() != null)
            ff = rack.getPatchFile();

        if (ff != null && !primaryOnly)
            {
            fd.setFile(ff.getName());
            // dirFile should always exist if file exists
            fd.setDirectory(ff.getParentFile().getPath());
            }
        else
            {
            String name = rack.getPatchName();
            if (name == null) name = Sound.UNTITLED_PATCH_NAME;
            fd.setFile(name + PATCH_EXTENSION);
            if (dirFile != null)
                fd.setDirectory(dirFile.getParentFile().getPath());
            }

        rack.disableMenuBar();
        fd.setVisible(true);
        rack.enableMenuBar();
                
        File f = null; // make compiler happy

        PrintWriter p = null;
        if (fd.getFile() != null)
            {
            f = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), PATCH_EXTENSION));
                
            JSONObject obj = new JSONObject();

            Output out = rack.getOutput();
            if (!primaryOnly)
                Sound.saveGroups(out.getGroups(), out.getNumGroups(), obj);
            Sound.savePatchInfo(rack.getPatchInfo(), obj);
            Sound.savePatchDate(rack.getPatchDate(), obj);
            Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
            Sound.saveFlowVersion(obj);
            Sound.savePatchVersion(rack.getPatchVersion(), obj);

            if (rack.getPatchName() == null || rack.getPatchName().trim().equals(""))
                Sound.saveName(removeExtension(f.getName()), obj);
            else
                Sound.saveName(rack.getPatchName(), obj);

            rack.getOutput().lock();
            try
                {
                int numModulePanels = rack.allModulePanels.size();
                for(int i = 0; i < numModulePanels; i++)
                    {
                    rack.allModulePanels.get(i).updateForSave();
                    }
                rack.getOutput().getSound(0).saveModules(obj);
                p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(f)));
                p.println(obj);
                p.flush();
                p.close();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                try { if (p != null) p.close(); }
                catch (Exception e2) { }
                }
            finally 
                {
                rack.getOutput().unlock();
                }
            
            if (!primaryOnly)
                {
                file = f;
                dirFile = f;
                rack.setPatchFile(f);
                rack.setPatchName(rack.getPatchName());
                }
            }
        }

// From https://stackoverflow.com/questions/924394/how-to-get-the-filename-without-the-extension-in-java
    public static String removeExtension(String fileName)
        {
        if (fileName.indexOf(".") > 0) 
            {
            return fileName.substring(0, fileName.lastIndexOf("."));
            }
        else 
            {
            return fileName;
            }
        }


    // Produces the Load Patch menu
    static JMenuItem loadPatchMenu(Rack rack)
        {
        JMenuItem load = new JMenuItem("Load Patch...");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        load.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (rack.subpatchBox.getComponentCount() > 0)
                    {
                    if (!showSimpleConfirm("Load Patch?", "This patch has subpatches.\nAre you sure you want to load a new patch?", rack))
                        return;
                    }

                FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Patch File...", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                        }
                    });

                if (file != null)
                    {
                    System.err.println(file);
                    fd.setFile(file.getName());
                    fd.setDirectory(file.getParentFile().getPath());
                    }
                
                rack.disableMenuBar();
                fd.setVisible(true);
                rack.enableMenuBar();
                
                if (fd.getFile() != null)
                    {
                    doLoad(rack, fd, true);
                    rack.setPatchFile(new File(fd.getDirectory(), fd.getFile()));
                    rack.setPatchName(rack.getPatchName());
                    }
                }
            });
        return load;
        }
        
    // Produces the Load Patch menu
    static JMenuItem loadPrimaryPatchMenu(Rack rack)
        {
        JMenuItem load = new JMenuItem("Load Primary Patch...");
        load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_MASK));
        load.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // do demotion if necessary
                
                int result = rack.showMultiOption(rack, 
                    new String[] { }, 
                    new JComponent[] { }, 
                    "Demote Primary Patch", 
                    "Demote or replace the existing primary patch on load?",
                    new String[] { "Demote", "Replace", "Cancel" });

                if (result == 2 || result == -1) return;                        // when the ESC key is pressed we get a -1

                FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Primary Patch File...", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                        }
                    });

                if (file != null)
                    {
                    fd.setFile(file.getName());
                    fd.setDirectory(file.getParentFile().getPath());
                    }
                
                rack.disableMenuBar();
                fd.setVisible(true);
                rack.enableMenuBar();                
                if (fd.getFile() != null)
                    {
                    rack.getOutput().lock();
                    try
                        {
                        if (result == 0)  // demote
                            {
                            if (!rack.getOutput().copyPrimaryGroup(true))
                                {
                                showSimpleError("Cannot demote", "There are too many subpatches.\nRemove a subpatch first.", rack);     
                                return;
                                }
                            else
                                {
                                rack.getOutput().getGroup(rack.getOutput().getNumGroups() - 1).setPatchName(rack.getPatchName());
                                }
                            }
                        doLoad(rack, fd, false);
                        }
                    finally 
                        {
                        rack.getOutput().unlock();
                        }
                    }
                }
            });
        return load;
        }


    static void doLoad(Rack rack, FileDialog fd, boolean clearSubpatches)
        {
        String[] patchName = new String[1];

        File f = new File(fd.getDirectory(), fd.getFile());
        try
            {
            doLoad(rack, new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(f)))), clearSubpatches);
            }
        catch(Exception ex) { ex.printStackTrace(); showSimpleError("Patch Reading Error", "The patch could not be loaded", rack); }
        file = f;
        dirFile = f;
        } 




    public static void doLoad(Rack rack, JSONObject obj, boolean clearSubpatches) throws Exception
        {
        String[] patchName = new String[1];
        rack.getOutput().lock();
        int flowVersion = 0;
        try 
            { 
            flowVersion = Sound.loadFlowVersion(obj);
            }
        catch (Exception ex) { ex.printStackTrace(); }
        // version
        try
            {
            Modulation[][] mods = new Modulation[rack.getOutput().getNumSounds()][];
            for(int i = 0; i < mods.length; i++)
                {
                mods[i] = Sound.loadModules(obj, flowVersion);
                }
                                                                                                                                                                                        
            // Remove old subpatches
            if (clearSubpatches)
                {
                rack.getOutput().setNumGroups(1);
                }

            // Create and update Modulations and create ModulePanels
            load(mods, rack, obj == null ? patchName[0] : Sound.loadName(obj));

            // reload
            Output out = rack.getOutput();
            if (obj != null)
                {
                rack.setPatchVersion(Sound.loadPatchVersion(obj));
                rack.setPatchInfo(Sound.loadPatchInfo(obj));
                rack.setPatchAuthor(Sound.loadPatchAuthor(obj));
                rack.setPatchDate(Sound.loadPatchDate(obj));
                                                        
                if (clearSubpatches)
                    {
                    int numNewGroups = Sound.loadGroups(out.getGroups(), obj);
                    if (numNewGroups > 0)
                        {
                        out.setNumGroupsUnsafe(numNewGroups + 1);
                        }
                    }
                }
            rack.getOutput().getGroup(Output.PRIMARY_GROUP).setBothNotes(0, 127);           // reset
            out.assignGroupsToSounds();
            rack.rebuildSubpatches();
            rack.checkOrder();
            }
        finally 
            {
            rack.getOutput().unlock();
            }
        rack.scrollToRight();
        ((Out.OutModulePanel)(rack.findOut())).updatePatchInfo();
        } 


    // Produces the Load Patch as Macro menu
    static JMenuItem loadMacroMenu(Rack rack)
        {
        JMenuItem macro = new JMenuItem("Load Patch as Macro...");
        macro.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Patch File as Macro...", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter()
                    {
                    public boolean accept(File dir, String name)
                        {
                        return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                        }
                    });

                if (file != null)
                    {
                    fd.setFile(file.getName());
                    fd.setDirectory(file.getParentFile().getPath());
                    }
                else
                    {
                    }
                
                rack.disableMenuBar();
                fd.setVisible(true);
                rack.enableMenuBar();
                File f = null; // make compiler happy
                if (fd.getFile() != null)
                    {
                    f = new File(fd.getDirectory(), fd.getFile());
                    rack.getOutput().lock();
                    try
                        {
                        rack.addMacro(f);
                        if (rack.getAddModulesAfter())  // need to move it
                            {
                            rack.move(rack.getAllModulePanels()[0], rack.getAllModulePanels().length - 2);
                            rack.scrollToRight();
                            }
                        else
                            {
                            rack.scrollToLeft();
                            }
                        rack.checkOrder();
                        }
                    finally 
                        {
                        rack.getOutput().unlock();
                        }
                    dirFile = f;
                    }
                }
            });
        return macro;
        }
        

    static boolean inSimpleError;

    /** Display a simple error message. */
    public static void showSimpleError(String title, String message, Rack rack)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        rack.disableMenuBar();
        JOptionPane.showMessageDialog(rack, message, title, JOptionPane.ERROR_MESSAGE);
        rack.enableMenuBar();
        inSimpleError = false;
        }

    /** Display a simple error message. */
    public static void showSimpleMessage(String title, String message, Rack rack)
        {
        // A Bug in OS X (perhaps others?) Java causes multiple copies of the same Menu event to be issued
        // if we're popping up a dialog box in response, and if the Menu event is caused by command-key which includes
        // a modifier such as shift.  To get around it, we're just blocking multiple recursive message dialogs here.
        
        if (inSimpleError) return;
        inSimpleError = true;
        rack.disableMenuBar();
        JOptionPane.showMessageDialog(rack, message, title, JOptionPane.INFORMATION_MESSAGE);
        rack.enableMenuBar();
        inSimpleError = false;
        }


    // Display a simple (OK / Cancel) confirmation message.  Return the result (ok = true, cancel = false).
    public static boolean showSimpleConfirm(String title, String message, Rack rack)
        {
        rack.disableMenuBar();
        boolean result = (JOptionPane.showConfirmDialog(rack, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
        rack.enableMenuBar();
        return result;
        }
        

    // Display a simple (OK / Cancel) text-input message message.  Return the result, or null if cancelled.
    public static String showSimpleInput(String title, String message, String initialText, Rack rack)
        {
        rack.disableMenuBar();
        // This is poorly documented by the Java team
        String result = (String)(JOptionPane.showInputDialog(rack, message, title, JOptionPane. QUESTION_MESSAGE, null,  null, initialText));
        rack.enableMenuBar();
        return result;
        }



    // Produces the New Patch menu
    static JMenuItem newPatchMenu(Rack rack)
        {
        JMenuItem newpatch = new JMenuItem("New Patch");
        newpatch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        newpatch.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                if (showSimpleConfirm("New Patch", "Clear the existing patch" + 
                        (rack.subpatchBox.getComponentCount() > 0 ? " and subpatches?" : "?"), rack))
                    {
                    doNew(rack, true);
                    }
                }
            });
        return newpatch;
        }
        
    // Produces the Push Primary Patch menu
    static JMenuItem newPrimaryPatchMenu(Rack rack)
        {
        JMenuItem newpatch = new JMenuItem("New Primary Patch");
        newpatch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.ALT_MASK));
        newpatch.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                int result = rack.showMultiOption(rack, 
                    new String[] { }, 
                    new JComponent[] { }, 
                    "Demote Primary Patch", 
                    "Demote or clear the existing primary patch?",
                    new String[] { "Demote", "Clear", "Cancel" });

                if (result == 2 || result == -1) return;                    // when the ESC key is pressed we get a -1

                rack.getOutput().lock();
                try
                    {
                    if (result == 0)
                        {
                        if (!rack.getOutput().copyPrimaryGroup(true))
                            {
                            showSimpleError("Cannot demote", "There are too many subpatches.\nRemove a subpatch first.", rack);     
                            return;
                            }
                        else
                            {
                            rack.getOutput().getGroup(rack.getOutput().getNumGroups() - 1).setPatchName(rack.getPatchName());
                            doNew(rack, false);
                            rack.rebuildSubpatches();
                            }
                        }
                    else
                        {
                        doNew(rack, false);
                        rack.rebuildSubpatches();
                        }
                    }
                finally 
                    {
                    rack.getOutput().unlock();
                    }
                }
            });
        return newpatch;
        }
        
    static void doNew(Rack rack, boolean clearSubpatches)
        {
        rack.getOutput().lock();
        try
            {
            rack.closeAll();
            rack.checkOrder();
            rack.add(Out.class);
            rack.setPatchFile(null);			// so we don't try to save again.  Must be done BEFORE setPatchName
            rack.setPatchName(null);
            rack.setPatchVersion(null);
            rack.setPatchInfo(null);
            rack.setPatchAuthor(null);
            rack.setPatchDate(null);
                                                                        
            // reset Out
            rack.findOut().updatePatchInfo();
            file = null;
            // don't reset dirFile

            if (clearSubpatches)
                {
                // Remove old subpatches
                rack.getOutput().setNumGroups(1);
                rack.rebuildSubpatches();
                }
                
            rack.getOutput().assignGroupsToSounds();
            }
        finally 
            {
            rack.getOutput().unlock();
            }
        }
        

    // Produces the New Patch menu
    static JMenuItem loadSubpatchMenu(Rack rack)
        {
        JMenuItem loadsubpatch = new JMenuItem("Load Subpatch...");
        loadsubpatch.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_MASK));
        loadsubpatch.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // this looks like an error but it's not.  
                // >= Output.MAX_GROUPS - 1     means that we've reached #15, which is the highest group
                // (Output.MAX_GROUPS - 1)              coincidentally is the number of additional groups we can make beyond the PRIMARY group
                if (rack.getOutput().getNumGroups() >= Output.MAX_GROUPS - 1)
                    {
                    showSimpleError("Too Many Subpatches", "You can only have up to " + (Output.MAX_GROUPS - 1) + " subpatches", rack);
                    }
                else
                    {
                    FileDialog fd = new FileDialog((JFrame)(SwingUtilities.getRoot(rack)), "Load Subpatch File...", FileDialog.LOAD);
                    fd.setFilenameFilter(new FilenameFilter()
                        {
                        public boolean accept(File dir, String name)
                            {
                            return ensureFileEndsWith(name, PATCH_EXTENSION).equals(name);
                            }
                        });

                    if (file != null)
                        {
                        fd.setFile(file.getName());
                        fd.setDirectory(file.getParentFile().getPath());
                        }
                    else
                        {
                        }
                                
                    rack.disableMenuBar();
                    fd.setVisible(true);
                    rack.enableMenuBar();
                    File f = null; // make compiler happy
                                
                    if (fd.getFile() != null)
                        {
                        f = new File(fd.getDirectory(), fd.getFile());
                        try 
                            {
                            JSONObject obj = new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(f)))); 

                            // check for subpatches
                            JSONArray array = null;
                            try { array = obj.getJSONArray("sub"); }
                            catch (Exception ex2) { }

                            if (array != null && array.length() > 0)  //  uh oh
                                {
                                showSimpleMessage("Patch with Subpatches",
                                    "This file contains a patch which itself has subpatches.\nThey will be discarded.\nOnly the primary patch will be loaded as a subpatch.", rack);
                                }

                            rack.addSubpatch(new SubpatchPanel(rack, obj));
                            rack.revalidate();
                            rack.repaint();
                            }
                        catch (Exception ex)
                            {
                            showSimpleError("Error", "An error occurred on loading this file.", rack);
                            ex.printStackTrace();
                            }
                        }
                    }
                }
            });
        return loadsubpatch;
        }


    static JMenuItem showDisplay(Rack rack)
        {
        final JCheckBoxMenuItem display = new JCheckBoxMenuItem("Show Displays");
        display.setSelected(Prefs.getShowsDisplays());
        rack.setShowsDisplays(display.isSelected());
        display.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.setShowsDisplays(display.isSelected());
                Prefs.setShowsDisplays(display.isSelected());
                }
            });
        return display;
        }

    static JMenuItem waterfallDisplay(Rack rack)
        {
        final JCheckBoxMenuItem waterfall = new JCheckBoxMenuItem("Waterfall Display");
        waterfall.setSelected(Prefs.getWaterfallDisplay());
        rack.display1.setWaterfall(waterfall.isSelected());
        rack.display2.setWaterfall(waterfall.isSelected());
        waterfall.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.display1.setWaterfall(waterfall.isSelected());
                rack.display2.setWaterfall(waterfall.isSelected());
                Prefs.setWaterfallDisplay(waterfall.isSelected());
                }
            });
        return waterfall;
        }

    static JMenuItem logAxisDisplay(Rack rack)
        {
        final JCheckBoxMenuItem log = new JCheckBoxMenuItem("Log-Axis Display");
        log.setSelected(Prefs.getLogAxisDisplay());
        rack.display1.setLogFrequency(log.isSelected());
        rack.display2.setLogFrequency(log.isSelected());
        log.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.display1.setLogFrequency(log.isSelected());
                rack.display2.setLogFrequency(log.isSelected());
                Prefs.setLogAxisDisplay(log.isSelected());
                }
            });
        return log;
        }

    public static final int DEFAULT_MAX_DISPLAYED_HARMONIC = 6;             // 150
        
    static JMenuItem maxDisplayedHarmonic(Rack rack)
        {
        final double maxHarm[] = new double[] { 31, 49, 63, 79, 99, 127, 149, 199, 255, 299, 399, 499 };
        final JMenu max = new JMenu("Max Displayed Harmonic");
        final JRadioButtonMenuItem[] buttons = new JRadioButtonMenuItem[] 
            {
            new JRadioButtonMenuItem("32"),
            new JRadioButtonMenuItem("50"),
            new JRadioButtonMenuItem("64"),
            new JRadioButtonMenuItem("80"),
            new JRadioButtonMenuItem("100"),
            new JRadioButtonMenuItem("128"),
            new JRadioButtonMenuItem("150"),
            new JRadioButtonMenuItem("200"),
            new JRadioButtonMenuItem("256"),
            new JRadioButtonMenuItem("300"),
            new JRadioButtonMenuItem("400"),
            new JRadioButtonMenuItem("500")
            };
        
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < buttons.length; i++)
            {
            max.add(buttons[i]);
            group.add(buttons[i]);
            }
        
        int sel = Prefs.getMaxDisplayedHarmonic();
        buttons[sel].setSelected(true);
        rack.display1.setMaxFrequency(maxHarm[sel]);
        rack.display2.setMaxFrequency(maxHarm[sel]);
                
        for(int q = 0; q < buttons.length; q++)
            {
            buttons[q].addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    // yuck
                    int selected = 0;
                    for(int i = 0; i < buttons.length; i++)
                        if (buttons[i].isSelected()) 
                            { 
                            selected = i; 
                            break; 
                            }
                        
                    rack.display1.setMaxFrequency(maxHarm[selected]);
                    rack.display2.setMaxFrequency(maxHarm[selected]);
                    Prefs.setMaxDisplayedHarmonic(selected);
                    }
                });
            }
        return max;
        }


    public static final int DEFAULT_MIN_DISPLAYED_HARMONIC = 4;             // 1/16
        
    static JMenuItem minDisplayedHarmonic(Rack rack)
        {
        final double minHarm[] = new double[] { 1.0, 0.5, 0.25, 0.125, 0.0625, 0.03125 };
        final JMenu min = new JMenu("Min Displayed Harmonic");
        final JRadioButtonMenuItem[] buttons = new JRadioButtonMenuItem[] 
            {
            new JRadioButtonMenuItem("Fundamental"),
            new JRadioButtonMenuItem("1/2"),
            new JRadioButtonMenuItem("1/4"),
            new JRadioButtonMenuItem("1/8"),
            new JRadioButtonMenuItem("1/16"),
            new JRadioButtonMenuItem("1/32") 
            };
        
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < buttons.length; i++)
            {
            min.add(buttons[i]);
            group.add(buttons[i]);
            }
        
        int sel = Prefs.getMinDisplayedHarmonic();
        buttons[sel].setSelected(true);
        rack.display1.setMinFrequency(minHarm[sel]);
        rack.display2.setMinFrequency(minHarm[sel]);
        for(int q = 0; q < buttons.length; q++)
            {
            buttons[q].addActionListener(new ActionListener()
                {
                public void actionPerformed(ActionEvent e)
                    {
                    // yuck
                    int selected = 0;
                    for(int i = 0; i < buttons.length; i++)
                        if (buttons[i].isSelected()) 
                            { 
                            selected = i; 
                            break; 
                            }
                        
                    rack.display1.setMinFrequency(minHarm[selected]);
                    rack.display2.setMinFrequency(minHarm[selected]);
                    Prefs.setMinDisplayedHarmonic(selected);
                    }
                });
            }
        return min;
        }


    static JMenuItem playFirstMenu(Rack rack)
        {
        final JCheckBoxMenuItem playFirst = new JCheckBoxMenuItem("Monophonic");
        playFirst.setSelected(Prefs.getLastOneVoice());
        rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
        playFirst.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
                Prefs.setLastOneVoice(playFirst.isSelected());
                rack.rebuildSubpatches();       // So they say "[M]"
                }
            });
        rack.rebuildSubpatches();       // So they say "[M]"
        return playFirst;
        }


    // Produces the Add New Modules At End menu
    static JMenuItem addModulesAfterMenu(Rack rack)
        {
        final JCheckBoxMenuItem addModulesAfter = new JCheckBoxMenuItem("Add New Modules At End");
        addModulesAfter.setSelected(Prefs.getLastAddModulesAfter());
        rack.setAddModulesAfter(addModulesAfter.isSelected());
        addModulesAfter.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.setAddModulesAfter(addModulesAfter.isSelected());
                Prefs.setLastAddModulesAfter(addModulesAfter.isSelected());
                }
            });
        return addModulesAfter;
        }

/*
// Produces the Add New Modules At End menu
static JMenuItem swapPrimaryMenu(Rack rack)
{
final JCheckBoxMenuItem swapPrimary = new JCheckBoxMenuItem("Include MIDI/Voice in Swap");
swapPrimary.setSelected(Prefs.getSwapPrimaryWithMIDIVoice());
rack.setSwapPrimaryWithMIDIVoice(swapPrimary.isSelected());
swapPrimary.addActionListener(new ActionListener()
{
public void actionPerformed(ActionEvent e)
{
rack.setSwapPrimaryWithMIDIVoice(swapPrimary.isSelected());
Prefs.setSwapPrimaryWithMIDIVoice(swapPrimary.isSelected());
}
});
return swapPrimary;
}
*/

    // Produces the Velocity Sensitive menu
    static JMenuItem velMenu(Rack rack)
        {
        final JCheckBoxMenuItem vel = new JCheckBoxMenuItem("Velocity Sensitive");
        vel.setSelected(Prefs.getVelocitySensitive());
        rack.getOutput().setVelocitySensitive(vel.isSelected());
        vel.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().setVelocitySensitive(vel.isSelected());
                Prefs.setVelocitySensitive(vel.isSelected());
                }
            });
        return vel;
        }

    // Produces the Responds to Bend menu
    static JMenuItem bendMenu(Rack rack)
        {
        final JCheckBoxMenuItem bend = new JCheckBoxMenuItem("Responds to Bend");
        bend.setSelected(Prefs.getRespondsToBend());
        rack.getOutput().getInput().setRespondsToBend(bend.isSelected());
        bend.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().getInput().setRespondsToBend(bend.isSelected());
                Prefs.setRespondsToBend(bend.isSelected());
                }
            });
        return bend;
        }

    // Produces the Responds to Bend menu
    static JMenuItem bendOctaveMenu(Rack rack)
        {
        JMenu bendOctave = new JMenu("Bend Octaves");

        final JRadioButtonMenuItem[] bendButtons = new JRadioButtonMenuItem[] 
            {
            new JRadioButtonMenuItem("1"),
            new JRadioButtonMenuItem("2"),
            new JRadioButtonMenuItem("3"),
            new JRadioButtonMenuItem("4"),
            new JRadioButtonMenuItem("5"),
            new JRadioButtonMenuItem("6"),
            new JRadioButtonMenuItem("7"),
            new JRadioButtonMenuItem("8")
            };
        
        ButtonGroup group = new ButtonGroup();
        for(int i = 0; i < bendButtons.length; i++)
            {
            bendOctave.add(bendButtons[i]);
            group.add(bendButtons[i]);
            }
        
        int sel = Prefs.getLastBendOctave();
        if (sel < 1 || sel > 8)
            sel = Input.DEFAULT_BEND_OCTAVE;
        bendButtons[sel - 1].setSelected(true);         // - 1 because octave starts at 1
        rack.getOutput().getInput().setBendOctave(sel);
        bendOctave.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                // yuck
                int selected = 0;
                for(int i = 0; i < bendButtons.length; i++)
                    if (bendButtons[i].isSelected()) 
                        { 
                        selected = i; 
                        break; 
                        }                
                rack.getOutput().getInput().setBendOctave(selected + 1);                // + 1 because octave starts at 1
                Prefs.setLastBendOctave(selected + 1);                                          // + 1 because octave starts at 1
                }
            });
        return bendOctave;
        }

    // Produces the Sync to MIDI Clock menu
    static JMenuItem syncMenu(Rack rack)
        {
        final JCheckBoxMenuItem sync = new JCheckBoxMenuItem("Synced to MIDI Clock");
        sync.setSelected(Prefs.getLastMIDISync());
        rack.getOutput().getInput().getMidiClock().setSyncing(sync.isSelected());
        sync.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().getInput().getMidiClock().setSyncing(sync.isSelected());
                Prefs.setLastMIDISync(sync.isSelected());
                }
            });
        return sync;
        }

    public static JMenuBar provideMenuBar(Rack rack)
        {
        JMenuBar menubar = new JMenuBar();
        menubar.add(provideFileMenu(rack));
        menubar.add(providePlayMenu(rack));
        menubar.add(provideModuleMenu(rack));
        menubar.add(provideOptionsMenu(rack));
        if (Style.isWindows() || Style.isUnix())
            {
            menubar.add(AppMenu.provideWindowsAboutMenu(rack));
            }
        return menubar;
        }

    static JMenuItem resetMenu(Rack rack)
        {
        JMenuItem reset = new JMenuItem("Reset");
        reset.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.reset();
                }
            });
        return reset;
        }

    // Produces the Play menu
    public static JMenu providePlayMenu(Rack rack)
        {
        JMenu menu = new JMenu("Play");

        menu.add(resetMenu(rack));
        menu.addSeparator();
        menu.add(playFirstMenu(rack));
        menu.add(bendOctaveMenu(rack));
        menu.add(bendMenu(rack));
        menu.add(velMenu(rack));
        menu.add(syncMenu(rack));
        menu.addSeparator();
//        menu.add(namePatchMenu(rack));
        return menu;
        }

    // Produces the Play menu
    public static JMenu provideOptionsMenu(Rack rack)
        {
        JMenu menu = new JMenu("Options");

        menu.add(showDisplay(rack));
        menu.add(logAxisDisplay(rack));
        menu.add(waterfallDisplay(rack));
        menu.add(maxDisplayedHarmonic(rack));
        menu.add(minDisplayedHarmonic(rack));
        menu.addSeparator();
        menu.add(addModulesAfterMenu(rack));
        //menu.add(swapPrimaryMenu(rack));
//              menu.addSeparator();
        menu.add(setupPatchMenu(rack));
        menu.add(setupTuningMenu(rack));
        return menu;
        }

    // Produces the File menu
    public static JMenu provideFileMenu(Rack rack)
        {
        JMenu menu = new JMenu("File");

        menu.add(newPatchMenu(rack));
        menu.add(loadPatchMenu(rack));
        menu.add(savePatchMenu(rack));
        menu.add(saveAsPatchMenu(rack));
        menu.addSeparator();
        menu.add(newPrimaryPatchMenu(rack));
        menu.add(loadPrimaryPatchMenu(rack));
        menu.add(loadSubpatchMenu(rack));
        menu.add(exportPrimaryPatchMenu(rack));

        if (!Style.isMac())
            {
            menu.addSeparator();
            menu.add(quitMenu(rack));
            }

        return menu;
        }

    // Produces the Module menu
    public static JMenu provideModuleMenu(Rack rack)
        {
        JMenu menu = new JMenu("Modules");

        menu.add(loadMacroMenu(rack));
        
        ArrayList<JMenuItem> modSources = new ArrayList<>();
        ArrayList<JMenuItem> modShapers = new ArrayList<>();
        ArrayList<JMenuItem> unitSources = new ArrayList<>();
        ArrayList<JMenuItem> unitShapers = new ArrayList<>();
        ArrayList<JMenuItem> miscellaneous = new ArrayList<>();

        Class[] modules = Modules.getModules();
        for(int i = 0; i < modules.length; i++)
            {
            Class c = modules[i];
            JMenuItem m = menuFor(c, rack);
                        
            if (c == flow.modules.Out.class)
                { } // do nothing
            else if (flow.Miscellaneous.class.isAssignableFrom(c))
                miscellaneous.add(m);
            else if (flow.UnitSource.class.isAssignableFrom(c))
                unitSources.add(m);
            else if (flow.ModSource.class.isAssignableFrom(c))
                modSources.add(m);
            else if (flow.Unit.class.isAssignableFrom(c))
                unitShapers.add(m);
            else  // Module
                modShapers.add(m);
            }
                
                
        JMenu sub = new JMenu("Modulation Sources");
        for(JMenuItem m : modSources)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Modulation Shapers");
        for(JMenuItem m : modShapers)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Partials Sources");
        for(JMenuItem m : unitSources)
            sub.add(m);
        menu.add(sub);

        sub = new JMenu("Partials Shapers");
        for(JMenuItem m : unitShapers)
            sub.add(m);
        menu.add(sub);
                        
        sub = new JMenu("Other");
        for(JMenuItem m : miscellaneous)
            sub.add(m);
        menu.add(sub);
        menu.add(sub);
                        
        return menu;
        }


    // Removes all modules from the rack, and
    // loads new modules from the given deserialized array.  The Modulations
    // are organized by Sound, then by Modulation.
    static void load(Modulation[][] mods, Rack rack, String patchName)
        {
        Output output = rack.getOutput();
        rack.getOutput().lock();
        try
            {
            // remove all existing panels
            rack.closeAll();
            rack.checkOrder();
            
            // Add the modulations as a group
            for(int i = 0; i < mods.length; i++)
                {
                Sound s = output.getSound(i);
                if (s.getGroup() == Output.PRIMARY_GROUP)
                    {
                    for(int j = 0; j < mods[i].length; j++)
                        {
                        s.register(mods[i][j]);
                        mods[i][j].setSound(s);
                        if (mods[i][j] instanceof Out)
                            {
                            s.setEmits((Out)(mods[i][j]));
                            }
                        mods[i][j].reset();
                        }
                    }
                }
                
            // Load ModulePanels for the new Modulations
            for(int j = 0; j < mods[0].length; j++)
                {
                ModulePanel modpanel = mods[0][j].getPanel();
                rack.addModulePanel(modpanel);
                }

            rack.setPatchName(patchName);

            // Connect and update ModulePanels
            rack.rebuild();
            rack.checkOrder();
            rack.setPatchName(rack.getPatchName());
            }
        finally 
            {
            rack.getOutput().unlock();
            }
        }

    public static JMenu provideWindowsAboutMenu(Rack rack)
        {
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutMenuItem = new JMenuItem("About Flow");
        aboutMenuItem.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                doAbout();
                }
            });
        helpMenu.add(aboutMenuItem);
        return helpMenu;
        }
                
    static void doAbout()
        {
        ImageIcon icon = new ImageIcon(AppMenu.class.getResource("About.png"));
        JFrame frame = new JFrame("About Flow");
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().setBackground(Color.BLACK);
        JLabel label = new JLabel(icon);
//        label.setBorder(BorderFactory.createMatteBorder(Color.GRAY, 4));
        frame.getContentPane().add(label, BorderLayout.CENTER);

        JPanel pane = new JPanel()
            {
            public Insets getInsets() { return new Insets(10, 10, 10, 10); }
            };
        pane.setBackground(Color.BLACK);
        pane.setLayout(new BorderLayout());

        JLabel edisyn = new JLabel("Flow");
        edisyn.setForeground(Color.WHITE);
        edisyn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        pane.add(edisyn, BorderLayout.WEST);

        Box box = new Box(BoxLayout.Y_AXIS);
        JLabel about = new JLabel("Version " + Flow.VERSION + " By Sean Luke");
        about.setForeground(Color.WHITE);
        about.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JLabel about2 = new JLabel("Copyright 2018 George Mason University");
        about2.setForeground(Color.WHITE);
        about2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JLabel about3 = new JLabel("http://github.com/eclab/flow/");
        about3.setForeground(Color.WHITE);
        about3.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        box.add(about);
        box.add(about2);
        box.add(about3);
        
        pane.add(box, BorderLayout.EAST);

        frame.add(pane, BorderLayout.SOUTH);
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        }
    }

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
        JMenuItem setup = new JMenuItem("MIDI and Audio Preferences");
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


    static JMenuItem namePatchMenu(Rack rack)
        {
        JMenuItem name = new JMenuItem("Patch Info...");

        name.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                String[] result = Rack.showPatchDialog(rack, rack.getPatchName(), rack.getPatchAuthor(), rack.getPatchDate(), rack.getPatchVersion(), rack.getPatchInfo());
                rack.setPatchName(result[0]);
                rack.setPatchAuthor(result[1]);
                rack.setPatchDate(result[2]);
                rack.setPatchVersion(result[3]);
                rack.setPatchInfo(result[4]);
                }
            });
        return name;
        }


    static JMenuItem quitPatchMenu(Rack rack)
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
                Modulation[] mods = new Modulation[rack.allModulePanels.size()];
                for(int i = 0; i < mods.length; i++)
                    {
                    mods[i] = rack.allModulePanels.get(i).getModulation();
                    }
                     
                if (file != null)
                    {
                    JSONObject obj = new JSONObject();
                    Sound.saveName(rack.getPatchName(), obj);
                    Sound.saveFlowVersion(obj);
                    Sound.savePatchVersion(rack.getPatchVersion(), obj);
                    Sound.savePatchInfo(rack.getPatchInfo(), obj);
                    Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
                    Sound.savePatchDate(rack.getPatchDate(), obj);

                    PrintWriter p = null;
                    FileOutputStream os = null;
                    try
                        {
                        os = new FileOutputStream(file);
                        rack.output.lock();
                        try
                            {
                            rack.output.getSound(0).saveModules(obj);
                            p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
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
                            rack.output.unlock();
                            }
                        }
                    catch (FileNotFoundException ex)
                        {
                        ex.printStackTrace();
                        }
                    }
                else
                    {
                    doSaveAs(rack);
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
                doSaveAs(rack);
                }
            });
        return save;
        }
    
        
    static void doSaveAs(Rack rack)
        {
        Modulation[] mods = new Modulation[rack.allModulePanels.size()];
        for(int i = 0; i < mods.length; i++)
            {
            mods[i] = rack.allModulePanels.get(i).getModulation();
            }
                     
        FileDialog fd = new FileDialog((Frame)(SwingUtilities.getRoot(rack)), "Save Patch to Sysex File...", FileDialog.SAVE);
                
        if (file != null)
            {
            fd.setFile(file.getName());
            // dirFile should always exist if file exists
            fd.setDirectory(file.getParentFile().getPath());
            }
        else
            {
            String name = rack.getPatchName();
            if (name == null) name = "Untitled";
            fd.setFile(name + PATCH_EXTENSION);
            if (dirFile != null)
                fd.setDirectory(dirFile.getParentFile().getPath());
            }

        rack.disableMenuBar();
        fd.setVisible(true);
        rack.enableMenuBar();
                
        File f = null; // make compiler happy
        FileOutputStream os = null;
        PrintWriter p = null;
        if (fd.getFile() != null)
            try
                {
                f = new File(fd.getDirectory(), ensureFileEndsWith(fd.getFile(), PATCH_EXTENSION));
                
                JSONObject obj = new JSONObject();
                if (rack.getPatchName() == null)
                    Sound.saveName(removeExtension(f.getName()), obj);
                else
                    Sound.saveName(rack.getPatchName(), obj);
                Sound.savePatchVersion(rack.getPatchVersion(), obj);
                Sound.savePatchInfo(rack.getPatchInfo(), obj);
                Sound.savePatchAuthor(rack.getPatchAuthor(), obj);
                Sound.saveFlowVersion(obj);
                os = new FileOutputStream(f);
                rack.output.lock();
                try
                    {
                    rack.output.getSound(0).saveModules(obj);
                    p = new PrintWriter(new GZIPOutputStream(new FileOutputStream(f)));
                    System.out.println(obj);
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
                    rack.output.unlock();
                    }
                file = f;
                dirFile = f;
                rack.setPatchName(removeExtension(f.getName()));
                }
            catch (FileNotFoundException ex)
                {
                ex.printStackTrace();
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
                
                String[] patchName = new String[1];
                
                if (fd.getFile() != null)
                    //try
                    {
                    f = new File(fd.getDirectory(), fd.getFile());
                    rack.output.lock();
                    try
                        {
                        JSONObject obj = null;
                        int flowVersion = 0;
                        try 
                            { 
                            obj = new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(f)))); 
                            flowVersion = Sound.loadFlowVersion(obj);
                            }
                        catch (Exception ex) { ex.printStackTrace(); }
                        // version
                        try
                            {
                            Modulation[][] mods = new Modulation[rack.getOutput().getNumSounds()][];
                            for(int i = 0; i < mods.length; i++)
                                {
                                if (obj == null)
                                    {
                                    mods[i] = Macro.deserialize(f, patchName);              // old version
                                    }
                                else
                                    mods[i] = Sound.loadModules(obj, flowVersion);
                                }
                                                                                                
                            // Create and update Modulations and create ModulePanels
                            load(mods, rack, obj == null ? patchName[0] : Sound.loadName(obj));
                            rack.setPatchVersion(Sound.loadPatchVersion(obj));
                            rack.setPatchInfo(Sound.loadPatchInfo(obj));
                            rack.setPatchAuthor(Sound.loadPatchAuthor(obj));
                            rack.setPatchDate(Sound.loadPatchDate(obj));
                            rack.checkOrder();
                            }
                        finally 
                            {
                            rack.output.unlock();
                            }
                        }
                    catch(Exception ex) { ex.printStackTrace(); showSimpleError("Patch Reading Error", "The patch could not be loaded", rack); }
                    file = f;
                    dirFile = f;
                    }
                }
            });
        return load;
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
                    //try
                    {
                    f = new File(fd.getDirectory(), fd.getFile());
                    rack.output.lock();
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
                        rack.output.unlock();
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
    static boolean showSimpleConfirm(String title, String message, Rack rack)
        {
        rack.disableMenuBar();
        boolean result = (JOptionPane.showConfirmDialog(rack, message, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null) == JOptionPane.OK_OPTION);
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
                if (showSimpleConfirm("New Patch", "Clear the existing patch?", rack))
                    {
                    rack.output.lock();
                    try
                        {
                        rack.closeAll();
                        rack.checkOrder();
                        rack.add(Out.class);
                        rack.setPatchName(null);
                        file = null;
                        // don't reset dirFile
                        }
                    finally 
                        {
                        rack.output.unlock();
                        }
                    }
                }
            });
        return newpatch;
        }
        

    static JMenuItem playFirstMenu(Rack rack)
        {
        final JCheckBoxMenuItem playFirst = new JCheckBoxMenuItem("Play One Voice Only");
        playFirst.setSelected(Prefs.getLastOneVoice());
        rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
        playFirst.addActionListener(new ActionListener()
            {
            public void actionPerformed(ActionEvent e)
                {
                rack.getOutput().setOnlyPlayFirstSound(playFirst.isSelected());
                Prefs.setLastOneVoice(playFirst.isSelected());
                }
            });
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


    // Produces the Sync to MIDI Clock menu
    static JMenuItem syncMenu(Rack rack)
        {
        final JCheckBoxMenuItem sync = new JCheckBoxMenuItem("Sync to MIDI Clock");
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
        menu.add(playFirstMenu(rack));
        menu.add(syncMenu(rack));
        menu.addSeparator();
        menu.add(namePatchMenu(rack));
        menu.addSeparator();
        menu.add(addModulesAfterMenu(rack));
        menu.add(setupPatchMenu(rack));
        menu.add(setupTuningMenu(rack));
        return menu;
        }

    // Produces the File menu
    public static JMenu provideFileMenu(Rack rack)
        {
        JMenu menu = new JMenu("File");

        menu.add(newPatchMenu(rack));
        menu.add(savePatchMenu(rack));
        menu.add(saveAsPatchMenu(rack));
        menu.add(loadPatchMenu(rack));

        if (!Style.isMac())
            {
            menu.addSeparator();
            menu.add(quitPatchMenu(rack));
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
        //JMenuItem outMenu = null;
        JMenuItem inMenu = null;

        for(int i = 0; i < modules.length; i++)
            {
            Class c = modules[i];
            JMenuItem m = menuFor(c, rack);
                        
            if (c == flow.modules.Out.class)
                { } // do nothing //outMenu = m;
            else if (c == flow.modules.In.class)
                inMenu = m;
            else if (flow.UnitSource.class.isAssignableFrom(c))
                unitSources.add(m);
            else if (flow.ModSource.class.isAssignableFrom(c))
                modSources.add(m);
            else if (flow.Unit.class.isAssignableFrom(c))
                unitShapers.add(m);
            else  // Module
                modShapers.add(m);
            }

        // do the same thing for a module loaded on the command line
        String modname = System.getProperty("module", null);
        System.err.println(modname);
        if (modname != null)
            {
            try
                {
                Class c = Class.forName(modname);
                JMenuItem m = menuFor(c, rack);

                if (c == flow.modules.Out.class)
                    { } // do nothing //outMenu = m;
                else if (c == flow.modules.In.class)
                    inMenu = m;
                else if (flow.UnitSource.class.isAssignableFrom(c))
                    unitSources.add(m);
                else if (flow.ModSource.class.isAssignableFrom(c))
                    modSources.add(m);
                else if (flow.Unit.class.isAssignableFrom(c))
                    unitShapers.add(m);
                else  // Module
                    modShapers.add(m);
                }
            catch (Exception ex)
                {
                ex.printStackTrace();
                }
            }

                
        //menu.add(outMenu);
        menu.add(inMenu);
                
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
                        
        return menu;
        }


    // Removes all modules from the rack, and
    // loads new modules from the given deserialized array.  The Modulations
    // are organized by Sound, then by Modulation.
    static void load(Modulation[][] mods, Rack rack, String patchName)
        {
        Output output = rack.getOutput();
        rack.output.lock();
        try
            {
            // remove all existing panels
            rack.closeAll();
            rack.checkOrder();
                   
            // Add the modulations as a group
            for(int i = 0; i < mods.length; i++)
                {
                Sound sound = output.getSound(i);
                for(int j = 0; j < mods[i].length; j++)
                    {
                    sound.register(mods[i][j]);
                    mods[i][j].setSound(sound);
                    if (mods[i][j] instanceof Out)
                        {
                        sound.setEmits((Out)(mods[i][j]));
                        }
                    mods[i][j].reset();
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
            rack.output.unlock();
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


    // A list of all the modules in the system which can appear in the
    // Modules menu.
    static final Class[] modules = new Class[]
    {
    flow.modules.All.class,
    flow.modules.AmpMath.class,
    flow.modules.Average.class,
    flow.modules.Buffer.class,
    flow.modules.Combine.class,
    flow.modules.Compress.class,
    flow.modules.DADSR.class,
    flow.modules.Delay.class,
    flow.modules.Dilate.class,
    flow.modules.Draw.class,
    flow.modules.Drawbars.class,
    flow.modules.Envelope.class,
    flow.modules.Fatten.class,
    flow.modules.Fill.class,
    flow.modules.Filter.class,
    flow.modules.FlangeFilter.class,
    flow.modules.In.class,
    flow.modules.Harmonics.class,
    flow.modules.Jitter.class,
    flow.modules.KHarmonics.class,
    flow.modules.LFO.class,
    flow.modules.LinearFilter.class,
    flow.modules.Map.class,
    flow.modules.MIDIIn.class,
    flow.modules.Mix.class,
    flow.modules.ModMath.class,
    flow.modules.Morph.class,
    flow.modules.Normalize.class,
    flow.modules.Noise.class,
    flow.modules.NRPN.class,
    flow.modules.Out.class,
    flow.modules.PartialLab.class,
    flow.modules.PartialMod.class,
    flow.modules.PartialFilter.class,
    flow.modules.Partials.class,
    flow.modules.Rand.class,
    flow.modules.Rectified.class,
    flow.modules.SampleAndHold.class,
    flow.modules.Sawtooth.class,
    flow.modules.Scale.class,
    flow.modules.Seq.class,
    flow.modules.Shift.class,
    flow.modules.Sine.class,
    flow.modules.Skeletonize.class,
    flow.modules.Smooth.class,
    flow.modules.Soften.class,
    flow.modules.Square.class,
    flow.modules.Squish.class,
    flow.modules.Stretch.class,
    flow.modules.Sub.class,
    flow.modules.Swap.class,
    flow.modules.Tinkle.class,
    flow.modules.Triangle.class,
    flow.modules.User.class,
    flow.modules.VCA.class,
    flow.modules.WaveTable.class,
    };      
    }

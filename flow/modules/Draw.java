// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import org.json.*;
import flow.utilities.*;

/** 
    A Unit which allows a user to load, save, and graphically edit all the partials.
    The user can also CAPTURE incoming partials from some unit source and edit that
    instead.  The user can STANDARDIZE, NORMALIZE, or MAXIMIZE (scale the partials
    until the highest one is 1.0 exactly) the partials.  He can also CLEAR all the
    partials (set them to 0.0).  And of course draw all of them.

    <p>Partials are written out and read in from files ending in FILENAME_EXTENSION
    (presently ".partials"), and consist simply of text files of lines 
    consisting of the PARTIAL INDEX, FREQUENCY, and AMPLITUDE of each partial.
    The partial index is only for text-editing convenience and is ignored on reading in;
    the partials are assumed to be complete and in order (indexes 0... NUM_PARTIALS-1).
        
    <p>The user can also constrain which partials he wishes to edit using constraints
    similar to the standard unit constraints.
        
    <p>Draw is particularly useful in combination with PartialsFilter.
*/

public class Draw extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int DO_UNDO = 0;
    public static final int DO_CAPTURE = 1;
    public static final int DO_STANDARDIZE = 2;
    public static final int DO_NORMALIZE = 3;
    public static final int DO_MAXIMIZE = 4;
    public static final int DO_CLEAR = 5;
    public static final int DO_SAVE = 6;
    public static final int DO_LOAD = 7;
    public static final int DO_LOAD_WAVE = 8;
    
    public static final String UNTITLED_PARTIALS_NAME = "Untitled";
    public static final String FILENAME_EXTENSION = ".partials";
    public static final int MAXIMUM_SAMPLES = 2048;
    public static final int WINDOW_SIZE = 65;

    public transient double[] backupFrequencies;
    public transient double[] backupAmplitudes;
        
    public Object clone()
        {
        Draw obj = (Draw)(super.clone());
        if (obj.backupFrequencies != null)
            obj.backupFrequencies = (double[])(obj.backupFrequencies.clone());
        if (obj.backupAmplitudes != null)
            obj.backupAmplitudes = (double[])(obj.backupAmplitudes.clone());
        return obj;
        }

    void backup()
        {
        backupFrequencies = getFrequencies(0).clone();
        backupAmplitudes = getAmplitudes(0).clone();
        }
        
    public Draw(Sound sound) 
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Source" });
        setClearOnReset(false);
        }
    
    public boolean isConstrainable() { return false; }
    
    public void takeSnapshot()
        {
        copyFrequencies(0);
        copyAmplitudes(0);
        }
            
    public void restoreFromBackup()
        {
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        double[] tempAmp = new double[amplitudes.length];
        double[] tempFreq = new double[frequencies.length];
        System.arraycopy(amplitudes, 0, tempAmp, 0, tempAmp.length);
        System.arraycopy(frequencies, 0, tempFreq, 0, tempFreq.length);
        System.arraycopy(backupAmplitudes, 0, amplitudes, 0, amplitudes.length);
        System.arraycopy(backupFrequencies, 0, frequencies, 0, frequencies.length);
        backupFrequencies = tempFreq;
        backupAmplitudes = tempAmp;
        }

    void distributeToAllSounds()
        {
        int index = sound.findRegistered(this);
        Output output = sound.getOutput();
        int numSounds = output.getNumSounds();

        for(int i = 0; i < numSounds; i++)
            {
            Sound s = output.getSound(i);
            if (s.getGroup() == Output.PRIMARY_GROUP)
                {
                Draw draw = (Draw)(s.getRegistered(index));
                double[] amplitudes = getAmplitudes(0);
                double[] frequencies = getFrequencies(0);
                System.arraycopy(amplitudes, 0, draw.getAmplitudes(0), 0, amplitudes.length);
                System.arraycopy(frequencies, 0, draw.getFrequencies(0), 0, frequencies.length);
                }
            }
        }

    public boolean isConstrained(int[] constraints, int index)
        {
        // special case -- constrained partials is empty.  Then ALL are allowed
        // (the opposite of what you'd think)
        if (constraints.length == 0)
            {
            return true;
            }
        else 
            {
            for(int i = 0; i < constraints.length; i++)
                {
                if (constraints[i] == index)
                    {
                    return true;
                    }
                }
            }
        return false;
        }

    public ModulePanel getPanel()
        {
        final int[][] constrainedPartials = new int[1][0];
        final boolean[][] constrained = new boolean[1][getFrequencies(0).length];
        
        final Display display = new Display(sound.getOutput(), false, true)
            {
            public void prepareColorsForPartials()
                {
                Output output = getSound().getOutput();
                output.lock();
                try
                    {
                    for(int i = 0; i < constrained[0].length; i++)
                        constrained[0][i] = false;
                    if (constrainedPartials[0].length == 0)  // special case
                        {
                        for(int i = 0; i < constrained[0].length; i++)
                            constrained[0][i] = true;
                        }
                    else
                        {
                        for(int i = 0; i < constrainedPartials[0].length; i++)
                            constrained[0][constrainedPartials[0][i]] = true;
                        }
                    }
                finally 
                    {
                    output.unlock();
                    }
                }
                        
            public Color getColorForPartial(int partial, double amp)
                {
                Color c = super.getColorForPartial(partial, amp);
                if (!constrained[0][partial])
                    {
                    return new Color(c.getRed(), c.getGreen(), c.getBlue(), 100);
                    }
                else return c;
                }

            public void updatePartial(int index, double amp, boolean continuation)
                {
                if (amp < 0)
                    {
                    amp = 0;
                    }
                                        

                // Display has already grabbed the lock
                    {
                    if (!continuation)
                        backup();
                    if (isConstrained(constrainedPartials[0], index))
                        {
                        getAmplitudes(0)[index] = amp;
                        distributeToAllSounds();
                        }
                    }
                }
            public Dimension getPreferredSize() { return new Dimension(600, 200); }
            };
            
        display.setLogFrequency(false);
        display.setBoundPartials(true);
        
        ModulePanel p = new ModulePanel(Draw.this)
            {
            public void loadFile(File file, Rack rack)
                {
                try
                    {
                    Draw draw = (Draw)(getModulation());
                    Scanner scanner = new Scanner(file, "US-ASCII");
                    getRack().getOutput().lock();
                    try
                        {
                        draw.backup();
                        double[] frequencies = ((Unit)getModulation()).getFrequencies(0);
                        double[] amplitudes = ((Unit)getModulation()).getAmplitudes(0);
                        for(int i = 0; i < frequencies.length; i++)
                            {
                            if (scanner.hasNextInt())
                                scanner.nextInt();  // throw away
                            if (scanner.hasNextDouble())
                                frequencies[i] = scanner.nextDouble();
                            if (scanner.hasNextDouble())
                                amplitudes[i] = scanner.nextDouble();
                            }
                        }
                    finally 
                        {
                        getRack().getOutput().unlock();
                        }
                    scanner.close();
                    draw.distributeToAllSounds();
                    }
                catch (FileNotFoundException ex)
                    {
                    ex.printStackTrace();
                    }
                }                   
                
            public void doSave(JComponent root, Draw draw) 
                { 
                FileDialog fd = new FileDialog((Frame)(SwingUtilities.getRoot(root)), "Save Partials...", FileDialog.SAVE);
                
                fd.setFile(UNTITLED_PARTIALS_NAME + FILENAME_EXTENSION);                        // "Untitled.partials"
                if (AppMenu.dirFile != null)
                    fd.setDirectory(AppMenu.dirFile.getParentFile().getPath());
                
                getRack().disableMenuBar();
                fd.setVisible(true);
                getRack().enableMenuBar();
                                                                                
                File f = null; // make compiler happy
                PrintStream os = null;
                if (fd.getFile() != null)
                    try
                        {
                        f = new File(fd.getDirectory(), AppMenu.ensureFileEndsWith(fd.getFile(), FILENAME_EXTENSION));
                        os = new PrintStream(new FileOutputStream(f));
                        getRack().getOutput().lock();
                        try
                            {
                            draw.backup();
                            double[] frequencies = ((Unit)getModulation()).getFrequencies(0);
                            double[] amplitudes = ((Unit)getModulation()).getAmplitudes(0);
                            for(int i = 0; i < frequencies.length; i++)
                                os.println("" + i + " " + frequencies[i] + "\t" + amplitudes[i]);
                            }
                        finally 
                            {
                            getRack().getOutput().unlock();
                            }
                        os.close();
                        AppMenu.dirFile = f;
                        }
                    catch (FileNotFoundException ex)
                        {
                        ex.printStackTrace();
                        }
                }
            
            public JComponent buildPanel()
                {
                JComponent comp = super.buildPanel();
                
                
                final JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                panel.add(comp, BorderLayout.SOUTH);
                                
                Box box = new Box(BoxLayout.X_AXIS);
                
                PushButton button = new PushButton("Menu", new String[] { "Undo / Redo", "Capture", "Standardize Frequencies", "Normalize Amplitudes", "Maximize Amplitudes", "Clear Amplitudes", "Save Partials As...", "Load Partials...", "Load Wave..." })
                    {
                    public void perform(int val)
                        {
                        Draw draw = (Draw)(getModulation());

                                                
                        switch(val)
                            {
                            case DO_UNDO:
                                {                                               
                                getRack().getOutput().lock();
                                try
                                    {
                                    if (backupFrequencies != null)
                                        {
                                        draw.restoreFromBackup();
                                        draw.distributeToAllSounds();
                                        }
                                    }
                                finally 
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_CAPTURE:
                                {
                                getRack().getOutput().lock();
                                try
                                    {
                                    draw.backup();
                                    draw.takeSnapshot();  // this doesn't distribute right now; we'll do it later
                                    draw.distributeToAllSounds();
                                    }
                                finally 
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_STANDARDIZE:
                                {
                                getRack().getOutput().lock();
                                try
                                    {
                                    draw.backup();
                                    draw.standardizeFrequencies();
                                    draw.distributeToAllSounds();
                                    }
                                finally 
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_NORMALIZE:
                                {
                                getRack().getOutput().lock();
                                try
                                    {
                                    draw.backup();
                                    draw.normalizeAmplitudes();
                                    draw.distributeToAllSounds();
                                    }
                                finally 
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_MAXIMIZE:
                                {
                                getRack().getOutput().lock();
                                try
                                    {
                                    draw.backup();
                                    double[] amplitudes = getAmplitudes(0);
                                    double max = amplitudes[getLoudestPartial(0)];
                                    if (max == 0) return;
                                    double alpha = 1.0 / max;
                                    for(int i = 0; i < amplitudes.length; i++)
                                        {
                                        amplitudes[i] *= alpha;
                                        }
                                    draw.distributeToAllSounds();
                                    }
                                finally 
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_CLEAR:
                                {
                                getRack().getOutput().lock();
                                try
                                    {
                                    draw.backup();
                                    double[] amplitudes = getAmplitudes(0);
                                    for(int i = 0; i < amplitudes.length; i++)
                                        {
                                        amplitudes[i] = 0;
                                        }
                                    draw.distributeToAllSounds();
                                    }
                                finally
                                    {
                                    getRack().getOutput().unlock();
                                    }
                                break;
                                }
                            case DO_SAVE:
                                {
                                doSave(panel, draw);
                                break;
                                }
                            case DO_LOAD:
                                {
                                doLoad("Load Partials...", FILENAME_EXTENSION);
                                break;
                                }
                            case DO_LOAD_WAVE:
                                {
                                doLoadWave(panel);
                                break;
                                }
                            default:
                                {
                                warn("modules/Draw.java", "default occured when it shouldn't be possible");
                                break;
                                }
                            }
                        }
                    };
                        
                box.add(button);
                
                JLabel comboLabel = new JLabel("       Constrain ");
                comboLabel.setFont(Style.SMALL_FONT());
                box.add(comboLabel);

                String[] names = Draw.this.constraintNames.clone();
                names[0] = "All";
                JComboBox combo = new JComboBox(names)
                    {
                    public Dimension getMinimumSize() 
                        {
                        return getPreferredSize(); 
                        }
                    };

                combo.putClientProperty("JComponent.sizeVariant", "small");
                combo.setEditable(false);
                combo.setFont(Style.SMALL_FONT());
                combo.setMaximumRowCount(32);
                combo.setSelectedIndex(getConstraint());
                
                combo.addItemListener(new ItemListener()
                    {
                    public void itemStateChanged(ItemEvent e)
                        {
                        int val = combo.getSelectedIndex();
                        if (val == CONSTRAINT_NONE)
                            constrainedPartials[0] = new int[0];
                        else
                            {
                            setConstraint(val);
                            constrainedPartials[0] = getConstrainedPartials();
                            setConstraint(CONSTRAINT_NONE);
                            }
                        }
                    });
                                        
                box.add(combo);
                box.add(box.createGlue());
                JPanel panel3 = new JPanel();
                panel3.setLayout(new BorderLayout());
                panel3.add(box, BorderLayout.WEST);
                                
                JPanel panel2 = new JPanel();
                panel2.setLayout(new BorderLayout());
                panel2.add(panel3, BorderLayout.NORTH);
                panel2.add(display, BorderLayout.CENTER);
                panel2.add(Strut.makeHorizontalStrut(5), BorderLayout.EAST);
                panel2.add(Strut.makeVerticalStrut(5), BorderLayout.SOUTH);
                
                JLabel disclosureLabel = new JLabel("Show  ");
                disclosureLabel.setFont(Style.SMALL_FONT());
                DisclosurePanel disclosure = new DisclosurePanel(disclosureLabel, panel2, null);
                panel.add(disclosure, BorderLayout.CENTER);

                return panel;
                }


            public void doLoadWave(JComponent root)
                {
                //// FIRST we have the user choose a file
                Rack rack = getRack();
        
                File file = doLoad("Load Wave...", new String[] { "wav", "WAV" }, false);
                if (file == null) return;
                    
        
                double[] waves = null;
                double[] buffer = new double[256];
                int count = 0;
        
                WavFile wavFile = null;
                try 
                    {
                    double[] _waves = new double[MAXIMUM_SAMPLES];
                    wavFile = WavFile.openWavFile(file);
                
                    while(true)
                        {
                        // Read frames into buffer
                        int framesRead = wavFile.readFrames(buffer, buffer.length);
                        if (count + framesRead > MAXIMUM_SAMPLES)
                            {
                            AppMenu.showSimpleError("File Too Large", "This file may contain no more than " + MAXIMUM_SAMPLES + " samples.", rack);
                            return;
                            }
                        System.arraycopy(buffer, 0, _waves, count, framesRead);
                        count += framesRead;
                        if (framesRead < buffer.length) 
                            break;
                        }
                    waves = new double[count];
                    System.arraycopy(_waves, 0, waves, 0, count);
                    }
                catch (IOException ex)
                    {
                    AppMenu.showSimpleError("File Error", "An error occurred on reading the file.", rack);
                    return;
                    }
                catch (WavFileException ex)
                    {
                    AppMenu.showSimpleError("Not a proper WAV file", "WAV files must be mono 16-bit.", rack);
                    return;
                    }

                try
                    {
                    wavFile.close();
                    }
                catch (Exception ex) { }
        
                int desiredSampleSize = Unit.NUM_PARTIALS * 2;                          // because we have up to 256 samples
                int currentSampleSize = waves.length;
                                        
                /// Resample to Flow's sampling rate
                double[] newvals = WindowedSinc.interpolate(
                    waves,
                    currentSampleSize,
                    desiredSampleSize,              // notice desired and current are swapped -- because these are SIZES, not RATES
                    WINDOW_SIZE,
                    true);           
                
                // Note no window.  Should still be okay (I think?)
                double[] harmonics = FFT.getHarmonics(newvals);
                double[] finished = new double[harmonics.length / 2];           // must be 256
                for (int s=1 ; s < harmonics.length / 2; s++)                   // we skip the DC offset (0) and set the Nyquist frequency bin (harmonics.length / 2) to 0
                    {
                    finished[s - 1] = (harmonics[s] >= WaveTable.MINIMUM_AMPLITUDE ? harmonics[s]  : 0 );
                    }

                double max = 0;
                for(int i = 0; i < finished.length; i++)
                    {
                    if (max < finished[i])
                        max = finished[i];
                    }
                        
                if (max > 0)
                    {
                    for(int i = 0; i < finished.length; i++)
                        {
                        finished[i] /= max;
                        }
                    }
                                                                
                rack.getOutput().lock();

                int index = sound.findRegistered(Draw.this);
                Output output = sound.getOutput();
                int numSounds = output.getNumSounds();

                try
                    {
                    for(int i = 0; i < numSounds; i++)
                        {
                        Draw unit = (Draw)(output.getSound(i).getRegistered(index));
                        unit.standardizeFrequencies();
                        double[] amplitudes = unit.getAmplitudes(0);
                        System.arraycopy(finished, 0, amplitudes, 0, Math.min(finished.length, amplitudes.length));
                        }
                    }
                finally 
                    {
                    rack.getOutput().unlock();
                    }
                }

            };
        display.setModulePanel(p);
        return p;
        }


    //// SERIALIZATION STUFF

    public JSONObject getData()
        {
        // not necessary to store orders
        JSONObject obj = new JSONObject();
        JSONArray amps = new JSONArray();
        JSONArray freqs = new JSONArray();
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        for(int i = 0; i < frequencies.length; i++)
            {
            amps.put(i, amplitudes[i]);
            freqs.put(i, frequencies[i]);
            }
        obj.put("amp", amps);
        obj.put("freq", freqs);
        return obj;
        }
        
    public void setData(JSONObject data, int moduleVersion, int patchVersion)
        {
        // not necessary to store orders
        JSONArray amps = data.getJSONArray("amp");
        JSONArray freqs = data.getJSONArray("freq");
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        for(int i = 0; i < frequencies.length; i++)
            {
            amplitudes[i] = amps.optDouble(i, 0);
            frequencies[i] = freqs.optDouble(i, i);
            }
        } 

    }
    
    
    
    
    
    
    

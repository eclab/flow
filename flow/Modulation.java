// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import flow.modules.*;
import flow.gui.*;
import java.util.*;
import java.io.*;
import org.json.*;
import flow.utilities.*;
import javax.swing.JToolTip;

/**
   This is the top-level class of modules in the synthesizer.
   Modules which are direct subclasses of Modulation are meant
   to provide modulation values (numbers 0.0 ... 1.0) to other
   modules.  Modulation can provide any number of sources for
   these values (known as Modulation Outputs).  
           
   <p>A Modulation can also receive modulation values from other modules
   through any number of Modulation Inputs.

   <p>As part of updating itself to provide a new value for modulate(num),
   a Modulation may also provide a *Trigger*.  This is a boolean
   value which indicates that a trigger event has occurred.  For example,
   an LFO typically issues a trigger every completed LFO cycle.  Triggers
   are useful to provide various clock pulses.  You can query a Modulation
   to determine if a trigger has occurred this timestep by calling isTriggered(num).
                
   <p>The general procedure for manipulating a Modulation is as follows.
        
   <ul>
   <li>reset() is called to reset the Modulation to some canonical start state.
   Always call super.reset();
   <li>gate() is called to inform the Modulation that a key has just been pressed.
   Always call super.gate();
   <li>release() is called to inform the Modulation that a key has just been released.
   Always call super.release();
   <li>restart() is called to inform the Modulation that MIDI START was sent.  This
   is often the same as reset(), depending on whether the Modulation in question is
   responding to MIDI Clock.
   <li>go() is called to step the Modulation for the next time step.  If a timestep
   has a reset, gate, or release event, these methods will be called as appropriate
   prior to go() for that timestep.
   Always call super.go();
   </ul>
   
   <p>In your go() method, you should call setModulationOutput(...) to set the value
   of a modulation output to be read by downstream modules.  You should also, if
   appropriate, call updateTrigger(...) to indicate that a trigger has been set.  Note that
   super.go() clears all current triggers.
   
   <p>You define the number of your modulation outputs with defineModulationOutputs(...), 
   providing names for each of them.  
   
   <p>Modulations also have modulation inputs where they receive modulation.  You define
   the number of your modulation inputs with defineModulations(...), providing some N
   default modulations (typically Constants -- see Constant.java for common ones) and N
   names for the inputs. Each incoming
   Modulation can have some M modulation outputs (just as you can).  By default
   defineModulations(...) sets the modulation output of each of the modulations to 0.
   
   <p>At any time you can change what Modulation, and its Output, is connected to a given
   Modulation Input of yours, using setModulation(...).   You query the value of the incoming
   moduation and output attached to your ModulationInput with modulate(...).  You can also
   determine whether a trigger has occurred with isTriggered(...) and what the trigger count
   is with getTrigger(...).
   
   <p>It is common for a Modulation to have one or more boolean
   or integer options (for example, whether a value is inverted,
   or what operation to perform on a value).  You would implement
   these in the normal fashion: with getters and setters.  However
   to inform the GUI of the existence of these options, you also
   should define a set of *Options* which collectively define
   how to access these getters and setters in a consistent way.
**/

public abstract class Modulation implements Cloneable
    {
    private static final long serialVersionUID = 1;

    /** The Modulation's macro backpointer.  This is is null if the Modulation is not under a Macro. */
    protected Macro macro = null;
    public Macro getMacro() { return macro; }
    public void setMacro(Macro macro) { this.macro = macro; }       

    /** The Modulation's modulation panel backpointer.  This is is null if the Modulation is not associated with a modulation panel (typically because it's not part of Sound 0). */
    protected ModulePanel modPanel = null;
    public ModulePanel getModulePanel() { return modPanel; }
    public void setModulePanel(ModulePanel modPanel) { this.modPanel = modPanel; }  

    /** The Modulation's sound backpointer. */
    protected Sound sound;
        
    public Sound getSound() { return sound; }
    public void setSound(Sound sound) { this.sound = sound; }
        
    public Modulation(Sound sound) 
        { 
        this.sound = sound;
        if (sound != null)              // this is the case for constants
            sound.register(this);
        defineModulationOutputs(new String[] { "Mod" });
        }

    /** Returns the version number for this Modulation.  By default this
        method returns 0. */
    public int getVersion() { return 0; }
        
    /** Create your own static getName() method to return a custom name
        for your module.  This needs to be a very short name. 
        This would be used to list the modulation in a menu. */
    public static String getName() { return "Modulation"; }

    /** Returns the custom name for the given Class module, else returns
        its simple name. */
    public static String getNameForModulation(Class cls)
        {
        try
            {
            return (String)(cls.getDeclaredMethod("getName").invoke(null));
            }
        catch (Exception e)
            {
            return cls.getSimpleName();
            }
        }
        
    /** Returns a custom name for this *instance*, not class.
        By default all instances of the same class have the same
        name, and so this method just calls getNameForModulation(this.getClass()).
        But Macros override this to customize their names on their modulation
        panels.  Otherwise you generally shouldn't override this method -- override
        getName() instead. */
    public String getNameForModulation() { return getNameForModulation(this.getClass()); }

    ///// OPERATION

    /** Called when the system wishes the Modulation to reset itself to a pristine state. 
        Be sure to call super.reset(); */
    public void reset() { }

    /** Called when the system wishes to inform the Modulation that a NOTE ON message was received. 
        Be sure to call super.gate(); */
    public void gate() { }

    /** Called when the system wishes to inform the Modulation that a NOTE OFF message was received. 
        Be sure to call super.release(); */
    public void release() { }

    /** Called when the system wishes to inform the Modulation that a MIDI START message was received. 
        Be sure to call super.restart(); */
    public void restart() { }
        
    void _go()
        {
        go();
        }
        
    /** Called (many times) when the system wishes to inform the Modulation to revise itself. 
        Be sure to call super.go(); */
    public void go() 
        { 
        // reset all triggers
        for(int i = 0; i < triggered.length; i++)
            {
            resetTrigger(i);
            }
        }
                


    /// OPTIONS
    /// These represent methods which take either an integer (a choice)
    /// or a boolean value.  optionNames[i] hold a short human-readable version
    /// of parameter #i.  OptionValues[i] holds a list of the various options 
    /// for parameter #i.  You can get the current parameter with getOptionValue(option)
    /// or set with setOptionValue(option).  These two methods need to be overridden;
    /// it is up to the Element subclass to call the appropriate internal method to
    /// fulfill these methods.
    ///
    /// Note that if OptionValues[i] is an array of size ONE, then we assume that
    /// this is a boolean option, and the value provided is the name for when the
    /// option is TRUE.  This makes it convenient for building checkboxes etc.
        
    String[] optionNames = new String[0];
    String[][] optionValues = new String[0][0];
    
    /** Defines various options by their names and arrays of names of possible (integer 0... n) values. 
        The names array can be ragged.  */
    public void defineOptions(String[] names, String[][] values)
        {
        optionNames = names;
        optionValues = values;
        }
        
    /** Returns the value of the given option.  */
    public int getOptionValue(int option) { return 0; }

    /** Sets the value of the given option.  */
    public void setOptionValue(int option, int value) { }

    /** Returns the number of options.  */
    public int getNumOptions() { return optionNames.length; }

    /** Returns the name of the given option.  */
    public String getOptionName(int option) { return optionNames[option]; }

    /** Returns the names of all possible values the option can take on.  */
    public String[] getOptionValues(int option) { return optionValues[option]; }
        
    /** Override this to provide tooltips for options.  This array by default is null. 
        If any given String is null or empty, no tooltip is generated for it.  */
    public String[] getOptionHelp() { return null; }
        
        
        
    ////// MODULATION INPUT
        
    Modulation[] modulations = new Modulation[0];
    Constant[] defaultModulations = new Constant[0];
    Constant[] lastModulations = new Constant[0];
    
    int[] modulationIndexes = new int[0];
    String[] modulationNames = new String[0];

    /** Defines various INPUT modulations by their initial values (as Constants), and their names. */
    public void defineModulations(Constant[] modulations, String[] names)
        {
        defaultModulations = modulations;
        lastModulations = new Constant[modulations.length];
        System.arraycopy(modulations, 0, lastModulations, 0, modulations.length);
        this.modulations = new Modulation[modulations.length];  // can't clone :-(
        for (int i = 0; i < modulations.length; i++)
            this.modulations[i] = modulations[i];
        modulationIndexes = new int[modulations.length];  // all zero initially
        modulationNames = names;
        }

    /** Returns true if the given input modulation port is set to its default Modulation. */
    public boolean isDefaultModulation(int num)
        {
        return (defaultModulations[num] == modulations[num]) && (modulationIndexes[num] == 0);
        }
                
    /** Sets Input Modulation port 0 to Output Modulation port 0 of the provided modulations. */
    public void setModulation(Modulation mod) { setModulations(mod); }

    /** Sets Input Modulation port 0 to Output Modulation port 0 of the provided modulations. */
    public void setModulations(Modulation mod)
        {
        setModulations(new Modulation[] { mod });
        }

    /** Sets Input Modulation ports 0 and 1 to Output Modulation port 0 of the provided modulations. */
    public void setModulations(Modulation mod1, Modulation mod2)
        {
        setModulations(new Modulation[] { mod1, mod2 });
        }

    /** Sets Input Modulation ports 0, 1, and 2 to Output Modulation port 0 of the provided modulations. */
    public void setModulations(Modulation mod1, Modulation mod2, Modulation mod3)
        {
        setModulations(new Modulation[] { mod1, mod2, mod3 });
        }

    /** Sets Input Modulation ports 0 through (moulations.length - 1) to Output Modulation port 0 of the provided modulations. */
    public void setModulations(Modulation[] modulations)
        {
        setModulations(modulations, new int[modulations.length]);
        }
                
    /** Sets Input Modulation ports 0 through (moulations.length - 1) to Output Modulation port indexes[0] 
        through indexes[moulations.length - 1] of the provided modulations. */
    public void setModulations(Modulation[] modulations, int[] indexes)
        {
        for(int i = 0; i < modulations.length; i++)
            {
            setModulation(modulations[i], i, indexes[i]);
            }
        }
                
    /** Sets Input Modulation ports 0 through (constants.length - 1) to Constants of the given values. */
    public void setModulations(double[] constants)
        {
        for(int i = 0; i < constants.length; i++)
            setModulation(new Constant(constants[i]), i);
        }
                
    /** Sets Input Modulation port NUM to the Output Modulation port 0 of the given modulation. */
    public void setModulation(Modulation mod, int num)
        {
        setModulation(mod, num, 0);
        }
        
    /** Sets Input Modulation port NUM to the Output Modulation port INDEX of the given modulation. */
    public void setModulation(Modulation mod, int num, int index)
        {
        if (mod instanceof Constant)
            lastModulations[num] = (Constant)mod;
        modulations[num] = mod;
        modulationIndexes[num] = index;
        }
        
    /** Returns Input Modulation port NUM. */
    public Modulation getModulation(int num)
        {
        return modulations[num];
        }
                
    /** Returns the Output Modulation port index associated with Input Modulation port NUM. */
    public int getModulationIndex(int num)
        {
        return modulationIndexes[num];
        }
                
    /** Returns name of Input Modulation port NUM. */
    public String getModulationName(int num)
        {
        return modulationNames[num];
        }

    /** Returns the name of all Input Modulation ports. */
    public String[] getModulationNames()
        {
        return modulationNames;
        }

    /** Returns Input Modulation port NUM to its default Modulation (a Constant). */
    public void clearModulation(int num)
        {
        lastModulations[num] = defaultModulations[num];
        modulations[num] = defaultModulations[num];
        modulationIndexes[num] = 0;
        }
                
    /** Returns Input Modulation port NUM to its last Constant value. */
    public void restoreModulation(int num)
        {
        modulations[num] = lastModulations[num];
        modulationIndexes[num] = 0;
        }
                
    /** Returns the number of Input Modulation ports. */
    public int getNumModulations()
        {
        return modulationNames.length;
        }

    /** Sets the name of Input Modulation NUM to the given string. */
    public void setModulationName(int num, String string)
        {
        modulationNames[num] = string;
        }
                
    /** Returns what the CURRENT text string should be, for Input Modulation port NUM, 
        given its current numerical VALUE and whether or not the port has a CONSTANT
        (as opposed to something plugged into it).  Override this as you see fit. In general,
        if the port doesn't have a Constant in it, you should return "".  You shouldn't 
        call modulate(num) -- instead, use the value.    */
    public String getModulationValueDescription(int num, double value, boolean isConstant)
        {
        if (isConstant)
            return String.format("%.4f", value);
        else return "";
        }
    
    /** Returns what the CURRENT text string should be, for Input Modulation port NUM. 
        Override this as you see fit, but you MUST also override getModulationValueDescription(num, value, isConstant).
        This method will call that method, but other methods call that method as well.
        Overriding this method would update the JLabel directly on the widget, but not
        the proposed update information in an JOptionPane resulting from double-clicking
        on a dial. */
    public String getModulationValueDescription(int num)
        {
        return getModulationValueDescription(num, modulate(num), isModulationConstant(num));
        }

    /** Returns the strings to appear in a Modulation Dial's popup options.
        If you return null, then the Dial will instead use its internal
        options (see ModulationInput for ways to customize those). */
    public String[] getPopupOptions(int modulation)
        {
        return null;
        }

    public static final double NO_POPUP_CONVERSION_IMPLEMENTED = -1;
    /** Returns the double value in the range (0.0 ... 1.0) corresponding to the
        Modulation Dial's popup menu index, or NO_POPUP_CONVERSION_IMPLEMENTED if
        this method is not implemented (that's the default return value). */
    public double getPopupConversion(int modulation, int index)
        {
        return NO_POPUP_CONVERSION_IMPLEMENTED;
        }

    /** Returns the modulation value for the modulation currently at Input Modulation port INDEX */
    public final double modulate(int index)
        {
        return modulations[index].getModulationOutput(modulationIndexes[index]);
        }
        
    /** Returns the trigger count for the modulation currently at Input Modulation port INDEX */
    public int getTriggerCount(int index)
        {
        return modulations[index].getOutputTriggerCount(modulationIndexes[index]);
        }

    /** Returns the trigger value for the modulation currently at Input Modulation port INDEX */
    public boolean isTriggered(int index)
        {
        return modulations[index].isOutputTriggered(modulationIndexes[index]);
        }
        
    /** Returns whether the modulation at Input Modulation port INDEX is a Constant. */
    public boolean isModulationConstant(int index)
        {
        return modulations[index] instanceof Constant;
        }
        
    /** Override this to provide tooltips for modulation inputs.  This array by default is null. 
        If any given String is null or empty, no tooltip is generated for it.  */
    public String[] getModulationHelp() { return null; }



    //// MODULATION UTILITIES

    /** This is a list of modulation settings to use which correspond to the MIDI clock rates in MidiClock.CLOCK_NAMES,
        assuming that your module is applying modToRate(mod) to convert modulation values to clock rates. */
    public static final double[] MIDI_CLOCK_MOD_RATES;
    /** This is a list of modulation settings to use which correspond to the MIDI clock rates in MidiClock.CLOCK_NAMES,
        assuming that your module is applying modToLongRate(mod) to convert modulation values to clock rates. */
    public static final double[] MIDI_CLOCK_LONG_MOD_RATES;
        
    static
        {
        MIDI_CLOCK_MOD_RATES = new double[MidiClock.CLOCK_PULSES.length];
        MIDI_CLOCK_LONG_MOD_RATES = new double[MidiClock.CLOCK_PULSES.length];
        for(int i = 0; i < MidiClock.CLOCK_PULSES.length; i++)
            {
            // Notice that MOD RATES is 1.0 / x, but LONG MOD RATES is not.
            // This is because MOD RATES at present has low values for slower rates (like an LFO),
            // but LONG MOD RATE is used for times like in an envelope, so low values are FASTER rates. 
            // I don't know if this is a smart thing to do.
            MIDI_CLOCK_MOD_RATES[i] = rateToMod(1.0 / (MidiClock.CLOCK_PULSES[i] * MidiClock.TICKS_PER_PULSE * Output.INV_SAMPLING_RATE));
            MIDI_CLOCK_LONG_MOD_RATES[i] = longRateToMod(MidiClock.CLOCK_PULSES[i] * MidiClock.TICKS_PER_PULSE * Output.INV_SAMPLING_RATE);
            }
        } 

    double modToRate_lastMod = Double.NaN;
    double modToRate_lastRate;
        
    public double modToRate(double mod)
        {
        // Let's try: r =  2^((x * 20) - 5.57151) with a threshold at 0
        // This ranges from:                     x = 0,         r = 0           (infinity)
        //                                       x = 0.05       r = 0.042057    (about 47.5 secs)
        //                                       x = 0.25       r = 0.672912    (about 1.48 secs)
        //                                       x = 0.5        r = 21.5332     (about 0.05 secs)
        //                                       x = 0.75       r = 689.062     (about 0.00145 secs)
        //                                       x = 1.0        r = 22050       Half sampling rate

        // Because we're doing 32 samples at the moment, our maximal rate should be:
        // 5.57151 * 1.8974226.  This puts our max r (x=1.0) at 689.062, which is pretty close to 689.062 (our true frame rate)
                                
        if (mod != modToRate_lastMod)
            {
            modToRate_lastRate = (mod == 0 ? 0 : Math.pow(2, (mod * 20) - 5.57151));
            modToRate_lastMod = mod;
            }
        return modToRate_lastRate;
        }


    /** This is the exact inverse of modToRate(mod), that is, rateToMod(modToRate(mod)) == mod */
    // This can only get down to 0.21 or so....
    public static double rateToMod(double rate)
        {
        // if r = 2^((x * 20) - 5.57151), then
        // (x * 20) - 5.57151 = Log(r) / Log(2)
        // so x = (Log(r)/Log(2) + 5.57151) / 20
        
        if (rate == 0) return 0;
        else return (Math.log(rate) / Math.log(2.0) + 5.57151) / 20.0;
        }

    /** Converts a modulation 0...1 into a long rate of 0...10, for purposes of envelopes */
    public double modToLongRate(double mod)
        {
        return (0.0 - (mod * 0.96 / (mod * 0.96 - 1))) / 9.6 * 10.0 / 2.5 ;
        }
        
    /** This is the exact inverse of modToLongRate(mod), that is, longRateToMod(modToLongRate(mod)) == mod */
    public static double longRateToMod(double rate)
        {
        // if r = (0 - x*0.96 / (x * 0.96 - 1)) / 9.6 * 10.0 / 2.5
        // Then mathematica tells me x == (12.5 r) / (5 + 12 r)

        if (rate == 0) return 0;
        else return 12.5 * rate / (5 + 12 * rate);
        }

    /** Converts a modulation 0...1 into a frequency in the range 0 ... 22050 (which is 2^14.428491035332245).  */
    // It sounds too extreme to just use mod directly, so we soften it on the low end with makeSensitive
    double modToFrequency_lastMod = Double.NaN;
    double modToFrequency_lastFreq;
    public double modToFrequency(double mod)
        {
        if (mod != modToFrequency_lastMod)
            {
            modToFrequency_lastFreq = Math.pow(2, makeSensitive(mod) * 14.428491035332245) - 1;
            modToFrequency_lastMod = mod;
            }
        return modToFrequency_lastFreq;
        }

    /** Converts a modulation 0...1 into a frequency in the range 0 ... 22050 (which is 2^14.428491035332245).  */
    public double modToInsensitiveFrequency(double mod)
        {
        if (mod != modToFrequency_lastMod)
            {
            modToFrequency_lastFreq = Math.pow(2, mod * 14.428491035332245) - 1;
            modToFrequency_lastMod = mod;
            }
        return modToFrequency_lastFreq;
        }

    /** Converts a modulation 0...1 into a frequency in the range -11025 ... 11025 (which is +/- 2^13.428491035332245).  */
    double modToSignedFrequency_lastMod = Double.NaN;
    double modToSignedFrequency_lastFreq;
    public double modToSignedFrequency(double mod)
        {
        if (mod != modToSignedFrequency_lastMod)
            {
            if (mod >= 0.5)
                modToSignedFrequency_lastFreq = Math.pow(2, (mod - 0.5) * 2 * 13.428491035332245) - 1;
            else
                {
                modToSignedFrequency_lastFreq = 0 - Math.pow(2, (0.5 - mod) * 2 * 13.428491035332245) - 1;
                }
            modToSignedFrequency_lastMod = mod;
            }
        return modToSignedFrequency_lastFreq;
        }

    /** Converts a modulation 0...1 into a frequency in the range -11025 ... 11025 (which is +/- 2^13.428491035332245).  */
    double modToRelativeFrequency_lastMod = Double.NaN;
    double modToRelativeFrequency_lastFreq;
    public double modToRelativeFrequency(double mod)
        {
        if (mod != modToRelativeFrequency_lastMod)
            {
            if (mod >= 0.5)
                modToRelativeFrequency_lastFreq = Math.pow(2, (mod - 0.5) * 2 * 13.428491035332245) - 1;
            else
                {
                modToRelativeFrequency_lastFreq = Math.pow(2, 0 - (0.5 - mod) * 2 * 13.428491035332245) - 1;
                }
            modToRelativeFrequency_lastMod = mod;
            }
        return modToRelativeFrequency_lastFreq;
        }
                
                
    /** Maps the modulation value to a function more sensitive to lower values. */
    public double makeSensitive(double mod)
        {
        return mod * mod;
        }

    /** Maps the modulation value to a function MUCH more sensitive to lower values. */
    public double makeVerySensitive(double mod)
        {
        return mod * mod * mod * mod;
        }

    /** Maps the modulation value to a function more sensitive to higher values. */
    public double makeInsensitive(double mod)
        {
        return 1 - (1 - mod) * (1 - mod);
        }

    /** Maps the modulation value to a function MUCH more sensitive to higher values. */
    public double makeVeryInsensitive(double mod)
        {
        return 1 - (1 - mod) * (1 - mod) * (1 - mod) * (1 - mod);
        }




    //// MODULATION OUTPUT
        
        
    public static int NO_TRIGGER = 0;
    boolean[] triggered;
    int[] triggerCount;
    double[] modulationOutputs;
    String[] modulationOutputNames = new String[] { "Mod" };

    /** Defines various OUTPUT modulations ports by their names. */
    public void defineModulationOutputs(String[] names)
        {
        modulationOutputs = new double[names.length];
        triggerCount = new int[names.length];
        triggered = new boolean[names.length];
        modulationOutputNames = names;
        }
        
    /** Returns the name of output modulation port VAL */
    public String getModulationOutputName(int val) { return modulationOutputNames[val]; }

    /** Sets the name of output modulation port VAL */
    public void setModulationOutputName(int val, String string) { modulationOutputNames[val] = string; }

    /** Returns the names of all output modulation ports */
    public String[] getModulationOutputNames() { return modulationOutputNames; }
    
    // This little bit of nastiness allows us to print a one-time error if we've received
    // a request to set a modulation value outside of 0...1, which will bomb all sorts of stuff.
    // We're doing it this way so that setModulationOutput(...) is still under 35 bytes and therefore
    // is inlined.
    static boolean printedModulationOutputError = false;
    void printModulationOutputError(double val) 
        { 
        if (!printedModulationOutputError)
            {
            printedModulationOutputError = true;
            new RuntimeException("Modulation Ouput Set to " + val + "\n This will be printed only once.").printStackTrace();
            }
        }
    
    /** Sets the current output value of modulation port INDEX to VAL. */
    public void setModulationOutput(int index, double val) 
        {
        if (val < 0 || val > 1) printModulationOutputError(val);
        else modulationOutputs[index] = val; 
        }
    
    /** Returns the current output value of modulation port INDEX to VAL. */
    public double getModulationOutput(int index) { return modulationOutputs[index]; }
    
    /** Returns number of modulation output ports. */
    public int getNumModulationOutputs() { return modulationOutputNames.length; }

    /** Override this to provide tooltips for modulation outputs.  This array by default is null. 
        If any given String is null or empty, no tooltip is generated for it.  */
    public String[] getModulationOutputHelp() { return null; }

    /** Resets all triggers of Output Modulation ports and their trigger counts. */
    protected void resetTrigger(int num)
        {
        setTriggerValues(false, NO_TRIGGER, num);
        }

    /** Sets trigger NUM of Output Modulation port, and increments its count. */
    protected void updateTrigger(int num)
        {
        setTriggerValues(true, triggerCount[num] + 1, num);
        }

    /** Returns the trigger count for the trigger of Output Modulation port NUM. */
    public int getOutputTriggerCount(int num)
        {
        return triggerCount[num];
        }

    /** Returns whether the trigger of Output Modulation port NUM is set. */
    public boolean isOutputTriggered(int num)
        {
        return triggered[num];
        }
        
    /** Sets trigger NUM of Output Modulation port to the given ISTRIGGERED value, and sets its count to TRIGGERCOUNT. */
    // this is public, rather than protected, so Macro can override it
    public void setTriggerValues(boolean isTriggered, int _triggerCount, int num)
        {
        triggered[num] = isTriggered;
        triggerCount[num] = _triggerCount;
        }
    
    /** Returns the clock tick value.  If we are syncing to MIDI clock,
        this is returned.  Else the global wall clock is returned. */
    public int getSyncTick(boolean sync)
        {
        Output output = sound.getOutput();
        MidiClock clock = output.getInput().getMidiClock();
        if (sync && clock.isSyncing())
            {
            return clock.getTick();
            }
        else
            {
            return output.getTick();
            }
        }
    
    /** Returns true if my sound is the first sound in the output.  This is 
        mostly used for debugging, to reduce the number of print statements
        etc. generated by modules. */
    public boolean isFirstSound() 
        { 
        if (sound == null) return false;
        
        Sound s = sound.getOutput().getInput().getLastPlayedSound();
        if (s == null)
            return (sound.getOutput().getSoundUnsafe(0) == sound);
        else
            return (s == sound);
        }

    /** For debugging... */
    protected void print(String str)
        {
        if (isFirstSound()) System.err.println(str);
        }

    protected void warn(String where, String what)
        {
        print("WARNING (" + where + "): " + what);
        }

    protected void warnAlways(String where, String what)
        {
        System.err.println("WARNING (" + where + "): " + what);
        }
                 
    /** Returns the ModulePanel associated with this Modulation.  Use the default here, or override this to create your own ModulePanel. */
    public ModulePanel getPanel()
        {
        return new ModulePanel(this);
        }
        
    public static final int MAX_TOOL_TIP_WIDTH = 400;
    public String[] wrapHelp(String[] help)
        {
        if (help == null) return null;
        String[] newHelp = new String[help.length];
        JToolTip tip = new JToolTip();
        for(int i = 0; i < help.length; i++)
            {
            if (help[i] != null)
                {
                tip.setTipText(help[i]);
                int width = (int)(tip.getPreferredSize().getWidth());
                if (width > MAX_TOOL_TIP_WIDTH)
                    newHelp[i] = "<html><p width=" + MAX_TOOL_TIP_WIDTH + ">" +
                        help[i] + "</p></html>";
                else
                    newHelp[i] = help[i];
                }
            }
        return newHelp;
        }
    
    /** Print some statistics regarding triggers and modulations. */
    public void printStats()
        {
        System.err.println("\nELEMENT " + this);
        System.err.println("\nSOUND " + sound);
        for(int i = 0; i < modulations.length; i++)                     
            System.err.println("" + i + " MOD IN: " + modulations[i] + " DEFAULT: " + defaultModulations[i]);
        for(int i = 0; i < modulationOutputs.length; i++)                       
            System.err.println("" + i + " MOD OUT: " + modulationOutputs[i]);
        for(int i = 0; i < triggered.length; i++)                       
            System.err.println("" + i + " TRIGGER: " + triggerCount[i] + " TRIGGERED: " + triggered[i]);
        for(int i = 0; i < getNumOptions(); i++)
            {
            System.err.println("" + i + " OPTION: " + getOptionName(i) + " VAL: " + 
                    (getOptionValues(i).length == 1 ? (getOptionValue(i) == 1 ? "t" : "f") :
                    getOptionValues(i)[getOptionValue(i)]));
            }
        }
    
    public Object clone()
        {
        Modulation obj = null;
        try
            {
            obj = (Modulation)(super.clone());
            }
        catch (CloneNotSupportedException ex) { ex.printStackTrace(); }  // never happens
        
        // ---- Copy over options ----
        // Option Names
        obj.optionNames = (String[])(optionNames.clone());
        // Option Values
        obj.optionValues = (String[][])(optionValues.clone());
        for(int i = 0; i < optionValues.length; i++)
            obj.optionValues[i] = (String[])(obj.optionValues[i].clone());
        // Set the values
        for(int i = 0; i < optionNames.length; i++)
            obj.setOptionValue(i, getOptionValue(i));
                
        // --- Copy over modulation inputs.  We retain a pointer to the old modulation inputs unless they're Constants. ---
        // Input Modulation Names
        obj.modulationNames = (String[])(modulationNames.clone());
        // Input Modulations.  Copy if we're Constant.
        obj.modulations = (Modulation[])(modulations.clone());
        for(int i = 0; i < obj.modulations.length; i++)
            {
            if (obj.modulations[i] instanceof Constant)
                obj.modulations[i] = (Modulation)(obj.modulations[i].clone());
            }
        // Modulation Indexes
        obj.modulationIndexes = (int[])(modulationIndexes.clone());     
        // Copy Default Modulations
        obj.defaultModulations = (Constant[])(defaultModulations.clone());
        for(int i = 0; i < obj.defaultModulations.length; i++)
            obj.defaultModulations[i] = (Constant)(obj.defaultModulations[i].clone());
        // Copy Last Constant Modulations Used
        obj.lastModulations = (Constant[])(lastModulations.clone());
        for(int i = 0; i < obj.lastModulations.length; i++)
            obj.lastModulations[i] = (Constant)(obj.lastModulations[i].clone());

        // --- Copy over modulation outputs ---
        // Output names
        obj.modulationOutputNames = (String[])(modulationOutputNames.clone());          
        // Current output values
        obj.modulationOutputs = (double[])(modulationOutputs.clone());
        // Current output triggers
        obj.triggered = (boolean[])(triggered.clone());
        obj.triggerCount = (int[])(triggerCount.clone());

        return obj;
        }


    ///// JSON Serialization
    public static final int SERIALIZATION_NOT_FOUND = -1;
    String id;
    public void setID(String id) { this.id = id; }
    public String getID() { return "" + id; }
    
    /** A hook which is called on the modulation during load-time, after all modulations have been constructed and
        their IDs have been set, but before any of their options, modulation inputs/outputs, or unit input/outputs
        have been set.   By default this method does nothing. */
    public void preprocessLoad(int moduleVersion, int patchVersion) { }
    /** A hook which is called on the modulation during load-time, after all modulations have had
        their options, modulation inputs/outputs, and unit input/outputs set.  By default this method does nothing. */
    public void postprocessLoad(int moduleVersion, int patchVersion) { }
    /** Called to return a String to be used as a JSON key for the given modulation input. 
        This must be unique within modulation inputs in the modulation.  By default the name of the modulation input is used.  */
    public String getKeyForModulation(int input) { return "" + getModulationName(input); }
    /** Called to return a String to be used as a JSON key for the given modulation output. 
        This must be unique within modulation outputs in the modulation.  By default the name of the modulation output is used.  */
    public String getKeyForModulationOutput(int output) { return "" + getModulationOutputName(output); }
    /** Called to return a String to be used as a JSON key for the given option. 
        This must be unique within options in the modulation.  By default the name of the option is used.  */
    public String getKeyForOption(int option) { return "" + getOptionName(option); }
    /** Called to return the Data object to store in the file for the modulation, or null if none should be stored. 
        By default null is returned. */
    public JSONObject getData() { return null; }
    /** Called to update the modulation with respect to the the Data object currently stored in the file 
        for the modulation.  This data object may be null.  This method will be called before the
        modulation outputs/inputs, unit output/inputs, and options have been loaded. By default, does nothing.  */
    public void setData(JSONObject data, int moduleVersion, int patchVersion) throws Exception { }  // this method may have null passed in if there was no data (which might be an error?)
    /** Returns the modulation output number for a given JSON Key, or SERIALIZATION_NOT_FOUND if the key is not found.
        This method is the inverse of getKeyForModulationOutput(...)  */
    public int getModulationOutputForKey(String key)
        {
        // Terribly inefficient but it'll suffice
        for(int i = 0; i < getNumModulationOutputs(); i++)
            {
            if (getKeyForModulationOutput(i).equals(key)) return i;
            }
        return SERIALIZATION_NOT_FOUND;
        }

    /** Saves the Modulation to a JSON Object. */
    public JSONObject save() throws JSONException
        {
        JSONObject obj = new JSONObject();
                
        // class
        obj.put("class", this.getClass().getCanonicalName());

        // id
        obj.put("id", getID());
                
        // version
        obj.put("v", getVersion());

        // options
        JSONObject options = new JSONObject();
        for(int i = 0; i < getNumOptions(); i++)
            options.put(getKeyForOption(i), getOptionValue(i));
        obj.put("opt", options);
                
        // mods
        JSONObject mods = new JSONObject();
        for(int i = 0; i < getNumModulations(); i++)
            {
            Modulation mod = getModulation(i);
            int modIndex = getModulationIndex(i);
            if (mod instanceof Constant)
                {
                Constant c = (Constant)mod;
                mods.put(getKeyForModulation(i), c.getValue());
                }
            else
                {
                JSONObject m = new JSONObject();
                m.put("id", mod.getID());
                m.put("at", mod.getKeyForModulationOutput(modIndex));
                mods.put(getKeyForModulation(i), m);
                }
            }
        obj.put("mod", mods);
                
        // auxillary data
        Object data = getData();
        if (data != null)
            obj.put("data", data);
        return obj;
        }

    /** Loads options from the given JSON Object representing the option storage. */
    public void loadOptions(JSONObject options, int moduleVersion, int patchVersion)
        {
        for(int i = 0; i < getNumOptions(); i++)
            {
            int val = options.optInt(getKeyForOption(i), -1);
            if (val == -1)
                {
                warn("Modulation.java", "Could not load option " + getKeyForOption(i) + " in " + this);
                }
            else
                {
                setOptionValue(i, val);
                }
            }
        }
   
    /** Loads modulation inputs from the given JSON Object representing the modulation storage. */
    public void loadModulations(JSONObject mods, HashMap<String, Modulation> ids, int moduleVersion, int patchVersion)
        {
        for(int i = 0; i < getNumModulations(); i++)
            {
            double v = mods.optDouble(getKeyForModulation(i), Double.NaN);
            if (v == v)  // it's not NAN
                {
                setModulation(new Constant(v), i);
                }
            else            // It's an ID?
                {
                JSONObject m = mods.optJSONObject(getKeyForModulation(i));
                if (m == null)
                    {
                    // probably a Macro, possibly something new this patch didn't know about
                    //setModulation(new Constant(0), i);
                    clearModulation(i);             // set it to its default constant value
                    }
                else
                    {
                    Modulation mod = ids.get(m.getString("id"));
                    if (mod != null)
                        {
                        int modOutput = mod.getModulationOutputForKey(m.getString("at"));
                        if (modOutput >= 0 && modOutput < mod.getNumModulationOutputs())
                            {
                            setModulation(mod, i, modOutput);
                            }
                        else warn("Modulation.java", "invalid mod output (" + modOutput + ") for id " + id + " in " + this);
                        }
                    else warn("Modulation.java", "no modulation for id " + id + " in " + this);
                    }
                }
            }
        }

    /** Loads the Modulation from the given JSON Object. */
    public void load(JSONObject obj, HashMap<String, Modulation> ids, int patchVersion) throws Exception
        {
        // version
        int moduleVersion = obj.getInt("v");
                
        // auxillary data, possibly null
        setData(obj.optJSONObject("data"), moduleVersion, patchVersion);

        // options
        JSONObject options = obj.getJSONObject("opt");
        loadOptions(options, moduleVersion, patchVersion);
                
        // mods
        JSONObject mods = obj.getJSONObject("mod");
        loadModulations(mods, ids, moduleVersion, patchVersion);
        }

    public void testDenormals(double val, String s)
        {
        if (val > 0 && val <= 2250738585072012e-308)
            System.err.println(s + " is DENORMAL " + val);
        }

    ////// HELP

    /** Override this to provide help text in HTML format. 
        By default this method looks for an HTML file called Foo.html, where
        Foo is the name of your Modulation subclass, stored in
        a subdirectory called "html" located next to Foo.class.  It reads
        this file and returns it as a String.   If there is no such file,
        null is returned, indicating no help text is provided.
                
        <p>Some rules about the HTML.  First, you don't need to say <html>...</html>, those
        tags will be added later.  Second, the title of the help, which will be added automatically
        by Flow, will be in <h2>title</h2>.  So you shouldn't use <h2> or <h1>.
    */
    public String getHelpText() 
        { 
        InputStream str = this.getClass().getResourceAsStream("help/" + this.getClass().getSimpleName() + ".html");
        if (str == null) return null;
        else return StringUtilities.read(str);
        }
                
    }

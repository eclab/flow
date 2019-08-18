// Copyright 2017 by Sean Luke
// Licensed under the Apache 2.0 License

package flow;

import java.util.prefs.*;
import flow.gui.*;

/** 
    A simple cover class for Java's preferences system.
*/

public class Prefs
    {
    static final String GLOBAL_PREFERENCES = "edu.gmu.flow/global";
    static final String EDITOR_PREFERENCES = "edu.gmu.flow/module";

    /** Returns the preferences object associated with global preferences for the application. */
    public static Preferences getGlobalPreferences(String namespace)
        {
        return Preferences.userRoot().node(GLOBAL_PREFERENCES + "/" + namespace.replace('.','/'));
        }

    /** Returns the preferences object associated with per-module preferences for the application. */
    public static Preferences getAppPreferences(String module, String namespace)
        {
        return Preferences.userRoot().node(EDITOR_PREFERENCES + "/" + module.replace('.','/') + "/" + namespace.replace('.','/')); 
        }
    
    /** Removes global preferences. */
    public static boolean removeGlobalPreferences(String namespace)
        {
        try
            {
            getGlobalPreferences(namespace).removeNode();
            return true;
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return false;
            }
        }

    /** Removes per-module preferences. */
    public static boolean removeAppPreferences(String module, String namespace)
        {
        try
            {
            getAppPreferences(module, namespace).removeNode();
            return true;
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return false;
            }
        }

    /** Flushes out and saves preferences to disk. */
    public static boolean save(Preferences prefs)
        {
        try 
            {
            prefs.sync();
            return true;
            }
        catch (Exception ex)
            {
            ex.printStackTrace();
            return false;
            }
        }

    /** Given a preferences path X for a given module, sets X to have the given value.. 
        Also sets the global path X to the value. */
    public static void setLastX(String value, String x, String moduleName)
        {
        if (moduleName != null)
            {
            java.util.prefs.Preferences app_p = Prefs.getAppPreferences(moduleName, "data");
            app_p.put(x, value);
            Prefs.save(app_p);
            }
        }
        
    /** Given a global preferences path X, sets X to have the given value. */
    public static final void setLastX(String value, String x)
        {
        java.util.prefs.Preferences global_p = Prefs.getGlobalPreferences("data");
        global_p.put(x, value);
        Prefs.save(global_p);
        }
        
    /** Given a preferences path X for a given module, returns the value stored in X.
        If there is no such value, then returns null. */
    public static final String getLastX(String x, String moduleName)
        {
        if (moduleName != null)
            {
            String prop = System.getProperty("+" + moduleName + ":" + x);
            if (prop != null)
                {
                setLastX(prop, x, moduleName);
                }
            prop = System.getProperty(moduleName + ":" + x);
            if (prop != null)
                {
                return prop;
                }
            else return Prefs.getAppPreferences(moduleName, "data").get(x, null);
            }
        else return null;         
        }
    
    /** Given a preferences path X for a given module, returns the value stored in X.
        If there is no such value, then returns the value stored in X in the globals.
        If there again is no such value, returns null.  Typically this method is called by a
        a cover function */
    public static final String getLastX(String x)
        {
        String prop = System.getProperty("+" + x);
        if (prop != null)
            {
            setLastX(prop, x);
            }
        prop = System.getProperty(x);
        if (prop != null)
            {
            return prop;
            }
        else return Prefs.getGlobalPreferences("data").get(x, null);
        }

    /** Sets the last directory used by load, save, or save as */
    public void setLastDirectory(String path) { setLastX(path, "LastDirectory"); }
    
    /** Returns the last directory used by load, save, or save as */
    public String getLastDirectory() { return getLastX("LastDirectory"); }
    


    // various cover methods for preferences used only by Output to store various synth parameters

    public static void setLastNumVoices(int voices) { setLastX("" + voices, "Voices"); }
    public static int getLastNumVoices() 
        { 
        String s = getLastX("Voices"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Output.DEFAULT_NUM_VOICES;                       
        }

    public static void setLastNumVoicesPerThread(int voicesPerThread) { setLastX("" + voicesPerThread, "VoicesPerThread"); }
    public static int getLastNumVoicesPerThread() 
        { 
        String s = getLastX("VoicesPerThread"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Output.DEFAULT_NUM_VOICES_PER_THREAD;                            
        }

    public static void setLastNumOutputsPerThread(int outputsPerThread) { setLastX("" + outputsPerThread, "OutputsPerThread"); }
    public static int getLastNumOutputsPerThread() 
        { 
        String s = getLastX("OutputsPerThread"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Output.DEFAULT_NUM_OUTPUTS_PER_THREAD;                            
        }

    public static void setLastBufferSize(int bufferSize) { setLastX("" + bufferSize, "BufferSize"); }
    public static int getLastBufferSize() 
        { 
        String s = getLastX("BufferSize"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Output.DEFAULT_BUFFER_SIZE;                      
        }

/*
  public static void setLastSkip(int skip) { setLastX("" + skip, "Skip"); }
  public static int getLastSkip() 
  { 
  String s = getLastX("Skip"); 
  try
  {
  if (s != null)
  return Integer.parseInt(s);
  }
  catch (NumberFormatException e) { }
  return Output.SKIP;                     
  }
*/

    public static void setLastBendOctave(int bendOctave) { setLastX("" + bendOctave, "BendOctave"); }
    public static int getLastBendOctave() 
        { 
        String s = getLastX("BendOctave"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Input.DEFAULT_BEND_OCTAVE;                       
        }

    public static void setLastMidiDevice(String midiDevice) { setLastX("" + midiDevice, "MidiDevice"); }
    public static String getLastMidiDevice() 
        { 
        String s = getLastX("MidiDevice"); 
        if (s != null) return s;
        return Midi.NO_DEVICE;
        }

    public static void setLastMidiDevice2(String midiDevice) { setLastX("" + midiDevice, "MidiDevice2"); }
    public static String getLastMidiDevice2() 
        { 
        String s = getLastX("MidiDevice2"); 
        if (s != null) return s;
        return Midi.NO_DEVICE;
        }

    public static void setLastChannel(int channel) { setLastX("" + channel, "Channel"); }
    public static int getLastChannel() 
        { 
        String s = getLastX("Channel"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Input.CHANNEL_OMNI;                       
        }

    public static void setLastNumMPEChannels(int num) { setLastX("" + num, "NumMPEChannels"); }
    public static int getLastNumMPEChannels() 
        { 
        String s = getLastX("NumMPEChannels"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Input.DEFAULT_NUM_MPE_CHANNELS;                       
        }

    public static void setLastNumPartials(int num) { setLastX("" + num, "NumPartials"); }
    public static int getLastNumPartials() 
        { 
        String s = getLastX("NumPartials"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return Unit.DEFAULT_NUM_PARTIALS;                       
        }

    public static void setShowsDisplays(boolean val) { setLastX("" + val, "ShowDisplays"); }
    public static boolean getShowsDisplays() 
        { 
        String s = getLastX("ShowDisplays"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    public static void setWaterfallDisplay(boolean val) { setLastX("" + val, "WaterfallDisplay"); }
    public static boolean getWaterfallDisplay() 
        { 
        String s = getLastX("WaterfallDisplay"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    public static void setLogAxisDisplay(boolean val) { setLastX("" + val, "LogAxisDisplay"); }
    public static boolean getLogAxisDisplay() 
        { 
        String s = getLastX("LogAxisDisplay"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    public static void setMaxDisplayedHarmonic(int num) { setLastX("" + num, "MaxDisplayedHarmonic"); }
    public static int getMaxDisplayedHarmonic() 
        { 
        String s = getLastX("MaxDisplayedHarmonic"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return AppMenu.DEFAULT_MAX_DISPLAYED_HARMONIC;                       
        }

    public static void setMinDisplayedHarmonic(int num) { setLastX("" + num, "MinDisplayedHarmonic"); }
    public static int getMinDisplayedHarmonic() 
        { 
        String s = getLastX("MinDisplayedHarmonic"); 
        try
            {
            if (s != null)
                return Integer.parseInt(s);
            }
        catch (NumberFormatException e) { }
        return AppMenu.DEFAULT_MIN_DISPLAYED_HARMONIC;                       
        }

    public static void setLastOneVoice(boolean val) { setLastX("" + val, "OneVoice"); }
    public static boolean getLastOneVoice() 
        { 
        String s = getLastX("OneVoice"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    /** Parses a boolean from a string, returning the default if failure occurs */
    public static boolean parseBoolean(String s, boolean def)
        {
        if (def == false)
            {
            return Boolean.parseBoolean(s);  // easy case
            }
        else
            {
            return (s == null || !s.equalsIgnoreCase("false"));
            }
        }

    public static void setVelocitySensitive(boolean val) { setLastX("" + val, "VelocitySensitive"); }
    public static boolean getVelocitySensitive() 
        { 
        String s = getLastX("VelocitySensitive"); 
        return parseBoolean(s, true);                   // default is TRUE
        }


    public static void setRespondsToBend(boolean val) { setLastX("" + val, "RespondsToBend"); }
    public static boolean getRespondsToBend() 
        { 
        String s = getLastX("RespondsToBend"); 
        return parseBoolean(s, true);                   // default is TRUE
        }


    public static void setLastMIDISync(boolean val) { setLastX("" + val, "MIDISync"); }
    public static boolean getLastMIDISync() 
        { 
        String s = getLastX("MIDISync"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    public static void setLastAddModulesAfter(boolean val) { setLastX("" + val, "AddModulesAfter"); }
    public static boolean getLastAddModulesAfter() 
        { 
        String s = getLastX("AddModulesAfter"); 
        return Boolean.parseBoolean(s);         // default is FALSE
        }


    public static void setLastAudioDevice(String audioDevice) { setLastX("" + audioDevice, "AudioDevice"); }
    public static String getLastAudioDevice() 
        { 
        return getLastX("AudioDevice");         // null is default
        }

    public static void setLastMasterGain(double gain) { setLastX("" + gain, "MasterGain"); }
    public static double getLastMasterGain() 
        { 
        String s = getLastX("MasterGain"); 
        try
            {
            if (s != null)
            	{
            	double d = Double.parseDouble(s);
            	if (d < 0 || d > Output.MAX_MASTER_GAIN || d != d)
            		return Output.DEFAULT_MASTER_GAIN;
            	else
            		return d; 
            	}
            }
        catch (NumberFormatException e) { }
        return Output.DEFAULT_MASTER_GAIN;                       
        }


    }

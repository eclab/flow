// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import javax.sound.sampled.*;
import java.util.*;
import org.json.*;

/**
   The object which represents a single voice in the synthesizer.  This sound holds onto a list
   of modules (Modulations and Units) which have been registered with it.  In Sound's constructor
   it will similarly register itself with the Output.
*/

public class Sound
    {
    /** Default note setting (220Hz) returned by getNote() */
    public static final double DEFAULT_NOTE = 220;
    /** Default bend setting (1.0 -- none) returned by getBend() */
    public static final double DEFAULT_BEND = 1;
    /** Default velocity setting (0.0 - no velocity) returned by getVelocity() */
    public static final double DEFAULT_VELOCITY = 1.0; // this is NOT 0 so we can hear sounds without playing notes for testing purposes
    /** Default velocity setting (0.0 - no velocity) returned by getVelocity() */
    public static final double DEFAULT_RELEASE_VELOCITY = 0.5;
    /** Default aftertouch setting (0.0 -- no aftertouch) returned by getAftertouch() */
    public static final double DEFAULT_AFTERTOUCH = 0.0;
    
    // The Sound's output
    Output output;
    // Random number generator: each Sound has a unique random number generator
    // so they can be called in a threadsafe way
    Random random;
    // The elements (Modulations, Units) associated with this Sound
    ArrayList<Modulation> elements = new ArrayList<Modulation>();
    // The unit which will be queried to indicate the emitted partials at the end
    Unit emits;
    
    // What number Sound am I in the Output?
    volatile int index;
    
    // The current MIDI Note
    volatile int midiNote;
    // The current note (in Hz)
    volatile double note = DEFAULT_NOTE;
    // The current pitch bend (in multiples of the note)
    volatile double bend = DEFAULT_BEND;
    // The current pitch of the sound: equals the note times the bend
    volatile double pitch = DEFAULT_NOTE * DEFAULT_BEND;
    // The current aftertouch
    volatile double aftertouch = DEFAULT_AFTERTOUCH;
    // The current note velocity
    volatile double velocity = DEFAULT_VELOCITY;
    // The current note release velocity
    volatile double releaseVelocity = DEFAULT_RELEASE_VELOCITY;
    // Current cc values
    byte[] cc = new byte[128];
    // The channel assigned to this Sound.  Normally this should be exactly the same as
    // Input.channel, but MPE will assign this specially.  We'll assign this during
    // voice allocation in Input.java 
    volatile int channel = Input.CHANNEL_NONE;  // we need to assign it *something* initially...

    public Sound(Output output)
        {
        this.output = output;
        random = output.getNewRandom();
        output.register(this);
        }

    /** Returns the number of Modulations / Units registered with this Sound.
        This does not include Constants.   */
    public int getChannel() { return channel; }
    
    /** Returns a given registered Modulation / Unit */
    public void setChannel(int channel) { this.channel = channel; }
    

    /** Returns the Sound's index number in the Output */
    public int getIndex() { return index; }

    /** Returns the Sound's random number generator */
    public Random getRandom() { return random; }
    
    /** Returns the Sound's owner Output */
    public Output getOutput() { return output; }

    /** Adds a Modulation / Unit to the end of the registry. */
    public void register(Modulation mod) { elements.add(mod); }

    /** Returns the number of Modulations / Units registered with this Sound.
        This does not include Constants.   */
    public int getNumRegistered() { return elements.size(); }
    
    /** Returns a given registered Modulation / Unit */
    public Modulation getRegistered(int i) { return elements.get(i); }
    
    /** Returns all the given registered Modulations / Units */
    public ArrayList<Modulation> getRegistered() { return elements; }
    
    /** Removes a Modulation / Unit */
    public Modulation removeRegistered(int i) { return elements.remove(i); }
    
    /** Adds a Modulation / Unit at position i*/
    public void addRegistered(int i, Modulation modulation) { elements.add(i, modulation); }
    
    /** Value returned by findRegistered if it can't find a given Modulation / Unit in its registry */
    public static final int NOT_FOUND = -1;
    
    /** Returns the index of a Modulation or Unit in the registry, else NOT_FOUND */
    public int findRegistered(Modulation m)
        {
        for(int i = 0; i < elements.size(); i++)
            if (elements.get(i) == m)
                return i;
        return NOT_FOUND;
        }
        
    /** Sets the Pitch Bend.  The Bend is multiplied against the current note to determine the current pitch. */ 
    public void setBend(double bend) { this.bend = bend; if (output.getInput().getRespondsToBend()) this.pitch = note * bend; }
    /** Returns the Pitch Bend.  The Bend is multiplied against the current note to determine the current pitch. */ 
    public double getBend() { return bend; }
     
    /** Sets the velocity (0.0 .. 1.0)*/   
    public void setVelocity(double velocity) { this.velocity = velocity; }
    /** Returns the velocity (0.0 .. 1.0) */   
    public double getVelocity() { return velocity; }

    /** Sets the release velocity (0.0 .. 1.0)*/   
    public void setReleaseVelocity(double releaseVelocity) { this.releaseVelocity = releaseVelocity; }
    /** Returns the velocity (0.0 .. 1.0) */   
    public double getReleaseVelocity() { return releaseVelocity; }

    /** Sets the MIDI Note 0..127 */   
    public void setMIDINote(int midiNote) { this.midiNote = midiNote; }
    /** Returns the MIDI Note */   
    public int getMIDINote() { return midiNote; }
        
    /** Sets the Note (in Hz).  The Bend is multiplied against the current note to determine the current pitch.  */   
    public void setNote(double note) { this.note = note; if (output.getInput().getRespondsToBend()) this.pitch = note * bend; }
    /** Returns the Note (in Hz).  The Bend is multiplied against the current note to determine the current pitch.  */   
    public double getNote() { return note; }
    
    /** Sets the current value for a given CC */
    public void setCC(int num, int val) { synchronized(cc) { cc[num] = (byte)val; } }
    /** Returns the current value for a given CC */
    public int getCC(int num) { synchronized(cc) { return cc[num]; } }
        
    /** Sets the Aftertouch (0.0 ... 1.0)*/
    public void setAftertouch(double val) { aftertouch = val; }
    /** Returns the Aftertouch (0.0 ... 1.0)*/
    public double getAftertouch() { return aftertouch; }
                
    /** Returns the pitch.  The pitch is simply the note times the bend. */
    public double getPitch() { return pitch; }

    /** Sets the Unit responsible for emitting the final partials. */
    public void setEmits(Unit unit) { this.emits = unit; }
    /** Returns the Unit responsible for emitting the final partials. */
    public Unit getEmits() { return this.emits; }
    
    /** Causes all Modulations / Units to have their go() methods called, in order. */
    public void go()
        {
        int len = elements.size();
        for(int i = 0; i < len; i++)
            {
            elements.get(i).go();
            }
        }

    /** Causes all Modulations / Units to have their gate() methods called, in order.
        gate() informs a Modulation / Unit that the user has pressed the key. */
    public void gate()
        {
        int len = elements.size();
        for(int i = 0; i < len; i++)
            elements.get(i).gate();
        }

    /** Causes all Modulations / Units to have their release() methods called, in order.
        release() informs a Modulation / Unit that the user has released the key. */
    public void release()
        { 
        int len = elements.size();
        for(int i = 0; i < len; i++)
            elements.get(i).release();
        }

    /** Resets all Modulations / Units to their initial positions. */
    public void reset()
        {
        int len = elements.size();
        for(int i = 0; i < len; i++)
            elements.get(i).reset();
        }

    /** Informs all Modulations / Units that a clock reset, or MIDI CLOCK START, occurred. */
    public void restart()
        {
        int len = elements.size();
        for(int i = 0; i < len; i++)
            elements.get(i).restart();
        }






    ///// JSON Serialization

    /** Stores all the modules to a JSONArray, stored in the given object. */
    public void saveModules(JSONObject obj) throws JSONException
        {
        JSONArray array = new JSONArray();

        int id = 0;             
        int len = elements.size();
        for(int i = 0; i < len; i++)
            elements.get(i).setID("a" + (id++));

        for(int i = 0; i < len; i++)
            array.put(elements.get(i).save());
                
        obj.put("modules", array);
        }
        
    /** Stores the patch name to the given object. */
    public static void saveName(String name, JSONObject obj) throws JSONException
        {
        obj.put("name", name);
        }
                
    /** Loads the patch name from the given object. */
    public static String loadName(JSONObject obj) throws JSONException
        {
        return obj.optString("name", "Untitled");
        }
                
    /** Stores the patch version to the given object. */
    public static void savePatchVersion(String name, JSONObject obj) throws JSONException
        {
        obj.put("v", name);
        }
                
    /** Loads the patch version from the given object. */
    public static String loadPatchVersion(JSONObject obj) throws JSONException
        {
        return obj.optString("v", "");
        }
                
    /** Stores the patch version to the given object. */
    public static void savePatchDate(String name, JSONObject obj) throws JSONException
        {
        obj.put("on", name);
        }
                
    /** Loads the patch version from the given object. */
    public static String loadPatchDate(JSONObject obj) throws JSONException
        {
        return obj.optString("on", "");
        }
                
    /** Stores the patch version to the given object. */
    public static void savePatchAuthor(String name, JSONObject obj) throws JSONException
        {
        obj.put("by", name);
        }
                
    /** Loads the patch version from the given object. */
    public static String loadPatchAuthor(JSONObject obj) throws JSONException
        {
        return obj.optString("by", "");
        }
                
    /** Stores the patch version to the given object. */
    public static void savePatchInfo(String name, JSONObject obj) throws JSONException
        {
        obj.put("info", name);
        }
                
    /** Loads the patch version from the given object. */
    public static String loadPatchInfo(JSONObject obj) throws JSONException
        {
        return obj.optString("info", "");
        }
                
    /** Stores the patch version to the given object. */
    public static void saveFlowVersion(JSONObject obj) throws JSONException
        {
        obj.put("flow", Flow.VERSION);
        }
        
    /** Loads the patch version from the given object. */
    public static int loadFlowVersion(JSONObject obj) throws JSONException
        {
        int version = obj.optInt("flow", -1);
        if (version == -1)      // it's old
            return obj.getInt("v");
        else return version;
        }
                
    /** Loads all the modules from the given JSONObject, and returns them.
        Does not registers the modules: they are created with a null Sound. */
    public static Modulation[] loadModules(JSONObject obj, int patchVersion) throws Exception
        {
        JSONArray array = obj.getJSONArray("modules");
        HashMap<String, Modulation> ids = new HashMap<>();
        int len = array.length();
        Modulation[] result = new Modulation[len];

        for(int i = 0; i < len; i++)
            {
            JSONObject modobj = array.getJSONObject(i);
            Modulation mod = (Modulation)(Class.forName(modobj.getString("class")).getConstructor(Sound.class).newInstance((Sound)null));
            mod.setID(modobj.getString("id"));
            ids.put(mod.getID(), mod);
            result[i] = mod;
            }

        for(int i = 0; i < len; i++)
            {
            JSONObject modobj = array.getJSONObject(i);
            result[i].preprocessLoad(modobj.getInt("v"), patchVersion);
            }
                        
        for(int i = 0; i < len; i++)
            {
            result[i].load(array.getJSONObject(i), ids, patchVersion);
            }

        for(int i = 0; i < len; i++)
            {
            JSONObject modobj = array.getJSONObject(i);
            result[i].postprocessLoad(modobj.getInt("v"), patchVersion);
            }
                        
        return result;
        }

    }

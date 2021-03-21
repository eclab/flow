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
    
    int noteCounter = -1;
    public int getNoteCounter() { return noteCounter; }
    public void incrementNoteCounter() { noteCounter++; }
    
    int allocation = -1;
    public void setAllocation(int allocation) { this.allocation = allocation; }
    public int getAllocation() { return allocation; }
    
    int group = Output.PRIMARY_GROUP;
    public int getGroup() { return group; }
    public void setGroup(int g) { group = g; }
    
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
    // The channel assigned to this Sound.  Normally this should be exactly the same as
    // Input.channel, but MPE will assign this specially.  We'll assign this during
    // voice allocation in Input.java 
    volatile int channel = Input.CHANNEL_OMNI;  // we need to assign it *something* initially...

	volatile boolean requestReset = false;

    public Sound(Output output)
        {
        this.output = output;
        random = output.getNewRandom();
        output.register(this);
        }
        
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
    public void setNote(double note) { this.note = note; this.pitch = note * (output.getInput().getRespondsToBend() ? bend : 1); }
    /** Returns the Note (in Hz).  The Bend is multiplied against the current note to determine the current pitch.  */   
    public double getNote() { return note; }
    
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
        requestReset = true;
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
        requestReset = true;
        }

    /** Resets all Modulations / Units to their initial positions. */
    public void resetPartialPhases()
        {
        output.positions[index] = new double[Unit.NUM_PARTIALS];        // FIXME: maybe we should bzero
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
        
    /** Loads all the modules from the given JSONObject, and returns them.
        Does not register the modules: they are created with a null Sound. */
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

    /** Stores the patch name to the given object. */
    public static void saveName(String name, JSONObject obj) throws JSONException
        {
        if (name == null) name = "";
        obj.put("name", name);
        }
        
    public static final String UNTITLED_PATCH_NAME = "Untitled";
           
    /** Loads the patch name from the given object. */
    public static String loadName(JSONObject obj) throws JSONException
        {
        return obj.optString("name", UNTITLED_PATCH_NAME);
        }
                
    /** Stores the patch version to the given object. */
    public static void savePatchVersion(String name, JSONObject obj) throws JSONException
        {
        if (name == null) name = "";
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
        if (name == null) name = "";
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
        if (name == null) name = "";
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
        if (name == null) name = "";
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
                
    /** Loads all the groups EXCEPT THE FIRST GROUP, if any, from a JSONObject, and returns the number of NEW groups. */
    public static int loadGroups(Group[] groups, JSONObject obj) throws JSONException
        {
        JSONArray array = null;
        
        try { array = obj.getJSONArray("sub"); }                // no groups at all
        catch (Exception e) { return 0; }

        int len = array.length();
        int i;
        for( i = 0 ; i < len; i++)              // we load all but the #0
            {
            JSONObject patch = array.getJSONObject(i);
            try { groups[i + 1].setPatch(patch); }
            catch (Exception e) { System.err.println("Output.loadGroups() WARNING: missing or invalid patch " + (i + 1)); break; };
            // try { groups[i + 1].setPatchName(Sound.loadName(patch)); }
            // catch (Exception e) { System.err.println("Output.loadGroups() WARNING: missing or invalid patch name " + (i + 1)); break; };
            try { groups[i + 1].setGain(patch.getDouble("gain")); }
            catch (Exception e) { System.err.println("Output.loadGroups() WARNING: missing or invalid gain " + (i + 1)); break; };
            try { groups[i + 1].setPan(patch.getDouble("pan")); }
            catch (Exception e) { System.err.println("Output.loadGroups() WARNING: missing or invalid pan " + (i + 1)); break; };
            try { groups[i + 1].setNumRequestedSounds(patch.getInt("voices")); }
            catch (Exception e) { System.err.println("Output.loadGroups() WARNING: missing or invalid numRequestedSounds " + (i + 1)); break; };
            try { groups[i + 1].setChannel(patch.getInt("midi") - 1); }
            catch (org.json.JSONException e) { groups[i + 1].setChannel(Input.CHANNEL_NONE); }                  // this isn't in the documentation
            catch (NullPointerException e) { groups[i + 1].setChannel(Input.CHANNEL_NONE); }            // this might not exist if there's no current midi channel
            catch (ClassCastException e) {System.err.println("Output.loadGroups() WARNING: invalid midi " + (i + 1)); break; }
            try {  groups[i + 1].setBothNotes(patch.getInt("note")); }
            catch (org.json.JSONException e) { groups[i + 1].setBothNotes(0, 127); }    // this isn't in the documentation
            catch (NullPointerException e) { groups[i + 1].setBothNotes(0, 127); }              // this might not exist if there's no current midi channel
            catch (ClassCastException e) {System.err.println("Output.loadGroups() WARNING: invalid midi " + (i + 1)); break; }
            }
        return i;
        }

    /** Stores all the groups EXCEPT THE FIRST GROUP, if any, to a JSONArray, stored in the given object. */
    public static void saveGroups(Group[] group, int numPatches, JSONObject obj) throws JSONException
        {
        if (numPatches > 1)
            {
            JSONArray array = new JSONArray();

            for(int i = 1; i < numPatches; i++)             // note 1
                {
                JSONObject patch = group[i].getPatch();
                if (group[i].getChannel() >= 0) 
                    { 
                    patch.put("midi", group[i].getChannel() + 1);
                    };
                if (group[i].getMinNote() == group[i].getMaxNote() )            // for now we assume that != means full range
                    { 
                    patch.put("note", group[i].getMinNote());
                    };
                patch.put("voices", group[i].getNumRequestedSounds());
                patch.put("gain", group[i].getGain());
                patch.put("pan", group[i].getPan());
                array.put(patch);
                }
                                
            obj.put("sub", array);
            }
        else
            {
            // System.err.println("Sound.saveGroups() WARNING: there aren't any groups to save");
            }
        }
        
    }

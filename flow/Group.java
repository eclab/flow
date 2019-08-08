// Copyright 2019 by George Mason University
// Licensed under the Apache 2.0 License


package flow;
import org.json.*;

/**
   The object which represents a patch or subpatch and stores relevant parameters for it.  
   Each Sound in the synthesizer belongs to exactly one Group.
*/

public class Group
    {
    public static final double DEFAULT_GAIN = 1.0;
    public static final String EMPTY_JSON = "{ \"flow\":" + Flow.VERSION + ", modules: [ ] }";

    int channel = Input.CHANNEL_NONE;
    int minNote = 0;
    int maxNote = 127;
    int numRequestedSounds = 0;
    JSONObject patch = new JSONObject(EMPTY_JSON);
    double gain = DEFAULT_GAIN;
        
    /** Returns the group's current channel.  This can be any of
    	Input.CHANNEL_NONE, or 0 ... 15.  If this group is the Primary group,
    	then the channel can also be Input.CHANNEL_OMNI, Input.CHANNEL_LOWER_ZONE,
    	or Input.CHANNEL_UPPER_ZONE */
    public int getChannel() { return channel; }

    /** Sets the group's current channel.  This can be any of
    	Input.CHANNEL_NONE, or 0 ... 15.  If this group is the Primary group,
    	then the channel can also be Input.CHANNEL_OMNI, Input.CHANNEL_LOWER_ZONE,
    	or Input.CHANNEL_UPPER_ZONE */
    public void setChannel(int c) 
        { 
        if (c < Input.CHANNEL_NONE) 
            c = Input.CHANNEL_NONE; 
        else if (c >= Input.NUM_MIDI_CHANNELS)
            c = Input.CHANNEL_NONE;
        channel = c; 
        }

	/** Returns the minimum note in the Group's note range.  Sounds will not respond
		to notes below this value.  This can any value 0 ... 127 */         
    public int getMinNote() { return minNote; }

	/** Sets the minimum note in the Group's note range.  Sounds will note respond
		to notes below this value.  This can any value 0 ... 127.  If the
		passed in value is greater than the max note, then it is set to the max note. */         
    public void setMinNote(int n) 
        { 
        minNote = n; 
        if (minNote > 127) minNote = 127;
        if (minNote < 0) minNote = 0;
        if (minNote > maxNote)
            minNote = maxNote;
        }

	/** Returns the maximum note in the Group's note range.  Sounds will not respond
		to notes above this value.  This can any value 0 ... 127 */         
    public int getMaxNote() { return maxNote; }

	/** Sets the maximum note in the Group's note range.  Sounds will not respond
		to notes below this value.  This can any value 0 ... 127.  If the
		passed in value is less than the min note, then it is set to the min note. */         
    public void setMaxNote(int n) 
        { 
        maxNote = n; 
        if (maxNote > 127) maxNote = 127;
        if (maxNote < 0) maxNote = 0;
        if (maxNote < minNote)
            maxNote = minNote;
        }
        
    /** Sets both the minimum and maximum note to the given values.
    	If min > max, then max is set to min */
    public void setBothNotes(int min, int max)
    	{
    	if (min > 127) min = 127;
    	if (min < 0) min = 0;
    	if (max > 127) max = 127;
    	if (max < 0) max = 0;
    	if (max < min) max = min;
    	minNote = min;
    	maxNote = max;
    	}
    
    /** Sets both the minimum and maximum note to the given value. */
    public void setBothNotes(int n)
    	{
    	setBothNotes(n, n);
    	}
    
    /** Returns true if the note is between the min and max notes, inclusive. */
    public boolean isNoteInRange(int note) { return note >= minNote && note <= maxNote; }
    
    /** Returns the number of requested sounds. */
    public int getNumRequestedSounds() { return numRequestedSounds; }

    /** Sets the number of requested sounds. */
    public void setNumRequestedSounds(int n) { if (n < 0) n = 0; numRequestedSounds = n; }
    
    /** Returns the group's patch. */
    public JSONObject getPatch() { return patch; }

    /** Sets the group's patch. */
    public void setPatch(JSONObject p) 
        { 
        if (p == null)
            p = new JSONObject(EMPTY_JSON);
        patch = p; 
        }

    /** Returns the group's patch name. */
    public String getPatchName() 
    	{
    	try
    		{
    		return Sound.loadName(getPatch());
    		}
    	catch (Exception e)		// maybe we don't have a patch?
    		{
            System.err.println("Group.getPatchName() WARNING: no patch ");
    		return Sound.UNTITLED_PATCH_NAME;
    		}
    	}

    public void setPatchName(String p) 
        { 
        try
        	{
        	Sound.saveName(p, getPatch());
    		}
    	catch (Exception e)		// maybe we don't have a patch?
    		{
            System.err.println("Group.setPatchName() WARNING: no patch ");
    		}
        }

    /** Returns the group's current gain value (normally 0...1). */
    public double getGain() { return gain; }
    
    /** Sets the group's current gain value (normally 0...1). */
    public void setGain(double g) { if (g < 0) g = 0; gain = g; }
    }
        

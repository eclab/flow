// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Input handles MIDI Input.  It is owned by Output, and it in turn owns Midi, the low-level wrapper for
 * the Java midi subsystem.
 */
 

/*
  ALLOCATION RULES
        
  GROUP MIDI ASSIGNMENT
  Groups are assigned exactly as they are requested.  Sounds have their channels set.

  SOUND MIDI ASSIGNMENT
  This occurrs on Note On:
  - If the sound is the PRIMARY GROUP
  - If the main channel is LOWER MPE
  The sound is assigned according to our MPE rules
  - Else if the main channel is UPPER MPE
  The sound is assigned according to our MPE rules
  - Else
  The sound is assigned to the main channel (including being set to OMNI)
  - If the sound is some other group
  - The sound is assigned to the group's channel
*/


public class Input
    {
    Object lock = new Object[0];
    Midi midi;
    MidiClock midiClock;
    Output output;
    
    public Midi getMidi()
        {
        return midi;
        }
    
    public MidiClock getMidiClock()
        {
        return midiClock;
        }
    
    public Output getOutput()
        {
        return output;
        }



    public Input(Output output)
        {
        this.output = output;
        
        for(int i = 0; i < channel.length; i++)
            channel[i] = CHANNEL_NONE;
        
        bendOctave = Prefs.getLastBendOctave();
        if (bendOctave < 0 || bendOctave >= 8)
            bendOctave = DEFAULT_BEND_OCTAVE;
                
        for(int chan = 0; chan < NUM_MIDI_CHANNELS; chan++)
            {
            for (int i = 0; i < NUM_CC; i++)
                cc[chan][i] = UNSPECIFIED;
            }
        
        for (int i = 0; i < NUM_NRPN; i++)
            nrpn[i] = UNSPECIFIED;
        
        midi = new Midi(this);
        midiClock = new MidiClock(this);

        ArrayList<Midi.MidiDeviceWrapper> devices = getDevices();
        setupMIDI(new int[] { CHANNEL_OMNI }, DEFAULT_NUM_MPE_CHANNELS, devices.get(0));

        // by default Output has a single group, so we'll keep that
        // set up MIDI for first time
        ArrayList<Midi.MidiDeviceWrapper> devs = getDevices();
        Midi.MidiDeviceWrapper wrap = devs.get(0);
        String midiDevice = Prefs.getLastMidiDevice();
        for(int i = 0; i < devs.size(); i++)
            {
            if (devs.get(i).toString().equals(midiDevice))
                { wrap = devs.get(i); break; }
            }
            
        // we assume we have only one group at startp time
        setupMIDI(new int[] { Prefs.getLastChannel() }, Prefs.getLastNumMPEChannels(), wrap);
        }
    
    // Output calls this to add a Sound to the Input (it's added to the notesOff list)
    void addSound(Sound sound)
        {
        synchronized (lock)
            {
            notesOff.add(sound);
            }
        }
    





    ///// MIDI DEVICES AND SETUP

    
    Midi.MidiDeviceWrapper currentWrapper;
    
    public void rebuildMIDI()
        {
        setupMIDI((int[])(channel.clone()), numMPEChannels, currentWrapper);            // rebuilds from the same data
        }
    
    /**
     * Sets MIDI to the new device wrapper and channel.
     */
    public void setupMIDI(int[] channels, int numMPEChannels, Midi.MidiDeviceWrapper midiDeviceWrapper)
        {
        // set up device
        midi.setInReceiver(midiDeviceWrapper);
        currentWrapper = midiDeviceWrapper;

        // assign channels to groups
        for(int i = 0; i < channels.length; i++)        // may not be the same length as what's passed in
            channel[i] = CHANNEL_NONE;
        System.arraycopy(channels, 0, channel, 0, channels.length);
        
        // Assign channels for primary group
        int primaryChannel = channel[Output.PRIMARY_GROUP];
        this.numMPEChannels = numMPEChannels;
        for(int i = 0; i < NUM_MIDI_CHANNELS; i++)
            {
            group[i] = Output.NO_GROUP;
            mpeChannel[i] = false;
            }
                
        if (primaryChannel == CHANNEL_NONE)
            {
            // do nothing
            }
        else if (primaryChannel == CHANNEL_OMNI)
            {
            for(int i = 0; i < NUM_MIDI_CHANNELS; i++)
                group[i] = Output.PRIMARY_GROUP;
            }
        else if (primaryChannel == CHANNEL_LOWER_ZONE)
            {
            for(int i = 0; i < numMPEChannels + 1; i++)
                {
                group[i] = Output.PRIMARY_GROUP;
                mpeChannel[i] = true;
                }
            }
        else if (primaryChannel == CHANNEL_UPPER_ZONE)
            {
            for(int i = 0; i < numMPEChannels + 1; i++)
                {
                group[NUM_MIDI_CHANNELS - i - 1] = Output.PRIMARY_GROUP;
                mpeChannel[NUM_MIDI_CHANNELS - i - 1] = true;
                }
            }
        else
            {
            group[primaryChannel] = Output.PRIMARY_GROUP;
            }

        // assign channels for other groups and build
        for(int i = Output.PRIMARY_GROUP + 1; i < output.getNumGroups(); i++)
            {
            if (channel[i] >= 0)
                {
                group[channel[i]] = i;
                mpeChannel[channel[i]] = false;
                }
            }
        
        // clear bend
        omniBend = NO_BEND;
        mpeBend = NO_BEND;
        for(int i = 0; i < NUM_MIDI_CHANNELS; i++)
            globalBend[i] = NO_BEND;
        }
    
    
    /**
     * Returns all available MIDI device wrappers.
     */
    public ArrayList<Midi.MidiDeviceWrapper> getDevices()
        {
        ArrayList<Midi.MidiDeviceWrapper> devices = (ArrayList<Midi.MidiDeviceWrapper>) (midi.getInDevices().clone());
        devices.add(0, new Midi.MidiDeviceWrapper(null));  // a "None"
        return devices;
        }
    
    /**
     * Returns the current MidiDevice
     */
    public Midi.MidiDeviceWrapper getMidiDevice()
        {
        return currentWrapper;
        }
    
    
    



    ///// CHANNELS AND MPE

    public static final int NUM_MIDI_CHANNELS = 16;
    public static final int NUM_SPECIAL_CHANNELS = 3;  // Omni, Lower, Upper.  This is used by Rack.java to offset its pop-up menu
    public static final int CHANNEL_NONE = -4;
    public static final int CHANNEL_LOWER_ZONE = -3;
    public static final int CHANNEL_UPPER_ZONE = -2;
    public static final int CHANNEL_OMNI = -1;
    public static final int CHANNEL_GLOBAL_FOR_LOWER_ZONE = 0;
    public static final int CHANNEL_GLOBAL_FOR_UPPER_ZONE = 15;
    public static final int DEFAULT_NUM_MPE_CHANNELS = 14;
    public static final int MPE_CONFIGURATION_RPN_NUMBER = 6;

        
    // Each group can have a channel, or CHANNEL_NONE.  group[Output.PRIMARY_GROUP] (that is, group[0]) can also have CHANNEL_OMNI, CHANNEL_LOWER_ZONE or CHANNEL_UPPER_ZONE
    int channel[/*GROUP*/] = new int[Output.MAX_GROUPS];
    /// each channel is assigned to a unique GROUP, or to null
    int group[/*CHANNEL*/] = new int[NUM_MIDI_CHANNELS];
        
    // The number of mpe channels specified by the user for the primary group
    int numMPEChannels = DEFAULT_NUM_MPE_CHANNELS;
    
    // Those channels that are actually assigned to be MPE channels in the primary group.
    // The primary group could have requested some, but was overridden by sub-patch
    // MIDI assignments, so we have to watch for that.
    boolean[] mpeChannel = new boolean[NUM_MIDI_CHANNELS];
        
    /**
     * returns true if we are in MPE mode
     **/
    public boolean isMPE()
        {
        return channel[Output.PRIMARY_GROUP] == CHANNEL_LOWER_ZONE || channel[Output.PRIMARY_GROUP] == CHANNEL_UPPER_ZONE;
        }

    public boolean isOMNI()
        {
        return channel[Output.PRIMARY_GROUP] == CHANNEL_OMNI;
        }
            
    public int getGroup(int channel)
        {
        return group[channel];
        }
        
    public int getChannel(int group)
        {
        return channel[group];
        }
    
    public int[] getChannels()
        {
        return channel;
        }
    
    public void setChannel(int group, int channel)
        {
        this.channel[group] = channel;
        }
    
    boolean isMPEChannel(int channel)
        {
        if (channel < 0) return false;
        return mpeChannel[channel];
        }
    
    int getMPEGlobalChannel()
        {
        if (channel[Output.PRIMARY_GROUP] == CHANNEL_LOWER_ZONE)
            {
            return CHANNEL_GLOBAL_FOR_LOWER_ZONE;
            }
        else if (channel[1] == CHANNEL_LOWER_ZONE)
            {
            return CHANNEL_GLOBAL_FOR_UPPER_ZONE;
            }
        else
            {
            return CHANNEL_NONE;
            }
        }








    //// PITCH BEND

    /**
     * The default value for the bend range in octaves
     */
    public static final int DEFAULT_BEND_OCTAVE = 2;
    public static final double NO_BEND = 1.0;
    // actual bend range in octaves
    volatile int bendOctave;
    // current bend values for each channel.  This is used as the default value when a new sound is allocated to a channel
    double omniBend;
    double mpeBend;
    double[] globalBend = new double[NUM_MIDI_CHANNELS];
    // Do we respond to pitch bend?
    volatile boolean respondsToBend = true;
    volatile int rawBend = 0;
        
    // Used by MidiIn.java, MPE.java
    public int getRawBend() 
        { 
        return rawBend; 
        }
        
    public int getBendOctave()
        {
        return bendOctave;
        }
    
    public void setBendOctave(int val)
        {
        bendOctave = val;
        }
    
    public void setRespondsToBend(boolean val)
        {
        respondsToBend = val;
        }
    
    public boolean getRespondsToBend()
        {
        return respondsToBend;
        }

    // Processes a PITCH BEND message.
    void processPitchBend(ShortMessage sm)
        {
        if (!respondsToBend) return;
        
        int lsb = sm.getData1();
        int msb = sm.getData2();
        
        // Linux Java distros have a bug: pitch bend data is treated
        // as a signed two's complement integer, which is wrong, wrong, wrong.
        // So we have to special-case it here. See:
        //
        // https://bugs.openjdk.java.net/browse/JDK-8075073
        // https://bugs.launchpad.net/ubuntu/+source/openjdk-8/+bug/1755640
        
        if (flow.gui.Style.isUnix())
            {
            if (msb >= 64)
                {
                msb = msb - 64;
                }
            else
                {
                msb = msb + 64;
                }
            }
        
        rawBend = (lsb + msb * 128) - 8192;
        if (rawBend < -8191) rawBend = -8191;
        
        double d = Utility.hybridpow(2.0, rawBend / 8191.0 * getBendOctave());
        omniBend = d;
        if (sm.getChannel() == getMPEGlobalChannel())
            {
            mpeBend = d;
            }
        globalBend[sm.getChannel()] = d;
        
        synchronized (lock)
            {
            for (Sound sound : notesOn)
                {
                int c = sound.getChannel();
                if (c == CHANNEL_OMNI || c == sm.getChannel() || (isMPEChannel(c) && sm.getChannel() == getMPEGlobalChannel()))
                    {
                    sound.setBend(d);
                    }
                }
                        
            for (Sound sound : notesOff)
                {
                int c = sound.getChannel();
                if (c == CHANNEL_OMNI || c == sm.getChannel() || (isMPEChannel(c) && sm.getChannel() == getMPEGlobalChannel()))
                    {
                    sound.setBend(d);
                    }
                }
            }
        }






    ///// CC AND NRPN


    boolean sustain = false;
    ArrayList<Sound> sustainQueue = new ArrayList<Sound>();
    
    public static final int CC_SUSTAIN_PEDAL = 64;
    
    /**
     * CC values and NRPN values are all set to this initially
     * to indicate that the system has not received any value to indicate
     * what they should be set to.
     */
    public static final byte UNSPECIFIED = -1;

	/** The "channel" for OMNI, (which is normally -1) to be used in the CC array instead. */
    public static final byte CC_OMNI = NUM_MIDI_CHANNELS;
    
    // CC array
    byte[][] cc = new byte[NUM_MIDI_CHANNELS + 1][NUM_CC];              // one more for "omni"

    // The array of NRPN values
    short[] nrpn = new short[NUM_NRPN];
    boolean[] nrpnMSBWasSentLast = new boolean[NUM_NRPN];
    
    
    // The number of CC values.
    static final int NUM_CC = 128;
    // The maximum legal CC value
    static final int MAX_CC_VAL = 127;
    // The number of NRPN Values.
    static final int NUM_NRPN = 16384;
    // The maximum legal NRPN value
    static final int MAX_NRPN_VAL = 16383;
    
        
    /**
     * Returns the current value for the given CC on the given channel, or UNSPECIFIED
     */
    public byte getCC(int channel, int num)
        {
        if (channel == CHANNEL_NONE) return UNSPECIFIED;
        if (channel == CHANNEL_OMNI) return cc[NUM_MIDI_CHANNELS][num];
        return cc[channel][num];
        }
    
    /**
     * Returns the current value for the given NRPN, or UNSPECIFIED
     */
    public short getNRPN(int num)
        {
        return nrpn[num];
        }
        
    /**
     * Returns the whether the most recent value of NRPN was due to an MSB.
     */
    public boolean getNRPNMSBWasSentLast(int num)
        {
        return nrpnMSBWasSentLast[num];
        }
    
    // Processes an incoming CC message
    void processCC(ShortMessage sm)
        {
        Midi.CCData ccdata = midi.getParser().processCC(sm, false, false);
        
        if (ccdata == null)             // bleah
            {
            return;
            }
            
        if (ccdata.type == Midi.CCData.TYPE_RAW_CC)
            {
            byte val = (byte)ccdata.value;
            if (val < 0) val = 0;
            if (val > MAX_CC_VAL) val = MAX_CC_VAL;
            cc[ccdata.channel][ccdata.number] = val;
            cc[CC_OMNI][ccdata.number] = val;                  // set it for OMNI too
                                
            // if it's global mpe, we need to distribute to all the MPE channels
            if (sm.getChannel() == getMPEGlobalChannel())
                {
                for (int i = 0; i < NUM_MIDI_CHANNELS; i++)
                    {
                    if (isMPEChannel(i))
                        {
                        cc[i][ccdata.number] = val;
                        }
                    }
                }


            output.lock();
            try
                {
                int num = output.getNumSounds();
                if (ccdata.number == CC_SUSTAIN_PEDAL)
                    {
                    if (ccdata.value >= 64)        // sustain is down
                        {
                        sustain = true;
                        }
                    else
                        {
                        // release all the sounds in the sustain queue
                        for (Sound sound : sustainQueue)
                            {
                            sound.release();
                            }
                        sustainQueue.clear();
                        sustain = false;
                        }
                    }
                } 
            finally
                {
                output.unlock();
                }
                
            }
        else if (ccdata.type == Midi.CCData.TYPE_NRPN)
            {
            if (ccdata.increment)
                {
                nrpn[ccdata.number] = (short) (nrpn[ccdata.number] + ccdata.value);
                if (nrpn[ccdata.number] < 0)
                    {
                    nrpn[ccdata.number] = 0;
                    }
                else if (nrpn[ccdata.number] > MAX_NRPN_VAL)
                    {
                    nrpn[ccdata.number] = MAX_NRPN_VAL;
                    }
                nrpnMSBWasSentLast[ccdata.number] = false;
                }
            else
                {
                nrpn[ccdata.number] = (short) ccdata.value;
                nrpnMSBWasSentLast[ccdata.number] = ccdata.msbSentLast;
                }
            }
        else if (ccdata.type == Midi.CCData.TYPE_RPN)
            {
            if (ccdata.number == MPE_CONFIGURATION_RPN_NUMBER)
                {
                if (ccdata.validMSB)
                    {
                    int msb = ccdata.value;
                    int num = msb >> 7;
                    // kinda silly to do this since we're just setting channel[] with it, but...
                    int[] c = new int[channel.length];
                    System.arraycopy(channel, 0, c, 0, channel.length);
                    if (sm.getChannel() == 0)
                        {
                        c[Output.PRIMARY_GROUP] = CHANNEL_LOWER_ZONE;
                        }
                    else
                        {
                        c[Output.PRIMARY_GROUP] = CHANNEL_UPPER_ZONE;
                        }
                    setupMIDI(c, num, currentWrapper);
                    
                    // We might want to update the menu too....
                    Prefs.setLastNumMPEChannels(num);
                    Prefs.setLastChannel(c[Output.PRIMARY_GROUP]);
                    }
                }
            }
        }
    
    









    // AFTERTOUCH
        
        
    // Processes a POLY AFTERTOUCH message.
    void processPolyAftertouch(ShortMessage sm)
        {
        int i = sm.getData1();
        double d = sm.getData2() / 127.0;
        synchronized (lock)
            {
            for (Sound sound : notesOn)
                {
                int c = sound.getChannel();
                if (c == CHANNEL_OMNI || c == sm.getChannel() || (isMPEChannel(c) && sm.getChannel() == getMPEGlobalChannel()))
                    {                
                    if (sound.getMIDINote() == i)
                        {
                        sound.setAftertouch(d);
                        return;
                        }
                    }
                }
            }
        }
    
    // Processes a CHANNEL AFTERTOUCH message.
    void processChannelAftertouch(ShortMessage sm)
        {
        double d = sm.getData1() / 127.0;
        synchronized (lock)
            {
            for (Sound sound : notesOn)
                {
                int c = sound.getChannel();
                if (c == CHANNEL_OMNI || c == sm.getChannel() || (isMPEChannel(c) && sm.getChannel() == getMPEGlobalChannel()))
                    {
                    sound.setAftertouch(d);
                    }
                }
                
            for (Sound sound : notesOff)
                {
                int c = sound.getChannel();
                if (c == CHANNEL_OMNI || c == sm.getChannel() || (isMPEChannel(c) && sm.getChannel() == getMPEGlobalChannel()))
                    {
                    sound.setAftertouch(d);
                    }
                }
            }
        }
    




    
    
    // NOTE ON
        
    
    // List of sounds with a note-on
    LinkedList<Sound> notesOn = new LinkedList<Sound>();
    // List of sounds with a note-off
    LinkedList<Sound> notesOff = new LinkedList<Sound>();
    // List of keystrokes currently held down, in order
    LinkedList<Integer> notesOnMono = new LinkedList<Integer>();
    // Last sound which was started in response to a NOTE ON
    volatile Sound lastPlayedSound = null;
    
    /**
     * Returns the last sound which was played as a result of NOTE_ON, or null.
     */
    public Sound getLastPlayedSound()
        {
        return lastPlayedSound;
        }
        
    // Processes a NOTE ON message.
    void processNoteOn(ShortMessage sm)
        {
        Sound sound = null;
        int i = sm.getData1();
        boolean noteCurrentlyOn = false;
        int group = 0;
        synchronized (lock)
            {
            group = getGroup(sm.getChannel());

            if (group == Output.NO_GROUP)                       // we have no one who listens in on this channel
                {
                return;
                }
                        
            if (group == Output.PRIMARY_GROUP && output.getOnlyPlayFirstSound())
                {
                notesOnMono.add(Integer.valueOf(i));
                sound = output.getSoundUnsafe(0);  // I think I can do this because they're not changing at this point
                
                // Find Sound 0 and remove it from wherever it is
                if (!notesOff.remove(sound))
                    {
                    notesOn.remove(sound);
                    noteCurrentlyOn = true;
                    }
                }
            else
                {                
                // look through notesOff first
                for(int j = notesOff.size() - 1; j >= 0; j--)
                    {
                    Sound s = notesOff.get(j);
                    if (s.getGroup() == group)
                        {
                        sound = s;
                        notesOff.remove(j);
                        break;
                        }
                    }
                                
                if (sound == null)
                    {
                    // look through notesOn next
                    for(int j = notesOn.size() - 1; j >= 0; j--)
                        {
                        Sound s = notesOn.get(j);
                        if (s.getGroup() == group)
                            {
                            sound = s;
                            notesOn.remove(j);
                            break;
                            }
                        }
                    }
                
                // handle sustain queue for non-mono sounds.  We need to release the old sound
                if (sustain && sustainQueue.contains(sound))
                    {
                    sustainQueue.remove(sound);
                    sound.release();
                    }
                }
            
            notesOn.addFirst(sound);
            }
        
        double d = Math.pow(2.0, (double) (i - 69) / 12.0) * 440.0;
        
        output.lock();
        try
            {
            // set the channel, including OMNI
            if (getChannel(group) == CHANNEL_OMNI)
                {
                sound.setChannel(CHANNEL_OMNI);
                }
            else
                {
                sound.setChannel(sm.getChannel());
                }
            
            sound.setNote(d);
            sound.setMIDINote(i);
            sound.setVelocity((double) sm.getData2() / 127.0);
            if (sound.getChannel() == CHANNEL_OMNI)
                {
                sound.setBend(omniBend);
                }
            else
                {
                sound.setBend(globalBend[sound.getChannel()]);
                }
            if (!noteCurrentlyOn)
                {
                sound.gate();
                }
               
            if (sound.getGroup() == Output.PRIMARY_GROUP)
                {
                lastPlayedSound = sound;
                }
                
            } 
        finally
            {
            output.unlock();
            }
        }
    
    
    
    
    
    
    
    // Processes a NOTE OFF message.
    void processNoteOff(ShortMessage sm)
        {
        Sound sound = null;
        int i = sm.getData1();
        synchronized (lock)
            {
            notesOnMono.remove(Integer.valueOf(i));
            Iterator iterator = notesOn.iterator();
            while (true)
                {
                if (!iterator.hasNext())
                    {
                    break;
                    }
                Sound sound1 = (Sound) iterator.next();
                if (sound1.getMIDINote() != i)
                    {
                    continue;
                    }
                sound = sound1;
                break;
                }
            
            output.lock();
            try
                {
                boolean monoIsEmpty = notesOnMono.isEmpty();
                boolean onlyPlayFirstSound = output.getOnlyPlayFirstSound();
                if (sound == null)
                    {
                    //System.err.println("WARNING(Input.java): Couldn't find the sound to turn off!!!");
                    }
                else
                    {
                    if (!onlyPlayFirstSound || monoIsEmpty)        // release our sound
                        {
                        // add to queue but don't release if we're sustaining
                        if (sustain && !sustainQueue.contains(sound))
                            {
                            sustainQueue.add(sound);
                            }
                        else
                            {
                            sound.release();
                            }
                        notesOn.remove(sound);
                        notesOff.addFirst(sound);
                        
                        // we do the following because Roli's MPE will typically immediately reuse the channel.
                        // See Page 11 of the MPE spec:
                        //
                        // "The prevention of per-note control after Note Off allows rapid reuse of unoccupied Channels,
                        // and applies even to notes that are kept active by a Damper Pedal message or a long release envelope."
                        
                        if (isMPEChannel(sound.getChannel()))
                            {
                            sound.setChannel(CHANNEL_NONE);
                            }
                        }
                    else        // just reassign the sound
                        {
                        int j = i;
                        i = notesOnMono.getLast().intValue();
                        double d = Math.pow(2.0, (double) (i - 69) / 12.0) * 440.0;

                        // set the channel, including OMNI
                        if (getChannel(sound.getGroup()) == CHANNEL_OMNI)
                            {
                            sound.setChannel(CHANNEL_OMNI);
                            }
                        else
                            {
                            sound.setChannel(sound.getGroup());
                            }

                        sound.setNote(d);
                        sound.setMIDINote(i);

                        if (sound.getGroup() == Output.PRIMARY_GROUP)
                            {
                            lastPlayedSound = sound;
                            }
                        }
                    
                    // either way, let's set the release velocity
                    sound.setReleaseVelocity((double) sm.getData2() / 127.0);
                    }
                } finally
                {
                output.unlock();
                }
            }
        }
    
    







    ////// TOP LEVEL


    // Pulses the Input.  Called by Output's voice sync thread's go() method.
    void go()
        {
        MidiMessage[] messages = midi.getNextMessages();
        for (int i = 0; i < messages.length; i++)
            {
            MidiMessage message = messages[i];
            if (message == null || !(message instanceof ShortMessage))
                {
                continue;
                }
            ShortMessage sm = (ShortMessage) message;
            if ( sm.getStatus() >= 0x80 && sm.getStatus() < 0xF0 )                      // voice message
                {
                Midi.CCData ccdata;
               
                /* 
                   if ((sm.getChannel() == channel) || // If it's in our channel
                   (isOMNI()) || // or we're looking for all messages
                   (isMPE() && isMPEChannel(sm.getChannel())) || // or we're mpe and in the mpe range
                   (
                   sm.getCommand() == ShortMessage.CONTROL_CHANGE && // or we're a RPN message for configuring the MPE message
                   (ccdata = midi.getParser().processCC(sm, false, false)) != null &&
                   ccdata.type == Midi.CCData.TYPE_RPN &&
                   ccdata.number == MPE_CONFIGURATION_RPN_NUMBER)
                   )
                   {
                */
                int command = sm.getCommand();                  // Note not getStatus().  See below.
                if (command == ShortMessage.NOTE_OFF || command == ShortMessage.NOTE_ON && sm.getData2() == 0)
                    {
                    processNoteOff(sm);
                    }
                else if (command == ShortMessage.NOTE_ON)
                    {
                    processNoteOn(sm);
                    }
                else if (command == ShortMessage.PITCH_BEND)
                    {
                    processPitchBend(sm);
                    }
                else if (command == ShortMessage.CONTROL_CHANGE)
                    {
                    processCC(sm);
                    }
                else if (command == ShortMessage.CHANNEL_PRESSURE)
                    {
                    processChannelAftertouch(sm);
                    }
                else if (command == ShortMessage.POLY_PRESSURE)
                    {
                    processPolyAftertouch(sm);
                    }
                }
            }
        
        midiClock.go();
        }
    
    }

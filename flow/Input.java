// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import java.util.*;
import javax.sound.midi.*;

/**
   Input handles MIDI Input.  It is owned by Output, and it in turn owns Midi, the low-level wrapper for
   the Java midi subsystem.
*/

public class Input
    {
    Midi midi;
    MidiClock midiClock;
    Output output;

	public static final int CC_SUSTAIN_PEDAL = 64;

    /** The value which represents OMNI for incoming MIDI channels */
    public static final int MPE_CONFIGURATION_RPN_NUMBER = 6;
    public static final int CHANNEL_NONE = -4;

    public static final int CHANNEL_LOWER_ZONE = -3;
    public static final int CHANNEL_UPPER_ZONE = -2;
    public static final int CHANNEL_OMNI = -1;
    public static final int NUM_SPECIAL_CHANNELS = 3;  // CHANNEL_LOWER_ZONE through CHANNEL_OMNI
    public static final int DEFAULT_NUM_MPE_CHANNELS = 7;
    int channel = CHANNEL_OMNI;

    /** CC values and NRPN values are all set to this initially
        to indicate that the system has not received any value to indicate
        what they should be set to. */
    public static final byte UNSPECIFIED = -1;

    // The array of CC values.  Some of these are not legal, since they are part of the NRPN facility.
    byte cc[];

    // The number of mpe channels. This needs to be handled differently depending on the zone.
    int numMPEChannels = DEFAULT_NUM_MPE_CHANNELS;

    // The array of channels that are in-zone for the current MPE settings
    boolean mpeChannels[] = new boolean[16];

    // The raw bend value
    int rawBend = 0;

    public int getRawBend() { return rawBend; }

    // The array of NRPN values
    short nrpn[];
    boolean msbSentLast[];

	public Midi getMidi() { return midi; }
	
    public MidiClock getMidiClock() { return midiClock; }

    /** The default value for the bend range in octaves */
    public static final int DEFAULT_BEND_OCTAVE = 2;
    // actual bend range in octaves
    int bendOctave;
    // current global bend value
    double globalBend = 1.0;

    public int getBendOctave() { return bendOctave; }
    public void setBendOctave(int val)
        {
        bendOctave = val;
        Prefs.setLastBendOctave(val);
        }


    // Do we respond to pitch bend?
    volatile boolean respondsToBend = true;

    /** Sets whether we respond to Pitch Bend. */
    public void setRespondsToBend(boolean val) { respondsToBend = val; }
    /** Returns whether we respond to Pitch Bend. */
    public boolean getRespondsToBend() { return respondsToBend; }
    

    // The number of CC values.
    static final int NUM_CC = 128;
    // The number of NRPN Values.
    static final int NUM_NRPN = 16384;
    // The maximum legal NRPN value
    static final int MAX_NRPN_VAL = 16383;

    Object lock = new Object[0];
    // List of sounds with a note-on
    LinkedList<Sound> notesOn = new LinkedList<Sound>();
    // List of sounds with a note-off
    LinkedList<Sound> notesOff = new LinkedList<Sound>();
    // List of keystrokes currently held down, in order
    LinkedList<Integer>notesOnMono = new LinkedList<Integer>();

	boolean sustain = false;
	ArrayList<Sound> sustainQueue = new ArrayList<Sound>();

    /** returns true if we are in MPE mode **/
    public boolean isMPE() { return channel == CHANNEL_LOWER_ZONE  || channel == CHANNEL_UPPER_ZONE; }

    public Output getOutput() { return output; }

    public Input(Output output)
        {
        this.output = output;
        bendOctave = Prefs.getLastBendOctave();

        channel = 0;
        cc = new byte[NUM_CC];
        nrpn = new short[NUM_NRPN];
        msbSentLast = new boolean[NUM_NRPN];
        
        for (int i = 0; i < NUM_CC; i++)
            cc[i] = UNSPECIFIED;

        for (int i = 0; i < NUM_NRPN; i++)
            nrpn[i] = UNSPECIFIED;

        midi = new Midi(this);
        midiClock = new MidiClock(this);
        }

    // Output calls this to add a Sound to the Input (it's added to the notesOff list)
    void addSound(Sound sound)
        {
        synchronized(lock)
            {
            notesOff.add(sound);
            }
        }



    ///// MIDI DEVICES AND SETUP


    /** Sets MIDI to the new device wrapper and channel. */
    Midi.MidiDeviceWrapper currentWrapper;
    public void setupMIDI(int channel, int numMPEChannels, Midi.MidiDeviceWrapper mididevicewrapper)
        {
        Prefs.setLastChannel(channel);
        Prefs.setLastNumMPEChannels(numMPEChannels);
        this.channel = channel;
        this.numMPEChannels = numMPEChannels;
        this.globalBend = 1.0; //1.0 is no bend
        if (isMPE())
            {
            setupMPEArray();
            }
        currentWrapper = mididevicewrapper;
        midi.setInReceiver(mididevicewrapper);
        }

    private void setupMPEArray() 
        {
        for (int i = 0; i < 16; i++) 
            {
            if (  (channel == CHANNEL_LOWER_ZONE && i <= numMPEChannels)
                ||(channel == CHANNEL_UPPER_ZONE && 15 - i <= numMPEChannels))
                {
                mpeChannels[i] = true;
                } 
            else 
                {
                mpeChannels[i] = false;
                }
            }
        }

    /** Sets MIDI to the first available device wrapper, and channel 0. */
    public void setupMIDI()
        {
        ArrayList<Midi.MidiDeviceWrapper> devices = getDevices();
        String str = Prefs.getLastMidiDevice();
        Midi.MidiDeviceWrapper wrap = null;
        for (Midi.MidiDeviceWrapper device : devices)
            {
            if (device.toString().equals(str))
                {
                wrap = device;
                }
            }
        if (wrap == null)
            {
            wrap = devices.get(0);
            }

        int ch = Prefs.getLastChannel();
        int num = Prefs.getLastNumMPEChannels();
        setupMIDI(ch, num, wrap);
        }

    /** Returns all available MIDI device wrappers. */
    public ArrayList<Midi.MidiDeviceWrapper> getDevices()
        {
        ArrayList<Midi.MidiDeviceWrapper> devices = (ArrayList<Midi.MidiDeviceWrapper>)(midi.getInDevices().clone());
        devices.add(0, new Midi.MidiDeviceWrapper(null));  // a "None"
        return devices;
        }

    /** Returns the current channel. */
    public int getChannel() { return channel; }

    /** Returns the current MidiDevice */
    public Midi.MidiDeviceWrapper getMidiDevice() { return currentWrapper; }


    ///// CC AND NRPN


    /** Returns the current value for the given CC, or UNSPECIFIED */
    public byte getCC(int num)
        {
        return cc[num];
        }

    /** Returns the current value for the given NRPN, or UNSPECIFIED */
    public short getNRPN(int num)
        {
        return nrpn[num];
        }

    /** Returns the whether the most recent value of NRPN was due to an MSB. */
    public boolean getMSBSentLast(int num)
        {
        return msbSentLast[num];
        }

    // Processes an incoming CC message
    void processCC(ShortMessage sm)
        {
        Midi.CCData ccdata = midi.getParser().processCC(sm, false, false);
        if(ccdata == null)
        {
            return;
            }
        if (ccdata.type == Midi.CCData.TYPE_RAW_CC)
            {
            cc[ccdata.number] = (byte)ccdata.value;
            if (cc[ccdata.number] < 0)
                {
                cc[ccdata.number] = 0;
                }

            // At present we're just distributing to all the sounds.
            // With MPE this needs to change.

            output.lock();
            try
                {
                int num = output.getNumSounds();
                if (!isMPE()) 
                    {
                    for (int i = 0; i < num; i++) 
                        {
                        output.getSound(i).setCC(ccdata.number, ccdata.value);
                        }

					// We'll handle the sustain pedal for non-MPE here.
					// Should MPE support sustain globally maybe?

					if (ccdata.number == CC_SUSTAIN_PEDAL)
						{
						if (ccdata.value >= 64)		// sustain is down
							{
							sustain = true;
							}
						else
							{
							// release all the sounds in the sustain queue
							for(Sound sound : sustainQueue)
								{
								sound.release();
								}
							sustainQueue.clear();
							sustain = false;
							}
						}
                    } 
                else 
                    {
                    for (int i = 0; i < num; i++) 
                        {
                        Sound sound = output.getSound(i);
                        if (sm.getChannel() == sound.getChannel() || sm.getChannel() == getMPEGlobalChannel()) 
                            {
                            sound.setCC(ccdata.number, ccdata.value);
                            break;  // there can only be one
                            }
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
                nrpn[ccdata.number] = (short)(nrpn[ccdata.number] + ccdata.value);
                if (nrpn[ccdata.number] < 0)
                    {
                    nrpn[ccdata.number] = 0;
                    }
                else if (nrpn[ccdata.number] > MAX_NRPN_VAL)
                    {
                    nrpn[ccdata.number] = MAX_NRPN_VAL;
                    }
                msbSentLast[ccdata.number] = false;
                }
            else
                {
                nrpn[ccdata.number] = (short)ccdata.value;
                msbSentLast[ccdata.number] = ccdata.msbSentLast;
                }
            }
        else if (ccdata.type == Midi.CCData.TYPE_RPN)
            {
            if(ccdata.number == MPE_CONFIGURATION_RPN_NUMBER)
            	{
                if(ccdata.validMSB)
                	{
                    int msb = ccdata.value;
                    int num = msb >> 7;
                    if(sm.getChannel() == 0)
                    	{
                        setupMIDI(CHANNEL_LOWER_ZONE,num,currentWrapper);
                        } 
                        else 
                        {
                        setupMIDI(CHANNEL_UPPER_ZONE,num,currentWrapper);
                        }
                    }
                }
            }
        }


    volatile Sound lastSound = null;

    /** Returns the last sound which was played as a result of NOTE_ON, or null. */
    public Sound getLastPlayedSound() { return lastSound; }

    /** Moves all notes to off and calls release on those notes. */
    public void allOff()
        {
        synchronized(lock)
            {
            output.lock();
            try
                {
                int num = notesOn.size();
                for(int i = 0; i < num; i++)
                    {
                    Sound sound = notesOn.removeLast();
                    sound.release();
                    notesOff.add(sound);
                    }
                notesOnMono.clear();
                }
            finally
                {
                output.unlock();
                }
            }
        }

    // Processes a NOTE ON message.
    void processNoteOn(ShortMessage sm)
        {
        boolean noteCurrentlyOn = false;

        Sound sound = null; 
        int i = sm.getData1();
        synchronized(lock)
            {
            if (output.getOnlyPlayFirstSound())
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
            	if (notesOff.isEmpty())
					{
					sound = (Sound)notesOn.removeLast();
					}
				else
					{
					sound = (Sound)notesOff.removeLast();
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
            
        double d = Math.pow(2.0, (double)(i - 69) / 12.0) * 440.0;

        output.lock();
        try
            {
            sound.setChannel(sm.getChannel());
            sound.setNote(d);
            sound.setMIDINote(i);
            sound.setVelocity((double)sm.getData2() / 127.0);
            sound.setBend(globalBend);
            if (!noteCurrentlyOn) sound.gate();
            lastSound = sound;
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
        synchronized(lock)
            {
            notesOnMono.remove(Integer.valueOf(i));
            Iterator iterator = notesOn.iterator();
            while (true)
                {
                if (!iterator.hasNext()) break;
                Sound sound1 = (Sound)iterator.next();
                if (sound1.getMIDINote() != i) continue;
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
						if (!onlyPlayFirstSound || monoIsEmpty)		// release our sound
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

							if (isMPE())
								sound.setChannel(Input.CHANNEL_NONE);  
							}
						else		// just reassign the sound
							{
							int j = i;
							i = notesOnMono.getLast().intValue();
							double d = Math.pow(2.0, (double)(i - 69) / 12.0) * 440.0;
            				sound.setChannel(sm.getChannel());
							sound.setNote(d);
							sound.setMIDINote(i);
							lastSound = sound;
							}

						// either way, let's set the release velocity
						sound.setReleaseVelocity((double)sm.getData2() / 127.0);
						}
				}
			finally
				{
				output.unlock();
				}
            }
        }

    // Processes a POLY AFTERTOUCH message.
    void processPolyAftertouch(ShortMessage sm)
        {
        int i = sm.getData1();
        double d = sm.getData2() / 127.0;
        synchronized(lock)
            {
            for (Sound sound : notesOn)
                {
                if (sound.getMIDINote() == i)
                    {
                    sound.setAftertouch(d);
                    return;
                    }
                }
            }
        }

    // Processes a CHANNEL AFTERTOUCH message.
    void processChannelAftertouch(ShortMessage sm)
        {
        double d = sm.getData1() / 127.0;
        synchronized(lock)
            {
            if (!isMPE()) 
                {
                for (Sound sound : notesOn) 
                    {
                    sound.setAftertouch(d);
                    }

                for (Sound sound : notesOff) 
                    {
                    sound.setAftertouch(d);
                    }
                } 
            else 
                {
                for (Sound sound : notesOn) 
                    {
                    if (sound.getChannel() == sm.getChannel() || sm.getChannel() == getMPEGlobalChannel()) 
                        {
                        sound.setAftertouch(d);
                        }
                    }

                for (Sound sound : notesOff) 
                    {
                    if (sound.getChannel() == sm.getChannel() || sm.getChannel() == getMPEGlobalChannel()) 
                        {
                        sound.setAftertouch(d);
                        }
                    }
                }
            }
        }

    // Processes a PITCH BEND message.
    void processPitchBend(ShortMessage sm)
        {
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

        if (rawBend < - 8191)
            {
            rawBend = - 8191;
            }

        double d = Utility.hybridpow(2.0, rawBend / 8191.0 * bendOctave);

        if(!isMPE() || (isMPE() && sm.getChannel() == getMPEGlobalChannel())){
            globalBend = d;
            }

        synchronized(lock)
            {
            if (!isMPE()) 
                {
                for (Sound sound : notesOn) 
                    {
                    sound.setBend(d);
                    }

                for (Sound sound : notesOff) 
                    {
                    sound.setBend(d);
                    }
                } 
            else 
                {
                for (Sound sound : notesOn) 
                    {
                    if (sound.getChannel() == sm.getChannel() || sm.getChannel() == getMPEGlobalChannel()) 
                        {
                        sound.setBend(d);
                        }
                    }

                for (Sound sound : notesOff) 
                    {
                    if (sound.getChannel() == sm.getChannel()) 
                        {
                        sound.setBend(d);
                        }
                    }
                }
            }

        }

    boolean isVoiceMessage(ShortMessage m)
        {
        return (m.getStatus() >= 0x80 &&
            m.getStatus() < 0xF0);
        }

    // Pulses the Input.  Called by Output's voice sync thread's go() method.
    void go()
        {
        MidiMessage messages[] = midi.getNextMessages();
        for (int i = 0; i < messages.length; i++)
            {
            MidiMessage message = messages[i];
            if (message == null || !(message instanceof ShortMessage))
                continue;
            ShortMessage sm = (ShortMessage)message;
            if (isVoiceMessage(sm))
                {
                Midi.CCData ccdata;

                if ((sm.getChannel() == channel) || // If it's in our channel
                    (channel == CHANNEL_OMNI) || // or we're looking for all messages
                    (isMPE() && isInMPEZone(sm.getChannel())) || // or we're mpe and in the mpe range
                        (sm.getCommand() == ShortMessage.CONTROL_CHANGE && // or we're a RPN message for configuring the MPE message
                        (ccdata = midi.getParser().processCC(sm, false, false)) != null &&
                        ccdata.type == Midi.CCData.TYPE_RPN && 
                        ccdata.number == MPE_CONFIGURATION_RPN_NUMBER)
                    )
                    {
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
            }

        midiClock.go();
        }

    private boolean isInMPEZone(int channel) 
        {
        return mpeChannels[channel];
        }

    private int getMPEGlobalChannel(){
        if(getChannel() == CHANNEL_LOWER_ZONE){
            return 0;
            } else {
            return 15;
            }
        }
    }

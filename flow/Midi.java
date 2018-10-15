// Copyright 2017 by Sean Luke
// Licensed under the Apache 2.0 License


package flow;

import javax.sound.midi.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;

/**** 
      Static class which contains methods for handling the global MIDI device facility.
      This method provides wrappers around MIDI Devices.  Owned by Input.
      
      <p>To use this class properly you should be using CoreMIDI4J, as OS X has some ugly
      MIDI bugs.
*/


public class Midi
    {
    Input input;
    
    public Midi(Input input)
        {
        this.input = input;
        }
    
    ///////// DEVICES
    
    public static final String NO_DEVICE = "No Device";

    /** A wrapper for a MIDI device which displays its name in a pleasing and
        useful format for the user, and which can set its receiver  */
                
    public static class MidiDeviceWrapper
        {
        MidiDevice device;
        
        MidiDeviceWrapper(MidiDevice device)
            {
            this.device = device;
            }
        
        /** Returns a useful name to display to represent the MIDI Device in question. */        
        public String toString() 
            { 
            if (device == null)
                return "No Device";

            String desc = device.getDeviceInfo().getDescription().trim();
            String name = device.getDeviceInfo().getName();
            if (name == null) 
                name = "";
            if (desc == null || desc.equals("")) 
                desc = "MIDI Device";
            
            // All CoreMIDI4J names begin with "CoreMIDI4J - "
            if (name.startsWith("CoreMIDI4J - "))
                name = name.substring(13).trim();
            else
                name = name.trim();

            if (name.equals(""))
                return desc.trim(); 
            else 
                return name;
            }
        
        // Attaches the Receiver as a receiver for the given MIDI Device
        void setReceiver(Receiver receiver) 
            {
            if (device == null)
                return;
            try
                {
                if (!device.isOpen()) 
                    device.open();
                device.getTransmitter().setReceiver(receiver);
                }
            catch(Exception e) { e.printStackTrace(); }
            }
        }


    // A list of all MIDI Devices
    static ArrayList<MidiDeviceWrapper> allDevices;
    
    // A list of all MIDI Devices for incoming MIDI Data
    static ArrayList<MidiDeviceWrapper> inDevices;
    
    // A list of all MIDI Devices for outgoing MIDI Data
    static ArrayList<MidiDeviceWrapper> outDevices;

    // updates the current MIDI devices registered
    static void updateDevices()
        {
        MidiDevice.Info[] midiDevices;
        try
            {
            midiDevices = uk.co.xfactorylibrarians.coremidi4j.CoreMidiDeviceProvider.getMidiDeviceInfo();
            //System.err.println("Loaded CoreMIDI4J.");
            }
        catch (Exception ex)
            {
            midiDevices = MidiSystem.getMidiDeviceInfo();
            //System.err.println("Couldn't load CoreMIDI4J.  Falling back on standard Java Midi.");
            }
        midiDevices = MidiSystem.getMidiDeviceInfo();

        ArrayList allDevices = new ArrayList();
        for(int i = 0; i < midiDevices.length; i++)
            {
            try
                {
                MidiDevice d = MidiSystem.getMidiDevice(midiDevices[i]);
                // get rid of java devices
                if (d instanceof javax.sound.midi.Sequencer ||
                    d instanceof javax.sound.midi.Synthesizer)
                    continue;
                if (d.getMaxTransmitters() != 0 || d.getMaxReceivers() != 0)
                    {
                    allDevices.add(new MidiDeviceWrapper(d));
                    }
                }
            catch(Exception e) { }
            }
            
        // Do they hold the same exact devices?
        if (Midi.allDevices != null && Midi.allDevices.size() == allDevices.size())
            {
            Set set = new HashSet();
            for(int i = 0; i < Midi.allDevices.size(); i++)
                {
                set.add(((MidiDeviceWrapper)(Midi.allDevices.get(i))).device);
                }
                
            boolean same = true;
            for(int i = 0; i < allDevices.size(); i++)
                {
                if (!set.contains(((MidiDeviceWrapper)(allDevices.get(i))).device))
                    {
                    same = false;  // something's different
                    break;
                    }
                }
                
            if (same)
                {
                return;  // they're identical
                }
            }
                
        // at this point allDevices isn't the same as Midi.allDevices, so set it and update
        Midi.allDevices = allDevices;

        inDevices = new ArrayList();
        for(int i = 0; i < allDevices.size(); i++)
            {
            try
                {
                MidiDeviceWrapper mdn = (MidiDeviceWrapper)(allDevices.get(i));
                if (mdn.device.getMaxTransmitters() != 0)
                    {
                    inDevices.add(mdn);
                    }
                }
            catch(Exception e) { }
            }

        outDevices = new ArrayList();
        for(int i = 0; i < allDevices.size(); i++)
            {
            try
                {
                MidiDeviceWrapper mdn = (MidiDeviceWrapper)(allDevices.get(i));
                if (mdn.device.getMaxReceivers() != 0)
                    {
                    outDevices.add(mdn);
                    }
                }
            catch(Exception e) { }
            }
        }
        
    static
        {
        updateDevices();
        }
    
    /** Returns all incoming MIDI Devices */
    public ArrayList<MidiDeviceWrapper> getInDevices()
        {
        updateDevices();
        return inDevices;
        }

    // Returns all MIDI Devices period, incoming or outgoing */
    ArrayList<MidiDeviceWrapper> getAllDevices()
        {
        updateDevices();
        return allDevices;
        }
        
    Object lock = new Object[0];

    // All current MIDI Messages which have not been grabbed yet
    ArrayList<MidiMessage> nextMessages = new ArrayList<MidiMessage>();
    MidiMessage[] empty = new MidiMessage[0];
    /** Returns all MIDI Messages, in order, that have not yet been processed.  By calling this,
        the messages are processed and removed from this queue. */    
    public MidiMessage[] getNextMessages()
        {
        MidiMessage[] ret = empty;
        synchronized(lock)
            {
            if (nextMessages.size() == 0) return empty;  // fast return
            ret = (MidiMessage[]) nextMessages.toArray(new MidiMessage[nextMessages.size()]);
            nextMessages.clear();
            }
        return ret;
        }
        
    // Our special kind of receiver.
    class InReceiver implements Receiver
        {
        boolean live = true;

        // these have to be public because the superclass has them public         
        public void close() 
            { 
            synchronized(lock)
                {
                live = false; 
                }
            }
               
        // these have to be public because the superclass has them public         
        public void send(MidiMessage message, long timeStamp)
            {
            boolean l = false;
            int command = message.getStatus();              // Note NOT getCommand().  getCommand() only works for channel messages.

            synchronized(lock)
                {                       
                // first things first, get out of the lock as fast as we can.
                // We do that by adding the message if we need to.

                l = live;
                if (live && (command < ShortMessage.TIMING_CLOCK || command > ShortMessage.STOP))
                    {
                    nextMessages.add(message);
                    return;
                    }
                }            
                
            // Now we can pulse the clock -- it has its own separate lock.
            // We do it here rather than letting the voice sync thread handle
            // these messages when it grabs all nextMessages() because the
            // voice sync thread is too slow; it's typically 1/3 the speed
            // of MIDI.  We want to update the timing clock as soon as humanly
            // possible so we can properly interpolate.  Other messages don't
            // matter nearly as much.
            
            if (l)
                {
                if (command == ShortMessage.TIMING_CLOCK)
                    {
                    input.getMidiClock().pulseClock();
                    }
                else if (command == ShortMessage.START)
                    {
                    input.getMidiClock().startClock();
                    }
                else if (command == ShortMessage.STOP)
                    {
                    input.getMidiClock().stopClock();
                    }
                else if (command == ShortMessage.CONTINUE)
                    {
                    input.getMidiClock().continueClock();
                    }
                }
            }
        }
                        
    InReceiver inReceiver = null;
        
    /** Sets the In Reciever to receive from the device in the given wrapper */
    public void setInReceiver(MidiDeviceWrapper wrapper)
        {
        synchronized(lock)
            {
            if (inReceiver != null)
                {
                inReceiver.close();
                }
            inReceiver = new InReceiver();
            wrapper.setReceiver(inReceiver);
            }
        }
        
        
                
    /// UTILITIES FOR PARSING        

    /** Data returned by the parser indicating the type of parsed message,
        its number and value and channel, and whether or not the data was increment or decrement. */
    public static class CCData
        {
        /** Data type CC */
        public static final int TYPE_RAW_CC = 0;
             
        /** Data type NRPN */
        public static final int TYPE_NRPN = 1;   
        
        /** Data type RPN */   
        public static final int TYPE_RPN = 2;      

        /** The type of the data */
        public int type;
        
        /** The (CC/RPN/NRPN) number of the data */
        public int number;
        
        /** The value of the data */
        public int value;
        
        /** The channel */
        public int channel;
        
        /** Is the RPN/NRPN MSB value valid?  That is, have we seen one yet? (When not valid, it's 0 by default) */
        public boolean validMSB = false;

        /** Is the RPN/NRPN LSB value valid?  That is, have we seen one yet? (When not valid, it's 0 by default) */
        public boolean validLSB = false;
        
        /** If the data is NRPN or RPN, and the incoming signal was an INCREMENT or DECREMENT,
            then this flag is set.  In this case, the VALUE is no longer the absolute value, but
            is rather a DELTA to increment or decrement (if decrementing, the DELTA is negative). */
        public boolean increment;
        
        public CCData(int type, int number, int value, int channel, boolean increment)
            { this.type = type; this.number = number; this.value = value; this.increment = increment; this.channel = channel; }
        }
        
        
    public static class Parser
        {
        ///// INTRODUCTION TO THE CC/RPN/NRPN PARSER
        ///// The parser is located in handleGeneralControlChange(...), which
        ///// can be set up to be the handler for CC messages by the MIDI library.
        /////
        ///// CC messages take one of a great many forms, which we handle in the parser
        /////
        ///// 7-bit CC messages:
        ///// 1. number >=64 and < 96 or >= 102 and < 120, with value
        /////           -> handleControlChange(channel, number, value, VALUE_7_BIT_ONLY)
        /////
        ///// Potentially 7-bit CC messages, with MSB:
        ///// 1. number >= 0 and < 32, other than 6, with value
        /////           -> handleControlChange(channel, number, value * 128 + 0, VALUE_MSB_ONLY)
        /////
        ///// Full 14-bit CC messages:
        ///// 1. number >= 0 and < 32, other than 6, with MSB
        ///// 2. same number + 32, with LSB
        /////           -> handleControlChange(channel, number, MSB * 128 + LSB, VALUE)
        /////    NOTE: this means that a 14-bit CC message will have TWO handleControlChange calls.
        /////          There's not much we can do about this, as we simply don't know if the LSB will arrive.  
        /////
        ///// Continuing 14-bit CC messages:
        ///// 1. number >= 32 and < 64, other than 38, with LSB, where number is 32 more than the last MSB.
        /////           -> handleControlChange(channel, number, former MSB * 128 + LSB, VALUE)
        /////
        ///// Lonely 14-bit CC messages (LSB only)
        ///// 1. number >= 32 and < 64, other than 38, with LSB, where number is NOT 32 more than the last MSB.
        /////           -> handleControlChange(channel, number, 0 + LSB, VALUE)
        /////           
        /////
        ///// NRPN Messages:
        ///// All NRPN Messages start with:
        ///// 1. number == 99, with MSB of NRPN parameter
        ///// 2. number == 98, with LSB of NRPN parameter
        /////           At this point NRPN MSB is set to 0
        /////
        ///// NRPN Messages then may have any sequence of:
        ///// 3.1 number == 6, with value   (MSB)
        /////           -> handleNRPN(channel, parameter, value * 128 + 0, VALUE_MSB_ONLY)
        /////                           At this point we set the NRPN MSB
        ///// 3.2 number == 38, with value   (LSB)
        /////           -> handleNRPN(channel, parameter, current NRPN MSB * 128 + value, VALUE_MSB_ONLY)
        ///// 3.3 number == 96, with value   (Increment)
        /////       If value == 0
        /////                   -> handleNRPN(channel, parameter, 1, INCREMENT)
        /////       Else
        /////                   -> handleNRPN(channel, parameter, value, INCREMENT)
        /////       Also reset current NRPN MSB to 0
        ///// 3.4 number == 97, with value
        /////       If value == 0
        /////                   -> handleNRPN(channel, parameter, 1, DECREMENT)
        /////       Else
        /////                   -> handleNRPN(channel, parameter, value, DECREMENT)
        /////       Also reset current NRPN MSB to 0
        /////
        /////
        ///// RPN Messages:
        ///// All RPN Messages start with:
        ///// 1. number == 99, with MSB of RPN parameter
        ///// 2. number == 98, with LSB of RPN parameter
        /////           At this point RPN MSB is set to 0
        /////
        ///// RPN Messages then may have any sequence of:
        ///// 3.1 number == 6, with value   (MSB)
        /////           -> handleRPN(channel, parameter, value * 128 + 0, VALUE_MSB_ONLY)
        /////                           At this point we set the RPN MSB
        ///// 3.2 number == 38, with value   (LSB)
        /////           -> handleRPN(channel, parameter, current RPN MSB * 128 + value, VALUE_MSB_ONLY)
        ///// 3.3 number == 96, with value   (Increment)
        /////       If value == 0
        /////                   -> handleRPN(channel, parameter, 1, INCREMENT)
        /////       Else
        /////                   -> handleRPN(channel, parameter, value, INCREMENT)
        /////       Also reset current RPN MSB to 0
        ///// 3.4 number == 97, with value
        /////       If value == 0
        /////                   -> handleRPN(channel, parameter, 1, DECREMENT)
        /////       Else
        /////                   -> handleRPN(channel, parameter, value, DECREMENT)
        /////       Also reset current RPN MSB to 0
        /////

        ///// NULL messages:            [RPN 127 with value of 127]
        ///// 1. number == 101, value = 127
        ///// 2. number == 100, value = 127
        /////           [nothing happens, but parser resets]
        /////
        /////
        ///// The big problem we have is that the MIDI spec allows a bare MSB or LSB to arrive and that's it!
        ///// We don't know if another one is coming.  If a bare LSB arrives we're supposed to assume the MSB is 0.
        ///// But if the bare MSB comes we don't know if the LSB is next.  So we either have to ignore it when it
        ///// comes in (bad bad bad) or send two messages, one MSB-only and one MSB+LSB.  
        ///// This happens for CC, RPN, and NRPN.
        /////
        /////
        ///// Our parser maintains four bytes in a struct called ControlParser:
        /////
        ///// 0. status.  This is one of:
        /////             INVALID: the struct holds junk.  CC: the struct is building a CC.  
        /////                     RPN_START, RPN_END: the struct is building an RPN.
        /////                     NRPN_START, NRPN_END: the struct is building an NRPN.
        ///// 1. controllerNumberMSB.  In the low 7 bits.
        ///// 2. controllerNumberLSB.  In the low 7 bits.
        ///// 3. controllerValueMSB.  In the low 7 bits. This holds the previous MSB for potential "continuing" messages.

        // Parser status values
        public static final int  INVALID = 0;
        public static final int  NRPN_START = 1;
        public static final int  NRPN_END = 2;
        public static final int  RPN_START = 2;
        public static final int  RPN_END = 3;

        int[] status = new int[16];  //  = INVALID;
                
        // The high bit of the controllerNumberMSB is either
        // NEITHER_RPN_NOR_NRPN or it is RPN_OR_NRPN. 
        int[] controllerNumberMSB = new int[16];
                
        // The high bit of the controllerNumberLSB is either
        // RPN or it is NRPN
        int[] controllerNumberLSB = new int[16];
                
        // The controllerValueMSB[channel] is either a valid MSB or it is (-1).
        int[] controllerValueMSB = new int[16];

        // The controllerValueLSB is either a valid LSB or it is  (-1).
        int[] controllerValueLSB = new int[16];
  

        // we presume that the channel never changes
        CCData parseCC(int channel, int number, int value, boolean requireLSB, boolean requireMSB)
            {
            // BEGIN PARSER

            // Start of NRPN
            if (number == 99)
                {
                status[channel] = NRPN_START;
                controllerNumberMSB[channel] = value;
                return null;
                }

            // End of NRPN
            else if (number == 98)
                {
                controllerValueMSB[channel] = 0;
                if (status[channel] == NRPN_START)
                    {
                    status[channel] = NRPN_END;
                    controllerNumberLSB[channel] = value;
                    controllerValueLSB[channel]  = -1;
                    controllerValueMSB[channel]  = -1;
                    }
                else status[channel] = INVALID;
                return null;
                }
                
            // Start of RPN or NULL
            else if (number == 101)
                {
                if (value == 127)  // this is the NULL termination tradition, see for example http://www.philrees.co.uk/nrpnq.htm
                    {
                    status[channel] = INVALID;
                    }
                else
                    {
                    status[channel] = RPN_START;
                    controllerNumberMSB[channel] = value;
                    }
                return null;
                }

            // End of RPN or NULL
            else if (number == 100)
                {
                controllerValueMSB[channel] = 0;
                if (value == 127)  // this is the NULL termination tradition, see for example http://www.philrees.co.uk/nrpnq.htm
                    {
                    status[channel] = INVALID;
                    }
                else if (status[channel] == RPN_START)
                    {
                    status[channel] = RPN_END;
                    controllerNumberLSB[channel] = value;
                    controllerValueLSB[channel]  = -1;
                    controllerValueMSB[channel]  = -1;
                    }
                return null;
                }

            else if ((number == 6 || number == 38 || number == 96 || number == 97) && (status[channel] == NRPN_END || status[channel] == RPN_END))  // we're currently parsing NRPN or RPN
                {
                int controllerNumber =  (((int) controllerNumberMSB[channel]) << 7) | controllerNumberLSB[channel] ;
                        
                if (number == 6)
                    {
                    controllerValueMSB[channel] = value;
                    if (requireLSB && controllerValueLSB[channel] == -1)
                        return null;
                    if (status[channel] == NRPN_END)
                        return handleNRPN(channel, controllerNumber, controllerValueLSB[channel] == -1 ? 0 : controllerValueLSB[channel], controllerValueMSB[channel], controllerValueLSB[channel] != -1, controllerValueMSB[channel] != -1 );
                    else
                        return handleRPN(channel, controllerNumber, controllerValueLSB[channel] == -1 ? 0 : controllerValueLSB[channel], controllerValueMSB[channel], controllerValueLSB[channel] != -1, controllerValueMSB[channel] != -1 );
                    }
                                                                                                                        
                // Data Entry LSB for RPN, NRPN
                else if (number == 38)
                    {
                    controllerValueLSB[channel] = value;
                    if (requireMSB && controllerValueMSB[channel] == -1)
                        return null;          
                    if (status[channel] == NRPN_END)
                        return handleNRPN(channel, controllerNumber, controllerValueLSB[channel], controllerValueMSB[channel] == -1 ? 0 : controllerValueMSB[channel], controllerValueLSB[channel] != -1 , controllerValueMSB[channel] != -1 );
                    else
                        return handleRPN(channel, controllerNumber, controllerValueLSB[channel], controllerValueMSB[channel] == -1 ? 0 : controllerValueMSB[channel], controllerValueLSB[channel] != -1 , controllerValueMSB[channel] != -1 );
                    }
                                                                                                                        
                // Data Increment for RPN, NRPN
                else if (number == 96)
                    {
                    if (value == 0)
                        value = 1;
                    if (status[channel] == NRPN_END)
                        return handleNRPNIncrement(channel, controllerNumber, value);
                    else
                        return handleRPNIncrement(channel, controllerNumber, value);
                    }

                // Data Decrement for RPN, NRPN
                else // if (number == 97)
                    {
                    if (value == 0)
                        value = -1;
                    if (status[channel] == NRPN_END)
                        return handleNRPNIncrement(channel, controllerNumber, -value);
                    else
                        return handleRPNIncrement(channel, controllerNumber, -value);
                    }
                                
                }
                        
            else  // Some other CC
                {
                // status[channel] = INVALID;           // I think it's fine to send other CC in the middle of NRPN or RPN
                return handleRawCC(channel, number, value);
                }
            }
        
        /** Top-level method to start the processor on a given CC message.  */
        public CCData processCC(ShortMessage message, boolean requireLSB, boolean requireMSB)
            {
            int num = message.getData1();
            int val = message.getData2();
            int channel = message.getChannel();
            return parseCC(channel, num, val, requireLSB, requireMSB);
            }
        
        /** Parses an NRPN message */
        public CCData handleNRPN(int channel, int controllerNumber, int _controllerValueLSB, int _controllerValueMSB, boolean validLSB, boolean validMSB)
            {
            if (_controllerValueLSB < 0 || _controllerValueMSB < 0)
                System.err.println("WARNING, LSB or MSB < 0.  RPN: " + controllerNumber + "   LSB: " + _controllerValueLSB + "  MSB: " + _controllerValueMSB);
            CCData data =  new CCData(CCData.TYPE_NRPN, controllerNumber, _controllerValueLSB | (_controllerValueMSB << 7), channel, false);
            data.validMSB = validMSB;
            data.validLSB = validLSB;
            return data;
            }
        
        /** Parses an NRPN increment message */
        public CCData handleNRPNIncrement(int channel, int controllerNumber, int delta)
            {
            return new CCData(CCData.TYPE_NRPN, controllerNumber, delta, channel, true);
            }

        /** Parses an RPN message */
        public CCData handleRPN(int channel, int controllerNumber, int _controllerValueLSB, int _controllerValueMSB, boolean validLSB, boolean validMSB)
            {
            if (_controllerValueLSB < 0 || _controllerValueMSB < 0)
                System.err.println("WARNING, LSB or MSB < 0.  RPN: " + controllerNumber + "   LSB: " + _controllerValueLSB + "  MSB: " + _controllerValueMSB);
            CCData data =  new CCData(CCData.TYPE_RPN, controllerNumber, _controllerValueLSB | (_controllerValueMSB << 7), channel, false);
            data.validMSB = validMSB;
            data.validLSB = validLSB;
            return data;
            }
        
        /** Parses an RPN increment message */
        public CCData handleRPNIncrement(int channel, int controllerNumber, int delta)
            {
            return new CCData(CCData.TYPE_RPN, controllerNumber, delta, channel, true);
            }

        /** Parses a CC message */
        public CCData handleRawCC(int channel, int controllerNumber, int value)
            {
            return new CCData(CCData.TYPE_RAW_CC, controllerNumber, value, channel, false);
            }
        }
                
    Parser parser = new Parser();
    
    /** Returns the CC/NRPN/RPN parser */ 
    public Parser getParser() { return parser; }
            
    /** Returns a useful string describing the given MidiMessage */
    public static String format(MidiMessage message)
        {
        if (message instanceof MetaMessage)
            {
            return "A MIDI File MetaMessage (shouldn't happen)";
            }
        else if (message instanceof SysexMessage)
            {
            return "Sysex (" + getManufacturerForSysex(((SysexMessage)message).getData()) + ")";
            }
        else // ShortMessage
            {
            ShortMessage s = (ShortMessage) message;
            int c = s.getChannel();
            String type = "Unknown";
            switch(s.getStatus())
                {
                case ShortMessage.ACTIVE_SENSING: type = "Active Sensing"; c = -1; break;
                case ShortMessage.CHANNEL_PRESSURE: type = "Channel Pressure"; break;
                case ShortMessage.CONTINUE: type = "Continue"; c = -1; break;
                case ShortMessage.CONTROL_CHANGE: type = "Control Change"; break;
                case ShortMessage.END_OF_EXCLUSIVE: type = "End of Sysex Marker"; c = -1; break;
                case ShortMessage.MIDI_TIME_CODE: type = "Midi Time Code"; c = -1; break;
                case ShortMessage.NOTE_OFF: type = "Note Off"; break;
                case ShortMessage.NOTE_ON: type = "Note On"; break;
                case ShortMessage.PITCH_BEND: type = "Pitch Bend"; break;
                case ShortMessage.POLY_PRESSURE: type = "Poly Pressure"; break;
                case ShortMessage.PROGRAM_CHANGE: type = "Program Change"; break;
                case ShortMessage.SONG_POSITION_POINTER: type = "Song Position Pointer"; c = -1; break;
                case ShortMessage.SONG_SELECT: type = "Song Select"; c = -1; break;
                case ShortMessage.START: type = "Start"; c = -1; break;
                case ShortMessage.STOP: type = "Stop"; c = -1; break;
                case ShortMessage.SYSTEM_RESET: type = "System Reset"; c = -1; break;
                case ShortMessage.TIMING_CLOCK: type = "Timing Clock"; c = -1; break;
                case ShortMessage.TUNE_REQUEST: type = "Tune Request"; c = -1; break;
                }
            return type + (c == -1 ? "" : (" (Channel " + c + ")"));
            }
        }


    
    
    static HashMap manufacturers = null;
    
    // Builds (if necessary) and returns a list of all current manufacturers hashed by sysex ID
    static HashMap getManufacturers()
        {
        if (manufacturers != null)
            return manufacturers;
                        
        manufacturers = new HashMap();
        Scanner scan = new Scanner(Midi.class.getResourceAsStream("Manufacturers.txt"));
        while(scan.hasNextLine())
            {
            String nextLine = scan.nextLine().trim();
            if (nextLine.equals("")) continue;
            if (nextLine.startsWith("#")) continue;
                        
            int id = 0;
            Scanner scan2 = new Scanner(nextLine);
            int one = scan2.nextInt(16);  // in hex
            if (one == 0x00)  // there are two more to read
                {
                id = id + (scan2.nextInt(16) << 8) + (scan2.nextInt(16) << 16);
                }
            else
                {
                id = one;
                }
            manufacturers.put(new Integer(id), scan.nextLine().trim());
            }
        return manufacturers;
        }

    /** Returns the manufacturer for a given sysex string. This works with or without F0 as the first data byte */
    public static String getManufacturerForSysex(byte[] data)
        {
        int offset = 0;
        if (data[0] == (byte)0xF0)
            offset = 1;
        HashMap map = getManufacturers();
        if (data[0 + offset] == (byte)0x7D)             // educational use
            {
            return (String)(map.get(new Integer(data[0 + offset]))) + 
                "\n\nNote that unregistered manufacturers or developers typically\n use this system exclusive region.";
            }
        else if (data[0 + offset] == (byte)0x00)
            {
            return (String)(map.get(new Integer(
                        0x00 + 
                        ((data[1 + offset] < 0 ? data[1 + offset] + 256 : data[1 + offset]) << 8) + 
                        ((data[2 + offset] < 0 ? data[2 + offset] + 256 : data[2 + offset]) << 16))));
            }
        else
            {
            return (String)(map.get(new Integer(data[0 + offset])));
            }
        }

            
        

    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**  
     A Modulation with a variety of useful incoming MIDI signals.  MIDIIn has:
        
     <ol>
     <li> The GATE value: 1 or 0 depending on whether a note is ON (being played) or OFF (released).
     Additionally, when a note is pressed, this modulation is triggered.
     <li> The MIDI Note of the current note: a value 0.0...1.0, corresponding to a MIDI note value of 0...127.  Middle C is 60.
     Additionally, when a note is pressed, this modulation is triggered. 
     <li> The Attack Velocity of the current note, ranging from 0.0 to 1.0 (maximum).  
     Additionally, when a note is pressed, this modulation is triggered. 
     <li> The Release Velocity of the current note, ranging from 0.0 to 1.0 (maximum).  
     Additionally, when a note is released, this modulation is triggered. 
     <li> The current presssure (aftertouch), ranging from 0.0 to 1.0 (maximum).
     <li> A trigger whenever a MIDI Clock signal is received.
     <li> The current Pitch Bend value.  0.5 is no bend.  0.0 is lowest bend, and 1.0 is highest bend.
     <li> Eight CC values.  You specify which CC parameters are being exposed.  Note that
     CC parameters 6, 38, 98, 99, 100, and 101 are inoperative: they are reserved by the synth for NRPN,
     and cannot be used.
     </ol>
*/
        
public class MIDIIn extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;
        
    public MIDIIn(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { new Constant(74/127.0), new Constant(1/127.0), Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO},
            new String[] { "CC", "CC", "CC", "CC", "CC", "CC", "CC", "CC" });
        defineModulationOutputs( new String[] { "Gate", "Note", "Velocity", "Release", "Pressure", "Clock", "Bend", "", "", "", "", "", "", "", "" });

        for(int i = 0; i < lastCC.length; i++)
            lastCC[i] = Double.NaN;
        }

    public static final int MOD_GATE = 0;
    public static final int MOD_NOTE = 1;
    public static final int MOD_VELOCITY = 2;
    public static final int MOD_RELEASE_VELOCITY = 3;
    public static final int MOD_PRESSURE = 4;
    public static final int MOD_CLOCK = 5;
    public static final int MOD_BEND = 6;
    public static final int MOD_CC = 7;
    
    public static final int NUM_CC = 8;
    double[] lastCC = new double[NUM_CC];


    public Object clone()
        {
        MIDIIn obj = (MIDIIn)(super.clone());
        obj.lastCC = (double[])(obj.lastCC.clone());
        return obj;
        }


    boolean gated;
    boolean noteOnTrigger;
    boolean noteOffTrigger;
        
                
    public void gate()
        {
        super.gate();
        gated = true;
        noteOnTrigger = true;
        }
                
    public void release()
        {
        super.release();
        gated = false;
        noteOffTrigger = true;
        }

    public void go()
        {
        super.go();

        Sound sound = getSound();
        Output output = sound.getOutput();
        Input input = output.getInput();
        
        setModulationOutput(MOD_GATE, gated ? 1 : 0);
        setModulationOutput(MOD_NOTE, sound.getMIDINote() / 127.0);
        setModulationOutput(MOD_VELOCITY, sound.getVelocity());
        setModulationOutput(MOD_RELEASE_VELOCITY, sound.getReleaseVelocity());
        setModulationOutput(MOD_PRESSURE, sound.getAftertouch());
        setModulationOutput(MOD_BEND, (input.getRawBend() / 8191.0 + 1.0) / 2.0);

        if (noteOnTrigger)
            {
            updateTrigger(MOD_GATE);
            updateTrigger(MOD_NOTE);
            updateTrigger(MOD_VELOCITY);
            noteOnTrigger = false;
            }
        else if (noteOffTrigger)
            {
            updateTrigger(MOD_RELEASE_VELOCITY);
            noteOffTrigger = false;
            }

        if (input.getMidiClock().getClockPulseTrigger())
            { 
            updateTrigger(MOD_CLOCK); 
            setModulationOutput(MOD_CLOCK, 1.0); 
            }
        else setModulationOutput(MOD_CLOCK, 0.0);

        if (input.getMidiClock().getClockStartTrigger())
            { 
            resetTrigger(MOD_CLOCK); 
            setModulationOutput(MOD_CLOCK, 0.0);
            }
        
        for(int i = 0; i < NUM_CC; i++)
            {
            Input in = sound.getOutput().getInput();
            int cc = in.getCC(sound.getChannel(), (int)(modulate(i) * 127));
            if (cc != Input.UNSPECIFIED)
            	{
                double d = cc/127.0;
				if (d != lastCC[i])
					{
					updateTrigger(MOD_CC + i);
					setModulationOutput(MOD_CC + i, d);
					lastCC[i] = d;
					}
				}
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        return "" + ((int)(value * 127));
        }


    public ModulePanel getPanel()
        {
        return new ModulePanel(MIDIIn.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < MOD_CC; i++)
                    {
                    box.add(new ModulationOutput(mod, i, this));
                    }
                
                for(int i = 0; i < NUM_CC; i++)
                    {
                    Box hbox = new Box(BoxLayout.X_AXIS);
                    hbox.add(new ModulationInput(mod, i, this));
                    hbox.add(new ModulationOutput(mod, i + MOD_CC, this));
                    box.add(hbox);
                    }
                return box;
                }
            };
        }

    //// SERIALIZATION STUFF

    public static final String[] MOD_NAMES = new String[] { "CC_A", "CC_B", "CC_C", "CC_D", "CC_E", "CC_F", "CC_G", "CC_H" };

    public static final String[] MOD_OUT_NAMES = new String[] { "Gate", "Note", "Velocity", "Release", "Pressure", "Clock", "Bend", "Out_A", "Out_B", "Out_C", "Out_D", "Out_E", "Out_F", "Out_G", "Out_H" };

    public String getKeyForModulation(int input)
        {
        return MOD_NAMES[input];
        }
         
    public String getKeyForModulationOutput(int output)
        {
        return MOD_OUT_NAMES[output];
        }
    }

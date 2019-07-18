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
        
public class MPE extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;
        
    public MPE(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, new Constant(3.0/4.0), Constant.HALF, new Constant(3.0 / 4.0) },
            new String[] { "Offset", "Variance", "Offset", "Variance" });
        defineModulationOutputs( new String[] { "Velocity", "Release", "Bend", "Pressure", "Y (CC 74)", });
        }

    public static final int MOD_VELOCITY = 0;
    public static final int MOD_RELEASE_VELOCITY = 1;
    public static final int MOD_BEND = 2;
    public static final int MOD_PRESSURE = 3;
    public static final int MOD_Y = 4;
    
    public static final int MOD_IN_PRESSURE_OFFSET = 0;
    public static final int MOD_IN_PRESSURE_VARIANCE = 1;
    public static final int MOD_IN_Y_OFFSET = 2;
    public static final int MOD_IN_Y_VARIANCE = 3;
                    
    public void go()
        {
        super.go();

        Sound sound = getSound();
        Output output = sound.getOutput();
        Input input = output.getInput();
        
        setModulationOutput(MOD_VELOCITY, sound.getVelocity());
        setModulationOutput(MOD_RELEASE_VELOCITY, sound.getReleaseVelocity());
        setModulationOutput(MOD_BEND, (input.getRawBend() / 8191.0 + 1.0) / 2.0);

        double pressureOffset = modulate(MOD_IN_PRESSURE_OFFSET);
        double pressureVariance = (modulate(MOD_IN_PRESSURE_VARIANCE) - 0.5) * 4.0;
        double yOffset = modulate(MOD_IN_Y_OFFSET) - 0.5;
        double yVariance = (modulate(MOD_IN_Y_VARIANCE) - 0.5) * 4.0;
        
        double at = sound.getAftertouch() * Math.abs(pressureVariance) + pressureOffset;
        if (at < 0) at = 0;
        if (at > 1) at = 1;
        if (pressureVariance < 0) at = 1 - at;
        setModulationOutput(MOD_PRESSURE, at);
        
        int cc = input.getCC(sound.getChannel(), 74);
        if (cc != Input.UNSPECIFIED)
            {
            double y = (cc / 127.0) * Math.abs(yVariance) + yOffset;
            if (y < 0) y = 0;
            if (y > 1) y = 1;
            if (yVariance < 0) y = 1 - y;

            setModulationOutput(MOD_Y, y);
            }
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(MPE.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);

                box.add(new ModulationOutput(mod, MOD_VELOCITY, this));
                box.add(new ModulationOutput(mod, MOD_RELEASE_VELOCITY, this));
                box.add(new ModulationOutput(mod, MOD_BEND, this));
                box.add(new ModulationOutput(mod, MOD_PRESSURE, this));
                box.add(new ModulationInput(mod, MOD_IN_PRESSURE_OFFSET, this));
                box.add(new ModulationInput(mod, MOD_IN_PRESSURE_VARIANCE, this));
                box.add(new ModulationOutput(mod, MOD_Y, this));
                box.add(new ModulationInput(mod, MOD_IN_Y_OFFSET, this));
                box.add(new ModulationInput(mod, MOD_IN_Y_VARIANCE, this));

                return box;
                }
            };
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (modulation == MOD_IN_PRESSURE_VARIANCE || modulation == MOD_IN_Y_VARIANCE)
            {
            return String.format("%.4f", ((value - 0.5) * 4.0));
            }
        else return super.getModulationValueDescription(modulation, value, isConstant);
        }




    //// SERIALIZATION STUFF

    public static final String[] MOD_NAMES = new String[] { "Pressure Offset", "Pressure Variance", "Y Offset", "Y Variance" };

    public String getKeyForModulation(int input)
        {
        return MOD_NAMES[input];
        }
    }

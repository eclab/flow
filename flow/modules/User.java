// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**
   A Unit which shifts the frequency of all partials.  There are three
   options:
   
   <ol>
   <li>Pitch.  Shift all partials by a certain pitch (for example, up an octave).
   This multiplies their frequencies by a certain amount.
   <li>Frequency.  Add a certain amount to the frequency of all partials.
   partials based on their distance from the nearest
   <li>Partials.  Move all partials towards the frequency of the next partial.
   </ol>
   
   The degree of shifting depends on the SHIFT modulation, bounded by the
   BOUND modulation.
*/

public class User extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_MODULATIONS = 4;
    
    public static final String[] MODULATION_NAMES =   new String[] { "A", "B", "C", "D" };
    public static final String[] MODULATION_OUTPUT_NAMES =   new String[] { "A", "B", "C", "D" };
        
    public User(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO}, MODULATION_NAMES);
        defineModulationOutputs(MODULATION_OUTPUT_NAMES);  // even though they're unlabeled, it's important that these have names so they save properly.
        }
        
    boolean[] trigger = new boolean[NUM_MODULATIONS];
        
    public void go()
        {
        super.go();
        
        for(int i = 0; i < NUM_MODULATIONS; i++)
            {
            setModulationOutput(i, modulate(i));
            if (isTriggered(i) || trigger[i]) updateTrigger(i);
            trigger[i] = false;
            }
        }       

    public ModulePanel getPanel()
        {
        return new ModulePanel(User.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_MODULATIONS; i++)
                    {
                    final int _i = i;

                    Box hbox = new Box(BoxLayout.X_AXIS);
                    ModulationOutput output = new ModulationOutput(mod, i, this);
                    output.setTitleText("", false);
                    ModulationInput input = new ModulationInput(mod, i, this);
                    hbox.add(input);
                    hbox.add(output);
                    box.add(hbox);
                    }

                for(int i = 0; i < NUM_MODULATIONS; i++)
                    {
                    final int _i = i;
                    PushButton button = new PushButton("Tr " + MODULATION_NAMES[i])
                        {
                        public void perform()
                            {
                            Output output = getRack().getOutput();
                            output.lock();
                            int numSounds = output.getNumSounds();
                            try
                                {
                                if (output.getOnlyPlayFirstSound())
                                    {
                                    // This triggers sound 0
                                    int index = sound.findRegistered(User.this);

                                    Sound s = output.getInput().getLastPlayedSound();
                                    try 
                                        {
                                        if (s == null) 
                                            s = output.getSound(0);
                                        }
                                    catch (Exception e)
                                        {
                                        s = null;
                                        }
                                    if (s != null)
                                        {
                                        if (s.getGroup() == Output.PRIMARY_GROUP)
                                            {
                                            User user = (User)(s.getRegistered(index));
                                            user.trigger[_i] = true;
                                            }
                                        }
                                    }
                                else
                                    {
                                    // This triggers *all* the sounds.
                                    int index = sound.findRegistered(User.this);
                                    for(int j = 0; j < numSounds; j++)
                                        {
                                        Sound s = output.getSound(j);
                                        if (s.getGroup() == Output.PRIMARY_GROUP)
                                            {
                                            User user = (User)(s.getRegistered(index));
                                            user.trigger[_i] = true;
                                            }
                                        }
                                    }                                
                                }
                            finally 
                                {
                                output.unlock();
                                }
                            }
                        };
                    box.add(button);
                    }
                return box;
                }
            };
        }
    }

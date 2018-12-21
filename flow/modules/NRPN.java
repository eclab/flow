// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**
   A Modulation which provides multiple signals associated with
   the current values of up to six NRPN parameters.  You specify 
   the MSB and LSB of the NRPN parameters in question with their
   corresponding incoming LSB and MSB modulations.
   
   <p>Inputs to NRPN are always expected to be "Fine", that is,
   values starting at 0 and potentially ranging all the way to 
   16383 without skipping any (another approach is to simulate
   just 128 values by going from 0...16383 jumping by 128 each
   step.  This is sometimes called "Coarse" or "MSB-only".  We
   don't do that).
*/

public class NRPN extends Modulation implements ModSource
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_NRPN = 6;

	double current[] = new double[NUM_NRPN];
    public NRPN(Sound sound)
        {
        super(sound);
                
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO },
            new String[] { "MSB", "LSB", "MSB", "LSB", "MSB", "LSB", "MSB", "LSB", "MSB", "LSB", "MSB", "LSB", });
        defineModulationOutputs( new String[] { "", "", "", "", "", "" });
        }

    public void go()
        {
        super.go();

        Sound sound = getSound();        
        for(int i = 0; i < NUM_NRPN; i++)
            {
            Input in = sound.getOutput().getInput();
            int num = ((int)modulate(i * 2) * 127 * 128) + (int)(modulate(i * 2 + 1) * 127);
            int val = in.getNRPN(num);
            if (!in.getMSBSentLast(num))				// it wasn't just an MSB, which we ignore
            	current[i] = (val / 128.0) / 127.0;
            setModulationOutput(i, current[i]);
            }
        }


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        return "" + ((int)(value * 127));
        }

    public ModulePanel getPanel()
        {
        final JLabel[] lsbLabels = new JLabel[NUM_NRPN];
        final ModulationInput[] lsbInputs = new ModulationInput[NUM_NRPN];
        final ModulationInput[] msbInputs = new ModulationInput[NUM_NRPN];
        final JLabel example = new JLabel("  16383");
        example.setFont(Style.SMALL_FONT());

        return new ModulePanel(NRPN.this)
            {
            public JLabel getAuxModulationInputTitle(int number)
                {
                if (number % 2 == 0) return null;
                        
                JLabel label = new JLabel("  0");
                label.setPreferredSize(example.getPreferredSize());
                label.setFont(Style.SMALL_FONT());
                lsbLabels[number / 2] = label;
                return label;
                }

            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();

                Box box = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < NUM_NRPN; i++)
                    {
                    final int _i = i;

                    Box hbox = new Box(BoxLayout.X_AXIS);
                    ModulationOutput output = new ModulationOutput(mod, i, this);
                    ModulationInput msb = new ModulationInput(mod, i * 2, this)
                        {
                        public void setState(double state)
                            {
                            super.setState(state);
                            lsbLabels[_i].setText("  " + ((((int)(msbInputs[_i].getState() * 127)) * 128) + (int)(lsbInputs[_i].getState() * 127)));
                            }
                        };
                    msbInputs[i] = msb;
                    hbox.add(msb);
                    hbox.add(output);
                    box.add(hbox);
                    ModulationInput lsb = new ModulationInput(mod, i * 2 + 1, this)
                        {
                        public void setState(double state)
                            {
                            super.setState(state);
                            lsbLabels[_i].setText("  " + ((((int)(msbInputs[_i].getState() * 127)) * 128) + (int)(lsbInputs[_i].getState() * 127)));
                            }
                        };
                    lsbInputs[i] = lsb;
                    box.add(lsb);
                    }
                return box;
                }
            };
        }


    //// SERIALIZATION STUFF

    public static final String[] MOD_NAMES = 
        new String[] { "MSB_A", "LSB_A", "MSB_B", "LSB_B", "MSB_C", "LSB_C", "MSB_D", "LSB_D", "MSB_E", "LSB_E", "MSB_F", "LSB_F" };

    public static final String[] MOD_OUT_NAMES =
        new String[] { "Out_A", "Out_B", "Out_C", "Out_D", "Out_E", "Out_F" };

    public String getKeyForModulation(int input)
        {
        return MOD_NAMES[input];
        }
         
    public String getKeyForModulationOutput(int output)
        {
        return MOD_OUT_NAMES[output];
        }
    }

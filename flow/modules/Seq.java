// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;

public class Seq extends Modulation
    {       
    private static final long serialVersionUID = 1;

	public static final String[] PITCH_NAMES = new String[]
		{
		"C  -12",
		"Db -11",
		"D  -10",
		"Eb -9",
		"E  -8",
		"F  -7",
		"Gb -6",
		"G  -5",
		"Ab -4",
		"A  -3",
		"Bb -2",
		"B  -1",
		"C  0",
		"Db 1",
		"D  2",
		"Eb 3",
		"E  4",
		"F  5",
		"Gb 6",
		"G  7",
		"Ab 8",
		"A  9",
		"Bb 10",
		"B  11",
		"C  12",
		};
	
	public static final double[] PITCHES = new double[]
		{
		(1.0 - 12/12.0) / 4.0 + 0.25,
		(1.0 - 11/12.0) / 4.0 + 0.25,
		(1.0 - 10/12.0) / 4.0 + 0.25,
		(1.0 - 9/12.0) / 4.0 + 0.25,
		(1.0 - 8/12.0) / 4.0 + 0.25,
		(1.0 - 7/12.0) / 4.0 + 0.25,
		(1.0 - 6/12.0) / 4.0 + 0.25,
		(1.0 - 5/12.0) / 4.0 + 0.25,
		(1.0 - 4/12.0) / 4.0 + 0.25,
		(1.0 - 3/12.0) / 4.0 + 0.25,
		(1.0 - 2/12.0) / 4.0 + 0.25,
		(1.0 - 2/12.0) / 4.0 + 0.25,
		(1) / 4.0 + 0.25,
		(1.0 + 1/12.0) / 4.0 + 0.25,
		(1.0 + 2/12.0) / 4.0 + 0.25,
		(1.0 + 3/12.0) / 4.0 + 0.25,
		(1.0 + 4/12.0) / 4.0 + 0.25,
		(1.0 + 5/12.0) / 4.0 + 0.25,
		(1.0 + 6/12.0) / 4.0 + 0.25,
		(1.0 + 7/12.0) / 4.0 + 0.25,
		(1.0 + 8/12.0) / 4.0 + 0.25,
		(1.0 + 9/12.0) / 4.0 + 0.25,
		(1.0 + 10/12.0) / 4.0 + 0.25,
		(1.0 + 11/12.0) / 4.0 + 0.25,
		(1.0 + 12/12.0) / 4.0 + 0.25,
		};

	public static final int MOD_STEPS = 16;
	public static final int MOD_TRIGGER = 17;
	public static final int NUM_STATES = 16;
	
	int state = 0;
	boolean free = false;
	boolean sample = true;

    public boolean getFree() { return free; }
    public void setFree(boolean val) { free = val; }
    public boolean getSample() { return sample; }
    public void setSample(boolean val) { sample = val; }
        
    public static final int OPTION_FREE = 0;
    public static final int OPTION_SAMPLE = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FREE: return (getFree() ? 1 : 0);
            case OPTION_SAMPLE: return (getSample() ? 1 : 0);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FREE: setFree(value != 0); return;
            case OPTION_SAMPLE: setSample(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public Seq(Sound sound)
        {
        super(sound);
        defineOptions(new String[] { "Free", "Sample" }, new String[][] { { "Free" }, { "Sample" } });
        defineModulations(new Constant[] 
        	{ Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
        	  Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
        	  Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF,
        	  Constant.HALF, Constant.HALF, Constant.HALF, Constant.HALF, 
        	  Constant.ONE, Constant.ZERO, },
        	new String[] {
        	  "1", "2", "3", "4", 
        	  "5", "6", "7", "8",
        	  "9", "10", "11", "12", 
        	  "13", "14", "15", "16", "Steps", "Trigger"
        	});
        }

	public void reset()
		{
		super.gate();
		state = 0;
        setModulationOutput(0, modulate(state));
        updateTrigger(0);
		}
		
	public void gate()
		{
		super.gate();
		if (!free) 
			{
			state = 0;
			setModulationOutput(0, modulate(state));
			updateTrigger(0);
			}
		}
	
    public void go()
        {
        super.go();
        
        if (isTriggered(MOD_TRIGGER))
        	{
        	state++;
        	int maxState = (int)(modulate(MOD_STEPS) * (NUM_STATES - 1) + 1);
        	if (state >= maxState) state = 0;
       		setModulationOutput(0, modulate(state));
        	updateTrigger(0);
        	}
        else if (!sample)
        	{
	        setModulationOutput(0, modulate(state));
	        }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_STEPS)
                {
                return "" + (int)(modulate(MOD_STEPS) * (NUM_STATES - 1) + 1);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Seq.this)
            {
            public JComponent buildPanel()
                {
                JLabel example = new JLabel("0.0000 ");
                example.setFont(Style.SMALL_FONT());
                
                Modulation mod = getModulation();
                Box box = new Box(BoxLayout.Y_AXIS);
                box.add(new ModulationOutput(mod, 0, this));

                for(int i = 0; i < NUM_STATES; i+=4)
                    {
                    Box box2 = new Box(BoxLayout.X_AXIS);
                    for(int j = i; j < i + 4; j++)
                    	{
						ModulationInput m = new ModulationInput(mod, j, this)
							{
							public String[] getOptions() { return PITCH_NAMES; }
							public double convert(int elt) 
								{
								return PITCHES[elt];
								}
							};
						m.getData().setPreferredSize(example.getMinimumSize());
						box2.add(m);
						}
					box.add(box2);
                    }
                box.add(new ModulationInput(mod, MOD_STEPS, this));
                box.add(new ModulationInput(mod, MOD_TRIGGER, this));

                for(int i = 0; i < mod.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(mod, i));
                    }
                    
                return box;
                }
            };
        }

    }

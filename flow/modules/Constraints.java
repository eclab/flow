// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/**
   A Unit which provides useful constraints to feed into the Constraints input of various modules.
*/

public class Constrain extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public Constrain(Sound sound) 
        {
        super(sound);
        defineOptions(new String[] {  "Not1", "Not2", "Not3", "Not4", "Harmonics1", "Harmonics2", "Harmonics3", "Harmonics4" }, 
        	new String[][] { { "Not1" }, { "Not2" }, { "Not3" }, { "Not4" }, constraintNames, constraintNames, constraintNames, constraintNames } );
		setOptionValue(1, 1);		// turn on Not
		setOptionValue(2, 1);		// turn on Not
        defineInputs(new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Only1", "Only2", "Only3", "Only4" });
        }

    boolean not0;
    boolean not1;
    boolean not2;
    boolean not3;
    int only0;
    int only1;
    int only2;
    int only3;
        
    public boolean getNot0() { return not0; }
    public void setNot0(boolean val) { not0 = val; }
    public boolean getNot1() { return not1; }
    public void setNot1(boolean val) { not1 = val; }
    public boolean getNot2() { return not2; }
    public void setNot2(boolean val) { not2 = val; }
    public boolean getNot3() { return not3; }
    public void setNot3(boolean val) { not3 = val; }
    public int getOnly0() { return only0; }
    public void setOnly0(int val) { only0 = val; }
    public int getOnly1() { return only1; }
    public void setOnly1(int val) { only1 = val; }
    public int getOnly2() { return only2; }
    public void setOnly2(int val) { only2 = val; }
    public int getOnly3() { return only3; }
    public void setOnly3(int val) { only3 = val; }
        
    public static final int OPTION_NOT_0 = 0;
    public static final int OPTION_NOT_1 = 1;
    public static final int OPTION_NOT_2 = 2;
    public static final int OPTION_NOT_3 = 3;
    public static final int OPTION_ONLY_0 = 4;
    public static final int OPTION_ONLY_1 = 5;
    public static final int OPTION_ONLY_2 = 6;
    public static final int OPTION_ONLY_3 = 7;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_NOT_0: return getNot0() ? 1 : 0;
            case OPTION_NOT_1: return getNot1() ? 1 : 0;
            case OPTION_NOT_2: return getNot2() ? 1 : 0;
            case OPTION_NOT_3: return getNot3() ? 1 : 0;
            case OPTION_ONLY_0: return getOnly0();
            case OPTION_ONLY_1: return getOnly1();
            case OPTION_ONLY_2: return getOnly2();
            case OPTION_ONLY_3: return getOnly3();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_NOT_0: setNot0(value != 0); return;
            case OPTION_NOT_1: setNot1(value != 0); return;
            case OPTION_NOT_2: setNot2(value != 0); return;
            case OPTION_NOT_3: setNot3(value != 0); return;
            case OPTION_ONLY_0: setOnly0(value); return;
            case OPTION_ONLY_1: setOnly1(value); return;
            case OPTION_ONLY_2: setOnly2(value); return;
            case OPTION_ONLY_3: setOnly3(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    public void go()
        {
        super.go();
        
        double[] amp = getAmplitudes(0);

		// BASE...
		
        if (isInputNil(0))
        	{
        	boolean not = getNot0();
        	int only = getOnly0();
        	int[] harm = (not ? ANTI_HARMONICS[only] : HARMONICS[only]);
        	for(int i = 0; i < amp.length; i++) { amp[i] = 0; }
        	for(int i = 0; i < harm.length; i++)
        		{
        		amp[harm[i]] = 1;
        		}
        	}
        else
        	{
        	double[] in = getAmplitudesIn(0);
        	boolean not = getNot0();
        	for(int i = 0; i < amp.length; i++)
        		{
        		amp[i] = (not ? (in[i] > 0 ? 0 : 1) :
        						(in[i] > 0 ? 1 : 0));
        		}
        	}


		// OR...
		
        if (isInputNil(1))
        	{
        	boolean not = getNot1();
        	int only = getOnly1();
        	int[] harm = (not ? ANTI_HARMONICS[only] : HARMONICS[only]);
        	for(int i = 0; i < harm.length; i++)
        		{
        		amp[harm[i]] = 1;
        		}
        	}
        else
        	{
        	double[] in = getAmplitudesIn(1);
        	boolean not = getNot1();
        	for(int i = 0; i < amp.length; i++)
        		{
        		amp[i] = (not ? (in[i] > 0 ? amp[i] : 1) :
        						(in[i] > 0 ? 1 : amp[i]));
        		}
        	}


		// OR...
		
        if (isInputNil(2))
        	{
        	boolean not = getNot2();
        	int only = getOnly2();
        	int[] harm = (not ? ANTI_HARMONICS[only] : HARMONICS[only]);
        	for(int i = 0; i < harm.length; i++)
        		{
        		amp[harm[i]] = 1;
        		}
        	}
        else
        	{
        	double[] in = getAmplitudesIn(2);
        	boolean not = getNot2();
        	for(int i = 0; i < amp.length; i++)
        		{
        		amp[i] = (not ? (in[i] > 0 ? amp[i] : 1) :
        						(in[i] > 0 ? 1 : amp[i]));
        		}
        	}


		// AND...
		
        if (isInputNil(3))
        	{
        	boolean not = getNot3();
        	int only = getOnly3();
        	int[] harm = (not ? HARMONICS[only] : ANTI_HARMONICS[only]);	// inverted
        	for(int i = 0; i < harm.length; i++)
        		{
        		amp[harm[i]] = 0;
        		}
        	}
        else
        	{
        	double[] in = getAmplitudesIn(3);
        	boolean not = getNot3();
        	for(int i = 0; i < amp.length; i++)
        		{
        		amp[i] *= (not ? (in[i] > 0 ? 0 : amp[i] ) :
        						 (in[i] > 0 ? amp[i] : 0));
        		}
        	}	
        }
        
    public boolean isConstrainable() { return false; }


    public ModulePanel getPanel()
        {
        return new ModulePanel(Constraints.this)
            {
            public JComponent buildPanel()
                {               
                Modulation mod = getModulation();
				Unit unit = (Unit) mod;
				OptionsChooser only = null;
				OptionsChooser not = null;
				UnitInput input = null;
				
                Box box = new Box(BoxLayout.Y_AXIS);
				box.add(new UnitOutput(unit, 0, this));				
				
				Box box2 = null;
				
				box2 = new Box(BoxLayout.X_AXIS);
				only = new OptionsChooser(mod, 4);
				only.setTitleText("Include...");
				box.add(only);
				input = new UnitInput(unit, 0, this);
				input.setTitleText("Only", false);
				box2.add(input);
				not = new OptionsChooser(mod, 0);
				not.setTitleText("Not");
				box2.add(not);
				box.add(box2);
				box.add(Strut.makeVerticalStrut(10));
				
				box2 = new Box(BoxLayout.X_AXIS);
				only = new OptionsChooser(mod, 5);
				only.setTitleText("Also Include...");
				box.add(only);
				input = new UnitInput(unit, 1, this);
				input.setTitleText("Only", false);
				box2.add(input);
				not = new OptionsChooser(mod, 1);
				not.setTitleText("Not");
				box2.add(not);
				box.add(box2);
				box.add(Strut.makeVerticalStrut(10));

				box2 = new Box(BoxLayout.X_AXIS);
				only = new OptionsChooser(mod, 6);
				only.setTitleText("Also Include...");
				box.add(only);
				input = new UnitInput(unit, 2, this);
				input.setTitleText("Only", false);
				box2.add(input);
				not = new OptionsChooser(mod, 2);
				not.setTitleText("Not");
				box2.add(not);
				box.add(box2);
				box.add(Strut.makeVerticalStrut(10));

				box2 = new Box(BoxLayout.X_AXIS);
				only = new OptionsChooser(mod, 7);
				only.setTitleText("Reduce To...");
				box.add(only);
				input = new UnitInput(unit, 3, this);
				input.setTitleText("Only", false);
				box2.add(input);
				not = new OptionsChooser(mod, 3);
				not.setTitleText("Not");
				box2.add(not);
				box.add(box2);
				box.add(Strut.makeVerticalStrut(10));

                return box;
                }
            };
        }

    }

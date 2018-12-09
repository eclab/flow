package flow.modules;

import flow.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;


/** 
    A Unit which allows you to shape the amplitudes of harmonics
    using a combination of three different functions, each with
    its own constraints, gain, function type, and function argument.
    The first function takes precedence over the second, which
    takes precedence over the third (with regard to constraints).
*/

public class PartialLab extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_FUNCTIONS = 3;
        
    public static final int TYPE_LINEAR = 0;
    public static final int TYPE_POW_OF_ONE_MINUS_X = 1;
    public static final int TYPE_POW_OF_ONE_PLUS_X = 2;
    public static final int TYPE_ONE_MINUS_POW_OF_X = 3;
        
    int[] oldType = new int[NUM_FUNCTIONS];
    int[] type = new int[NUM_FUNCTIONS];
    int[] oldConstraint = new int[NUM_FUNCTIONS];
    int[] constraint = new int[NUM_FUNCTIONS];
    double[] lastMod = new double[NUM_FUNCTIONS];
    double[] lastGain = new double[NUM_FUNCTIONS];
        
    public int getType(int num) { return type[num]; }
    public void setType(int num, int val) { type[num] = val; }
    public int getConstraintOf(int num) { return constraint[num]; }
    public void setConstraintOf(int num, int val) { constraint[num] = val; }
        
    public Object clone()
        {
        PartialLab obj = (PartialLab)(super.clone());
        obj.oldType = (int[])(obj.oldType.clone());
        obj.type = (int[])(obj.type.clone());
        obj.oldConstraint = (int[])(obj.oldConstraint.clone());
        obj.constraint = (int[])(obj.constraint.clone());
        obj.lastMod = (double[])(obj.lastMod.clone());
        obj.lastGain = (double[])(obj.lastGain.clone());
        return obj;
        }


    public PartialLab(Sound sound) 
        {
        super(sound);
        
        String[] typeNames = new String[] { "Linear", "(1-x)^a", "(1+x)^-a", "1-(x^a)" };
        String[] constNames = new String[constraintNames.length + 1];
        System.arraycopy(constraintNames, 0, constNames, 0, constraintNames.length);
        constNames[constNames.length - 1] = "All";
        
        defineModulations(new Constant[] { Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE, Constant.ONE }, 
            new String[] { "Amt 1", "Amt 2", "Amt 3", "Gain 1", "Gain 2", "Gain 3", });
        defineOptions(new String[] { "Type 1", "Type 2", "Type 3", "Constraint 1", "Constraint 2", "Constraint 3" }, 
            new String[][] { typeNames, typeNames, typeNames, constNames, constNames, constNames });

        setClearOnReset(false);
        boolean firstTime = true;
        }
        
    public static final int OPTION_TYPE_0 = 0;
    public static final int OPTION_TYPE_1 = 1;
    public static final int OPTION_TYPE_2 = 2;
    public static final int OPTION_CONSTRAINT_0 = 3;
    public static final int OPTION_CONSTRAINT_1 = 4;
    public static final int OPTION_CONSTRAINT_2 = 5;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TYPE_0: return getType(0);
            case OPTION_TYPE_1: return getType(1);
            case OPTION_TYPE_2: return getType(2);
            case OPTION_CONSTRAINT_0: return getConstraintOf(0);
            case OPTION_CONSTRAINT_1: return getConstraintOf(1);
            case OPTION_CONSTRAINT_2: return getConstraintOf(2);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TYPE_0: setType(0, value); return;
            case OPTION_TYPE_1: setType(1, value); return;
            case OPTION_TYPE_2: setType(2, value); return;
            case OPTION_CONSTRAINT_0: setConstraintOf(0, value); return;
            case OPTION_CONSTRAINT_1: setConstraintOf(1, value); return;
            case OPTION_CONSTRAINT_2: setConstraintOf(2, value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
    
    boolean firstTime;
    static final int[] ALL_PARTIALS = new int[NUM_PARTIALS];


    static boolean done = false;
    public void doStatic()
        {
        if (done) return;
        done = true;

        for(int i = 0; i < NUM_PARTIALS; i++)
            ALL_PARTIALS[i] = i;
        }
        
    public void go()
        {
        super.go();
                
        // load mods and determine if we should proceed with all these Math.pow calls
        boolean changed = false;
        if (firstTime) changed = true;
        firstTime = false;
        for(int i = 0; i < NUM_FUNCTIONS; i++)
            {
            double mod = modulate(i);
            if (mod != lastMod[i])
                { 
                changed = true; 
                lastMod[i] = mod; 
                }
            double gain = modulate(i + NUM_FUNCTIONS);
            if (gain != lastGain[i])
                { 
                changed = true; 
                lastGain[i] = gain; 
                }
            if (oldType[i] != type[i])
                {
                changed = true;
                oldType[i] = type[i];
                }
            if (oldConstraint[i] != constraint[i])
                {
                changed = true;
                oldConstraint[i] = constraint[i];
                }
            }
        if (!changed) return;
                
        double[] amplitudes = getAmplitudes(0);
                
        // okay, here we go
        for(int i = 0; i < amplitudes.length; i++)
            amplitudes[i] = 0;

        for(int i = NUM_FUNCTIONS - 1 ; i >= 0; i--)
            {
            double a = lastMod[i];
            double g = lastGain[i];
            if (constraint[i] == 0)  // constraint none, we deal with it specially
                continue;
                        
            int[] partials = ALL_PARTIALS;
            if (constraint[i] != constraintNames.length)  // that's our special "All"
                {
                setConstraint(constraint[i]);
                partials = getConstrainedPartials();
                }
                                
            // We're using Math.pow instead of Utility.hybridpow to avoid unsightly jumps.  :-(
                                
            for(int j = 0; j < partials.length; j++)
                {
                double x = (partials[j] / 127.0);
                switch(type[i])
                    {
                    case TYPE_LINEAR:
                        {
                        // Max(0, 1 - x * Tan((1 - a) pi / 2))                                          
                        amplitudes[partials[j]] = g * Math.max(0, 1 - x * Math.tan((1.0 - a) * Math.PI / 2.0));
                        }
                    break;
                    case TYPE_POW_OF_ONE_MINUS_X:
                        {
                        // (1 - x) ^ (((a - 1) * 2) ^8)
                        double b = (a - 1.0) * 2;
                        b = b * b;
                        b = b * b;
                        b = b * b;  // ^8
                        amplitudes[partials[j]] = g * Utility.fastpow(1.0 - x, b);
                        }
                    break;
                    case TYPE_POW_OF_ONE_PLUS_X:
                        {
                        // (1 + x) ^ -(((a - 1) * 4) ^4)        Note minus sign
                        double b = (a - 1.0) * 4;
                        b = b * b;
                        b = b * b;  // ^4
                        amplitudes[partials[j]] = g * Utility.fastpow(1.0 + x, 0 - b);
                        }
                    break;
                    case TYPE_ONE_MINUS_POW_OF_X:
                        {
                        // 1 - x ^ (a ^ 4 * 16 + 0.01)
                        double b = a;
                        b = b * b;
                        b = b * b;  // ^4
                        amplitudes[partials[j]] = g * (1.0 - Utility.fastpow(x, b * 16 + 0.01));
                        }
                    break;
                    default:
                    	{
                    	warn("modules/PartialLab.java", "default occurred when it shouldn't be possible");
                    	break;
                    	}
                    }
                }
            }
        }


    public ModulePanel getPanel()
        {
        return new ModulePanel(PartialLab.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                                
                for(int i = 0; i < NUM_FUNCTIONS; i++)
                    {
                    Box box2 = new Box(BoxLayout.X_AXIS);
                    box2.add(new ModulationInput(unit, i, this));
                    box2.add(new ModulationInput(unit, i + NUM_FUNCTIONS, this));
                    box.add(box2);
                    box.add(new OptionsChooser(unit, i));
                    box.add(new OptionsChooser(unit, i + NUM_FUNCTIONS));
                    }

                return box;
                }
            };
        }


    }

// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;

/**
   A Modulation which puts two incoming modulation signals through
   some function, producing an outgoing signal.  If the signals are
   A and B, the possible functions are:
   
   <ol>
   <li> ADD: Min(0, A + B)
   <li> SUBTRACT: Max(0, A - B)
   <li> MULTIPLY: A * B
   <li> MIN: Min(A, B)
   <li> MAX: Max(A, B)
   <li> SQUARE: Min(A^2 + B, 1)
   <li> SQRT: Min(Sqrt(A) + B, 1)
   <li> CUBE: Min(A^3 + B, 1)
   <li> DISCRETIZE: Discretizes A into Floor(B * 128) chunks
   <li> MAP HI: A * (1 - B) + B
   <li> AVG: (A + B) / 2
   <li> THRESHOLD: If A >= B, 1 else 0
   </ol>
   
   Additionally ModMath performs functions on the two incoming
   triggers, resulting in an outgoing trigger.  Specifically:
   
   <ol>
   <li> A: Trigger on A only
   <li> A or B: Trigger on A or B
   <li> A and B: Only trigger when *both* A and B trigger simultaneously
   <li> A and not B:  Only trigger when A has triggered but B is not simultaneously triggering
   <li> 2 A through 192 A: Rate divider.  Trigger every N times A triggers.
   </ol>
*/

public class ModMath extends Modulation
    {       
    private static final long serialVersionUID = 1;

    public static final int MOD_A = 0;
    public static final int MOD_B = 1;

    public static String getName() { return "Mod Math"; }

    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int MIN = 3;
    public static final int MAX = 4;
    public static final int SQUARE = 5;
    public static final int SQUARE_ROOT = 6;
    public static final int CUBE = 7;
    public static final int DISCRETIZE = 8;
    public static final int MAP_HIGH = 9;           // MAP_LOW is otherwise known as MULTIPLY
    public static final int AVERAGE = 10;
    public static final int THRESHOLD = 11;
    
    public static final int TRIGGER_A = 0;
    public static final int TRIGGER_A_OR_B = 1;
    public static final int TRIGGER_A_AND_B = 2;
    public static final int TRIGGER_A_AND_NOT_B = 3;
    public static final int TRIGGER_2_A = 4;
    public static final int TRIGGER_3_A = 5;
    public static final int TRIGGER_4_A = 6;
    public static final int TRIGGER_5_A = 7;
    public static final int TRIGGER_6_A = 8;
    public static final int TRIGGER_7_A = 9;
    public static final int TRIGGER_8_A = 10;
    public static final int TRIGGER_9_A = 11;
    public static final int TRIGGER_10_A = 12;
    public static final int TRIGGER_11_A = 13;
    public static final int TRIGGER_12_A = 14;
    public static final int TRIGGER_16_A = 15;
    public static final int TRIGGER_18_A = 16;
    public static final int TRIGGER_24_A = 17;
    public static final int TRIGGER_36_A = 18;
    public static final int TRIGGER_48_A = 19;
    public static final int TRIGGER_64_A = 20;
        
    public static final int DISCRETIZATION = 128;
        
    public static final String[] OPERATION_NAMES = new String[] { "+", "-", "x", "min", "max", "square", "sqrt", "cube", "discretize", "map hi", "average", "threshold"  };
    public static final String[] TRIGGER_NAMES = new String[] { "A", "A or B", "A and B", "A but not B", "2 A", "3 A", "4 A", "5 A", "6 A", "7 A", "8 A", "9 A", "10 A", "11 A", "12 A", "16 A", "18 A", "24 A", "32 A", "36 A", "48 A", "64 A", "72 A", "96 A", "144 A", "192 A" };
    public static final int[] TRIGGER_DIVIDERS = new int[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 16, 18, 24, 32, 36, 48, 64, 72, 96, 144, 192 };
    int operation = ADD;
    int triggerOperation = TRIGGER_A;
    int triggerCount = 0;
                
    public int getOperation() { return operation; }
    public void setOperation(int val) { operation = val; }

    public int getTriggerOperation() { return triggerOperation; }
    public void setTriggerOperation(int val) { triggerOperation = val; }

    public static final int OPTION_OPERATION = 0;
    public static final int OPTION_TRIGGER_OPERATION = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_OPERATION: return getOperation();
            case OPTION_TRIGGER_OPERATION: return getTriggerOperation();
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_OPERATION: setOperation(value); return;
            case OPTION_TRIGGER_OPERATION: setTriggerOperation(value); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public ModMath(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ZERO, Constant.ONE }, new String[] { "Signal A", "Signal B" });
        defineOptions(new String[] { "Operation", "Trigger On" }, new String[][] { OPERATION_NAMES, TRIGGER_NAMES } );
        }

    public void go()
        {
        super.go();
        
        double mod0 = modulate(MOD_A);
        double mod1 = modulate(MOD_B);
                                
        double val = 0;
                
        switch(operation)
            {
            case ADD:
                {
                val = Math.min(1.0, mod1 + mod0);
                }
            break;
            case SUBTRACT:
                {
                val = Math.max(0.0, mod0 - mod1);
                }
            break;
            case MULTIPLY:
                {
                val = mod1 * mod0;
                }
            break;
            case MIN:
                {
                val = Math.min(mod1, mod0);
                }
            break;
            case MAX:
                {
                val = Math.max(mod1, mod0);
                }
            break;
            case SQUARE:
                {
                val = Math.min(1.0, mod1 + mod0 * mod0);
                }
            break;
            case SQUARE_ROOT:
                {
                // dunno if this could benefit from an approximation
                val = Math.min(1.0, mod1 + Utility.fastSqrt(mod0)); //Math.sqrt(mod0);
                }
            break;
            case CUBE:
                {
                val = Math.min(1.0, mod1 + mod0 * mod0 * mod0);
                }
            break;
            case DISCRETIZE:
                {
                int d = (int)(mod1 * DISCRETIZATION);
                if (d == 0) 
                    {
                    val = 0;
                    }
                else
                    {
                    int f = (int)(mod0 * (d + 1));
                    if (f > d) f = d;
                    val = f / (double) d;
                    }
                }
            break;
            case MAP_HIGH:
                {
                val = (1.0 - mod1) * mod0 + mod1;
                }
            break;
            case AVERAGE:
                {
                val = (mod1 + mod0) / 2.0;
                }
            break;
            case THRESHOLD:
                {
                val = (mod0 >= mod1 ? 1.0 : 0.0);
                }
            break;
            default:
            	{
            	System.err.println("WARNING(modules/ModMath.java): default occurred when it shouldn't be possible");
            	val = 0;
            	}
            }
                
        if (val > 1)
            val = 1;
        if (val < 0)
            val = 0;
                        
        // determine trigger
        switch(triggerOperation)
            {
            case TRIGGER_A:
                if (isTriggered(MOD_A))
                    updateTrigger(0);
                break;
            case TRIGGER_A_OR_B:
                if (isTriggered(MOD_A) ||
                    isTriggered(MOD_B))
                    updateTrigger(0);
                break;
            case TRIGGER_A_AND_B:
                if (isTriggered(MOD_A) &&
                    isTriggered(MOD_B))
                    updateTrigger(0);
                break;
            case TRIGGER_A_AND_NOT_B:
                if (isTriggered(MOD_A) &&
                    !isTriggered(MOD_B))
                    updateTrigger(0);
                break;
            case TRIGGER_2_A:
            case TRIGGER_3_A:
            case TRIGGER_4_A:
            case TRIGGER_5_A:
            case TRIGGER_6_A:
            case TRIGGER_7_A:
            case TRIGGER_8_A:
            case TRIGGER_9_A:
            case TRIGGER_10_A:
            case TRIGGER_11_A:
            case TRIGGER_12_A:
            case TRIGGER_16_A:
            case TRIGGER_18_A:
            case TRIGGER_24_A:
            case TRIGGER_36_A:
            case TRIGGER_48_A:
            case TRIGGER_64_A:
                if (isTriggered(MOD_A))
                    triggerCount++;
                if (triggerCount >= TRIGGER_DIVIDERS[triggerOperation - TRIGGER_2_A])
                    {
                    triggerCount = 0;
                    updateTrigger(0);
                    }
                break;
            }
        setModulationOutput(0, val);
        }

    public void resetTrigger(int num)
        {
        super.resetTrigger(num);        
        triggerCount = 0;
        }


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 1 && operation == DISCRETIZE)  // signal b
                {
                return "" + (int)(value * DISCRETIZATION);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    // all this work just to update one lousy item
    public ModulePanel getPanel()
        {
        final ModulePanel[] m = new ModulePanel[1];
        m[0] = new ModulePanel(this)
            {
            public OptionsChooser buildOptionsChooser(Modulation modulation, int number)
                {
                return new OptionsChooser(modulation, number)
                    {
                    public void optionChanged(int optionNumber, int value)
                        {
                        super.optionChanged(optionNumber, value);
                        m[0].rebuild(); 
                        }
                    };
                }
            };
        return m[0];
        }

    }

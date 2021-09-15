// Copyright 2021 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.util.*;
import javax.swing.*;

/**
   This is basically the same class as KHARMONICS, except that the harmonics being
   loaded are derived from waves loaded from the AdventureKid Waveform collection.
*/

public class AKWF extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

	public static final int[] NUM_WAVES = 
		{
		100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 
		100, 100, 100, 100, 100, 100, 100, 100, 100, 111, 
		38, 26, 14, 68, 73, 4, 50, 10, 42, 52, 
		12, 100, 52, 25, 32, 19, 25, 33, 69, 45, 
		70, 22, 154, 73, 17, 122, 44, 50, 104, 85, 
		13, 158, 44, 30, 9, 36, 16, 47, /* 0,*/ 6, 
		17, 26, 137, 64, 14
		};

	public static final String[] CATEGORIES = 
		{
		"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", 
		"11", "12", "13", "14", "15", "16", "17", "18", "19", "20", 
		"aguitar", "altosax", "birds", "bitreduced", "bw blended", "bw perfectwaves", "bw saw", "bw sawbright", "bw sawgap", "bw sawrounded", 
		"bw sin", "bw squ", "bw squrounded", "bw tri", "c604", "cello", "clarinet", "clavinet", "dbass", "distorted", 
		"ebass", "eguitar", "eorgan", "epiano", "flute", "fmsynth", "granular", "hdrawn", "hvoice", "linear", 
		"oboe", "oscchip", "overtone", "piano", "pluckalgo", "raw", "sinharm", "snippets", /* "stereo.zip",*/ "stringbox", 
		"symetric", "theremin", "vgame", "vgamebasic", "violin"
		};
	
	public static String getName(int category, double mod)
		{
		int len = NAMES[category].length;
		return NAMES[category][(int)(mod * (len - 1))].substring(5).replace("_", " ");
		}
	
	public static double[] getHarmonics(int category, double mod)
		{
		int len = HARMONICS[category].length;
		return HARMONICS[category][(int)(mod * (len - 1))];
		}
	
	// HARMONICS[category][wave][harmonics]
	public static double[][][] HARMONICS = null;
	// NAMES[category][name]
	public static String[][] NAMES = null;

    public static final int MAX_HARMONICS = 256;  // regardless of number of partials
    public static final double MAX_AMPLITUDE = 999.0;  // highest legal amplitude

	public static final int MOD_MIX = 0;
	public static final int MOD_SOUND_1 = 1;
	public static final int MOD_SOUND_2 = 2;

    boolean normalize;
    boolean oldNormalize;
    public boolean getNormalize() { return normalize; }
    public void setNormalize(boolean val) { normalize = val; setHarmonics(getHarmonics()); }
    
    double mix = Double.NaN;
    double sound1 = 0;
    double sound2 = 0;
        
    int harmonics = 0;
    int oldHarmonics = 0;
    public int getHarmonics() { return harmonics; }
    public void setHarmonics(int harm) { harmonics = harm; }

    int harmonics2 = 0;
    int oldHarmonics2 = 0;
    public int getHarmonics2() { return harmonics2; }
    public void setHarmonics2(int harm) { harmonics2 = harm; }

    public static final int OPTION_HARMONICS = 0;
    public static final int OPTION_HARMONICS_2 = 1;
    public static final int OPTION_NORMALIZE = 2;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_HARMONICS: return getHarmonics();
            case OPTION_HARMONICS_2: return getHarmonics2();
            case OPTION_NORMALIZE: return getNormalize() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_HARMONICS: setHarmonics(value); break;
            case OPTION_HARMONICS_2: setHarmonics2(value); break;
            case OPTION_NORMALIZE: setNormalize(value != 0); break;
            default: throw new RuntimeException("No such option " + option);
            }
            
        // gotta update the labels for the dials
        ModulePanel pan = getModulePanel();
        if (pan != null) SwingUtilities.invokeLater(new Runnable() { public void run() { pan.repaint(); } });
        }
        
    static boolean done = false;
    public void doStatic()
        {
        if (done) return;
        done = true;
        
        NAMES = new String[NUM_WAVES.length][];
        HARMONICS = new double[NUM_WAVES.length][][];
        
        Scanner scan = new Scanner(AKWF.class.getResourceAsStream("waves/akwf.out"), "US-ASCII");
        for(int i = 0; i < NUM_WAVES.length; i++)
            {
            NAMES[i] = new String[NUM_WAVES[i]];
            HARMONICS[i] = new double[NUM_WAVES[i]][MAX_HARMONICS];
            
            for(int k = 0; k < NUM_WAVES[i]; k++)
            	{
				NAMES[i][k] = scan.nextLine();
				double max = 0;
				for(int j = 0; j < MAX_HARMONICS; j++)
					{
					HARMONICS[i][k][j] = Double.parseDouble(scan.next());	//scan.nextDouble();
					if (max < HARMONICS[i][k][j]) 
						max = HARMONICS[i][k][j] ;
					}
			
				// Normalize
				if (max != 0.0)
					{
					for(int j = 0; j < MAX_HARMONICS; j++)
						{
						HARMONICS[i][k][j] = HARMONICS[i][k][j] / max;
						}
					}
				scan.nextLine();
				}
            }
        scan.close();
        }


    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_SOUND_1)
                {
                return getName(getHarmonics(), value);
                }
            else if (modulation == MOD_SOUND_2)
                {
                return getName(getHarmonics2(), value);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


    public void go()
        {
        super.go();
        
        double s1 = modulate(MOD_SOUND_1);
        double s2 = modulate(MOD_SOUND_2);
        double m = modulate(MOD_MIX);
        if (m != mix || s1 != sound1 || s2 != sound2 || normalize != oldNormalize || oldHarmonics != harmonics || oldHarmonics2 != harmonics2)
            {
            mix = m;
            sound1 = s1;
            sound2 = s2;
            oldNormalize = normalize;
            oldHarmonics = harmonics;
            oldHarmonics2 = harmonics2;
                
            double[] amplitudes = getAmplitudes(0);
                        
            if (mix == 0)
                {
                double[] h1 = getHarmonics(harmonics, sound1);
                System.arraycopy(h1, 0, amplitudes, 0, Math.min(h1.length, amplitudes.length));
                }
            else if (mix == 1)
                {
                double[] h2 = getHarmonics(harmonics2, sound2);
                System.arraycopy(h2, 0, amplitudes, 0, Math.min(h2.length, amplitudes.length));
                }
            else
                {
                double[] h1 = getHarmonics(harmonics, sound1);
                double[] h2 = getHarmonics(harmonics2, sound2);
                                
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = (1 - m) * h1[i] + mix * h2[i];
                    }
                }
                                
            if (normalize) 
                normalizeAmplitudes();
            }
        }


    public ModulePanel getPanel()
        {
        return new ModulePanel(AKWF.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                
                box.add(new ModulationInput(unit, 0, this));
                box.add(Strut.makeVerticalStrut(8));
                box.add(new OptionsChooser(unit, 0));
                box.add(new ModulationInput(unit, 1, this));
                box.add(Strut.makeVerticalStrut(8));
                box.add(new OptionsChooser(unit, 1));
                box.add(new ModulationInput(unit, 2, this));
                return box;
                }
            };
        }

                
    public AKWF(Sound sound) 
        {
        super(sound);
        doStatic();

        defineOptions(new String[] { "Category" , "Category 2", "Normalize" }, new String[][] { CATEGORIES , CATEGORIES, { "Normalize" } } );
        defineModulations(new Constant[] { new Constant(0), new Constant(0), new Constant(0) }, new String[] { "Mix", "Sound", "Sound 2" });
        setClearOnReset(false);
        }
                
    }

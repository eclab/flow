// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

import flow.*;

public class FormantFilter extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_INTERPOLATION = 0;
    public static final int MOD_NUM_FORMANTS = 1;
    public static final int MOD_GAIN = 2;
    
    public static final int NUM_FORMANTS = 8;
    
    public static final double MAX_GAIN = 16;
    
    public FormantFilter(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        String[] formantNames = new String[formants.length];
        for(int i = 0; i < formantNames.length; i++)
        	{
        	formantNames[i] = formants[i].name;
        	}
        defineOptions(new String[] { "Vowel 1", "Vowel 2", "Vowel 3", "Vowel 4", "Vowel 5", "Vowel 6", "Vowel 7", "Vowel 8", "Four Pole" }, 
        						new String[][] { formantNames, formantNames, formantNames, formantNames, formantNames, formantNames, formantNames, formantNames, { "Four Pole" }});
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.HALF }, new String[] { "Interpolation", "Num Vowels", "Gain" });
        }

    int formantIndex[] = { 0, 0, 0, 0, 0, 0, 0, 0 };
    public int getFormant(int f) { return formantIndex[f]; }
    public void setFormant(int f, int val) { formantIndex[f] = val; }

	boolean fourPole = false;
	public boolean getFourPole() { return fourPole; }
	public void setFourPole(boolean val) { fourPole = val; }
        
    public static final int OPTION_FORMANT_1 = 0;
    public static final int OPTION_FORMANT_2 = 1;
    public static final int OPTION_FORMANT_3 = 2;
    public static final int OPTION_FORMANT_4 = 3;
    public static final int OPTION_FORMANT_5 = 4;
    public static final int OPTION_FORMANT_6 = 5;
    public static final int OPTION_FORMANT_7 = 6;
    public static final int OPTION_FORMANT_8 = 7;
    public static final int OPTION_FOUR_POLE = 8;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_FORMANT_1: return getFormant(0);
            case OPTION_FORMANT_2: return getFormant(1);
            case OPTION_FORMANT_3: return getFormant(2);
            case OPTION_FORMANT_4: return getFormant(3);
            case OPTION_FORMANT_5: return getFormant(4);
            case OPTION_FORMANT_6: return getFormant(5);
            case OPTION_FORMANT_7: return getFormant(6);
            case OPTION_FORMANT_8: return getFormant(7);
            case OPTION_FOUR_POLE: return (getFourPole() ? 1 : 0);
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_FORMANT_1: setFormant(0, value); return;
            case OPTION_FORMANT_2: setFormant(1, value); return;
            case OPTION_FORMANT_3: setFormant(2, value); return;
            case OPTION_FORMANT_4: setFormant(3, value); return;
            case OPTION_FORMANT_5: setFormant(4, value); return;
            case OPTION_FORMANT_6: setFormant(5, value); return;
            case OPTION_FORMANT_7: setFormant(6, value); return;
            case OPTION_FORMANT_8: setFormant(7, value); return;
            case OPTION_FOUR_POLE: setFourPole(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }

    double formantFilter(double frequency, int index1, int index2, double alpha)
    	{
    	Formant form1 = formants[index1];
    	Formant form2 = formants[index2];
    	double[] f1 = form1.f;
    	double[] f2 = form2.f;
    	double[] b1 = form1.b;
    	double[] b2 = form2.b;
    	double[] a1 = form1.a;
    	double[] a2 = form2.a;
    	double alpha1 = 1.0 - alpha;
   		
    	return 
    		singleFormantFilter(frequency, 	f1[0] * alpha1 + f2[0] * alpha,
    										b1[0] * alpha1 + b2[0] * alpha,
    										a1[0] * alpha1 + a2[0] * alpha) +
    		singleFormantFilter(frequency, 	f1[1] * alpha1 + f2[1] * alpha,
    										b1[1] * alpha1 + b2[1] * alpha,
    										a1[1] * alpha1 + a2[1] * alpha) +
    		singleFormantFilter(frequency, 	f1[2] * alpha1 + f2[2] * alpha,
    										b1[2] * alpha1 + b2[2] * alpha,
    										a1[2] * alpha1 + a2[2] * alpha) +
    		singleFormantFilter(frequency, 	f1[3] * alpha1 + f2[3] * alpha,
    										b1[3] * alpha1 + b2[3] * alpha,
    										a1[3] * alpha1 + a2[3] * alpha) +
    		singleFormantFilter(frequency, 	f1[4] * alpha1 + f2[4] * alpha,
    										b1[4] * alpha1 + b2[4] * alpha,
    										a1[4] * alpha1 + a2[4] * alpha);    										  
    	}
    
    	// the resonant frequency is the same as the cutoff
    	// According to http://www.users.cloud9.net/~stark/elchap21.pdf
    	// 	Q = resonantFrequency / bandwidth
    double singleFormantFilter(double frequency, double f, double b, double a)
    	{
    	return bandpassFilter(frequency, f/b, f) * a;
    	}
    	
    // four pole bandpass filter.  See Filter.java
    double bandpassFilter(double frequency, double q, double cutoff)
        {
        // bandpass numerator
        double d = frequency / (cutoff * q);
        double numerator = d * d;
        
        double ff = frequency * frequency;
        double cc = cutoff * cutoff;
        double a = 1.0 - ff / cc;
        double b = frequency / (cutoff * q);
        double denominator = a * a + b * b;
        
        // to convert this to two pole, use sqrt(numerator/denomator)
        double res = numerator / denominator;
        if (!fourPole) res = Math.sqrt(res);
        return res;
        }
    
    public void go()
        {
        super.go();
                
        pushFrequencies(0);
        copyAmplitudes(0);
                        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        double pitch = sound.getPitch();
        double gain = modulate(MOD_GAIN) * MAX_GAIN;

        int numFormants = (int)(modulate(MOD_NUM_FORMANTS) * NUM_FORMANTS + 1);
        if (numFormants > NUM_FORMANTS) numFormants = NUM_FORMANTS;
        double ramp = modulate(MOD_INTERPOLATION);
        int lowFormant = (int) (ramp * (numFormants - 1));
        if (numFormants > 1 && lowFormant >= (numFormants - 1)) lowFormant = numFormants - 2;
        double alpha = ramp * (numFormants - 1) - lowFormant;
        
        int lowIndex = formantIndex[lowFormant];
        int hiIndex = lowIndex;
        if (numFormants > 1) hiIndex = formantIndex[lowFormant + 1];
                
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = amplitudes[i] * gain * formantFilter(frequencies[i] * pitch, lowIndex, hiIndex, alpha);
            }

        constrain();
        }       

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_NUM_FORMANTS)
                {
                int v = (int)(value * NUM_FORMANTS);
                if (v == NUM_FORMANTS) v--;
				v += 1;                
                return "" + v;
                }
            else if (modulation == MOD_GAIN)
                {
                return super.getModulationValueDescription(modulation, value * MAX_GAIN, isConstant);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }


static class Formant
	{
	public static double dbToA(double db) { return Math.pow(10.0, (db / 20.0)); }
	public String name;
	public double[] f;
	public double[] a;
	public double[] b;
	
	/** f1...f5 is frequency in Hz.  a1...a5 is amplitude in DB (a1 is normally 0).  b1...b5 is bandwidth in Hz. 
		From http://www.glottopedia.org/index.php/Formant_bandwidth we have:
		"The bandwidth is defined as the frequency region in which the amplification differs 
		less than 3 dB from the amplification at the centre frequency."  */
	public Formant(String name,
		double f1, double f2, double f3, double f4, double f5,
		double a1, double a2, double a3, double a4, double a5,
		double b1, double b2, double b3, double b4, double b5)
			{
			this.name = name;
			this.f = new double[] { f1, f2, f3, f4, f5 };
			this.a = new double[] { dbToA(a1), dbToA(a2), dbToA(a3), dbToA(a4), dbToA(a5) };
			this.b = new double[] { b1, b2, b3, b4, b5 };
			}
	}
	
/** This is stolen from http://www.csounds.com/manual/html/MiscFormants.html */
static Formant[] formants = new Formant[]
	{
	new Formant("Bass A", 600, 1040, 2250, 2450, 2750, 0, -7, -9, -9, -20, 60, 70, 110, 120, 130),
	new Formant("Bass E", 400, 1620, 2400, 2800, 3100, 0, -12, -9, -12, -18, 40, 80, 100, 120, 120),
	new Formant("Bass I", 250, 1750, 2600, 3050, 3340, 0, -30, -16, -22, -28, 60, 90, 100, 120, 120),
	new Formant("Bass O", 400, 750, 2400, 2600, 2900, 0, -11, -21, -20, -40, 40, 80, 100, 120, 120),
	new Formant("Bass U", 350, 600, 2400, 2675, 2950, 0, -20, -32, -28, -36, 40, 80, 100, 120, 120),
	new Formant("Tenor A", 650, 1080, 2650, 2900, 3250, 0, -6, -7, -8, -22, 80, 90, 120, 130, 140),
	new Formant("Tenor E", 400, 1700, 2600, 3200, 3580, 0, -14, -12, -14, -20, 70, 80, 100, 120, 120),
	new Formant("Tenor I", 290, 1870, 2800, 3250, 3540, 0, -15, -18, -20, -30, 40, 90, 100, 120, 120),
	new Formant("Tenor O", 400, 800, 2600, 2800, 3000, 0, -10, -12, -12, -26, 70, 80, 100, 130, 135),
	new Formant("Tenor U", 350, 600, 2700, 2900, 3300, 0, -20, -17, -14, -26, 40, 60, 100, 120, 120),
	new Formant("Contra Tenor A", 660, 1120, 2750, 3000, 3350, 0, -6, -23, -24, -38, 80, 90, 120, 130, 140),
	new Formant("Contra Tenor E", 440, 1800, 2700, 3000, 3300, 0, -14, -18, -20, -20, 70, 80, 100, 120, 120),
	new Formant("Contra Tenor I", 270, 1850, 2900, 3350, 3590, 0, -24, -24, -36, -36, 40, 90, 100, 120, 120),
	new Formant("Contra Tenor O", 430, 820, 2700, 3000, 3300, 0, -10, -26, -22, -34, 40, 80, 100, 120, 120),
	new Formant("Contra Tenor U", 370, 630, 2750, 3000, 3400, 0, -20, -23, -30, -34, 40, 60, 100, 120, 120),
	new Formant("Alto A", 800, 1150, 2800, 3500, 4950, 0, -4, -20, -36, -60, 80, 90, 120, 130, 140),
	new Formant("Alto E", 400, 1600, 2700, 3300, 4950, 0, -24, -30, -35, -60, 60, 80, 120, 150, 200),
	new Formant("Alto I", 350, 1700, 2700, 3700, 4950, 0, -20, -30, -36, -60, 50, 100, 120, 150, 200),
	new Formant("Alto O", 450, 800, 2830, 3500, 4950, 0, -9, -16, -28, -55, 70, 80, 100, 130, 135),
	new Formant("Alto U", 325, 700, 2530, 3500, 4950, 0, -12, -30, -40, -64, 50, 60, 170, 180, 200),
	new Formant("Soprano A", 800, 1150, 2900, 3900, 4950, 0, -6, -32, -20, -50, 80, 90, 120, 130, 140),
	new Formant("Soprano E", 350, 2000, 2800, 3600, 4950, 0, -20, -15, -40, -56, 60, 100, 120, 150, 200),
	new Formant("Soprano I", 270, 2140, 2950, 3900, 4950, 0, -12, -26, -26, -44, 60, 90, 100, 120, 120),
	new Formant("Soprano O", 450, 800, 2830, 3800, 4950, 0, -11, -22, -22, -50, 40, 80, 100, 120, 120),
	new Formant("Soprano U", 325, 700, 2700, 3800, 4950, 0, -16, -35, -40, -60, 50, 60, 170, 180, 200),
	};
    
	}

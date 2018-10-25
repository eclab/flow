// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.awt.*;
import javax.swing.*;

/** 
    A Unit which approximates the harmonics of a drawbar tonewheel organ.
    There are eleven drawbars: 9 classic ones and two more "extra" ones
    found on more expensive organs which control two harmonics each.
    There are also a large number of presets, currently drawn from drawbar settings
    described in http://www.hammondtoday.com/category/drawbar-settings/
        
    <p>This organ does not have a Leslie of course: you'll need to add that
    with a combination of pitch shifting and amplitude shifting...
*/

public class Drawbars extends Unit implements UnitSource, Presetable
    {
    private static final long serialVersionUID = 1;

    public static final int NUM_DRAWBARS = 11;
    public Drawbars(Sound sound) 
        {
        super(sound);
        defineModulations(new Constant[] 
            { 
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            Constant.ZERO,
            }, 
            new String[] { "1  (16')", "2  (5 1/3')", "3  (8')", "4  (4')", "5  (2 2/3')", "6  (2')", "7  (1 3/5')", 
                           "8  (1 1/3')", "9  (1')",  "10", "11" }); // "10  (1 1/7', 8/9')", "11  (4/5', 2/3')" });
        setClearOnReset(false);
        setupOrganFrequencies();
        }
        
    public void setupOrganFrequencies()
        {
        double[] frequencies = getFrequencies(0);
        frequencies[0] = 8.0 / (16.0);
        frequencies[1] = 8.0 / (5.0 + 1.0/3.0); /// yes, they're flipped
        frequencies[2] = 8.0 / (8.0);           /// yes, they're flipped
        frequencies[3] = 8.0 / (4.0);
        frequencies[4] = 8.0 / (2.0 + 2.0/3.0);
        frequencies[5] = 8.0 / (2.0);
        frequencies[6] = 8.0 / (1.0 + 3.0/5.0);
        frequencies[7] = 8.0 / (1.0 + 1.0/3.0);
        frequencies[8] = 8.0 / (1.0);        
        frequencies[9] = 8.0 / (1.0 + 1.0/7.0);        
        frequencies[10] = 8.0 / (4.0 / 5.0);        
        frequencies[11] = 8.0 / (8.0 / 9.0);        
        frequencies[12] = 8.0 / (2.0 / 3.0);        
        }
        
    public void setPreset(int preset) 
        { 
        int[] bars = ((int[])(PRESETS[preset][1]));
        for(int i = 0; i < bars.length; i++)
            {
            if (isModulationConstant(i))
                {
                setModulation(new Constant(bars[i] / 8.0), i);
                }
            }
        }

    public String[] getPresets() 
        { 
        String[] names = new String[PRESETS.length];
        for(int i = 0; i < PRESETS.length; i++)
            {
            names[i] = (String)(PRESETS[i][0]);
            }

        return names; 
        }
   
    static final double SQRT_2 = Math.sqrt(2);
    
    double[] previousMods = new double[NUM_DRAWBARS];
    
    public Object clone()
        {
        Drawbars obj = (Drawbars)(super.clone());
        obj.previousMods = (double[])(obj.previousMods.clone());
        return obj;
        }

    public void reset() { for(int i = 0; i < previousMods.length; i++) previousMods[i] = Double.NaN; } 
    
    public void go()
        {
        super.go();
        
//        Drawbar 8 is value 1.0.  A drawbar stop corresponds to 3db, or a Sqrt(2) increase in volume/amplitude.
//                So (8d)^sqrt(2) = max
//                So d = (max^(1/sqrt(2))) / 8
//                if max == 1, then d = 1/8
//
//                Now this basically means that we should have a bar^sqrt(2) function, I believe.

//              See http://www.stefanv.com/electronics/hammond_drawbar_science.html

        double[] amplitudes = getAmplitudes(0);

        for(int i = 0; i < previousMods.length; i++)
            {
            double mod = modulate(i);
            if (mod != previousMods[i])
                {
                previousMods[i] = mod;
                amplitudes[i] = Utility.hybridpow(mod, SQRT_2);
                }
            }
        // add in additional amplitudes for 10 and 11
        amplitudes[11] = Utility.hybridpow(previousMods[9] , SQRT_2);
        amplitudes[12] = Utility.hybridpow(previousMods[10] , SQRT_2);
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            return String.format("%.4f", value * 8.0);
        else return "";
        }

    // Lots of Drawbar Settings here:
    // http://www.hammondtoday.com/category/drawbar-settings/

/*
  static final double[][] PRESETS = new double[][] { 
  { 0, 0, 5, 3, 2, 0, 0, 0, 0 },  // Stopped Fluted
  { 0, 0, 4, 4, 3, 2, 0, 0, 0 },  // Dulciana
  { 0, 0, 8, 7, 4, 0, 0, 0, 0 },  // French Horn
  { 0, 0, 4, 5, 4, 4, 2, 2, 2 },  // Salicional
  { 0, 0, 5, 4, 0, 3, 0, 0, 0 },  // Flutes 8' & 4'
  { 0, 0, 4, 6, 7, 5, 3, 0, 0 },  // Oboe
  { 0, 0, 5, 6, 4, 4, 3, 2, 0 },  // Swell Diapson
  { 0, 0, 6, 8, 7, 6, 5, 4, 0 },  // Trumpet
  { 3, 2, 7, 6, 4, 5, 2, 2, 2 },  // Full Swell
  { 0, 0, 4, 5, 4, 5, 4, 4, 0 },  // Cello
  { 0, 0, 4, 4, 2, 3, 2, 2, 0 },  // Flute & String
  { 0, 0, 7, 3, 7, 3, 4, 3, 0 },  // Clarinet
  { 0, 0, 4, 5, 4, 4, 2, 2, 0 },  // Diapson, Gamba, & Flute
  { 0, 0, 6, 6, 4, 4, 3, 2, 2 },  // Great without Reeds
  { 0, 0, 6, 8, 4, 5, 4, 3, 3 },  // Open Diapson
  { 0, 0, 8, 0, 3, 0, 0, 0, 0 },  // Full Great
  { 0, 0, 5, 3, 2, 0, 0, 0, 0 },  // Tibia Clausa
  { 4, 2, 7, 8, 6, 6, 2, 4, 4 },  // Full Great with 16'
  };
  static final String[] PRESET_NAMES = new String[] { "Stopped Flute", "Dulciana", "French Horn",
  "Salsicional", "Flutes 8' & 4'", "Oboe", "Swell Diapson", "Trumpet", "Full Swell", "Cello",
  "Flute & String", "Clarinet", "Diapson, Gamba, & Flute", "Great without Reeds",
  "Open Diapson", "Full Great", "Tibia Clausa", "Full Great with 16'" };
*/
    
    static final Object[][] PRESETS = new Object[][]
    {
    { "007740034 Alone in the City", new int[] { 0, 0, 7, 7, 4, 0, 0, 3, 4 } },
    { "887724110 America (Gospel) (U)", new int[] { 8, 8, 7, 7, 2, 4, 1, 1, 0 } },
    { "006606000 America (Gospel) (L)", new int[] { 0, 0, 6, 6, 0, 6, 0, 0, 0 } },
    { "885324588 Blues", new int[] { 8, 8, 5, 3, 2, 4, 5, 8, 8 } },
    { "888800000 Booker T. Jones 1", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "888630000 Booker T. Jones 2", new int[] { 8, 8, 8, 6, 3, 0, 0, 0, 0 } },
    { "888808008 Born to B3 (Gospel) (U)", new int[] { 8, 8, 8, 8, 0, 8, 0, 0, 8 } },
    { "007725400 Born to B3 (Gospel) (L)", new int[] { 0, 0, 7, 7, 2, 5, 4, 0, 0 } },
    { "888110000 Brian Auger 1", new int[] { 8, 8, 8, 1, 1, 0, 0, 0, 0 } },
    { "888805000 Brian Auger 2", new int[] { 8, 8, 8, 8, 0, 5, 0, 0, 0 } },
    { "878000456 Bright Comping", new int[] { 8, 7, 8, 0, 0, 0, 4, 5, 6 } },
    { "800000888 Brother Jack", new int[] { 8, 0, 0, 0, 0, 0, 8, 8, 8 } },
    { "843000000 Dark Comping", new int[] { 8, 4, 3, 0, 0, 0, 0, 0, 0 } },
    { "888888888 Dark Solo A (U)", new int[] { 8, 8, 8, 8, 8, 8, 8, 8, 8 } },
    { "662000000 Dark Solo A (L)", new int[] { 6, 6, 2, 0, 0, 0, 0, 0, 0 } },
    { "828200002 Dark Solo B (U)", new int[] { 8, 2, 8, 2, 0, 0, 0, 0, 2 } },
    { "606000000 Dark Solo B (L)", new int[] { 6, 0, 6, 0, 0, 0, 0, 0, 0 } },
    { "888000888 Fat", new int[] { 8, 8, 8, 0, 0, 0, 8, 8, 8 } },
    { "080080883 Fifth Organ (Gospel) (U)", new int[] { 0, 8, 0, 0, 8, 0, 8, 8, 3 } },
    { "008806000 Fifth Organ (Gospel) (L)", new int[] { 0, 0, 8, 8, 0, 6, 0, 0, 0 } },
    { "006802000 Flutes", new int[] { 0, 0, 6, 8, 0, 2, 0, 0, 0 } },
    { "888666888 Full and High", new int[] { 8, 8, 8, 6, 6, 6, 8, 8, 8 } },
    { "868868068 Full and Sweet", new int[] { 8, 6, 8, 8, 6, 8, 0, 6, 8 } },
    { "888888888 Full Organ", new int[] { 8, 8, 8, 8, 8, 8, 8, 8, 8 } },
    { "688600004 Funky Comping", new int[] { 6, 8, 8, 6, 0, 0, 0, 0, 4 } },
    { "888800000 Gimme Some Loving", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "808808008 Gospel 1", new int[] { 8, 0, 8, 8, 0, 8, 0, 0, 8 } },
    { "888000008 Gospel 2", new int[] { 8, 8, 8, 0, 0, 0, 0, 0, 8 } },
    { "868666568 Greg Allman 1", new int[] { 8, 6, 8, 6, 6, 6, 5, 6, 8 } },
    { "888600000 Greg Allman 2", new int[] { 8, 8, 8, 6, 0, 0, 0, 0, 0 } },
    { "886000040 Greg Allman 3", new int[] { 8, 8, 6, 0, 0, 0, 0, 4, 0 } },
    { "888800088 Greg Rolie 1", new int[] { 8, 8, 8, 8, 0, 0, 0, 8, 8 } },
    { "886400000 Greg Rolie 2", new int[] { 8, 8, 6, 4, 0, 0, 0, 0, 0 } },
    { "888886666 Greg Rolie 4", new int[] { 8, 8, 8, 8, 8, 6, 6, 6, 6 } },
    { "888420080 Groove Holmes (Gospel) (U)", new int[] { 8, 8, 8, 4, 2, 0, 0, 8, 0 } },
    { "000505000 Groove Holmes (Gospel) (L)", new int[] { 0, 0, 0, 5, 0, 5, 0, 0, 0 } },
    { "880000000 House Bass (Gospel) (U)", new int[] { 8, 8, 0, 0, 0, 0, 0, 0, 0 } },
    { "008080000 House Bass (Gospel) (L)", new int[] { 0, 0, 8, 0, 8, 0, 0, 0, 0 } },
    { "868600006 Jimmy McGriff 1", new int[] { 8, 6, 8, 6, 0, 0, 0, 0, 6 } },
    { "883200125 Jimmy McGriff 2 (Gospel) (U)", new int[] { 8, 8, 3, 2, 0, 0, 1, 2, 5 } },
    { "448650000 Jimmy McGriff 2 (Gospel) (L)", new int[] { 4, 4, 8, 6, 5, 0, 0, 0, 0 } },
    { "888888888 Jimmy Smith 1 (U)", new int[] { 8, 8, 8, 8, 8, 8, 8, 8, 8 } },
    { "007500000 Jimmy Smith 1 (L)", new int[] { 0, 0, 7, 5, 0, 0, 0, 0, 0 } },
    { "888000000 Jimmy Smith 2 (U)", new int[] { 8, 8, 8, 0, 0, 0, 0, 0, 0 } },
    { "838000000 Jimmy Smith 2 (L)", new int[] { 8, 3, 8, 0, 0, 0, 0, 0, 0 } },
    { "888000000 Jimmy Smith 3 (U)", new int[] { 8, 8, 8, 0, 0, 0, 0, 0, 0 } },
    { "808000000 Jimmy Smith 3 (L)", new int[] { 8, 0, 8, 0, 0, 0, 0, 0, 0 } },
    { "888400080 Joey DeFrancesco", new int[] { 8, 8, 8, 4, 0, 0, 0, 8, 0 } },
    { "884400000 Jon Lord", new int[] { 8, 8, 4, 4, 0, 0, 0, 0, 0 } },
    { "880060000 Latin (Gospel) (U)", new int[] { 8, 8, 0, 0, 6, 0, 0, 0, 0 } },
    { "006676000 Latin (Gospel) (L)", new int[] { 0, 0, 6, 6, 7, 6, 0, 0, 0 } },
    { "800808000 Matthew Fisher", new int[] { 8, 0, 0, 8, 0, 8, 0, 0, 0 } },
    { "868800004 Melvin Crispel", new int[] { 8, 6, 8, 8, 0, 0, 0, 0, 4 } },
    { "803600000 Mellow", new int[] { 8, 0, 3, 6, 0, 0, 0, 0, 0 } },
    { "007800453 Meditation Time (Gospel) (U)", new int[] { 0, 0, 7, 8, 0, 0, 4, 5, 3 } },
    { "006700540 Meditation Time (Gospel) (L)", new int[] { 0, 0, 6, 7, 0, 0, 5, 4, 0 } },
    { "886800300 Paul Shaffer 1", new int[] { 8, 8, 6, 8, 0, 0, 3, 0, 0 } },
    { "888768888 Paul Shaffer 2", new int[] { 8, 8, 8, 7, 6, 8, 8, 8, 8 } },
    { "888878678 Paul Shaffer 3", new int[] { 8, 8, 8, 8, 7, 8, 6, 7, 8 } },
    { "850005000 Pink Floyd", new int[] { 8, 5, 0, 0, 0, 5, 0, 0, 0 } },
    { "888800000 Power Chords", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "888800000 Progessive (Gospel) (U)", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "008884000 Progessive (Gospel) (L)", new int[] { 0, 0, 8, 8, 8, 4, 0, 0, 0 } },
    { "006876400 Ray Charles", new int[] { 0, 0, 6, 8, 7, 6, 4, 0, 0 } },
    { "808000008 Reggae", new int[] { 8, 0, 8, 0, 0, 0, 0, 0, 8 } },
    { "888800000 Rock, R&B (U)", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "848000000 Rock, R&B (L)", new int[] { 8, 4, 8, 0, 0, 0, 0, 0, 0 } },
    { "800388888 Screaming (Gospel) (U)", new int[] { 8, 0, 0, 3, 8, 8, 8, 8, 8 } },
    { "007033333 Screaming (Gospel) (L)", new int[] { 0, 0, 7, 0, 3, 3, 3, 3, 3 } },
    { "008888800 Shirley Scott", new int[] { 0, 0, 8, 8, 8, 8, 8, 0, 0 } },
    { "830000378 Simmering", new int[] { 8, 3, 0, 0, 0, 0, 3, 7, 8 } },
    { "876556788 Shouting 1", new int[] { 8, 7, 6, 5, 5, 6, 7, 8, 8 } },
    { "668848588 Shouting 2", new int[] { 6, 6, 8, 8, 4, 8, 5, 8, 8 } },
    { "878645466 Shouting 3 (Gospel) (U)", new int[] { 8, 7, 8, 6, 4, 5, 4, 6, 6 } },
    { "888800000 Shouting 3 (Gospel) (L)", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "008400000 Slow Balllad", new int[] { 0, 0, 8, 4, 0, 0, 0, 0, 0 } },
    { "068840003 Slowly", new int[] { 0, 6, 8, 8, 4, 0, 0, 0, 3 } },
    { "888700000 Soft Backing (Gospel) (U)", new int[] { 8, 8, 8, 7, 0, 0, 0, 0, 0 } },
    { "555400000 Soft Backing (Gospel) (L)", new int[] { 5, 5, 5, 4, 0, 0, 0, 0, 0 } },
    { "808400008 Soft Chords", new int[] { 8, 0, 8, 4, 0, 0, 0, 0, 8 } },
    { "678404231 Speaker Talking (Gospel) (U)", new int[] { 6, 7, 8, 4, 0, 4, 2, 3, 1 } },
    { "006602024 Speaker Talking (Gospel) (L)", new int[] { 0, 0, 6, 6, 0, 2, 0, 2, 4 } },
    { "888643200 Steppenwolf", new int[] { 8, 8, 8, 6, 4, 3, 2, 0, 0 } },
    { "888876788 Steve Winwood", new int[] { 8, 8, 8, 8, 7, 6, 7, 8, 8 } },
    { "876543211 Strings", new int[] { 8, 7, 6, 5, 4, 3, 2, 1, 1 } },
    { "008000000 Sweet", new int[] { 0, 0, 8, 0, 0, 0, 0, 0, 0 } },
    { "787746046 Testimony Service  (Gospel) (U)", new int[] { 7, 8, 7, 7, 4, 6, 0, 4, 6 } },
    { "008800673 Testimony Service  (Gospel) (L)", new int[] { 0, 0, 8, 8, 0, 0, 6, 7, 3 } },
    { "878656467 Theatre Organ (Gospel) (U)", new int[] { 8, 7, 8, 6, 5, 6, 4, 6, 7 } },
    { "008844000 Theatre Organ (Gospel) (L)", new int[] { 0, 0, 8, 8, 4, 4, 0, 0, 0 } },
    { "888800000 Tom Coster", new int[] { 8, 8, 8, 8, 0, 0, 0, 0, 0 } },
    { "800000008 Whistle 1", new int[] { 8, 0, 0, 0, 0, 0, 0, 0, 8 } },
    { "888000008 Whistle 2", new int[] { 8, 8, 8, 0, 0, 0, 0, 0, 8 } },
    { "688600000 Whiter Shade Of Pale 1 (U)", new int[] { 6, 8, 8, 6, 0, 0, 0, 0, 0 } },
    { "880070770 Whiter Shade Of Pale 1 (L)", new int[] { 8, 8, 0, 0, 7, 0, 7, 7, 0 } },
    { "888808006 Whiter Shade Of Pale 2 (U)", new int[] { 8, 8, 8, 8, 0, 8, 0, 0, 6 } },
    { "004440000 Whiter Shade Of Pale 2 (L)", new int[] { 0, 0, 4, 4, 4, 0, 0, 0, 0 } },
    { "866800000 Wide Leslie", new int[] { 8, 6, 6, 8, 0, 0, 0, 0, 0 } },
    };
    
    }

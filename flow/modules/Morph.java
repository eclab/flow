// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import flow.gui.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.border.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;


/**
   A Unit which morphs between up to 4 incoming Unit signals according to a
   given Modulation. 
   
   <p>The easiest situation is two Units, let's call them A and B.  If the 
   modulation is 0.0, then the output is just A.  As the moduation increases,
   both the amplitude and the frequency start changing until ultimately, at 
   1.0, the output is the same as B.  In-between, it's an interpolation between
   the two.
   
   <p>If you add a third unit (say C), then modulation 0.0 produces A.  As 
   it increases to 1.0, the output gradually becomes B.  When it reaches 1.0,
   as you drop back down to 0.0, the output gradually moves from B not back
   to A, but rather towards C.  When you reach 0.0 and start moving back up again,
   the output now morphs from C to A, and so on.
   
   <p>If you add a fourth unit, the pattern is the same: A->B->C->D->A...
   
   <p>Some Units are intended to start in a certain fashion and change over time,
   so when it's time for that unit to come online, it's helpful to inform it
   to reset itself in some way.  To do this, Morph provides triggers for each
   of the four units which fire when each unit will start being used.
   
   <p>Morph allows you to morph by frequency, by amplitude, or by both (the normal
   situation).  If you don't morph by frequency (or by amplitude), then its value
   will simply be copied over from the first Unit.  You can also choose to include
   the fundamental in the morph (or not).  And you can choose whether Morph is FREE,
   that is, whether its sequence is reset on gate or not.
   
   <p>You have a variety of choices with regard which partials morph into other partials.
   These are defined by the morph
   option TYPE.  Its choices are:
   
   <ol>
   <li> NORMAL: Linear interpolation from A to B.  This is the standard approach.
   <li> RANDOM: A partial in A is paired with a random partial in B and 
   gradually moves towards that other partial's position.  New
   pairings are chosen every gate.
   <li> 2-PAIR ... 8-PAIR: Every 2 (or 4, or 8) partials in A are paired with
   the "opposite" partial in that group in B: for example, for 4-pair,
   the pairing are 0:3, 1:2, then 4:7, 5:6, and so on.  The partial in A gradually 
   moves towards that other partial's position.
   <li> INCREASING: Partial 1 in A is paired with partial 1 in B.  Then Partials 2...3
   are paired with 2...3 in 2-pair fashion.  Then partials 5...8 are paired in 4-pair
   fashion.  Then partials 9...16 are paried in 8-pair fashion, and so on.
   A partial in A is paired with partial in B.  The partial in A gradually 
   moves towards that other partial's position.
   <li> RAND 2 ... RAND 128: Like RANDOM, except the partials are paired off using a specific
   fixed randomized pairing that never changes so things sound consistent.  The variance
   in the randomization is determined by the values 2...128.  The partial in A gradually 
   moves towards that other partial's position.
   </ol>
      
*/


/// Bug: if you build a Morph from scratch, it's initially got its lo and high inputs set to 3,
/// so the first interpolation scan won't work right.  But if you load one from a patch or do reset(),
/// it's already hooked up and so it'll find those initially and work right.

public class Morph extends Unit
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_INTERPOLATION = 0;
    public static final int MOD_VARIANCE = 1;
    public static final int MOD_SEED = 2;

    public Random random = null;

    int[] morphTo;
    int lastMorph;
    int morph;      
    boolean includesFundamental;
    boolean morphFrequency = true;
    boolean morphAmplitude = true;
    boolean shuffle = false; 
       
    public Object clone()
        {
        Morph obj = (Morph)(super.clone());
        obj.morphTo = (int[])(obj.morphTo.clone());
        return obj;
        }

    public void setMorphFrequency(boolean val) { morphFrequency = val; }
    public boolean getMorphFrequency() { lastMorph = -1; return morphFrequency; }
    public void setMorphAmplitude(boolean val) { morphAmplitude = val; }
    public boolean getMorphAmplitude() { return morphAmplitude; }
        
    public void setIncludesFundamental(boolean val) { lastMorph = -1; includesFundamental = val; }
    public boolean getIncludesFundamental() { return includesFundamental; }
        
    public void setShuffle(boolean val) { shuffle = val; }
    public boolean getShuffle() { return shuffle; }
        
    public void setMorph(int morph) { this.morph = morph; }
    public int getMorph() { return morph; }
    public int getNumMorphs() { return MORPH_PAIRS_INCREASING + 1; }
    public String getMorphName(int morph) { return MORPH_NAMES[morph]; }
        
    public void setFree(boolean val) { free = val; }
    public boolean getFree() { return free; }
        
    public static final int NUM_MORPH_PAIRS = 1024;  // regardless of the number of partials
        
    public static final int OPTION_MORPH_TYPE = 0;
    public static final int OPTION_INCLUDE_FUNDAMENTAL = 1;
    public static final int OPTION_MORPH_FREQUENCY = 2;
    public static final int OPTION_MORPH_AMPLITUDE = 3;
    public static final int OPTION_FREE = 4;
    public static final int OPTION_RANDOM = 5;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_MORPH_TYPE: return getMorph();
            case OPTION_INCLUDE_FUNDAMENTAL: return getIncludesFundamental() ? 1 : 0;
            case OPTION_MORPH_FREQUENCY: return getMorphFrequency() ? 1 : 0;
            case OPTION_MORPH_AMPLITUDE: return getMorphAmplitude() ? 1 : 0;
            case OPTION_FREE: return getFree() ? 1 : 0;
            case OPTION_RANDOM: return getShuffle() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_MORPH_TYPE: setMorph(value); return;
            case OPTION_INCLUDE_FUNDAMENTAL: setIncludesFundamental(value != 0); return;
            case OPTION_MORPH_FREQUENCY: setMorphFrequency(value != 0); return;
            case OPTION_MORPH_AMPLITUDE: setMorphAmplitude(value != 0); return;
            case OPTION_FREE: setFree(value != 0); return;
            case OPTION_RANDOM: setShuffle(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        
    public Morph(Sound sound)
        {
        super(sound);
        doStatic();
        
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B", "Input C", "Input D" });
        defineModulationOutputs(new String[] { "Trig A", "Trig B", "Trig C", "Trig D" });
        defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ONE }, new String[] { "Interpolation", "Variance", "Seed" });
        defineOptions(new String[] { "Type", "Fundamental", "Frequency", "Amplitude", "Free", "Random" }, new String[][] { MORPH_NAMES, { "Fundamental" }, { "Frequency" }, { "Amplitude" }, { "Free" }, { "Random" } });
        lastMorph = -1;
        lastRamp = -1;
        this.morph = MORPH_STANDARD;
        reset();                // this fixes a bug 
        }

    public void reset()
        {
        super.reset();
        reseed();
                
        // we do swap morphs every reset
        if (lastMorph != morph || morph == MORPH_ALL_RANDOM)
            {
            buildMorph();
            lastMorph = morph;
            }
                
        lastRamp = Double.NaN;
        loInput = -1;
        hiInput = -1;
        direction = 0;  // unknown
        }

    public void reseed()
        {
        double mod = modulate(MOD_SEED);
                
        if (mod != 0)
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }
        else if (random != null)
            {
            random = null;
            }
        }
                
    public void gate()
        {
        super.gate();
        reseed();

        // we do swap morphs every gate
        if (lastMorph != morph || morph == MORPH_ALL_RANDOM)
            {
            buildMorph();
            lastMorph = morph;
            }
                
        if (!free)
            {
            lastRamp = Double.NaN;
            loInput = -1;
            hiInput = -1;
            direction = 0;  // unknown
            }
        }

    boolean free = false;
    int loInput = 0;
    int hiInput = 1;
    int direction = 0;  // 0 is "unknown" or "unmoved"
    double lastRamp = Double.NaN;

    public int findNextAndUpdate(int input, int prevInput)
        {
        if (shuffle)
            {
            Random rand = (random == null ? getSound().getRandom() : random);
            int total = 0;
            int totalWithPrev = 0;
            for(int i = 0; i < inputs.length; i++)
                {
                if (!(inputs[i] instanceof Nil))
                    {
                    totalWithPrev++;
                    if (i != prevInput)
                        total++;
                    }
                }
            if (totalWithPrev == 0) return 0;
            if (total == 0) 
                {
                int in = (input == - 1 ? 0 : input);
                updateTrigger(in);
                return in;
                }
                
            while(true)
                {
                int v = rand.nextInt(4);
                if (!(inputs[v] instanceof Nil) && (v != prevInput))
                    {
                    updateTrigger(v);
                    return v;
                    }
                }
            }
        else
            {       
            for(int i = 0 ; i < inputs.length; i++)
                {
                input++;
                if (input >= inputs.length)
                    input = 0;
                if (!(inputs[input] instanceof Nil) && (input != loInput) && (input != hiInput))
                    {
                    break;
                    }
                }

            updateTrigger(input);
            return input;
            }
        }

    public void updateInputs(double ramp)
        {
        if (hiInput == -1 && loInput == -1)
            {
            loInput = findNextAndUpdate(-1, hiInput);
            hiInput = findNextAndUpdate(-1, loInput);
            }

        // is this our first time?
        if (direction == 0 && lastRamp != lastRamp)  // that is, lastRamp == NaN
            {
            lastRamp = ramp;
            }
        // do we have one previous ramp but it's not different?
        else if (direction == 0 && lastRamp == ramp)
            {
            // do nothing
            }
        // do we have a previous different ramp?
        else if (direction == 0 && lastRamp != ramp)
            {
            direction = (ramp > lastRamp? 1 : -1);
            }
        else if (direction == 1 && lastRamp > ramp)  // changed direction, going down
            {
            direction = -1;
            // we need a new lo input
            loInput = findNextAndUpdate(loInput, hiInput);
            }
        else if (direction == -1 && lastRamp < ramp)  // changed direction, going up
            {
            direction = 1;
            // we need a new hi input
            hiInput = findNextAndUpdate(hiInput, loInput);
            }
        else
            {
            // do nothing
            }

        for(int i = 0; i < inputs.length; i++)
            {
            if (i == loInput)
                {
                setModulationOutput(i, 1.0 - ramp);
                }
            else if (i == hiInput)
                {
                setModulationOutput(i, ramp);
                }
            else
                {
                setModulationOutput(i, 0.0);
                }
            }

        lastRamp = ramp;
        }

    double lastVariance;
    public void go()
        {
        super.go();

        // we do swap morphs only if they have changed (or the variance has changed)
        if (lastMorph != morph || (morph == MORPH_ALL_RANDOM && lastVariance != modulate(MOD_VARIANCE)))
            {
            buildMorph();
            lastMorph = morph;
            }
                
        double ramp = modulate(MOD_INTERPOLATION);
        updateInputs(ramp);
        
        double[] p1frequencies = getFrequenciesIn(loInput);
        double[] p2frequencies = getFrequenciesIn(hiInput);
        double[] p1amplitudes = getAmplitudesIn(loInput);
        double[] p2amplitudes = getAmplitudesIn(hiInput);
        
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        
        if (morphFrequency)
            {
            for(int i = 0; i < p1frequencies.length; i++)
                {
                frequencies[i] = (p2frequencies[morphTo[i]] * ramp) + (p1frequencies[i] * (1.0 - ramp));
                }
            }
        else 
            {
            System.arraycopy(p1frequencies, 0, frequencies, 0, frequencies.length);
            }
                
        if (morphAmplitude)
            {
            for(int i = 0; i < p1amplitudes.length; i++)
                {
                amplitudes[i] = (p2amplitudes[morphTo[i]] * ramp) + (p1amplitudes[i] * (1.0 - ramp));
                }
            }
        else 
            {
            System.arraycopy(p1amplitudes, 0, amplitudes, 0, amplitudes.length);
            }
                
        constrain();
            
        // always sort    
        simpleSort(0, false);
        }
                
        
    int chooseSwap(int x, double range, double low, Random random)
        {
        while(true)
            {
            int r = random.nextInt((int)(range * morphTo.length + 1));
            if ((int)range != range &&
                (!(random.nextFloat() <= range - (int)range)))
                {
                r++;
                }
            int v = x + (random.nextBoolean() ? r : (-r));
            if (v >= low && v < morphTo.length)
                return v;
            }
        }
        
    public void buildMorph()
        {
        if (morphTo == null) 
            {
            morphTo = new int[getAmplitudes(0).length];
            }
            
        if (!morphFrequency)            // just doing amplitude, so we're not morphing just crossfading
            {
            for(int x = 0; x < morphTo.length; x++) morphTo[x]= x;
            }        
        else if (morph == MORPH_ALL_RANDOM)
            {
            Random rand = (random == null ? getSound().getRandom() : random);
            lastVariance = modulate(MOD_VARIANCE);
            double range = makeVerySensitive(lastVariance);
                        
            if (range == 0)
                {
                System.arraycopy(morphs[MORPH_STANDARD], 0, morphTo, 0, morphTo.length);
                }
            else 
                {
                for(int x = 0; x < morphTo.length; x++) morphTo[x]= x;  // clear previous table
                                
                int end = (includesFundamental ? 0 : 1);
                                
                for(int x = morphTo.length - 1; x >= end; x--)
                    {
                    int r = chooseSwap(x, range, end, rand);
                                        
                    int temp = morphTo[x];
                    morphTo[x] = morphTo[r];
                    morphTo[r] = temp;
                    }
                }
            }
        else 
            {
            if (includesFundamental)
                {
                for(int i = 0; i < morphTo.length; i++)
                    {
                    morphTo[i] = morphs[morph][i + 1] - 1;
                    }
                }
            else
                {
                System.arraycopy(morphs[morph], 0, morphTo, 0, morphTo.length);
                }
            }
        }

        
    public static final int MORPH_STANDARD = 0;
    public static final int MORPH_ALL_RANDOM = 1;
    public static final int MORPH_PAIRS_2 = 2;
    public static final int MORPH_PAIRS_4 = 3;
    public static final int MORPH_PAIRS_8 = 4;
    public static final int MORPH_PAIRS_INCREASING = 5;
        
    /*
    // These are not compelling but they are consistent compared to Random
    public static final int MORPH_RANDOM_2 = 6;
    public static final int MORPH_RANDOM_3 = 7;
    public static final int MORPH_RANDOM_4 = 8;
    public static final int MORPH_RANDOM_5 = 9;
    public static final int MORPH_RANDOM_6 = 10;
    public static final int MORPH_RANDOM_8 = 11;
    public static final int MORPH_RANDOM_16 = 12;
    public static final int MORPH_RANDOM_32 = 13;
    public static final int MORPH_RANDOM_64 = 14;
    public static final int MORPH_RANDOM_128 = 15;
    */
                
    public static final String[] MORPH_NAMES = new String[]
    { "Normal", "Random", "2-Pair", "4-Pair", "8-Pair", "Increasing" }; //  "Rand 2", "Rand 3", "Rand 4", "Rand 5", "Rand 6", "Rand 8", "Rand 16", "Rand 32", "Rand 64", "Rand 128" };

    public static final int[][] morphs = new int[MORPH_PAIRS_INCREASING + 1][NUM_PARTIALS + 1];  // yes, *129*, in case we want to include the fundamental
        
    static boolean done = false;
    public void doStatic()
        {
        if (done) return;
        done = true;

        int len = morphs[0].length - 1;
                
        // standard and INITIALIZATION
        for(int j = 0; j < MORPH_PAIRS_INCREASING + 1; j++)
            {
            for(int i = 0; i < morphs[j].length; i++)
                {
                morphs[j][i] = i;
                }
            }
                
        // pairs except fundamental
        for(int i = 1; i < len - 1; i+=2)
            { 
            morphs[MORPH_PAIRS_2][i] = i + 1;  
            morphs[MORPH_PAIRS_2][i+1] = i; 
            }
        morphs[MORPH_PAIRS_2][len - 1] = len - 1;
                
        // spaced pairs except fundamental
        for(int i = 1; i < len - 3; i+=4)
            {
            morphs[MORPH_PAIRS_4][i] = i + 2;
            morphs[MORPH_PAIRS_4][i+1] = i + 3;
            morphs[MORPH_PAIRS_4][i+2] = i;
            morphs[MORPH_PAIRS_4][i+3] = i + 1;
            }
        morphs[MORPH_PAIRS_4][len - 1] = len - 3;
        morphs[MORPH_PAIRS_4][len - 2] = len - 2;
        morphs[MORPH_PAIRS_4][len - 3] = len - 1;
                
        // big spaced pairs except fundamental
        for(int i = 1; i < len - 7; i+=8)
            {
            morphs[MORPH_PAIRS_8][i] = i + 4;
            morphs[MORPH_PAIRS_8][i+1] = i + 5;
            morphs[MORPH_PAIRS_8][i+2] = i + 6;
            morphs[MORPH_PAIRS_8][i+3] = i + 7;
            morphs[MORPH_PAIRS_8][i+4] = i;
            morphs[MORPH_PAIRS_8][i+5] = i + 1;
            morphs[MORPH_PAIRS_8][i+6] = i + 2;
            morphs[MORPH_PAIRS_8][i+7] = i + 3;
            }
        morphs[MORPH_PAIRS_8][len - 1] = len - 5;
        morphs[MORPH_PAIRS_8][len - 2] = len - 6;
        morphs[MORPH_PAIRS_8][len - 3] = len - 7;       
        morphs[MORPH_PAIRS_8][len - 4] = len - 4;
        morphs[MORPH_PAIRS_8][len - 5] = len - 1;
        morphs[MORPH_PAIRS_8][len - 6] = len - 2;       
        morphs[MORPH_PAIRS_8][len - 7] = len - 3;

        // increasing pairs
        int current = 0;
        for(int i = 1; i < NUM_MORPH_PAIRS; i *= 2)
            {
            int j;
            for(j = current; j < current + i; j++)
                {
                if (j + i / 2 >= morphs[MORPH_PAIRS_INCREASING].length)
                    break;
                if (j < current + i / 2)
                    {
                    morphs[MORPH_PAIRS_INCREASING][j] = j + i / 2;
                    morphs[MORPH_PAIRS_INCREASING][j + i / 2] = j;
                    }
                }
            current = j;
            }
        morphs[MORPH_PAIRS_INCREASING][len - 1] = len - 1;

        /*
        // The random ones are fixed
                
        // 2 [except for 61, 70, 73, and 92]
        morphs[MORPH_RANDOM_2] = new int[] { 0, 2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11, 14, 13, 16, 15, 18, 17, 20, 19, 22, 21, 24, 23, 26, 25, 28, 27, 30, 29, 32, 31, 34, 33, 36, 35, 38, 37, 40, 39, 42, 41, 44, 43, 46, 45, 48, 47, 50, 49, 52, 51, 54, 53, 56, 55, 58, 57, 60, 59, 70, 63, 62, 65, 64, 67, 66, 69, 68, 61, 72, 71, 73, 75, 74, 77, 76, 79, 78, 81, 80, 83, 82, 85, 84, 87, 86, 89, 88, 91, 90, 92, 94, 93, 96, 95, 98, 97, 100, 99, 102, 101, 104, 103, 106, 105, 108, 107, 110, 109, 112, 111, 114, 113, 116, 115, 118, 117, 120, 119, 122, 121, 124, 123, 126, 125, 127, 128 };

        // 3 [except for 95 and 103]
        morphs[MORPH_RANDOM_3] = new int[] { 0, 2, 1, 4, 3, 6, 5, 9, 10, 7, 8, 12, 11, 14, 13, 16, 15, 19, 18, 17, 21, 20, 23, 22, 25, 24, 27, 26, 30, 31, 28, 29, 34, 35, 32, 33, 38, 39, 36, 37, 42, 43, 40, 41, 45, 44, 48, 47, 46, 51, 52, 49, 50, 54, 53, 56, 55, 58, 57, 60, 59, 62, 61, 64, 63, 66, 65, 69, 70, 67, 68, 73, 74, 71, 72, 77, 78, 75, 76, 80, 79, 82, 81, 85, 86, 83, 84, 87, 90, 91, 88, 89, 94, 103, 92, 93, 98, 99, 96, 97, 102, 95, 100, 101, 105, 104, 108, 109, 106, 107, 112, 113, 110, 111, 115, 114, 118, 119, 116, 117, 121, 120, 123, 122, 124, 126, 125, 127, 128 };

        // 4
        morphs[MORPH_RANDOM_4] = new int[] { 0, 2, 1, 4, 3, 7, 9, 5, 10, 6, 8, 14, 13, 12, 11, 17, 19, 15, 21, 16, 23, 18, 25, 20, 26, 22, 24, 28, 27, 32, 31, 30, 29, 34, 33, 36, 35, 38, 37, 41, 42, 39, 40, 45, 46, 43, 44, 50, 49, 48, 47, 54, 53, 52, 51, 58, 57, 56, 55, 60, 59, 62, 61, 66, 65, 64, 63, 70, 69, 68, 67, 73, 74, 71, 72, 76, 75, 80, 79, 78, 77, 82, 81, 84, 83, 86, 85, 89, 91, 87, 93, 88, 94, 90, 92, 96, 95, 100, 99, 98, 97, 102, 101, 104, 103, 106, 105, 109, 111, 107, 112, 108, 110, 114, 113, 117, 118, 115, 116, 121, 122, 119, 120, 124, 123, 126, 125, 127, 128 };

        // 5
        morphs[MORPH_RANDOM_5] = new int[] { 0, 3, 4, 1, 2, 7, 8, 5, 6, 12, 11, 10, 9, 15, 18, 13, 20, 19, 14, 17, 16, 22, 21, 25, 27, 23, 28, 24, 26, 32, 33, 34, 29, 30, 31, 36, 35, 39, 40, 37, 38, 45, 44, 47, 42, 41, 49, 43, 51, 46, 52, 48, 50, 56, 55, 54, 53, 59, 62, 57, 61, 60, 58, 66, 65, 64, 63, 69, 70, 67, 68, 75, 76, 74, 73, 71, 72, 78, 77, 81, 83, 79, 84, 80, 82, 87, 90, 85, 89, 88, 86, 93, 95, 91, 98, 92, 100, 101, 94, 102, 96, 97, 99, 104, 103, 108, 107, 106, 105, 110, 109, 115, 113, 112, 116, 111, 114, 118, 117, 120, 119, 122, 121, 126, 125, 124, 123, 127, 128 };

        // 6
        morphs[MORPH_RANDOM_6] = new int[] { 0, 4, 3, 2, 1, 8, 7, 6, 5, 14, 12, 13, 10, 11, 9, 16, 15, 22, 20, 21, 18, 19, 17, 28, 27, 26, 25, 24, 23, 31, 34, 29, 35, 37, 30, 32, 41, 33, 40, 43, 38, 36, 46, 39, 45, 44, 42, 52, 49, 48, 53, 54, 47, 50, 51, 57, 60, 55, 61, 63, 56, 58, 64, 59, 62, 70, 68, 71, 66, 73, 65, 67, 74, 69, 72, 77, 79, 75, 81, 76, 82, 78, 80, 84, 83, 87, 90, 85, 92, 91, 86, 89, 88, 97, 95, 94, 98, 93, 96, 104, 101, 100, 105, 106, 99, 102, 103, 110, 109, 108, 107, 116, 113, 112, 117, 118, 111, 114, 115, 120, 119, 126, 125, 124, 123, 122, 121, 127, 128 };

        // 8
        morphs[MORPH_RANDOM_8] = new int[] { 0, 6, 9, 5, 7, 3, 1, 4, 13, 2, 12, 18, 10, 8, 21, 20, 23, 24, 11, 22, 15, 14, 19, 16, 17, 27, 29, 25, 35, 26, 37, 34, 36, 38, 31, 28, 32, 30, 33, 45, 41, 40, 48, 44, 43, 39, 49, 53, 42, 46, 56, 54, 55, 47, 51, 52, 50, 62, 63, 60, 59, 67, 57, 58, 70, 66, 65, 61, 72, 73, 64, 74, 68, 69, 71, 79, 80, 84, 85, 75, 76, 83, 89, 81, 77, 78, 87, 86, 92, 82, 91, 90, 88, 96, 95, 94, 93, 98, 97, 100, 99, 103, 105, 101, 106, 102, 104, 112, 110, 115, 108, 114, 107, 116, 111, 109, 113, 118, 117, 120, 119, 122, 121, 124, 123, 126, 125, 127, 128 };

        // 16
        morphs[MORPH_RANDOM_16] = new int[] { 0, 5, 8, 6, 13, 1, 3, 19, 2, 11, 16, 9, 15, 4, 24, 12, 10, 29, 27, 7, 21, 20, 36, 25, 14, 23, 33, 18, 35, 17, 32, 45, 30, 26, 37, 28, 22, 34, 40, 42, 38, 48, 39, 50, 53, 31, 60, 58, 41, 57, 43, 63, 65, 44, 66, 59, 64, 49, 47, 55, 46, 73, 74, 51, 56, 52, 54, 71, 79, 78, 72, 67, 70, 61, 62, 77, 81, 75, 69, 68, 85, 76, 92, 91, 88, 80, 95, 98, 84, 101, 99, 83, 82, 100, 97, 86, 103, 94, 87, 90, 93, 89, 115, 96, 114, 110, 112, 108, 107, 121, 105, 123, 106, 118, 104, 102, 125, 119, 113, 117, 126, 109, 124, 111, 122, 116, 120, 127, 128 };

        // 32
        morphs[MORPH_RANDOM_32] = new int[] { 0, 3, 21, 1, 13, 34, 18, 33, 16, 38, 39, 19, 25, 4, 40, 23, 8, 47, 6, 11, 26, 2, 43, 15, 55, 12, 20, 49, 36, 54, 50, 61, 42, 7, 5, 44, 28, 45, 9, 10, 14, 68, 32, 22, 35, 37, 57, 17, 63, 27, 30, 67, 70, 69, 29, 24, 60, 46, 72, 75, 56, 31, 64, 48, 62, 81, 87, 51, 41, 53, 52, 91, 58, 88, 101, 59, 102, 85, 98, 84, 86, 65, 107, 112, 79, 77, 80, 66, 73, 103, 120, 71, 100, 118, 113, 99, 111, 106, 78, 95, 92, 74, 76, 89, 115, 124, 97, 82, 117, 122, 114, 96, 83, 94, 110, 104, 126, 108, 93, 125, 90, 123, 109, 121, 105, 119, 116, 127, 128 };

        // 64
        morphs[MORPH_RANDOM_64] = new int[] { 0, 2, 1, 36, 67, 6, 5, 38, 16, 62, 40, 70, 32, 20, 31, 46, 8, 71, 53, 21, 13, 19, 69, 47, 58, 37, 49, 29, 35, 27, 44, 14, 12, 88, 92, 28, 3, 25, 7, 42, 10, 55, 39, 95, 30, 77, 15, 23, 65, 26, 76, 75, 104, 18, 100, 41, 103, 99, 24, 85, 87, 97, 9, 123, 108, 48, 119, 4, 115, 22, 11, 17, 126, 91, 102, 51, 50, 45, 96, 120, 117, 121, 94, 84, 83, 59, 105, 60, 33, 112, 93, 73, 34, 90, 82, 43, 78, 61, 114, 57, 54, 113, 74, 56, 52, 86, 111, 124, 64, 110, 109, 106, 89, 101, 98, 68, 118, 80, 116, 66, 79, 81, 125, 63, 107, 122, 72, 127, 128 };

        // 128
        morphs[MORPH_RANDOM_128] = new int[] { 0, 67, 119, 97, 120, 107, 46, 20, 41, 63, 106, 77, 113, 40, 76, 102, 103, 81, 115, 44, 7, 61, 80, 42, 104, 108, 86, 28, 27, 92, 87, 60, 116, 95, 88, 112, 37, 36, 75, 93, 13, 8, 23, 122, 19, 94, 6, 114, 70, 85, 125, 111, 117, 82, 126, 79, 109, 110, 121, 100, 31, 21, 98, 9, 96, 74, 90, 1, 71, 89, 48, 68, 73, 72, 65, 38, 14, 11, 99, 55, 22, 17, 53, 118, 101, 49, 26, 30, 34, 69, 66, 124, 29, 39, 45, 33, 64, 3, 62, 78, 59, 84, 15, 16, 24, 123, 10, 5, 25, 56, 57, 51, 35, 12, 47, 18, 32, 52, 83, 2, 4, 58, 43, 105, 91, 50, 54, 127, 128 };
        */
        }

    public ModulePanel getPanel()
        {
        return new ModulePanel(Morph.this)
            {
            public JComponent buildPanel()
                {               
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                box.add(new UnitOutput(unit, 0, this));
                                
                Box box2 = new Box(BoxLayout.X_AXIS);
                Box box3 = new Box(BoxLayout.Y_AXIS);
                for(int i = 0; i < unit.getNumInputs(); i++)
                    {
                    box3.add(new UnitInput(unit, i, this));
                    }
                box2.add(box3);
                                
                box3 = new Box(BoxLayout.Y_AXIS);

                for(int i = 0; i < unit.getNumModulationOutputs(); i++)
                    {
                    ModulationOutput m = new ModulationOutput(unit, i, this);
                    m.setTitleText("Trig", false);
                    box3.add(m);
                    }
                box2.add(box3);
                box.add(box2);

                for(int i = 0; i < unit.getNumModulations(); i++)
                    {
                    box.add(new ModulationInput(unit, i, this));
                    }
                        
                for(int i = 0; i < unit.getNumOptions(); i++)
                    {
                    box.add(new OptionsChooser(unit, i));
                    }
                        
                box.add(new ConstraintsChooser(unit, this));

                return box;
                }
            };
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == MOD_VARIANCE)  // Variance
                {
                double range = makeVerySensitive(value);
                int f = (int)(range * NUM_PARTIALS + 1);
                if (f == 1) return "1 Partial";
                else return "" + f + " Partials";
                }
            else if (modulation == MOD_SEED)
                {
                return (value == 0.0 ? "Free" : String.format("%.4f" , value));
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }

    }


/*   How I generate the random arrays
     import java.util.*;

     public class MakeRandom
     {
     public static Random rand = new Random(System.currentTimeMillis());
        
     public static final int VAR = 128;
        
     public static void main(String[] args)
     {
     int[] partials = new int[128];

     int[] best = new int[128];
     int bestCount = 128;
                
     for(int t = 0; t < 100000; t++)
     {
                        
     for(int i = 0; i < 128; i++)
     partials[i] = i;
                        
     for(int j = 0; j < 10000; j++)
     {
     int q = rand.nextInt(126) + 1;
     if (partials[q] != q) continue;
                        
     int r;
     while(true)
     {
     r = q + rand.nextInt(VAR);
     if (r > 126) r = q - rand.nextInt(VAR);
     if (r < 1) continue;
     break;
     }
     if (partials[r] != r) continue;
     partials[r] = q;
     partials[q] = r;
     }
                        
     int count = 0;
     for(int i = 0; i < 128; i++)
     if (partials[i] == i) count++;
     if (count < bestCount)
     {
     System.arraycopy(partials, 0, best, 0, partials.length);
     bestCount = count;
     }
     }
                        
     System.out.print("{ ");
     for(int i = 0; i < 128; i++)
     {
     System.out.print("" + best[i] );
     if (i != 127) System.out.print(", ");
     }
     System.out.println(" };");
                
     int count = 0;
     for(int i = 0; i < 128; i++)
     { if (best[i] == i) { System.err.println(best[i]); count++;} }
     System.err.println(count);
     }
     }
*/


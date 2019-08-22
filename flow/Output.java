// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import javax.sound.sampled.*;
import java.util.*;
import java.util.zip.*;
import flow.modules.*;
import flow.utilities.*;
import org.json.*;
import java.io.*;

/**
   Output is the root singleton object of the synthesizer.  It is responsible for
   a variety of tasks, including:
   
   <ul>
   <p><li>Maintaining the sound output thread, which converts the latest output
   partials into samples and adds them to Java's audio buffer.
   <p><li>Choosing from available audio output lines.
   <p><li>Providing random number generators.
   <p><li>Maintaining the current <b>tick</b>, which is the standard timestep
   in the synthesizer to which everything is locked.  The tick is based
   on the current number of outputted samples.
   <p><li>Registering and managing the Sound objects, one per voicer
   <p><li>Running Sound voice threads [yep, the Sound objects don't do that themselves]
   <p><li>Running the primary voice sync thread loop, which ties together all the
   other threads.
   <p><li>Holds onto Input, which handles MIDI.
   </ul>
        
   <p>Generally speaking, here's what you do to set things up:
        
   <ol>
   <p><li>You first create an Output(), which gets the Sound output
   thread going.  
        
   <p><li>Then you create up to (and typically exactly) numVoices Sounds; in 
   their constructors they will register themselves with the Output.
        
   <p><li>Next, you either start calling go() in a tight loop, or call 
   startPrimaryVoiceThread(), which creates a separate thread
   (the primary voice sync thread) that does the same thing.  And you're off and 
   running!  go() method will build the various sound threads on the fly,
   so you want to make sure you've got all your sounds registered before you start
   calling it either directly or via startPrimaryVoiceThread().
   </ol>
**/


public class Output
    {
    /** Sampling rate of sound */
    public static final float SAMPLING_RATE = 44100.0f;
    public static final double NYQUIST = SAMPLING_RATE / 2.0;
        
    /** 1 / Sampling rate */
    public static final double INV_SAMPLING_RATE = 1.0 / SAMPLING_RATE;

    /** Size of the Java SourceDataLine audio buffer.  
        Larger and we have more latency.  Smaller and the system can't handle it.  
        It appears &geq 1024 is required on the Mac for 44100 KHz
    */
    public static final int DEFAULT_BUFFER_SIZE = 1152;                     
    static int bufferSize = -1;                     
    
    /** Number of samples emitted before reading the next partials output.
        Ideally this is 1; but it uses more juice.  If this is a large number
        then it contributes to lag because we interpolate from the previous partials
        to the current one.  At any rate, SKIP &leq BUFFER_SIZE for sure,
        typically significantly smaller.  I've found 32 seems to work well on my Mac.  16 seems to be okay too.
    */
    public static final int SKIP = 32;

    /** The most voices you're permitted to register with the Output. 
        Obviously more voices, more CPU usage.
    */
    public static final int DEFAULT_NUM_VOICES = 8;
    public static final int MAX_VOICES = 32;
    static int numVoices = -1;
    
    public static int getNumVoices() { return numVoices; }

    /** The default number of voices allocated to a single thread, by default 1.
        If we have lots of overhead in context switching, then we might want to set this higher,
        perhaps even to numVoices,  but having it be 1 would ideally allow us to distribute each 
        voice thread to a separate CPU -- if we have very costly voice threads, then this might be
        useful. 
    */
    public static final int DEFAULT_NUM_VOICES_PER_THREAD = 8;
    static int numVoicesPerThread = -1;
    
    public static final int DEFAULT_NUM_OUTPUTS_PER_THREAD = 2;
    static int numOutputsPerThread = -1;

    public static final double DEFAULT_VOLUME_MULTIPLIER = 2000;

    public static final float DEFAULT_MASTER_GAIN = 0.0f;

    // If a partial's volume is very low, we don't even bother computing its sample contribution, but just set it to zero.
    static final double MINIMUM_VOLUME = (1.0 / 65536 / 256);  // 0.0001

    // Output holds the input.  That makes total sense, right?  Right.  :-) 
    Input input;
    
    // The current Mixer
    Mixer.Info mixer;
    
    // The Audio Format
    AudioFormat audioFormat;
    
    // The audio output
    SourceDataLine sdl;

    // Audio buffer, which the audio output drains.
    // It's the Output Thread's job to keep this sucker filled as much as possible.
    byte[] audioBuffer = new byte[SKIP * 2];
    
    // The current number of voices spawned so far.  This increases as sounds register themselves.
    // This is will be threadsafe even though we increment it with ++ because that's only done in one thread.
    volatile int numSounds = 0;   
    
    static
        {
        // Load preferences
        numVoices = Prefs.getLastNumVoices();
        numVoicesPerThread = Prefs.getLastNumVoicesPerThread();
        numOutputsPerThread = Prefs.getLastNumOutputsPerThread();
        bufferSize = Prefs.getLastBufferSize();
        masterGain = Prefs.getLastMasterGain();
        //skip = Prefs.getLastSkip();   
        }
    
    public Output()
        {
        for(int i = 0; i < MAX_GROUPS; i++)
            {
            group[i] = new Group();
            }
                
        // We do NOT set this here because it confuse people doing Output programmatically.
        // Instead, we set it in AppMenu.playFirstMenu()
        // onlyPlayFirstSound = Prefs.getLastOneVoice();
        
        randomSeed = System.currentTimeMillis();
        sounds = new Sound[numVoices];
        positions = new double[numVoices][Unit.NUM_PARTIALS];
        audioFormat = new AudioFormat( SAMPLING_RATE, 16, 1, true, false );

        Mixer.Info[] mixers = getSupportedMixers();
        String mix = Prefs.getLastAudioDevice();
        boolean found = false;
        for (int i = 0; i < mixers.length; i++)
            {
            if (mixers[i].getName().equals(mix))
                {
                found = true;
                setMixer(mixers[i]);
                }
            }
        if (!found) setMixer(null); // sets to the first one, which is the default normally

        swap = new Swap();
        with = new Swap();
        startOutputThread();

        input = new Input(this);
        for(int i = 0; i < standardOrders.length; i++)
            standardOrders[i] = (byte)i;
        }

    /** Returns the currently used Mixer */
    public Mixer.Info getMixer()
        {
        return mixer;
        }
                
    /** Sets the currently used Mixer */
    public void setMixer(Mixer.Info mixer)
        {
        try
            {
            if (sdl != null)
                sdl.stop();
            if (mixer == null)
                {
                Mixer.Info[] m = getSupportedMixers();
                if (m.length > 0)
                    mixer = m[0];
                }
            if (mixer == null)
                sdl = AudioSystem.getSourceDataLine( audioFormat );
            else
                sdl = AudioSystem.getSourceDataLine( audioFormat, mixer );
            sdl.open(audioFormat, bufferSize);
            sdl.start();

            this.mixer = mixer;
            }
        catch (LineUnavailableException ex) { throw new RuntimeException(ex); }
        }

    /** Returns the available mixers which support the given audio format. */
    public Mixer.Info[] getSupportedMixers()
        {
        DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
        Mixer.Info[] info = AudioSystem.getMixerInfo();
        int count = 0;
        for (int i = 0; i < info.length; i++) 
            {
            Mixer m = AudioSystem.getMixer(info[i]);
            if (m.isLineSupported(lineInfo)) 
                {
                count++;
                }
            }

        Mixer.Info[] options = new Mixer.Info[count];
        count = 0;
        for (int i = 0; i < info.length; i++) 
            {
            Mixer m = AudioSystem.getMixer(info[i]);
            if (m.isLineSupported(lineInfo)) 
                options[count++] = info[i];
            }
        return options;
        }

    
    /* 
       Random Number Generation
    
       Each Sound has its own random number generator.
       You can get a new, more or less statistically independent generator from this method.
    */    
    Object randomLock = new Object[0];
    long randomSeed;
    Random getNewRandom() 
        { 
        synchronized(randomLock)
            {
            randomSeed += 10729347;  // or whatever
            return new Random(randomSeed);
            }
        }


    /** Returns the Input */
    public Input getInput()
        {
        return input;
        }
                

    /// Current Tick
    // Note NOT a long.  :-(  See note at getTick()
    private volatile int tick = 0;
    int syncTick = 0;

    void syncTick() { syncTick = tick; }

    /** Returns the Output's current tick.  Note that TICK is presently an INTEGER.  This means it will 
        roll over to a negative value once
        every 13 hours at 44100, or once every 26 hours at 22050.  It's not implemented as a long
        because there's no easy way to make tick a high-resolution rapid threadsafe counter without incurring
        a considerable efficiency cost, either as a volatile long (volatiles are good for occasional 
        writes by one one thread and lots of reads by another thread -- we have the opposite situation)
        or as a mutex.  This may change in the future, so be prepared to update ints to longs.  ;-)  */
    public int getTick() { return syncTick; }



    /* Locking and Registering Sounds */
        

    Sound[] sounds;
    java.util.concurrent.locks.ReentrantLock soundLock = new java.util.concurrent.locks.ReentrantLock(true);

    /** Acquires the global lock for the Voice Sync thread.  Acquiring this lock will stop the thread, and
        any of the individual voice threads, from reading or modifying any underlying Modulations, Units,
        or Sounds, so if you have acquired this lock, you are now free to modify them as you see fit, including
        registering new Modulations and Units, or even registering new sounds with the Output.  You should
        unlock this lock as quickly as you can after locking, by calling unlock().  So as to guarantee that you
        unlock the lock even if you do a premature return or an exception is thrown, the preferred pattern
        of your code is:
                
        <tt><pre>
        output.lock();
        try
        {
        // do your stuff here
        }
        finally
        {
        output.unlock();
        }
        </pre></tt>
                
        If you have already acquired this lock, calling lock() won't do anything but you should take care
        to call unlock() the same number of times as you called lock().  This lock is fair, meaning that if
        a thread has blocked waiting to acquire the lock, it won't ever get skipped over in favor of other 
        threads which tried to acquire after it (so-called "barging").
    */
    public void lock() 
        { 
        soundLock.lock(); 
        }
    
    
    
    /** Unlocks the global lock for the Voice Sync thread.  See lock().  You should
        unlock this lock as quickly as you can after locking, by calling unlock().  So as to guarantee that you
        unlock the lock even if you do a premature return or an exception is thrown, the preferred pattern
        of your code is:
                
        <tt><pre>
        output.lock();
        try
        {
        // do your stuff here
        }
        finally
        {
        output.unlock();
        }
        </pre></tt>
                
        If you do not have this lock, calling unlock() does nothing.
    */
    public void unlock() 
        {
        soundLock.unlock();
        }
        
    // Sounds register themselves with the Output using this method.
    void register(Sound sound) 
        { 
        lock();
        try
            {
            sound.index = numSounds; 
            sounds[numSounds++] = sound; 
            sound.setGroup(Output.PRIMARY_GROUP);
            input.addSound(sound);
            }
        finally
            {
            unlock();
            }
        }
    
    /** Returns the given Sound */  
    public Sound getSound(int i)
        {
        Sound s = null;
        lock();
        try
            {
            s = sounds[i];
            }
        finally
            {
            unlock();
            }
        return s;
        }
    
    /** Returns the given Sound without first locking on the sound lock.  This allows certain modules to
        test to see if their sound is (for example) Sound #0 without acquiring the sound lock, which they
        cannot do because in their go() methods it's owned by the main voice thread.  You should not play
        with this unless you know precisely what you are doing. */
    public Sound getSoundUnsafe(int i)
        {
        return sounds[i];
        }

    /** Returns the total number of Sounds */
    public int getNumSounds()
        {
        return numSounds;
        }
    
        
     
     
     
     
    ///// THREADS   

    ///// There are three kinds of threads in the system.
    /////
    ///// 1. The OUTPUT THREAD.  This is started in the constructor by calling the
    /////    startOutputThread() method.  This thread runs in the background and is
    /////    never killed.  It is responsible for occasionally grabbing the most
    /////    recent partials, stored in SWAP, and using them to produce samples
    /////    via buildSample, then emit them to the the audio system.  The Output
    /////    Thread also updates the current tick.
    /////
    ///// 2. The PRIMARY VOICE THREAD.  This thread could be the main() thread,
    /////    or it can be spawned independently via startPrimaryVoiceThread().
    /////    Either way, it does a single thing: it repeatedly calls Output.go().
    /////    This thread grabs the soundLock, so it knows nobody is modifying
    /////    the Sounds, then uses them to generate new partials, which are then
    /////    sent to the Output Thread.
    /////
    ///// 3. The PER-VOICE THREADS.  These threads may or may not exist depending
    /////    on the number of sounds and the value of VOICES_PER_SINGLE_THREAD.
    /////    The primary voice thread distributes sounds to each of these threads
    /////    as necessary to build the latest partials.  If needed, the primary
    /////    voice creates these via createPerVoiceThreads().
        


    // This is the lightweight semaphore between the Output thread and the primary
    // voice thread.  The primary voice thread sets this to TRUE to indicate that 
    // it has new partials to emit.  The Output thread grabs the partials and
    // starts emitting them, and then sets the variable to TRUE.  The primary
    // voice thread works in the background on the next partials, then spin-waits
    // until the variable is TRUE, then submits new partials etc.
    volatile boolean emitsReady;
    
    
    // This reminds the Primary Voice Thread that it has already created the
    // per-voice threads.
    boolean soundThreadsStarted = false;

    // Locks for negotiating between the primary voice thread and the per-voice threads
    // These are managed via blockVoiceUntil() and signalVoice()
    Object[] voiceLocks;
    boolean[] lightweightSemaphores;
        
    volatile boolean onlyPlayFirstSound;
    /** Returns whether we are only playing the first sound, or all sounds. */
    public boolean getOnlyPlayFirstSound() { return onlyPlayFirstSound; }
    /** Sets whether we are only playing the first sound, or all sounds. */
    public void setOnlyPlayFirstSound(boolean val) { onlyPlayFirstSound = val; }

    // notice that we're using lock() and unlock().  This is because velocitySensitive
    // is checked directly when building the Swap and we're already inside lock() and unlock() at that point
    // so we might as well use that.
    boolean velocitySensitive;
    /** Returns whether we are only playing the first sound, or all sounds. */
    public boolean getVelocitySensitive() { lock(); try { return velocitySensitive; } finally { unlock(); } }
    /** Sets whether we are only playing the first sound, or all sounds. */
    public void setVelocitySensitive(boolean val) { lock(); try { velocitySensitive = val; } finally { unlock(); } }


    // Contains the latest partials for the Output Thread to emit.  
    static class Swap
        {
//        double[][] amplitudes3;
//        double[][] amplitudes2;
        double[][] amplitudes;
        double[][] frequencies;
//        int pos;
//        int pos2;
//        int pos3;
//        int curpos;
        byte[][] orders;
        double[] pitches;
        double[] velocities;
        float reverbWet = 0.5f;
        float reverbRoomSize = 0.5f;
        float reverbDamp = 0.5f;
        boolean dephase[];
              
        public Swap()
            {
//            amplitudes2 = new double[numVoices][Unit.NUM_PARTIALS];
//            amplitudes3 = new double[numVoices][Unit.NUM_PARTIALS];
            amplitudes = new double[numVoices][Unit.NUM_PARTIALS];
            frequencies = new double[numVoices][Unit.NUM_PARTIALS];
            orders = new byte[numVoices][Unit.NUM_PARTIALS];
            pitches = new double[numVoices];
            velocities = new double[numVoices];
            dephase = new boolean[numVoices];
            }
        }
    
    // The primary voice thread sets these
    Swap swap;
    
    // The Output thread swaps "swap" with "with", and then uses the
    // with-partials while the primary thread is busy building new 
    // partials for swap again.
    Swap with;
        
    
    // Called by the Output Thread to check to see if new partials are
    // waiting, and if so, to swap and use them.    
    void checkAndSwap()
        {
        // notice that we're effectively spin-waiting here.  
        if (emitsReady)
            {
            Swap temp = swap;
            swap = with;
            with = temp;
            emitsReady = false;
            }
        else
            {
            //Thread.currentThread().yield();           // we don't want the output thread to yield...
            }
        }
      
    float[][] freeverbInput = new float[2][1];
    float[][] freeverbOutput = new float[2][1];
    FreeVerb freeverb = new FreeVerb();

    /// Current positions of the sine wave functions (from 0 ... 2PI)
    double[][] positions;

    // 0.05 is about 3 32-sample periods before we get to near to 100%
    // 0.03 is about 3 32-sample periods before we get to near to 95%
    // 0.025 is about 3 32-sample periods before we get to near to 92%
    // 0.02 is about 3 32-sample periods before we get to near to 86%
    // the problem is that if we move too rapidly, we get a slight, uhm, breathy click when partials move fast.
    // Warmth.flow is a good example of the sound.  I can't reproduce the sound audibly at 0.025, so that's what I'm
    // going with.
    static final double PARTIALS_INTERPOLATION_ALPHA = 0.025;
    static final double ONE_MINUS_PARTIALS_INTERPOLATION_ALPHA = 1.0 - PARTIALS_INTERPOLATION_ALPHA;
    
    /** A value that's significantly higher than IEEE 754 subnormals, used by Output.java and Smooth.java
        to make sure they're not dropping into subnormal math. */
    public static final double WELL_ABOVE_SUBNORMALS = 1.0e-200;
    
    public static final double PI2 = Math.PI * 2.0;
    
    public static double undenormalize(double val)              // assumes only positive values
        {
        if (val > 0 && val <= 1e-200)           // really it's 2250738585072012e-308, but I'm giving breathing room for multiplication
            val = 0;
        return val;
        }
        
    // can't quite squeeze this into 35 bytes :-(   It's 37, so not inlined
    public static void undenormalize(double[] val)              // assumes only positive values
        {
        for(int i = 0; i < val.length; i++)
            {
            if (val[i] > 0 && val[i] <= 1e-200)             // really it's 2250738585072012e-308, but I'm giving breathing room for multiplication
                val[i] = 0;
            }
        }
        

    public static void printDenormal(double val, String s)
        {
        if (val > 0 && val <= 2250738585072012e-308)
            System.err.println("Output.printDenormal() WARNING: " + s + " is DENORMAL " + val);
        }
    

    // Builds a single sample from the partials.  ALPHA is the current interpolation
    // factor (from 0...1) 
    double buildSample(int s, double[][] currentAmplitudes)
        {
        // build the sample
        double sample = 0;
        Swap _with = with;
        double[] amp = _with.amplitudes[s];
        double[] freq = _with.frequencies[s];
        byte[] orders = _with.orders[s];
        double[] pos = positions[s];
        double[] ca = currentAmplitudes[s];
        double v = _with.velocities[s];
        double pitch = _with.pitches[s];
        double tr = pitch * INV_SAMPLING_RATE;
        boolean dephase = _with.dephase[s];
        
        if (dephase)                    // this is a manual hoist
            {
            for (int i = 0; i < pos.length; i++)
                {
                double amplitude = amp[i];
                int oi = orders[i];
                if (oi < 0) oi += 256;          // if we're using 256 partials, they need to be all positive
                                        
                // incoming amplitudes are pre-denormalized by the voice threads.
                // However when we multiply by ONE_MINUS_PARTIALS_INTERPOLATION_ALPHA we can still
                // get denormalized.  So we undenormalize here.  It's theoretically possible that we
                // could still get denormalized when summing the samples below; but I have not been
                // able to cause that.
                double aa = (ca[oi] * ONE_MINUS_PARTIALS_INTERPOLATION_ALPHA);
                double bb = amplitude * PARTIALS_INTERPOLATION_ALPHA;
                amplitude = aa + bb;
                if (amplitude < 1e-200) amplitude = 0;          // undenormalize prior to next go-around
                ca[oi] = amplitude;

                double frequency = freq[i];
                                                                
                // Because we're mixing, we can just ignore the higher frequency stuff, which gives us a speed boost.
                // However if the user wants to switch back to non-mixing, it might produce a pop because we have
                // reset everything.
                if (frequency * pitch > NYQUIST)
                    {
                    break;
                    }
                if (amplitude <= MINIMUM_VOLUME)
                    {
                    continue;
                    }
                                
                double position = pos[oi] + frequency * tr;
                position = position - (int) position;                   // fun fact. this is 9 times faster than position = position % 1.0
                pos[oi] = position;
                        
                sample += Utility.fastSin(position * PI2 + MIXING[oi]) * amplitude;
                }
            }
        else
            {
            for (int i = 0; i < pos.length; i++)
                {
                double amplitude = amp[i];
                int oi = orders[i];
                if (oi < 0) oi += 256;          // if we're using 256 partials, they need to be all positive
                                                        
                // incoming amplitudes are pre-denormalized by the voice threads.
                // However when we multiply by ONE_MINUS_PARTIALS_INTERPOLATION_ALPHA we can still
                // get denormalized.  So we undenormalize here.  It's theoretically possible that we
                // could still get denormalized when summing the samples below; but I have not been
                // able to cause that.
                double aa = (ca[oi] * ONE_MINUS_PARTIALS_INTERPOLATION_ALPHA);
                double bb = amplitude * PARTIALS_INTERPOLATION_ALPHA;
                amplitude = aa + bb;
                if (amplitude < 1e-200) amplitude = 0;          // undenormalize prior to next go-around
                ca[oi] = amplitude;
                                
                double frequency = freq[i];
 
                double position = pos[oi] + frequency * tr;
                position = position - (int) position;                   // fun fact. this is 9 times faster than position = position % 1.0
                pos[oi] = position;
                        
                if (frequency * pitch <= NYQUIST && amplitude > MINIMUM_VOLUME)
                    {
                    sample += Utility.fastSin(position * PI2) * amplitude;
                    }
                }
            }

        return sample * v;
        }
        
    volatile boolean clipped = false;
    // Obviously this is not atomic, but it's not a big deal as we're just
    // using it in the GUI to display possible clips, so if we drop a clip by wild
    // chance it's not a big deal.
    public boolean getAndResetClipped() { boolean val = clipped; clipped = false; return val; }

    volatile boolean glitched = false;
    // Obviously this is not atomic, but it's not a big deal as we're just
    // using it in the GUI to display possible glitches, so if we drop a glitch by wild
    // chance it's not a big deal.
    public boolean getAndResetGlitched() { boolean val = glitched; glitched = false; return val; }




    // Locks for negotiating between the primary output thread and the per-output threads
    // These are managed via blockOutputUntil() and signalOutput()
    Object[] outputLocks;
    boolean[] lightweightOutputSemaphores;
        

    //// When the semaphore is FALSE, the per-output thread is in charge of its Sound.
    //// When the semaphore is TRUE, the primary output thread is in charge.
    ////
    //// The primary output thread signals TRUE, then blocks until FALSE.
    //// The reverse is true for the per-output threads.
    
    void blockOutputUntil(int output, boolean val)
        {
        synchronized(outputLocks[output])
            {
            while(lightweightOutputSemaphores[output] != val)
                {
                try { outputLocks[output].wait(); } catch (Exception e) { }
                }
            }
        }
                
    void signalOutput(int output, boolean val)
        {
        synchronized(outputLocks[output])
            {
            lightweightOutputSemaphores[output] = val;
            outputLocks[output].notify();
            }
        }



    double samples[][] = new double[0][SKIP];

    // Starts the output thread.  Called from the constructor.
    void startOutputThread()
        {
        Thread thread = new Thread(new Runnable()
            {
            public void run()
                {
                /// The last amplitudes (used for interpolation between the past partials and new ones)
                /// Note that these are indexed by ORDER, not by actual index position
                final double[][] currentAmplitudes = new double[numVoices][Unit.NUM_PARTIALS];
  
                lightweightOutputSemaphores = new boolean[numVoices];
                outputLocks = new Object[numVoices];
                for (int i = 0; i < numVoices; i++) 
                    {
                    outputLocks[i] = new Object[0];
                    lightweightOutputSemaphores[i] = true;
                    }

                for(int i = 0; i < numVoices; i += numOutputsPerThread)
                    {
                    final int _i = i;
                    Thread thread = new Thread(new Runnable()
                        {
                        public void run()
                            {
                            while(true) 
                                {
                                blockOutputUntil(_i, true); 
                                
                                int n = numVoices;
                                if (n >  _i + numOutputsPerThread)
                                    n =  _i + numOutputsPerThread;
                                        
                                for(int j = _i; j < n; j++)
                                    {
                                    if (j < samples.length)         // voice hasn't been loaded yet, hang tight
                                        {
                                        double[] samplessnd = samples[j];
                                        for (int skipPos = 0; skipPos < SKIP; skipPos++)
                                            {
                                            samplessnd[skipPos] = buildSample(j, currentAmplitudes) * DEFAULT_VOLUME_MULTIPLIER;
                                            }
                                        }
                                    }
                                                        
                                signalOutput(_i, false);
                                }
                            }
                        });
                    thread.setName("Output " + _i);
                    thread.setDaemon(true);
                    thread.start();
                    }

                             
                while(true)
                    {
                    int solo = -1;
                    
                    int available = sdl.available();
                    if (available >= bufferSize - 128)
                        {
                        glitched = true;
                        }
                        
                    if (samples.length != numSounds)
                        {
                        samples = new double[numSounds][SKIP];
                        }

                    checkAndSwap();
                    
                    if (onlyPlayFirstSound)
                        {
                        Sound sound = input.getLastPlayedSound();
                        if (sound == null)
                            solo = 0;
                        else
                            solo = sound.getIndex();
                                                        
                        double[] samplessnd = samples[solo];
                        for (int skipPos = 0; skipPos < SKIP; skipPos++)
                            {
                            samplessnd[skipPos] = buildSample(solo, currentAmplitudes) * DEFAULT_VOLUME_MULTIPLIER;
                            }
                        }
                    else
                        {
                        // Fire up output threads
                        for(int snd = 0; snd < numSounds; snd += numOutputsPerThread)
                            {
                            signalOutput(snd, true);
                            }
                        for(int snd = 0; snd < numSounds; snd += numOutputsPerThread)
                            {
                            blockOutputUntil(snd, false);
                            }
                        }
                        
                    if (with.reverbWet > 0.0f)
                        {        
                        freeverb.setWet(with.reverbWet);
                        freeverb.setRoomSize(with.reverbRoomSize);
                        freeverb.setDamp(with.reverbDamp);
                        }
                        
                    double gain = masterGain;           // so we're not reading a volatile variable!

                    for (int skipPos = 0; skipPos < SKIP; skipPos++)
                        {
                        double d = 0;
                        if (solo != -1)
                            {
                            d += samples[solo][skipPos];
                            }
                        else
                            {
                            for(int snd = 0; snd < samples.length; snd++)
                                {
                                d += samples[snd][skipPos];
                                }
                            }
                            
                        // add reverb?
                        if (with.reverbWet > 0.0f)
                            {
                            // I think freeverb sounds better going in both channels and taking
                            // both channel results.  But you may have a different opinion, in
                            // which I think you do: 
                            //
                            //freeverbInput[0][0] = (float)d;
                            //freeverb.compute(1, freeverbInput, freeverbOutput);
                            //d = freeverbOutput[0][0]; 
                                
                            freeverbInput[0][0] = (float)d;
                            freeverbInput[1][0] = (float)d;
                            freeverb.compute(1, freeverbInput, freeverbOutput);
                            d = (freeverbOutput[0][0] + freeverbOutput[1][0]) / 2; 
                            }
                                                    
                        d *= gain;
                                                            
                        if (d > 32767)
                            {
                            d = 32767;
                            clipped = true;
                            }
                        else if (d < -32768)
                            {
                            d = -32768;
                            clipped = true;
                            }
                        
                        int val = (int)(d);
                        audioBuffer[skipPos * 2 + 1] = (byte)((val >> 8) & 255);
                        audioBuffer[skipPos * 2] = (byte)(val & 255);
                        tick++;                                 /// See documentation elsewhere about threadsafe nature of tick
                        }
                    
                    sdl.write(audioBuffer, 0, SKIP * 2);
                    }
                }
            });
        
        thread.setName("Sound Output");
        thread.setDaemon(true);
        thread.start();
        }
        


    // Starts the per-voice threads.  Called from primary voice thread if it needs to.
    void startPerVoiceThreads(int numThreads)
        {
        // build the sound threads
        soundThreadsStarted = true;
        lightweightSemaphores = new boolean[numThreads];
        voiceLocks = new Object[numThreads];
        for (int i = 0; i < numThreads; i++) 
            {
            voiceLocks[i] = new Object[0];
            lightweightSemaphores[i] = true;
            }
                                                
        for (int i = 0; i < numThreads; i++)
            {
            final int _i = i;
            Thread thread = new Thread(new Runnable()
                {
                public void run()
                    {
                    long lastTick = -1;
                    int tickCount = 0;
                    double tickAvg = 0;
                                                
                    while(true) 
                        {
                        blockVoiceUntil(_i, true); 
                        for (int j = 0; j < numVoicesPerThread; j++)
                            {
                            int voice = _i * numVoicesPerThread + j;
                            if (voice >= numSounds) 
                                break;
                            else
                                {
                                sounds[voice].go(); 
                                }
                            }
                        signalVoice(_i, false);
                        }
                    }
                });
            thread.setName("Voice " + _i);
            thread.setDaemon(true);
            thread.start();
            }       
        }
                
                
    /**
       Starts the primary voice thread.  You either have to create this thread
       or you have to do 
       <p>
       <p>while(true) output.go();
       <p>
       <p>
       ... in the thread of your choice (typically main()'s thread, after
       building the GUI and/or setting up the sounds.
    */
    public void startPrimaryVoiceThread()
        {
        Thread thread = new Thread(new Runnable()
            {
            public void run()
                {
                while(true)
                    {
                    go();
                    }
                }
            });
        thread.setName("Voice Management");
        thread.setDaemon(true);
        thread.start();
        }
        
        
    //// When the semaphore is FALSE, the per-voice thread is in charge of its Sound.
    //// When the semaphore is TRUE, the primary voice thread is in charge.
    ////
    //// The primary voice thread signals TRUE, then blocks until FALSE.
    //// The reverse is true for the per-voice threads.
    
    void blockVoiceUntil(int thread, boolean val)
        {
        synchronized(voiceLocks[thread])
            {
            while(lightweightSemaphores[thread] != val)
                {
                try { voiceLocks[thread].wait(); } catch (Exception e) { }
                }
            }
        }
                
    void signalVoice(int thread, boolean val)
        {
        synchronized(voiceLocks[thread])
            {
            lightweightSemaphores[thread] = val;
            voiceLocks[thread].notify();
            }
        }



    double[] zeroAmplitudes = new double[Unit.NUM_PARTIALS];
    double[] zeroFrequencies = new double[Unit.NUM_PARTIALS];
    byte[] standardOrders = new byte[Unit.NUM_PARTIALS];
    
    /** Called to pulse the Output.  This will cause the Output to wait until the user is no longer
        modifying modules via the GUI, then have all the Sounds produce new partials by calling
        go() on them.  The resulting partials will then be loaded for the Output Thread to 
        convert into sound.   This method should be called in a while-loop from your main thread
        or via calling startPrimaryVoiceThread(). */ 
    public void go()
        {
        lock();
        try
            {
            syncTick();
            input.getMidiClock().syncTick();
            
            input.go();
            
            if (!soundThreadsStarted)
                for (int i = 0; i < numSounds; i++)
                    {
                    sounds[i].reset();
                    }
                
            if (numSounds <= numVoicesPerThread)
                {
                for (int i = 0; i < numSounds; i++)
                    {
                    sounds[i].go();
                    }
                soundThreadsStarted = true;
                }
            else
                {
                int numThreads = (int)(Math.ceil(numSounds / (double)numVoicesPerThread));
                if (!soundThreadsStarted)
                    {
                    startPerVoiceThreads(numThreads);

                    for (int i = 0; i < numThreads; i++)
                        {
                        blockVoiceUntil(i, false);
                        }
                    }

                for (int i = 0; i < numThreads; i++)
                    {
                    signalVoice(i, true);
                    }
                for (int i = 0; i < numThreads; i++)
                    {
                    blockVoiceUntil(i, false);
                    }
                }
            }
        finally 
            {
            unlock();
            }

        // Spin-wait.  It's both faster and more efficient than a mutex in this case, but it eats up cycles
        while(emitsReady)
            {
            Thread.currentThread().yield();
            }
                
        lock();
        try
            {
            Unit e = sounds[0].getEmits();
            for (int i = 0 ; i < numSounds; i++)
                {
                Unit emits = sounds[i].getEmits();
                if (emits != null)
                    {
//                    System.arraycopy(swap.amplitudes2[i], 0, swap.amplitudes3[i], 0, swap.amplitudes2[i].length); 
//                    System.arraycopy(swap.amplitudes[i], 0, swap.amplitudes2[i], 0, swap.amplitudes[i].length); 


                    System.arraycopy(emits.amplitudes[0], 0, swap.amplitudes[i], 0, emits.amplitudes[0].length); 
                    undenormalize(swap.amplitudes[i]);
                    System.arraycopy(emits.frequencies[0], 0, swap.frequencies[i], 0, emits.frequencies[0].length);
                    System.arraycopy(emits.orders[0], 0, swap.orders[i], 0, emits.orders[0].length);
                    }
                else
                    {
//                    System.arraycopy(zeroAmplitudes[i], 0, swap.amplitudes3, 0, zeroAmplitudes.length); 
//                    System.arraycopy(zeroAmplitudes, 0, swap.amplitudes2, 0, zeroAmplitudes.length); 
                    
                    
                    System.arraycopy(zeroAmplitudes, 0, swap.amplitudes[i], 0, zeroAmplitudes.length); 
                    System.arraycopy(zeroFrequencies, 0, swap.frequencies[i], 0, zeroFrequencies.length); 
                    System.arraycopy(standardOrders, 0, swap.orders[i], 0, standardOrders.length); 
                    }
                    
                swap.pitches[i] = sounds[i].getPitch();
                swap.velocities[i] = (velocitySensitive ? sounds[i].getVelocity() : Sound.DEFAULT_VELOCITY);
                if (emits instanceof Out)
                    {
                    swap.dephase[i] = ((Out)emits).getDephase();
                    }
                else
                    {
                    System.err.println("Output.go() WARNING, emits isn't an Out!");
                    }
                }
                
            if (e instanceof Out)       // we're only doing this for ONE sound, namely sounds[0]
                {
                Out out = (Out)e;
                swap.reverbWet = (float)(out.modulate(out.MOD_REVERB_WET));
                swap.reverbDamp = (float)(out.modulate(out.MOD_REVERB_DAMP));
                swap.reverbRoomSize = (float)(out.modulate(out.MOD_REVERB_ROOM_SIZE));
                }
            }
        finally 
            {
            unlock();
            }
        
        emitsReady = true;
        }  






    //// GROUPS
    ////
    //// Each sound is assigned a GROUP.
    //// By default sounds are assigned to the PRIMARY GROUP, which is group 0.
    //// Sound 0 is *always* assigned to the PRIMARY GROUP.
    //// Other sounds are overridden to fill as many slots as possible for
    //// the other groups according to their numRequestedSounds[]
        
    /** Maximum number of possible groups */    
    public static final int MAX_GROUPS = 32;
    /** Primary group.  This is always group 0. */
    public static final int PRIMARY_GROUP = 0;
    /** Indicates no group has been assigned */
    public static final int NO_GROUP = -1;
    
    int numGroups = 1;
    Group group[] = new Group[MAX_GROUPS];
    
    /** Returns the actual group for the given group index. */
    public Group getGroup(int i) { return group[i]; }
    
    /** Returns the groups. Be careful with this. */
    public Group[] getGroups() { return group; }
    
    /** Return the number of groups currently allocated */
    public int getNumGroups() { return numGroups; }

    public int getGroupOverridingPrimaryGroupInMIDI()
        {
        lock();
        try
            {
            for(int i = 1; i < numGroups; i++)
                {
                if (group[i].channel >= 0 &&
                    group[i].channel == group[0].channel)
                    return i;
                }
            return Output.PRIMARY_GROUP;
            }
        finally
            {
            unlock();
            }
        }

    /** Moves group index i to JUST ABOVE current index j */ 
    public void moveGroup(int i, int j)
        {
        if (i == j)
            {
            return;  // nothing will change, so why bother
            }
        else if (i == j + 1)
            {
            return;  // nothing will change, so why bother
            }
        else if (i == 0)
            {
            System.err.println("Output.moveGroup() WARNING: group is 0, cannot be moved");
            }
        else if (i >= numGroups)
            {
            System.err.println("Output.removeGroup() WARNING: group >= numGroups, should not exist");
            }
        else if (j < 0)
            {
            System.err.println("Output.moveGroup() WARNING: location is < 0, cannot be moved to");
            }
        else if (j >= numGroups)
            {
            System.err.println("Output.removeGroup() WARNING: location >= numGroups - 1, should not exist");
            }
        else
            {
            lock();
            try
                {
                // grab and remove the group we're moving
                Group g = group[i];
                removeGroup(i);
                
                // adjust the insertion location
                if (i < j)
                    j--;                    // it was shifted in removal
                                
                // make space
                setNumGroups(getNumGroups() + 1);
                for(int q = numGroups - 1; q > j + 1; q--)
                    {
                    group[q] = group[q - 1];
                    }
                                
                // insert
                group[j + 1] = g;
                assignGroupsToSounds();
                }
            finally 
                {
                unlock();
                }
            }
        }

    /** Sets the number of groups currently allocated, does not clear new ones */
    public void setNumGroupsUnsafe(int num) 
        {
        if (num < 1) num = 1; 
        
        // reset ABOVE num
        int minNum = num;
        for(int i = minNum; i < MAX_GROUPS; i++)
            {
            group[i] = new Group();
            }

        numGroups = num; 
        }

    /** Sets the number of groups currently allocated */
    public void setNumGroups(int num) 
        {
        if (num < 1) num = 1; 
        
        // reset requested sounds
        int minNum = (num > numGroups ? numGroups : num);
        for(int i = minNum; i < MAX_GROUPS; i++)
            {
            group[i] = new Group();
            }

        numGroups = num; 
        }
    
    /** Redistribute the gain for all groups except the primary group. */
    public void redistributeGains()
        {
        lock();
        try
            {
            for(int g = 1; g < numGroups; g++)
                {
                double val = group[g].getGain();
                // distribute to the gain of all the Out modules involved
                for(int i = 0; i < numSounds; i++)
                    {
                    Sound s = getSound(i);
                    if (s.getGroup() == g)
                        {
                        ((Out)(s.getEmits())).setModulation(new Constant(val), Out.MOD_GAIN);
                        }
                    }
                }
            }
        finally
            {
            unlock();
            }
        }
        
    /** Returns the number of allocated sounds for the given group. */
    public int getNumSounds(int group) 
        { 
        int counter = 0;
        lock();
        try
            {
            for(int j = 0; j < numSounds; j++)
                {
                if (sounds[j].getGroup() == group)
                    counter++;
                }
            }
        catch (Exception ex) 
            {
            ex.printStackTrace(); 
            }
        finally
            {
            unlock();
            }
        return counter; 
        }

    /** Reassign sounds to groups */    
    public void assignGroupsToSounds()
        {
        lock();
        try
            {
            // by default we're the primary group
            for(int j = 0; j < numSounds; j++)
                {
                sounds[j].setGroup(Output.PRIMARY_GROUP);
                }
            
            // override by sub-patches
            int snd = 1;                                                        // because sound 0 always belongs to the primary group
            for(int i = 1; i < numGroups; i++)              // note 1, we skip the primary group
                {
                for(int j = 0; j < group[i].getNumRequestedSounds(); j++)
                    {
                    if (snd < numSounds)                            // we still have space
                        {
                        sounds[snd].setGroup(i);
                        snd++;
                        }
                    }
                }
                                
            /// FIXME -- this won't save out the subpatches will it?
            sounds[0].saveModules(group[0].getPatch());                // so we have the latest when we reload them
                                
            // reload patches.  We assume we have the correct patches in each group, and the latest and greatest in group 0
            for(int i = 1; i < numSounds; i++)          // the first sound is already assigned to group 0 and doesn't change, else we'd have to update the GUI module panels
                {
                int g = sounds[i].getGroup();
                Modulation[] mods = new Modulation[0];
                try 
                    { 
                    // load modules into an array to prepare to load into the sound
                    mods = Sound.loadModules(group[g].getPatch(), Sound.loadFlowVersion(group[g].getPatch()));
                    }
                catch (Exception ex) { ex.printStackTrace(); }
                // version
                                        
                // remove any old modules from sound
                int numRegistered = sounds[i].getNumRegistered();
                for(int j = 0; j < numRegistered; j++)
                    sounds[i].removeRegistered(0);
                                                
                // Add new modules from the array
                for(int j = 0; j < mods.length; j++)
                    {
                    sounds[i].register(mods[j]);
                    mods[j].setSound(sounds[i]);
                    if (mods[j] instanceof Out)
                        {
                        sounds[i].setEmits((Out)(mods[j]));
                        }
                    mods[j].reset();
                    }
                }
            redistributeGains();
            }
        finally
            {
            unlock();
            }
        }

    public void removeGroup(int g)
        {
        if (g == 0) // can't remove that one
            {
            System.err.println("Output.removeGroup() WARNING: group is 0, cannot be removed");
            }
        else if (g >= numGroups)
            {
            System.err.println("Output.removeGroup() WARNING: group >= numGroups, should not exist");
            }
        else
            {
            lock();
            try
                {
                Group ret = group[g];
                for(int i = g; i < numGroups - 1; i++)
                    {
                    group[i] = group[i + 1];
                    }
                setNumGroups(getNumGroups() - 1);
                assignGroupsToSounds();
                }
            finally 
                {
                unlock();
                }
            }
        }

    public void swapWithPrimaryGroup(int g)
        {
        lock();
        try
            {
            // save patch info
            sounds[0].saveModules(group[0].getPatch());                // so we have the latest when we reload them
            Group primary = group[0];
            group[0] = group[g];
            group[g] = primary;
            assignGroupsToSounds();
            }
        finally 
            {
            unlock();
            }
        }


    public boolean copyPrimaryGroup(int to, boolean resetMIDI)
        {
        lock();
        try
            {
            // save patch info
            sounds[0].saveModules(group[0].getPatch());                // so we have the latest when we reload them
            group[to] = new Group(group[0]);
            if (resetMIDI)
                {
                group[to].setChannel(Input.CHANNEL_NONE);
                }
                                                        
            // rebuild
            assignGroupsToSounds();
            }
        finally 
            {
            unlock();
            }
        return true;
        }

    public boolean copyPrimaryGroup(boolean resetMIDI)
        {
        lock();
        try
            {
            // save patch info
            sounds[0].saveModules(group[0].getPatch());                // so we have the latest when we reload them
            if (addGroup(new Group(group[0])))
                {
                if (resetMIDI)
                    group[numGroups - 1].setChannel(Input.CHANNEL_NONE);
                                                        
                // rebuild
                assignGroupsToSounds();
                }
            else
                {
                return false;
                }
            }
        finally 
            {
            unlock();
            }
        return true;
        }
        
    public boolean addGroup(Group g)
        {
        if (numGroups >= MAX_GROUPS - 1)
            {
            System.err.println("Output.addGroup() WARNING: numGroups >= MAX_GROUP - 1, cannot increase");
            return false;
            }
        else
            {
            lock();
            try
                {
                // increment group
                setNumGroups(getNumGroups() + 1);
                group[getNumGroups() - 1] = g;
                }
            finally
                {
                unlock();
                } 
            return true;
            }
        }
                
    public int addGroup(JSONObject obj)
        {
        // copy the patch so it can be modified by others
        obj = new JSONObject(obj, JSONObject.getNames(obj));
                
        if (numGroups >= MAX_GROUPS - 1)
            {
            System.err.println("Output.removeGroup() WARNING: numGroups >= MAX_GROUP - 1, cannot increase");
            return -1;
            }
        else
            {
            lock();
            try
                {
                // increment group
                setNumGroups(getNumGroups() + 1);
                Group g = group[getNumGroups() - 1];
                                
                try 
                    { 
                    // determine gain.  This will be costly.
                    // we have to build a patch to find Out.
                    // We could do this by searching through the patch JSON, but this is simpler and stupider
                    
                    g.setGain(Group.DEFAULT_GAIN);
                    Modulation[] mods = Sound.loadModules(obj, Sound.loadFlowVersion(obj));
                    for(int i = 0; i < mods.length; i++)
                        {
                        if (mods[i] instanceof Out)  // got it
                            {
                            g.setGain(((Out)mods[i]).modulate(Out.MOD_GAIN));
                            break;
                            }
                        }                    
                    g.setNumRequestedSounds(2);
                    g.setPatch(obj); 
                    g.setChannel(Input.CHANNEL_NONE);
                    }
                catch (Exception ex) { ex.printStackTrace(); }
                assignGroupsToSounds();
                }
            finally 
                {
                unlock();
                }
            return getNumGroups() - 1;
            }
        }
    
    static volatile double masterGain = 1.0;
    
    public double getMasterGain() 
        { 
        return masterGain;
        }      
                
    public void setMasterGain(double val) 
        { 
        masterGain = val;
        Prefs.setLastMasterGain(val);
        }      

    public static final double MAX_MASTER_GAIN = 4.0;

    final static double[] MIXING = new double[]
    {
    4.3930522285718725, 
    2.0980074779050573, 
    5.911617141284784, 
    3.302416782999798, 
    0.532648003019616, 
    3.864552401356139, 
    0.6828907136007456, 
    3.309690303702759, 
    3.78626929045274, 
    2.880490449092737, 
    0.6412955842415297, 
    4.210561997218116, 
    1.329093445651415, 
    4.423635829349905, 
    0.9697059483036133, 
    3.3800872309232752, 
    5.190082241176484, 
    4.179658018481378, 
    3.1761418769465792, 
    1.8514888920948884, 
    5.672079791211344, 
    2.80457371107737, 
    5.752510144539967, 
    2.0542370131054426, 
    4.059706623677195, 
    5.059878289798797, 
    3.2535931944316436, 
    1.251760344228206, 
    1.1697673184395325, 
    5.499988425488455, 
    2.5487075166480784, 
    5.671174843247418, 
    1.5889337469209264, 
    3.4966660573449286, 
    4.1128697586043845, 
    1.32223042596809, 
    3.634874408433536, 
    5.4727212070712845, 
    4.050330922059182, 
    1.329430792144477, 
    4.347179582872594, 
    0.49531926549671, 
    1.9981399038451473, 
    2.2024506973065567, 
    5.422728478571278, 
    4.342491898622478, 
    2.340585893867574, 
    1.595375474206211, 
    5.534647380650129, 
    2.1854360354552314, 
    4.407323953511916, 
    5.875238413239378, 
    5.807281013442006, 
    2.611730008727033, 
    2.404221048946509, 
    4.5142628863423, 
    4.585857046489191, 
    2.7585550894249278, 
    6.034609601973635, 
    1.784818848275311, 
    2.9385777003341786, 
    2.7379768217657237, 
    5.204281144711419, 
    2.5743565427801682, 
    5.633773284292306, 
    0.37163019591190505, 
    5.4767684684458136, 
    2.469637673803118, 
    4.796244127749591, 
    6.131515108692101, 
    5.442227729315259, 
    1.6393092165445262, 
    2.905279921897077, 
    5.887610452331277, 
    2.231151580725118, 
    1.311410267937173, 
    5.1320011670472665, 
    1.0637225421542722, 
    2.161631508466618, 
    1.542156486372099, 
    3.7292113803036364, 
    1.7137086767243934, 
    5.4642677550381755, 
    3.0853750211403574, 
    3.0173444667823857, 
    0.6315436517090349, 
    3.3421180705036546, 
    4.209646257875283, 
    3.7926270717945605, 
    5.819113544074465, 
    1.904641568725281, 
    3.909964870239804, 
    3.5835554395539133, 
    3.8436429642943346, 
    2.197853136078777, 
    4.866540140523055, 
    6.18086741431614, 
    5.4147864419797465, 
    1.123181428416226, 
    5.767777606674638, 
    1.2247017979747263, 
    3.2021001566014204, 
    4.509373040115699, 
    3.7362376775556543, 
    0.08683319620390151, 
    1.2405920482832018, 
    0.8724415122969259, 
    2.961759106078713, 
    2.0771434354580864, 
    2.923727915641903, 
    2.7159263377789484, 
    5.20302805883522, 
    3.8574313667909914, 
    0.9620003437805832, 
    2.758032882552134, 
    2.110632917575416, 
    4.423350054567123, 
    4.6696945714405045, 
    3.366131602771689, 
    1.4757790606175893, 
    4.423642339560315, 
    4.92807311466011, 
    2.067548270280224, 
    3.772824744655635, 
    2.84617535065996, 
    4.6283185725205, 
    0.092443463110639, 
    6.154303648109571, 
    5.764843632300961, 
    3.7978438062146234, 
    4.581524010352971, 
    2.6704472245847883, 
    4.28496763451278, 
    3.079068811519279, 
    6.215571764084275, 
    1.6727154189264666, 
    1.5702907676316764, 
    3.0167494385334965, 
    3.5141258234246533, 
    4.5840366922727425, 
    2.7706972066454743, 
    4.294461552185262, 
    3.807630008844287, 
    0.2630172179015329, 
    2.115045951553029, 
    3.423269843873172, 
    5.7978670935946, 
    1.6945359810872747, 
    3.0436962789661797, 
    2.368298216504867, 
    4.8554730921226374, 
    4.159204477042065, 
    0.21666124977089396, 
    3.381162299738797, 
    0.1441628078280086, 
    1.541825983163458, 
    5.529820230628496, 
    3.0811151395049796, 
    1.463250254179737, 
    3.9225001868930343, 
    4.133792216068891, 
    0.9242787399407808, 
    4.52161640487037, 
    3.9304320982687724, 
    1.6630928269575334, 
    3.6124930881561648, 
    1.5756809048093188, 
    1.2284009214428435, 
    3.6667071469745096, 
    3.731393225939248, 
    1.3805717439790721, 
    4.3356357222344934, 
    2.808892673945348, 
    2.1194589855109074, 
    1.1925169734369456, 
    5.850783005068567, 
    5.6640714433910375, 
    3.0339273779002425, 
    4.201648701469412, 
    0.3289178314175416, 
    5.161934219713718, 
    5.932017725015613, 
    5.54990471757801, 
    3.2685298202768385, 
    3.3400508259054345, 
    4.61241753082556, 
    2.024160879689441, 
    3.108107374510848, 
    0.21973074174871066, 
    4.536135686410281, 
    3.023281955038178, 
    0.35146993875006327, 
    6.06360994679823, 
    1.6810294052530046, 
    3.1837768890758444, 
    0.6374076523146917, 
    1.6375793184645058, 
    6.142404473157965, 
    5.048278576240986, 
    3.8565616882630644, 
    3.781988825321103, 
    0.6626461774182255, 
    4.511015710407045, 
    1.3781495182202912, 
    0.3048210194240344, 
    1.2281990925819724, 
    5.530568411276023, 
    3.9826621922332768, 
    6.148919688034672, 
    0.3849323271843632, 
    2.3895972379985033, 
    2.0351897098873533, 
    5.478720725311914, 
    4.5101107624565735, 
    0.5674943806341197, 
    4.667407863259131, 
    4.180649474473559, 
    0.761845235610469, 
    1.0511013301219272, 
    2.078027466814059, 
    3.922709192162581, 
    2.9191198961157077, 
    3.6120440358146144, 
    5.938650489751697, 
    3.5529228291672803, 
    4.252974513646061, 
    0.07321558074413037, 
    4.606624069065216, 
    1.1381464803518826, 
    0.6300786117796688, 
    6.265717644929687, 
    0.8430061348604436, 
    3.1486688816017, 
    2.187113308963469, 
    2.55904691435907, 
    0.9064953139550527, 
    5.37692637169442, 
    5.853139585850388, 
    1.575131271143313, 
    2.952143357394929, 
    5.66264462166993, 
    1.6711595897952538, 
    0.5699793026370283, 
    2.3172163459061377, 
    1.7397750472133227, 
    0.40865290826939393, 
    1.9322709447427506, 
    3.7488814181123082, 
    0.999910036276269, 
    2.202464383904762, 
    6.225511119055286, 
    0.4607922129972568, 
    0.18347089591674237, 
    0.7998209062335242, 
    5.0746310546021345, 
    1.75341780686486
    };

        


    }

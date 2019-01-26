// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

import javax.sound.sampled.*;
import java.util.*;
import flow.modules.*;
import flow.utilities.*;

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
    public static final int MAX_VOICES = 16;
    static int numVoices = -1;
    
    public static int getNumVoices() { return numVoices; }

    /** The default number of voices allocated to a single thread, by default 1.
        If we have lots of overhead in context switching, then we might want to set this higher,
        perhaps even to numVoices,  but having it be 1 would ideally allow us to distribute each 
        voice thread to a separate CPU -- if we have very costly voice threads, then this might be
        useful. 
    */
    public static final int DEFAULT_NUM_VOICES_PER_THREAD = 1;
    static int numVoicesPerThread = -1;

    public static final double DEFAULT_VOLUME_MULTIPLIER = 2000;

    // If a partial's volume is very low, we don't even bother computing its sample contribution, but just set it to zero.
    static final double MINIMUM_VOLUME_SQUARED = 0; //0.0001 * 0.0001;

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
        bufferSize = Prefs.getLastBufferSize();
        //skip = Prefs.getLastSkip();   
    	}
    	
    public Output()
        {
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
        input.setupMIDI();   // first device
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
       You can get a new, more or less statistically  independent generator from this method.
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
        int n = 0;
        lock();
        try
        	{
	        n = numSounds;
	        }
	    finally
	    	{
	        unlock();
	        }
        return n;
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
        int count;
        int reads;
        double[][] amplitudes;
        double[][] frequencies;
        byte[][] orders;
        double[] pitches;
        double[] velocities;
        float reverbWet = 0.5f;
        float reverbRoomSize = 0.5f;
        float reverbDamp = 0.5f;
        //float c = 0.0f;
        //float r = 0.0f;
              
        public Swap()
            {
            count = 1;
            reads = 1;
            amplitudes = new double[numVoices][Unit.NUM_PARTIALS];
            frequencies = new double[numVoices][Unit.NUM_PARTIALS];
            orders = new byte[numVoices][Unit.NUM_PARTIALS];
            pitches = new double[numVoices];
            velocities = new double[numVoices];
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
//        totalOutputTicks++;
        if (emitsReady)
            {
            Swap temp = swap;
            swap = with;
            with = temp;
            swap.reads++;
            emitsReady = false;
            }
        else
            {
            Thread.currentThread().yield();
//            totalOutputWaits++;
            }
        }
        
        
    float[][] freeverbInput = new float[2][1];
    float[][] freeverbOutput = new float[2][1];
    FreeVerb freeverb = new FreeVerb();

    /// Current positions of the sine wave functions (from 0 ... 2PI)
    double[][] positions;

    // 0.05 is about 3 32-sample periods before we get to near to 100%
    static final double PARTIALS_INTERPOLATION_ALPHA = 0.05;
    
    /** A value that's significantly higher than IEEE 754 subnormals, used by Output.java and Smooth.java
        to make sure they're not dropping into subnormal math. */
    public static final double WELL_ABOVE_SUBNORMALS = 1.0e-200;
    
    // Builds a single sample from the partials.  ALPHA is the current interpolation
    // factor (from 0...1) 
    double buildSample(int s, double[][] currentAmplitudes/*, double[][] currentFrequencies*/)
        {
        // build the sample
        double sample = 0;
        Swap _with = with;
        double[] amp = _with.amplitudes[s];
        double[] freq = _with.frequencies[s];
        byte[] orders = _with.orders[s];
        double[] pos = positions[s];
        double[] ca = currentAmplitudes[s];
        //double[] cf = currentFrequencies[s];
        double v = _with.velocities[s];
        double pitch = _with.pitches[s];
        double tr = pitch * Math.PI * 2 * INV_SAMPLING_RATE;
            
        for (int i = 0; i < pos.length; i++)
            {
            double amplitude = amp[i];
            int oi = orders[i];
            if (oi < 0) oi += 256;          // if we're using 256 partials, they need to be all positive
                
            // This was a difficult bug to nail down.  Because we're using our (1-alpha) trick, if we
            // slowly drop to zero, we'll find our way into subnormals, and it appears that subnormals are handled
            // by Java in *software*, resulting in a radical slowdown in this region.  Subnormals show up around
            // e^-308, so here if both ca[i] or what we're dropping to is even close to subnormals,
            // we just shut ca[0] straight to 0 to skip the whole subnormal range.
            double aa = ca[oi] * (1.0 - PARTIALS_INTERPOLATION_ALPHA);
            double bb = amplitude * PARTIALS_INTERPOLATION_ALPHA;
            if (aa > WELL_ABOVE_SUBNORMALS || bb > WELL_ABOVE_SUBNORMALS)
                ca[oi] = aa + bb;
            else //if (ca[oi] != 0)
                {
                ca[oi] = 0;
                }
            amplitude = ca[oi];
            /// End Difficult Bug
                                
            if (amplitude * amplitude > MINIMUM_VOLUME_SQUARED)
                {
                double frequency = freq[i];
                double absoluteFrequency = frequency * pitch;
                                        
                if (absoluteFrequency > SAMPLING_RATE / 2.0)  // beyond Nyquist.  We may assume that ALL later frequencies are also beyond Nyquist since we're sorted.
                    {
                    break;          // continue;
                    }
                
                if (absoluteFrequency <= 0.0)  // don't bother, though this shouldn't happen (well, it might be 0.0)
                    {
                    continue;
                    }
                
                /*
                double freqtr = frequency * tr;
                if (cf[oi] < 0)  // uninitialized
                	{
                	cf[oi] = freqtr;
                	}
            	else
            		{
            		double faa = cf[oi] * (1.0 - (PARTIALS_INTERPOLATION_ALPHA));
	 	           	double fbb = freqtr * PARTIALS_INTERPOLATION_ALPHA;
	                cf[oi] = faa + fbb;
                	}
            	freqtr = cf[oi];
                double position = pos[oi] + freqtr;
				*/
				
                double position = pos[oi] + frequency * tr;
                if (position >= Math.PI * 2)
                    {
                    position = position - (Math.PI * 2);
                            
                    if (position >= Math.PI * 2)
                        {
                        position = position % (Math.PI * 2);
                        }
                    }
                pos[oi] = position;

                double smp = Utility.fastSin(position) * amplitude * v;
                sample += smp;
                }
            }
        return sample;
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



    public static final int NUM_OUTPUTS_PER_THREAD = 2;

    // long lastTimeFoo = 0;
    // int timeCountFoo = 0;
    
    /*
    double filter = 0;
    double filter2 = 0;
    double filter3 = 0;
    double filter4 = 0;
    */
    
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
                /*
                final double[][] currentFrequencies = new double[numVoices][Unit.NUM_PARTIALS];
                for(int ii = 0; ii < currentFrequencies.length; ii++)
                	for(int jj = 0; jj < currentFrequencies[ii].length; jj++)
                		currentFrequencies[ii][jj] = -1;
                */
  
                lightweightOutputSemaphores = new boolean[MAX_VOICES];
                outputLocks = new Object[MAX_VOICES];
                for (int i = 0; i < MAX_VOICES; i++) 
                    {
                    outputLocks[i] = new Object[0];
                    lightweightOutputSemaphores[i] = true;
                    }

                for(int i = 0; i < MAX_VOICES; i += NUM_OUTPUTS_PER_THREAD)
                    {
                    final int _i = i;
                    Thread thread = new Thread(new Runnable()
                        {
                        public void run()
                            {
                            while(true) 
                                {
                                blockOutputUntil(_i, true); 
                                                
                                for(int j = _i; j < Math.min(MAX_VOICES, _i + NUM_OUTPUTS_PER_THREAD); j++)
                                    {
                                    if (j < samples.length)         // voice hasn't been loaded yet, hang tight
                                        {
                                        double[] samplessnd = samples[j];
                                        for (int skipPos = 0; skipPos < SKIP; skipPos++)
                                            {
                                            samplessnd[skipPos] = buildSample(j, currentAmplitudes/*, currentFrequencies*/) * DEFAULT_VOLUME_MULTIPLIER;
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
                        
                    if (samples.length != getNumSounds())
                        samples = new double[getNumSounds()][SKIP];

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
                            samplessnd[skipPos] = buildSample(solo, currentAmplitudes/*, currentFrequencies*/) * DEFAULT_VOLUME_MULTIPLIER;
                            }
                        }
                    else
                        {
                        // Fire up output threads
                        for(int snd = 0; snd < numSounds; snd += NUM_OUTPUTS_PER_THREAD)
                            {
                            signalOutput(snd, true);
                            }
                        for(int snd = 0; snd < numSounds; snd += NUM_OUTPUTS_PER_THREAD)
                            {
                            blockOutputUntil(snd, false);
                            }
                        }
                                              
                    float wet = with.reverbWet;  
                    freeverb.setWet(wet);
                    freeverb.setRoomSize(with.reverbRoomSize);
                    freeverb.setDamp(with.reverbDamp);

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
                                                            
                        if (d > 32767)
                            {
                            d = 32767;
                            clipped = true;
                            }
                        if (d < -32768)
                            {
                            d = -32768;
                            clipped = true;
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
                            
                        /*
                        filter = (1 - 0.5) * filter + 0.5 * d;
                        d = filter;
                        filter2 = (1 - 0.5) * filter2 + 0.5 * d;
                        d = filter2;
                        filter3 = (1 - 0.5) * filter3 + 0.5 * d;
                        d = filter3;
                        filter4 = (1 - 0.5) * filter4 + 0.5 * d;
                        d = filter4;
                    	*/

//                        d = lowPassFilter(d, with.c, with.r);
                        
                        int val = (int)(d);
                        audioBuffer[skipPos * 2 + 1] = (byte)((val >> 8) & 255);
                        audioBuffer[skipPos * 2] = (byte)(val & 255);
                        tick++;                                 /// See documentation elsewhere about threadsafe nature of tick
                        
                        //  timeCountFoo++;
                        //  if (timeCountFoo > 44100)
                        //  {
                        //  long vv = System.currentTimeMillis();
                        //  //System.err.println("-> " + (vv - lastTimeFoo));
                        //  lastTimeFoo = vv;
                        //  timeCountFoo = 0;
                        //  }
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
                            if (voice > numSounds) 
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
    
    void blockVoiceUntil(int voice, boolean val)
        {
        synchronized(voiceLocks[voice])
            {
            while(lightweightSemaphores[voice] != val)
                {
                try { voiceLocks[voice].wait(); } catch (Exception e) { }
                }
            }
        }
                
    void signalVoice(int voice, boolean val)
        {
        synchronized(voiceLocks[voice])
            {
            lightweightSemaphores[voice] = val;
            voiceLocks[voice].notify();
            }
        }



//    int totalPartialsTicks;
//    int totalPartialsWaits;
//	int totalOutputTicks;
// 	int totalOutputWaits;
//    static final int NUM_TICKS_PER_PRINT = 1024 * 5;
//    int avgTimeTick = 0;
//    long lastTime = -1;
    
    double[] zeroAmplitudes = new double[Unit.NUM_PARTIALS];
    double[] zeroFrequencies = new double[Unit.NUM_PARTIALS];
    byte[] standardOrders = new byte[Unit.NUM_PARTIALS];
    
    // int timeCountFoo2 = 0;
    // long lastTimeFoo2 = 0;
    
    /** Called to pulse the Output.  This will cause the Output to wait until the user is no longer
        modifying modules via the GUI, then have all the Sounds produce new partials by calling
        go() on them.  The resulting partials will then be loaded for the Output Thread to 
        convert into sound.   This method should be called in a while-loop from your main thread
        or via calling startPrimaryVoiceThread(). */ 
    public void go()
        {
        /*
          timeCountFoo2++;
          if (timeCountFoo2 > 44100 / 32)
          {
          long vv = System.currentTimeMillis();
          System.err.println("+> " + (vv - lastTimeFoo2));
          lastTimeFoo2 = vv;
          timeCountFoo2 = 0;
          }
        */

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
                int numThreads = numSounds / numVoicesPerThread;
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
//        totalPartialsTicks++;
        if (emitsReady)
            {
//            totalPartialsWaits++;
            Thread.currentThread().yield();
            }
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
                    System.arraycopy(emits.amplitudes[0], 0, swap.amplitudes[i], 0, emits.amplitudes[0].length); 
                    System.arraycopy(emits.frequencies[0], 0, swap.frequencies[i], 0, emits.frequencies[0].length);
                    System.arraycopy(emits.orders[0], 0, swap.orders[i], 0, emits.orders[0].length);
                    }
                else
                    {
                    System.arraycopy(zeroAmplitudes, 0, swap.amplitudes[i], 0, zeroAmplitudes.length); 
                    System.arraycopy(zeroFrequencies, 0, swap.frequencies[i], 0, zeroFrequencies.length); 
                    System.arraycopy(standardOrders, 0, swap.orders[i], 0, standardOrders.length); 
                    }
                    
                swap.pitches[i] = sounds[i].getPitch();
                swap.velocities[i] = (velocitySensitive ? sounds[i].getVelocity() : Sound.DEFAULT_VELOCITY);
                }
                
            if (e instanceof Out) 	// we're only doing this for ONE sound, namely sounds[0]
            	{
            	Out out = (Out)e;
            	swap.reverbWet = (float)(out.modulate(out.MOD_REVERB_WET));
            	swap.reverbDamp = (float)(out.modulate(out.MOD_REVERB_DAMP));
            	swap.reverbRoomSize = (float)(out.modulate(out.MOD_REVERB_ROOM_SIZE));
            	//swap.c = (float)(out.modulate(out.MOD_REVERB_ROOM_SIZE + 1));
            	//swap.r = (float)(out.modulate(out.MOD_REVERB_ROOM_SIZE + 2));
            	}
            }
        finally 
            {
            unlock();
            }
            
        swap.count++;
        emitsReady = true;
/*
        avgTimeTick++;
        if (avgTimeTick == NUM_TICKS_PER_PRINT)
            {
            avgTimeTick = 0;
            if (lastTime == -1)
                lastTime = System.currentTimeMillis();
            else
                {
                long time = System.currentTimeMillis();
                System.err.println("H% " + String.format("%.2f", (totalPartialsWaits / (double)totalPartialsTicks)) +
                    ": " + String.format("%.2f", (swap.reads / (double)swap.count)) + " " + swap.reads + " " + swap.count +
                    " O% " + String.format("%.2f", (totalOutputWaits / (double)totalOutputTicks)) + 
                    " Wait " + String.format("%.2f", (time - lastTime) / (double)NUM_TICKS_PER_PRINT));
                lastTime = time;
                }
            } 
*/
        }  
        /*
	static final double T = 1.0 / 44100.0;

	static final double Q = Math.sqrt(0.5);
	static final double CUTOFF = 1000.0;
	static final double O = CUTOFF * 2 * Math.PI; //O = 2.0 / T * Math.tan(CUTOFF * Math.PI);
	static final double OOQTT = O * O * Q * T * T;
	static final double J = 4.0 * Q + 2.0 * O * T + OOQTT;
	static final double IJ = 1.0 / J;
	static final double b0 = IJ * OOQTT;
	static final double b1 = IJ * 2 * OOQTT;
	static final double b2 = IJ * OOQTT;
	static final double a1 = IJ * (-8 * Q + 2 * OOQTT);
	static final double a2 = IJ * (4 * Q - 2 * O * T + OOQTT);

    double[] N = new double[2];
    double[] M = new double[2];
    double lowPassFilter(double d, double cut, double res)
    	{
     double Q = (res * 10 + 1) * 0.7071;
	 final double CUTOFF = cut * 20000 + 25;
	 final double O = CUTOFF * 2 * Math.PI;
	 final double OOQTT = O * O * Q * T * T;
	 final double J = 4.0 * Q + 2.0 * O * T + OOQTT;
	 final double IJ = 1.0 / (J < 0.001 ? 0.001 : J ;
	 final double b0 = IJ * OOQTT;
	 final double b1 = IJ * 2 * OOQTT;
	 final double b2 = IJ * OOQTT;
	 final double a1 = IJ * (-8 * Q + 2 * OOQTT);
	 final double a2 = IJ * (4 * Q - 2 * O * T + OOQTT);

    	double y = d * b0 + N[0] * b1 + N[1] * b2 - a1 * M[0] - a2 * M[1];
    	N[1] = N[0];
    	N[0] = d;
    	M[1] = M[0];
    	M[0] = y;
    	if (y < WELL_ABOVE_SUBNORMALS && y > 0) System.err.println("subnormal");
    	if (y != y) System.err.println("NAN");
    	return y;
    	}    
     */
        
    }

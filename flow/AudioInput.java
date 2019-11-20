package flow;

import javax.sound.sampled.*;
import java.util.*;
import java.util.zip.*;
import flow.modules.*;
import flow.utilities.*;
import org.json.*;
import java.io.*;

/**
	AudioInput is the facility for sampling sounds from a microphone or other input source.
	It is currently only used by the AudioIn module, and so is only capable of doing a short-time
	FFT on the samples (making a robot effect).  Because it's only used by AudioIn, AudioInput
	is designed to pause itself otherwise so to save computational power.
*/

public class AudioInput
    {
    Mixer.Info mixer;
    TargetDataLine tdl;
    Thread audioInputThread = null;
    Object lock = new Object[0];
    boolean running = false;
    double[] amplitudes;
    Output output;

	/** Creates an AudioInput, initially in STOPPED state. */
	public AudioInput(Output output)
		{
		this.output = output;

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


		audioInputThread = new Thread(new Runnable()
			{
			public void run()
				{
				while(true)
					{
					synchronized(lock)
						{
						if (tdl == null || !running)
							{
							try
								{
								lock.wait();
								}
							catch (InterruptedException ex) { } // never happens
							}
						else
							{
							if (amplitudes == null || amplitudes.length != Unit.NUM_PARTIALS)
								{
								amplitudes = new double[Unit.NUM_PARTIALS];
								}
							}
						}
					// process will have is own internal lock
					if (amplitudes != null)
						process();
					}
				}
			});
		audioInputThread.setDaemon(true);
		audioInputThread.start();
		}
    
    
    // These are copies of Wavetable's constants.  Maybe we might want to tweak them.
    
     static final int RESAMPLING = 4;
     static final int WAVETABLE_SIZE = 256;
     static final double MINIMUM_AMPLITUDE = 0.001;
	int sampleSize = WAVETABLE_SIZE * RESAMPLING;
	double[] b = new double[sampleSize];
	double[] buffer = new double[WAVETABLE_SIZE];
	byte[] sampleBuffer = new byte[WAVETABLE_SIZE * 2];
	
	void process()
		{
		if (tdl.read(sampleBuffer, 0, sampleBuffer.length) == sampleBuffer.length)
			{
			// Read frames into buffer
			for(int i = 0, j = 0; i < sampleBuffer.length; i+=2, j++)
				{
				// I am converting to a short to make sure it converts to an int in signed form (negatives get sign-extended).  Is this right?
				int sample = (sampleBuffer[i] & 255) | (sampleBuffer[i + 1] << 8);
				buffer[j] = sample / 32768.0;
				}
		
			System.arraycopy(b, WAVETABLE_SIZE, b, 0, sampleSize - WAVETABLE_SIZE);
			System.arraycopy(buffer, 0, b, sampleSize - WAVETABLE_SIZE, WAVETABLE_SIZE);
			//System.arraycopy(b, 0, a, 0, sampleSize);		// maybe a is unnecessarty as applyHanningWindow already clones
	  
			// is Hanning COLA?     
			double[] a = FFT.applyHanningWindow(b);
			// we need options here for not allocating buffers over and over again
			double[] harmonics = FFT.getHarmonics(a);


			synchronized(lock)
				{
				if (running)
					{
					// is this the right size?
					for (int s=1 ; s < harmonics.length / 2 / RESAMPLING + 1; s++)
						{
						if ( s > amplitudes.length)  // note >
							{
							break;
							}
						amplitudes[s - 1] = (harmonics[s * RESAMPLING - 1] >= MINIMUM_AMPLITUDE ? harmonics[s * RESAMPLING - 1]  : 0 );
						}
					}
				}
			}
   		}
    
    double[] zeroAmplitudes;
    /** Returns the latest amplitudes of harmonics sampled from the audio input source.  If the facility
    	is currently stopped, then start() is called.  If there
     	are no amplitudes yet (perhaps because AudioInput is warming up), then zeros will be returned.  */
    public void getAmplitudes(double[] putHere)
    	{
    	synchronized(lock)
    		{
    		start();
    		
    		if (!running || amplitudes == null)
    			{
    			if (zeroAmplitudes == null || zeroAmplitudes.length != Unit.NUM_PARTIALS)
    				{
    				zeroAmplitudes = new double[Unit.NUM_PARTIALS];
    				}
    			System.arraycopy(zeroAmplitudes, 0, putHere, 0, putHere.length);
    			}
    		else
    			{
    			System.arraycopy(amplitudes, 0, putHere, 0, putHere.length);
	    		}
    		}
    	}
    
    /** Starts (or restarts) the audio facility. */
	public void start()
		{
		synchronized(lock)
			{
			if (tdl == null)
				{
				System.err.println("AudioInput.start() WARNING: Cannot Start: No Audio Input.");
				}
			else if (running)
				{
				// no need
				}
			else
				{
				// clear buffers
				b = new double[sampleSize];
				//a = new double[sampleSize];
				tdl.flush();  // clear any existing buffer gunk.  Dunno if this is necessary.
				tdl.start();
				running = true;
				lock.notify();
				}
			}
		}
		
    /** Stops (pauses) the AudioInput facility, reducing CPU load. */
	public void stop()
		{
		synchronized(lock)
			{
			running = false;
			}
		}
		
    /** Sets the currently used Mixer for the AudioInput. */
    public void setMixer(Mixer.Info mixer)
        {
        synchronized(lock)
        	{
			try
				{
				if (tdl != null)
					{
					tdl.flush();		// clear any existing buffer gunk.  Dunno if this is necessary.
					tdl.stop();
					}
				if (mixer == null)
					{
					Mixer.Info[] m = getSupportedMixers();
					if (m.length > 0)
						mixer = m[0];
					}
				if (mixer == null)
					tdl = AudioSystem.getTargetDataLine( output.audioFormat );
				else
					tdl = AudioSystem.getTargetDataLine( output.audioFormat, mixer );
				tdl.open(output.audioFormat, output.bufferSize);			// is this wise? Should we do something smaller?
				this.mixer = mixer;
				}
			catch (LineUnavailableException ex) { throw new RuntimeException(ex); }
			}
		}


    /** Returns the currently used Mixer for the AudioInput */
    public Mixer.Info getMixer()
        {
        return mixer;
        }
                

    /** Returns the available mixers which support the given audio format.  The audio format
    	is the same as the one used by Output.  */
    public Mixer.Info[] getSupportedMixers()
        {
        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, output.audioFormat);
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


    }
package flow.modules;
import flow.utilities.*;
import java.io.*;

public class ConvertWav
    {
    public static final int MAXIMUM_SAMPLES = 4096;
    public static final int WINDOW_SIZE = 65;
    public static final double MINIMUM_AMPLITUDE = 0.001;
        
    public static void main(String[] args) throws Exception
        {

        double[] waves = null;
        double[] buffer = new double[256];
        int count = 0;

        File file = new File(args[0]);
        WavFile wavFile = null;
        double[] _waves = new double[MAXIMUM_SAMPLES];
        wavFile = WavFile.openWavFile(file);
                
        while(true)
            {
            // Read frames into buffer
            int framesRead = wavFile.readFrames(buffer, buffer.length);
            if (count + framesRead > MAXIMUM_SAMPLES)
                {
                System.err.println("File Too Large");
                System.exit(1);
                }
            System.arraycopy(buffer, 0, _waves, count, framesRead);
            count += framesRead;
            if (framesRead < buffer.length) 
                break;
            }
        waves = new double[count];
        System.arraycopy(_waves, 0, waves, 0, count);
        wavFile.close();

        int desiredSampleSize = 512;                          // because we have up to 256 samples
        int currentSampleSize = waves.length;
                                        
        /// Resample to Flow's sampling rate
        double[] newvals = WindowedSinc.interpolate(
            waves,
            currentSampleSize,
            desiredSampleSize,              // notice desired and current are swapped -- because these are SIZES, not RATES
            WINDOW_SIZE,
            true);           
                
        // Note no window.  Should still be okay (I think?)
        double[] harmonics = FFT.getHarmonics(newvals);
        double[] finished = new double[harmonics.length / 2];           // must be 256
        for (int s=1 ; s < harmonics.length / 2; s++)                   // we skip the DC offset (0) and set the Nyquist frequency bin (harmonics.length / 2) to 0
            {
            finished[s - 1] = (harmonics[s] >= MINIMUM_AMPLITUDE ? harmonics[s]  : 0 );
            }

        double max = 0;
        for(int i = 0; i < finished.length; i++)
            {
            if (max < finished[i])
                max = finished[i];
            }
                        
        if (max > 0)
            {
            for(int i = 0; i < finished.length; i++)
                {
                finished[i] /= max;
                }
            }
        String name = file.getName();
        name = name.substring(0, name.length() - 4);  // .wav
        System.out.println(name);
        for(int i = 0; i < finished.length; i++)
            {
            int val = (int)(finished[i] * (999 + 10) - 10);
            if (val < 0) val = 0;
            System.out.print("" + val + " ");
            }
        System.out.println();
        }
    }

// Copyright 2020 by George Mason University
// Licensed under the Apache 2.0 License


import flow.*;
import flow.modules.*;
import flow.gui.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import org.json.*;
import flow.utilities.*;


public class ConvertWave
    {
    public static final int MAXIMUM_SAMPLES = 2048;
    public static final int WINDOW_SIZE = 65;
    public static final double MINIMUM_AMPLITUDE = 0.001;

    public static void main(String[] args)
        {
        File file = new File(args[0]);
        
        double[] waves = null;
        double[] buffer = new double[256];
        int count = 0;
        
        WavFile wavFile = null;
        try 
            {
            double[] _waves = new double[MAXIMUM_SAMPLES];
            wavFile = WavFile.openWavFile(file);
                
            while(true)
                {
                // Read frames into buffer
                int framesRead = wavFile.readFrames(buffer, buffer.length);
                if (count + framesRead > MAXIMUM_SAMPLES)
                    {
                    System.err.println("This file may contain no more than " + MAXIMUM_SAMPLES + " samples.");
                    return;
                    }
                System.arraycopy(buffer, 0, _waves, count, framesRead);
                count += framesRead;
                if (framesRead < buffer.length) 
                    break;
                }
            waves = new double[count];
            System.arraycopy(_waves, 0, waves, 0, count);
            }
        catch (IOException ex)
            {
            System.err.println("An error occurred on reading the file.");
            return;
            }
        catch (WavFileException ex)
            {
            System.err.println("WAV files must be mono 16-bit.");
            return;
            }

        try
            {
            wavFile.close();
            }
        catch (Exception ex) { }
        
        int desiredSampleSize = Unit.NUM_PARTIALS * 2;                          // because we have up to 256 samples
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
        for(int i = 0; i < finished.length; i++)
            {
            System.out.print("" + String.format("%1.4f", finished[i]) + " ");
            }                                        
        System.out.println();    
        }

    }

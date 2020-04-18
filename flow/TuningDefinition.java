package flow;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;

public abstract class TuningDefinition {
	int bases[] = new int[128];
	int detunes[] = new int[128];
    double freqs[] = new double[128];
	int rootMIDINote = 0;
	double rootFrequency = 1;
    boolean configured;

    public void setConfigured(boolean val) {
        configured = val;
    }

	public boolean isConfigured() {
		return configured;
	}

    public int[] getBases() {
	    return bases;
    }

	public int[] getDetunes() {
        return detunes;
    }

	public int getRootMIDINote() {
        return rootMIDINote;
    }

	public void setRootMIDINote(int val) {
        rootMIDINote = val;
    }

	public double getRootFrequency() {
        return rootFrequency;
    }

	public void setRootFrequency(double val) {
        rootFrequency = val;
    }

	static double LOG_2 = Math.log(2);
    static double INV_LOG_2 = 1 / LOG_2;
    static int TWO_TO_THE_14 = 16384; // (int) Math.pow(2, 14);

    public static double midiNumberToHz(int m) {
        return Math.pow(2, (m - 69) / 12.0) * 440.0;
    }

	public static double hzToMidiNumber(double hz) {
        return Math.log(hz / 440) * INV_LOG_2 * 12 + 69;
    }

	public static double centsAbove(double f2, double f1) {
        return 1200 * Math.log(f2 / f1) * INV_LOG_2;
    }

	public static int centsToTicks(double c) {
        return (int) ((c * TWO_TO_THE_14 / 100.0) + 0.5);
    }

	public void setNoteFrequency(int note_index, double freq) {
        int ind = (int) Math.floor(hzToMidiNumber(freq));
        double base = midiNumberToHz(ind);
        double cents = centsAbove(freq, base);
        int ticks = centsToTicks(cents);
        if (ticks == TWO_TO_THE_14) {
            ticks = 0;
            ind++;
        }
        bases[note_index] = ind;
        detunes[note_index] = ticks;
        freqs[note_index] = freq;
    }

	public abstract void popup();

	public abstract String getMenuName();

    JTextField rootMIDINoteF = null;
    JTextField rootFrequencyF = null;
    public static final int DEFAULT_ROOT_MIDI_NOTE = 69;
    public static final double DEFAULT_ROOT_FREQUENCY = 440.0;

    public JComponent getRootMIDINoteComponent() {
        String name = "microtuning";
        if (rootMIDINoteF == null)
            rootMIDINoteF = new JTextField("" + Prefs.getLastXAsInt("rootMIDINote", name, DEFAULT_ROOT_MIDI_NOTE));
        return rootMIDINoteF;
    }

	public JComponent getRootFrequencyComponent() {
		String name = "microtuning";
        getRootMIDINote(); // compute it
        rootFrequencyF = new JTextField("" + Prefs.getLastXAsDouble("rootFrequency", name, DEFAULT_ROOT_FREQUENCY));
        JButton compute = new JButton("Compute");
        compute.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int rmn = -1;
                try {
                    rmn = Integer.parseInt(rootMIDINoteF.getText());
                    if (rmn < 0 || rmn > 127)
                        throw new RuntimeException();
                } catch (Exception ex) {
                return;
                }
                rootFrequencyF.setText("" + TuningDefinition.midiNumberToHz(rmn));
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(rootFrequencyF, BorderLayout.CENTER);
        panel.add(compute, BorderLayout.EAST);
        return panel;
    }

	public int getRootMeanNoteValue() {
		String name = "microtuning";
        int rmn = -1;
        try {
            rmn = Integer.parseInt(rootMIDINoteF.getText());
            if (rmn < 0 || rmn > 127)
                throw new RuntimeException();
        } catch (Exception ex) {
            rootMIDINoteF.setText("" + Prefs.getLastXAsInt("rootMIDINote", name, DEFAULT_ROOT_MIDI_NOTE));
            return -1;
        }
        return rmn;
    }

	public double getRootFrequencyValue() {
		String name = "microtuning";
        double rf = -1;
        try {
            rf = Double.parseDouble(rootFrequencyF.getText());
            if (rf <= 0.0)
                throw new RuntimeException();
        } catch (Exception ex) {
            rootFrequencyF.setText("" + Prefs.getLastXAsDouble("rootFrequency", name, DEFAULT_ROOT_FREQUENCY));
            return -1;
        }
        return rf;
    }

	public void resetRootMIDINoteAndFrequency() {
		String name = "microtuning";
        Prefs.setLastX("" + DEFAULT_ROOT_MIDI_NOTE, "rootMIDINote", name);
        Prefs.setLastX("" + DEFAULT_ROOT_FREQUENCY, "rootFrequency", name);
        rootMIDINoteF.setText("" + DEFAULT_ROOT_MIDI_NOTE);
        rootFrequencyF.setText("" + DEFAULT_ROOT_FREQUENCY);
    }

	public void setRootMIDINoteAndFrequency( int midiNote, double frequency) {
		String name = "microtuning";
        Prefs.setLastX("" + midiNote, "rootMIDINote", name);
        Prefs.setLastX("" + frequency, "rootFrequency", name);
        // these are probably not necessary
        rootMIDINoteF.setText("" + midiNote);
        rootFrequencyF.setText("" + frequency);
    }

}

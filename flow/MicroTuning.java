package flow;

import java.io.*;
import java.util.*;

public class MicroTuning
    {
        double[] offsets;
        double equivalencyRatio;
        double freqs[] = new double[128];
        String name;
        boolean configured = false;

        public MicroTuning(Double[] offsets, String name) {

        this.offsets = new double[offsets.length - 1];
        for (int i = 0; i < offsets.length - 1; i++)
            {
            this.offsets[i] = offsets[i];
            }
        this.equivalencyRatio = frequencyAbove(offsets[offsets.length - 1], 1);
        this.name = name;
        }

        // (setf lsp-java-format-settings-url
        // "https://gist.githubusercontent.com/StEvUgnIn/2f89559b8aada966817fe95f48c54544/raw/dd11e6fb936dcc35d1590e6ee19fe439971ecd7d/eclipse-formatter.xml")
        // (setf lsp-java-format-settings-profile "Whitesmiths")
        public static MicroTuning fromScalaFile(File scalaFile,
                double rootFrequency, int rootNote) throws FileNotFoundException
            {
            Scanner scan = new Scanner(scalaFile);
            boolean scanned_count_line = false;
            boolean scanned_name_line = false;
            int count = 0;
            ArrayList<Double> cents = new ArrayList<Double>();
            while (scan.hasNextLine())
                {
                if (scanned_count_line && cents.size() >= count)
                    {
                    break;
                    }
                String line = scan.nextLine();
                String fixed = line.trim();
                if (fixed.equals(""))
                    {
                    continue;
                    }
                if (fixed.startsWith("!"))
                    {
                    continue;
                    }
                if (!scanned_name_line)
                    {
                    scanned_name_line = true;
                    continue;
                    }
                if (!scanned_count_line)
                    {
                    String[] tokens = fixed.split("\\s+");
                    if (!isInt(tokens[0]))
                        {
                        return null;
                        }
                    count = Integer.parseInt(tokens[0]);
                    scanned_count_line = true;
                    continue;

                    }
                String[] tokens = fixed.split("\\s+");
                if (tokens[0].contains(".")) // we've got a cent number
                    {
                    cents.add(Double.parseDouble(tokens[0]));
                    }
                else if (tokens[0].startsWith("-"))
                    {
                    return null;
                    }
                else
                    {
                    String[] ratios = tokens[0].split("/");
                    if (ratios.length > 2)
                        {
                        return null;
                        }
                    double numerator, denominator;
                    numerator = Double.parseDouble(ratios[0]);
                    if (ratios.length == 1)
                        {
                        denominator = 1;
                        }
                    else
                        {
                        denominator = Double.parseDouble(ratios[1]);
                        }
                    double cent = Math.log(numerator / denominator)
                            / Math.log(2) * 1200;
                    cents.add(cent);

                    }
                }
            cents.add(0, 0.0); // I expect a 0 at the beginning of my array.
            Double[] outarr = cents.toArray(new Double[0]);
            MicroTuning rs = new MicroTuning(outarr, "tuning");
            rs.realize(rootNote, rootFrequency);
            return rs;

            }

        static boolean isInt(String str)
            {
            Scanner sc = new Scanner(str.trim());
            if (!sc.hasNextInt())
                return false;
            sc.nextInt();
            return !sc.hasNext();
            }

        double frequencyAbove(double c2, double f1)
            {
            return f1 * Math.pow(2, c2 / 1200);
            }

        double getFrequency(int midiNum, int rootMIDINote, double rootFrequency)
            {
            if (midiNum == rootMIDINote)
                {
                return frequencyAbove(offsets[0], rootFrequency);
                }
            if (midiNum < rootMIDINote)
                {
                int diff = rootMIDINote - midiNum;
                int idx = offsets.length - (diff % offsets.length);
                if (idx == offsets.length)
                    {
                    idx = 0;
                    }
                int rootsBelow =
                        (int) Math.ceil((double) diff / offsets.length);
                double baseFreq =
                        rootFrequency / Math.pow(equivalencyRatio, rootsBelow);
                return frequencyAbove(offsets[idx], baseFreq);
                }
            if (midiNum > rootMIDINote)
                {
                int diff = midiNum - rootMIDINote;
                int idx = diff % offsets.length;
                int rootsAbove = diff / offsets.length;
                double baseFreq =
                        rootFrequency * Math.pow(equivalencyRatio, rootsAbove);
                return frequencyAbove(offsets[idx], baseFreq);
                }
            return -1;
            }

        double getLerpRatio(int midiNum1, int midiNum2, double alpha)
            {
            if (!configured)
                {
                return 0.0;
                }
            double f1 = freqs[midiNum1];
            double f2 = freqs[midiNum2];
            double r_above = Math.log(f2 / f1) / Math.log(2);
            double corrected_r = r_above * alpha;
            return Math.pow(2, corrected_r);
            }

        void realize(int rootMIDINote, double rootFrequency)
            {
            for (int i = 0; i < 128; i++)
                {

                freqs[i] = getFrequency(i, rootMIDINote, rootFrequency);
                }
            configured = true;
            }

    }

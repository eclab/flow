// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

/** 
    Module storage and reference. 
*/

public class Modules
    {
    public static final Class[] getModules()
        {
        String modName = System.getProperty("module", null);
        if (modName == null) return modules;
        else
            {
            try
                {
                Class c = Class.forName(modName);
                Class[] m = new Class[modules.length + 1];
                System.arraycopy(modules, 0, m, 0, modules.length);
                m[m.length - 1] = c;
                return m;
                }
            catch (Exception ex)
                {
                return modules;
                }
            }
        }
                
    // A list of all the modules in the system which can appear in the
    // Modules menu.
    static final Class[] modules = new Class[]
    {
    flow.modules.AHR.class,
    flow.modules.All.class,
    flow.modules.AmpMath.class,
    flow.modules.AudioIn.class,
    //flow.modules.Average.class,
    flow.modules.Buffer.class,
    flow.modules.Choice.class,
    flow.modules.Combine.class,
    flow.modules.Compress.class,
    flow.modules.Constraints.class,
    flow.modules.DADSR.class,
    flow.modules.Delay.class,
    flow.modules.Dilate.class,
    flow.modules.Draw.class,
    flow.modules.Drawbars.class,
    flow.modules.EitherOr.class,
    flow.modules.Envelope.class,
    flow.modules.Fatten.class,
    flow.modules.Fill.class,
    flow.modules.Filter.class,
    flow.modules.Fix.class,
    flow.modules.FlangeFilter.class,
    flow.modules.FormantFilter.class,
    flow.modules.Geiger.class,
    flow.modules.In.class,
    flow.modules.HarmonicLab.class,
    flow.modules.Harmonics.class,
    flow.modules.Jitter.class,
    flow.modules.KHarmonics.class,
    flow.modules.LFO.class,
    flow.modules.LinearFilter.class,
    flow.modules.Map.class,
    flow.modules.MIDIIn.class,
    flow.modules.Mix.class,
    flow.modules.ModMath.class,
    flow.modules.Morph.class,
    flow.modules.MPE.class,
    flow.modules.Normalize.class,
    flow.modules.Noise.class,
    flow.modules.Note.class,
    flow.modules.NRPN.class,
    flow.modules.Out.class,
    //flow.modules.PartialMod.class,
    flow.modules.PartialFilter.class,
    flow.modules.Partials.class,
    //flow.modules.Rand.class,
    flow.modules.Rectified.class,
    flow.modules.Rotate.class,
    flow.modules.SampleAndHold.class,
    flow.modules.Sawtooth.class,
    flow.modules.Scale.class,
    flow.modules.Seq.class,
    flow.modules.Shift.class,
    flow.modules.Sine.class,
    flow.modules.Skeletonize.class,
    // flow.modules.Skew.class,
    flow.modules.Smooth.class,
    flow.modules.Soften.class,
    flow.modules.Square.class,
    //flow.modules.Squish.class,
    //flow.modules.Stretch.class,
    flow.modules.Sub.class,
    //flow.modules.Swap.class,
    flow.modules.Switch.class,
    flow.modules.Tinkle.class,
    flow.modules.Triangle.class,
    flow.modules.User.class,
    flow.modules.VCA.class,
    flow.modules.Waves.class,
    flow.modules.WaveTable.class,
    };      
    }

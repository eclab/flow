// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import flow.gui.*;
import org.json.*;

/**  
     A special Unit which converts loaded patches into single modules to be used in higher-level
     macros.  Macro works in conjunction with the Units "In" and "Out" to route modulation
     and unit signals from the higher-level patch into the embedded lower-level patch and back
     out again.  Macro has four incoming modulations and four incoming units.  Signals
     connected to these incoming elements are routed out the equivalent elements in any "In"
     Unit inside the embedded patch.  Furthermore, Macro has four outgoing modulations and
     four outgoing units.  Signals attached to the equivalent elements in the last (rightmost)
     "Out" unit in the embedded patch will be routed out these to the higher level patch.
        
     <p>The "In" and "Out" units can have their unit and modulation connections named by the user.
     Macro respects these and names its equivalent elements in the same way.  Then name of the
     Macro, displayed in the title bar of its ModulePanel, will be the patch name.
*/
        
public class Macro extends Unit implements Cloneable
    {
    private static final long serialVersionUID = 1;

    public static final String PATCH_NAME_KEY = "Patch Name";

    Modulation[] modules = new Modulation[0];
    Out out;
    ArrayList<In> ins = new ArrayList<>();
    String patchName = "Untitled";
    
    boolean[] unitOuts = new boolean[4];
    boolean[] unitIns = new boolean[4];
    boolean[] modOuts = new boolean[4];
    boolean[] modIns = new boolean[4];

    public Object clone()
        {
        Macro obj = (Macro)(super.clone());
        obj.unitOuts = (boolean[])(obj.unitOuts.clone());
        obj.unitIns = (boolean[])(obj.unitIns.clone());
        obj.modOuts = (boolean[])(obj.modOuts.clone());
        obj.modIns = (boolean[])(obj.modIns.clone());
        obj.modules = (Modulation[])(obj.modules.clone());
        for(int i = 0; i < obj.modules.length; i++)
            obj.modules[i] = (Modulation)(obj.modules[i].clone());
                
        // Here we need to rewire the modules again
        // We first build a map of old modules to new ones
        HashMap <Modulation, Modulation> map = new HashMap<>();
        for(int i = 0; i < modules.length; i++)
            {
            map.put(modules[i], obj.modules[i]);
            }
        
        // Now build the modulation input and unit inputs, pointing to the new modules
        for(int i = 0; i < modules.length; i++)
            {
            for(int j = 0; j < modules[i].getNumModulations(); j++)
                {
                if (modules[i].getModulation(j) instanceof Constant)
                    {
                    obj.modules[i].setModulation((Modulation)(modules[i].getModulation(j).clone()), j, modules[i].getModulationIndex(j));
                    }
                else
                    {
                    obj.modules[i].setModulation(map.get(modules[i].getModulation(j)), j, modules[i].getModulationIndex(j));
                    }
                }
            if (modules[i] instanceof Unit)
                {
                for(int j = 0; j < ((Unit)modules[i]).getNumInputs(); j++)
                    {
                    if (((Unit)(modules[i])).getInput(j) instanceof Nil)
                        {
                        ((Unit)(obj.modules[i])).setInput(Unit.NIL, j, ((Unit)modules[i]).getInputIndex(j));
                        }
                    else
                        {
                        ((Unit)(obj.modules[i])).setInput(((Unit)(map.get(((Unit)modules[i]).getInput(j)))), j, ((Unit)modules[i]).getInputIndex(j));
                        }
                    }
                }
            }
        
        // We also need to identify the new "Out" and the ins again.
        // We do this by simply calling loadModules
        obj.ins = new ArrayList<In>();
        obj.out = null;
        obj.loadModules(obj.modules, obj.patchName);
    
    
        return obj;
        }

    public String getNameForModulation() { return patchName; }
    
    public void resetTrigger(int num)
        {
        if (out != null)
            out.resetTrigger(num);
        }
    
    public void setTrigger(int num, int val)
        {
        if (out != null)
            out.setTrigger(num, val);
        }
    
    public void updateTrigger(int num)
        {
        if (out != null)
            out.updateTrigger(num);
        }
    
    public int getTrigger(int num)
        {
        if (out != null)
            return out.getTrigger(num);
        else return NO_TRIGGER;
        }
    
    public boolean isTriggered(int num)
        {
        if (out != null)
            return out.isTriggered(num);
        else return false;
        }
    
    public void reset()
        {
        super.reset();
        for(int i = 0; i < modules.length; i++)
            modules[i].reset();
        }

    public void gate()
        {
        super.gate();
        for(int i = 0; i < modules.length; i++)
            modules[i].gate();
        }
 
    public void release()
        {
        super.release();
        for(int i = 0; i < modules.length; i++)
            modules[i].release();
        }
   
    public void go()
        {
        super.go();

        for(int i = 0; i < modules.length; i++)
            modules[i].go();
                
        if (out != null)
            {
            int len = out.getNumModulations() - 1;  // skip gain
            for(int i = 0; i < len; i++)
                {
                setModulationOutput(i, out.modulate(i));
                }
                        
            len = out.getNumInputs();
            for(int i = 0; i < len; i++)
                {
                double[] amplitudes = getAmplitudes(i);
                double[] frequencies = getFrequencies(i);
                byte[] orders = getOrders(i);
                        
                System.arraycopy(out.getFrequenciesIn(i), 0, frequencies, 0, frequencies.length);
                System.arraycopy(out.getOrdersIn(i), 0, orders, 0, orders.length);
                System.arraycopy(out.getAmplitudesIn(i), 0, amplitudes, 0, amplitudes.length);
                // we handle the gain here, since we are extracting data directly from out. 
                double gain = out.getGain();
                if (gain != 1.0)
                    for(int q = 0; q < amplitudes.length; q++)
                        amplitudes[q] *= gain;
                }
            }
        }
   
    public void setSound(Sound sound)
        {
        super.setSound(sound); 
        for(int i = 0; i < modules.length; i++)
            modules[i].setSound(sound);
        }

    /** This is called to build a Macro for the express purpose
        of serializing it out and then forgetting about it. */
    public Macro(Modulation[] modules)
        {
        super(null);    // mod hasn't been set yet
        this.modules = modules;
        }
    
    /** This is called to build a Macro and loading the sound. */
    public Macro(Sound sound, Modulation[] modules, String patchName)
        {
        super(sound);  // modules hasn't been set yet
        loadModules(modules, patchName);
        setSound(sound);  // distribute to modules
        }
                
    public void loadModules(Modulation[] modules, String patchName)
        {
        this.modules = modules;  // now it's set
        this.patchName  = patchName;
        // find the last Out
        for(int m = modules.length - 1; m >= 0; m--)
            {
            if (modules[m] instanceof Out)
                {
                // set the output
                out = (Out)(modules[m]);
                out.setMacro(this);
                
                // identify which outputs are being used so we can reduce them
                // to make the macro prettier
                for(int i = 0; i < 4; i++)
                    {
                    unitOuts[i] = !out.isInputNil(i);
                    modOuts[i] = !out.isModulationConstant(i);
                    }
                break;
                }
            }
                
        // find all the Ins, including first In
        for(int m = 0; m < modules.length; m++)
            {
            if (modules[m] instanceof In)
                {
                In in = (In)modules[m];
                ins.add((In)modules[m]);
                ((In)modules[m]).setMacro(this);
                
                // identify which inputs are being used so we can reduce them
                // to make the macro prettier
                // This is more complex because we don't have back-pointers
                // so we must search through all possible inputs
                for(int i = 0; i < 4; i++)
                    {
                    for(int j = 0; j < modules.length; j++)
                        {
                        if (modules[j] instanceof Unit)
                            {
                            Unit u = (Unit)(modules[j]);
                            for(int k = 0; k < u.getNumInputs(); k++)
                                {
                                if ( u.getInput(k) == in && u.getInputIndex(k) == i ) // found a backpointer
                                    {
                                    unitIns[u.getInputIndex(k)] = true;
                                    }
                                }
                            }

                        Modulation mm = (Modulation)(modules[j]);
                        for(int k = 0; k < modules[j].getNumModulations(); k++)
                            {
                            if ( mm.getModulation(k) == in && mm.getModulationIndex(k) == i ) // found a backpointer
                                {
                                modIns[mm.getModulationIndex(k)] = true;
                                }
                            }
                        }
                    }
                }
            }

        // set the mod outputs
        if (out != null)
            {
            defineOutputs(out.getInputNames());
            // eliminate "Gain"
            String[] names = new String[4];
            System.arraycopy(out.getModulationNames(), 0, names, 0, 4);
            defineModulationOutputs(names);
            }
        else
            {
            defineModulationOutputs(new String[] { "1", "2", "3", "4" });
            defineOutputs(new String[] { "A", "B", "C", "D" });
            }
            
        // set the inputs
        if (ins.size() > 0)
            {
            String[] s = ins.get(0).getModulationOutputNames();
            defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO}, 
                ins.get(0).getModulationOutputNames());
            defineInputs(new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, 
                ins.get(0).getOutputNames());
            }
        else
            {        
            defineModulations(new Constant[] { Constant.ZERO, Constant.ZERO, Constant.ZERO, Constant.ZERO}, new String[] { "A", "B", "C", "D"});
            defineInputs(new Unit[] { Unit.NIL, Unit.NIL, Unit.NIL, Unit.NIL }, new String[] { "1", "2", "3", "4" } );  
            }             
        }
    
    public void setData(JSONObject data, int moduleVersion, int patchVersion) throws Exception
        {
        loadModules(Sound.loadModules(data, patchVersion), Sound.loadName(data));
        }

    public JSONObject getData() 
        { 
        JSONObject obj = new JSONObject();
        
        Sound.saveName(patchName, obj);
        
        JSONArray array = new JSONArray();

        int id = 0;             
        int len = modules.length;
        for(int i = 0; i < len; i++)
            modules[i].setID("a" + (id++));

        for(int i = 0; i < len; i++)
            array.put(modules[i].save());
                
        obj.put("modules", array);
        return obj;
        }
    
    
    public static Macro loadMacro(Sound sound, File file)
        {
        try 
            { 
            JSONObject obj = new JSONObject(new JSONTokener(new GZIPInputStream(new FileInputStream(file)))); 
            return new Macro(sound, 
                Sound.loadModules(obj, Sound.loadPatchVersion(obj)), 
                Sound.loadName(obj));
            }
        catch (Exception ex)
            {
            // try the old way
            return deserializeAsMacro(sound, file);
            }
        }
    
    public static Macro deserializeAsMacro(Sound sound, File file)
        {
        String[] patchName = new String[1];
        return new Macro(sound, deserialize(file, patchName), patchName[0]);
        }
        
    public static Modulation[] deserialize(File file, String[] patchName)
        {
        ObjectInputStream s = null;
        try
            {
            s = 
                new ObjectInputStream(
                    new GZIPInputStream (
                        new BufferedInputStream (
                            new FileInputStream (file))));
            Modulation[] modules = (Modulation[]) s.readObject();
            
            // build the other elements map
            HashMap otherElements = new HashMap();
            ArrayList keys = (ArrayList)s.readObject();
            ArrayList values = (ArrayList)s.readObject();
            for(int i = 0; i < keys.size(); i++)
                otherElements.put(keys.get(i), values.get(i));
            patchName[0] = (String)(otherElements.get(PATCH_NAME_KEY));
                
            s.close();
            return modules;
            }
        catch (IOException e)
            { 
            e.printStackTrace();
            try { if (s != null) s.close(); } catch (IOException e0) { }
            return null;
            }
        catch (ClassNotFoundException e1)
            {
            e1.printStackTrace();
            try { if (s != null) s.close(); } catch (IOException e0) { }
            return null;
            }
        }
                        
    public ModulePanel getPanel()
        {
        return new ModulePanel(Macro.this)
            {
            public JComponent buildPanel()
                {
                Box box = new Box(BoxLayout.Y_AXIS);
                Unit unit = (Unit) getModulation();
                boolean hasIns = false;
                boolean hasOuts = false;

                // To simplify things, 
                // we're going to selectively display certain inputs and outputs
                // based on whether the underlying patch has attached to them
                                
                for(int i = 0; i < unit.getNumOutputs(); i++)
                    {
                    if (unitOuts[i])
                        {
                        hasOuts = true;
                        box.add(new UnitOutput(unit, i, this));
                        }
                    }                
                for(int i = 0; i < unit.getNumInputs(); i++)
                    {
                    if (unitIns[i])
                        {
                        hasIns = true;
                        box.add(new UnitInput(unit, i, this));
                        }
                    }                
                for(int i = 0; i < unit.getNumModulationOutputs(); i++)
                    {
                    if (modOuts[i])
                        {
                        box.add(new ModulationOutput(unit, i, this));
                        }
                    }                
                for(int i = 0; i < unit.getNumModulations(); i++)
                    {
                    if (modIns[i])
                        {
                        box.add(new ModulationInput(unit, i, this));
                        }
                    }                
                
                if (hasIns && hasOuts)
                    box.add(new ConstraintsChooser(unit, this));

                return box;
                }
            };
        }
    }

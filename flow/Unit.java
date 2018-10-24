// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;
import org.json.*;
import java.util.*;

/**
   This subclass of Modulation extends it to allow reading and providing partials
   in the form of arrays of tuples <frequency, amplitude, order>.  FREQUENCY
   is a real value which is a multiple of the current PITCH of the given sound.
   So a frequency value of 2.5 would correspond to 1100Hz if the current pitch is 440Hz.
   AMPLITUDE is a real value >= 0, typically < 1.  Finally, ORDER is a unique
   integer (a byte) from 0...(number of tuples - 1) which acts as the partial's unique identifier
   and which moves with it if you rearrange the partials, perhaps to sort them by
   frequency.  There are NUM_PARTIALS partials all told.  They're not stored in a single
   array, but rather in three separate arrays, one for frequencies, one for amplitudes,
   and one for orders.
        
   <p>Units can be attached to other Units to get partials from them.  The ports
   by which they are attached are their UNIT INPUT ports.  Units also provide partials
   via one or more UNIT OUTPUT ports (nearly always a single output port).
        
   <p>Units with input ports often are CONSTRAINABLE.  This means that after a Unit has 
   created output partials modified from the ones it received from input port 0 (and
   possibly other ports), we can automatically *reset* some of those partials back to the
   ones originally supplied by input port 0.  This has the effect of constraining changes
   made to partials to only the ones not reset.
        
   <p>Because Units are Modulations, they can both send and receive modulation signals.  But
   because sending Modulation signals via a Unit is not very common, by default all Units
   have zero output Modulation ports (and of course zero input Modulation ports).  You can
   override this by defining output ports.
        
   <p>Units can move the amplitudes and frequencies of partials from an input port to an 
   output port in one of two ways: either they can COPY them or they can make a DIRECT LINK
   to them.  If you copy amplitudes (or frequencies), then you are free to modify them.
   If you make a direct link, you cannot make any changes -- they are read-only, but this
   is more efficient. Orders are by default transferred by direct link, though you override that.
        
   <p>Units must always output their partials sorted by frequency.  There are two sorting
   algorithms available to do this.  simpleSort() is a sorting algorithm tuned for situations
   when only small changes are necessary to get the partials back in sort, and particularly
   where the out-of-order partials only need to move a few steps.  bigSort() is tuned for
   larger sorting situations.
*/

public class Unit extends Modulation
    {
    private static final long serialVersionUID = 1;

    /** Number of partials processed by Units -- the size of their amplitudes and frequencies arrays. */
    public static final int DEFAULT_NUM_PARTIALS = 128;
    public static int NUM_PARTIALS = Prefs.getLastNumPartials();
    
    /** A common default Unit.  Has standardized frequencies and amplitudes that are all zero. 
        Do NOT use (foo == Unit.NIL) as a test to see if a Nil is attached to a given port, because
        when patches are deserialized, this will not be the case.  Instead, you should use 
        the convenience method Unit.isInputNil(...). */
    public static final Unit NIL = new Nil();
        
    /** The default name for the primary output unit.  This is "Out". */
    public static final String DEFAULT_UNIT_OUT_NAME = "Out";
        
    public Unit(Sound sound)
        {
        super(sound);

        defineModulationOutputs(new String[] { } );  // no modulation outputs in a Unit by default
        defineOutputs(new String[] { "Out" });
        numOutputs = 1;
        amplitudes = new double[numOutputs][NUM_PARTIALS];
        frequencies = new double[numOutputs][NUM_PARTIALS];
        orders = new byte[numOutputs][NUM_PARTIALS];
        outputNames = new String[] { DEFAULT_UNIT_OUT_NAME };
        constraintIn = Unit.NIL;
        constraint = CONSTRAINT_NONE;
        invertConstraints = false;
        standardizeFrequencies();
        setOrders();
        }
        
        
    ////// OPERATION
        
    boolean clearOnReset = true;

    /**
       If clearOnReset is TRUE (the default), then reset() standardizes the frequencies
       and sets all amplitudes to zero.  You should always call super.reset(),
       but if you wish reset to not do these things, then you should set 
       clearOnReset() to FALSE to prevent this from happening.
    */
    protected void setClearOnReset(boolean val) { clearOnReset = val; }

    /** Called when the Unit is reset entirely.  
        Override this as you see fit, but be sure to call super.reset().
                
        <p>By default, super.reset() will set all amplitudes to zero and 
        standardizes frequencies, unless setClearOnReset(false) has been called first. */
                
    public void reset()
        {
        super.reset();
        if (clearOnReset)
            {
            amplitudes = new double[numOutputs][NUM_PARTIALS];
            frequencies = new double[numOutputs][NUM_PARTIALS];
            standardizeFrequencies();
            }
        }
                

    boolean pushOrders = true;
    
    /**
       If pushOrders is TRUE (the default), then go() pushes orders from input 0
       (assuming there is an input 0).  If you don't want this behavior, then
       you should first call setPushOrders(false).
    */
    public void setPushOrders(boolean val)
        {
        pushOrders = val;
        }
        
    /**
       Returns the value of pushOrders.  If pushOrders is TRUE (the default), then go() pushes orders from input 0
       (assuming there is an input 0).
    */
    public boolean getPushOrders()
        {
        return pushOrders;
        }
             
    /** Called to update the Unit.  Override this as you see fit, but be sure to call super.go().
        By default this method clears all triggers.   It also pushes orders from input 0, unless 
        setCopyOrders(false) has been called. */    
    public void go()
        {
        super.go();
        if (pushOrders && inputs.length > 0)
            {
            for(int i = 0; i < orders.length; i++)
                pushOrders(0, i);
            }
        }
        
        
        
        
    ////// OUTPUTS

    int numOutputs;
    // The amplitudes of the various partials.
    double[][] amplitudes;
    // The frequencies of the various partials.
    double[][] frequencies;
    /* The orders of the various partials.  An order is an integer which uniquely represents that
       partial.  For example, if you had four partials, they might have orders 0, 1, 2, 3, or
       perhaps 1, 3, 2, 0, or whatever, as long as each one is unique and they're ordered 0...n
       somehow.  These are basically unique "names" for your partials.
        
       <p> The reason for this array is as follows.  Units are required to keep their
       partials in sorted order by frequency.  If you have a unit whose partials wander about,
       the sort ordering will change.  This means that certain units (notably Smooth) which
       need to keep track of which partials are which cannot rely on their order in the array
       to do this because the order will change.  So instead, they can use the orders array
       to determine which partials are which.
        
       <p>Note that orders is a byte array.  Thus if you want more than 256 partials you'll have
       to change this to a short array.
    */
    byte[][] orders;
    String[] outputNames;
    
    /** Defines the UNIT OUTPUT ports by their names. */
    public void defineOutputs(String[] names)
        {
        numOutputs = names.length;
        outputNames = names;
        amplitudes = new double[numOutputs][NUM_PARTIALS];
        frequencies = new double[numOutputs][NUM_PARTIALS];
        orders = new byte[numOutputs][NUM_PARTIALS];
        standardizeFrequencies();
        }
    
    /** Returns the name of a given unit output port. */
    public final String getOutputName(int num) { return outputNames[num]; }

    /** Sets the name of a given unit output port. */
    public final void setOutputName(int num, String string) { outputNames[num] = string; }
        
    /** Returns the names of all unit output ports. */
    public final String[] getOutputNames() { return outputNames; }

    /** Returns the number unit output ports. */
    public int getNumOutputs() { return numOutputs; }

    /** Returns true if this unit displays its output ports.  Override this if you don't want it to (basically Out.java is the only
        Unit in this category).  */
    public boolean showsOutputs() { return true; }
    
    /** Returns all amplitude arrays for partials of all unit output ports. */
    public double[][] getAllAmplitudes() { return amplitudes; }

    /** Returns all frequency arrays for partials of all unit output ports. */
    public double[][] getAllFrequecies() { return frequencies; }

    /** Returns all orders arrays for partials of all unit output ports. */
    public byte[][] getAllOrders() { return orders; }

    /** Returns the amplitude array for the partials of the given unit output port. */
    public double[] getAmplitudes(int val) { return amplitudes[val]; }

    /** Returns the frequency array for the partials of the given unit output port. */
    public double[] getFrequencies(int val) { return frequencies[val]; }
 
    /** Returns the orders array for the partials of the given unit output port. */
    public byte[] getOrders(int val) { return orders[val]; }
    
            
    ////// INPUTS
    
        
    /** The Units which form inputs to this Unit. */
    protected Unit[] inputs = new Unit[0];
    /** The output port numbers of tne Unit which form inputs to this Unit. */
    protected int[] inputIndexes = new int[0];
    
    Unit[] defaultInputs = new Unit[0];
    String[] inputNames = new String[0];

    /** Returns TRUE if the default Unit for this Input Unit port is presently the Unit being used. */
    public boolean isDefaultInput(int num)
        {
        return defaultInputs[num] == inputs[num];
        }
                
    /** Returns TRUE if the default Unit for this Input Unit port is presently the Unit being used. */
    public void defineInputs(Unit[] inputs, String[] names)
        {
        this.inputs = inputs;
        defaultInputs = (Unit[])(inputs.clone());
        inputIndexes = new int[inputs.length];
        inputNames = names;
        }
    
    public boolean isInputNil(int num) { return getInput(num) instanceof Nil; }
    
    /** Sets Input Unit port 0.  */
    public void setInput(Unit input) { setInputs(input); }
        
    /** Sets Input Unit port 0. */
    public void setInputs(Unit input)
        {
        setInputs(new Unit[] { input });
        }
        
    /** Sets Input Unit ports 0 and 1. */
    public void setInputs(Unit input0, Unit input1)
        {
        setInputs(new Unit[] { input0, input1 });
        }
        
    /** Sets Input Unit ports 0, 1, and 2 */
    public void setInputs(Unit input0, Unit input1, Unit input2)
        {
        setInputs(new Unit[] { input0, input1, input2 });
        }
        
    /** Sets the first N Input Unit ports. */
    public void setInputs(Unit[] inputs)
        {
        for(int i = 0; i < inputs.length; i++)
            setInput(inputs[i], i, 0);
        }
        
    /** Sets Input Unit port NUM to the provide Unit, with its output port index set to 0. */
    public void setInput(Unit in, int num)
        {
        setInput(in, num, 0);
        }
                

    /** Sets Input Unit port NUM to the provide Unit, with its output port index set to INDEX. */
    public void setInput(Unit in, int num, int index)
        {
        inputs[num] = in;
        inputIndexes[num] = index;
        }
                
    /** Returns the unit attached to Unit Input Port NUM. */
    public Unit getInput(int num)
        {
        return inputs[num];
        }

    /** Returns the output port index of the Unit attached to Unit Input Port NUM. */
    public int getInputIndex(int num)
        {
        return inputIndexes[num];
        }

    /** Returns the name of the given Unit Input Port. */
    public String getInputName(int num) 
        { 
        return inputNames[num];
        }

    /** Sets the name of the given Unit Input Port. */
    public void setInputName(int num, String string)
        { 
        inputNames[num] = string; 
        }

    /** Returns the names of all Unit Input Ports. */
    public String[] getInputNames() 
        { 
        return inputNames;
        }
                                
    /** Resets the Unit Input Port to its default Unit (typically Unit.NIL). */
    public void clearInput(int num)
        {
        inputs[num] = defaultInputs[num];
        inputIndexes[num] = 0;
        }
                
    /** Returns the number of unit input ports. */
    public int getNumInputs() 
        {
        return inputNames.length;
        }

    /** Gets the frequencies array of the partials provided by the Unit attached to Unit Input Port INPUT.
        This array should be treated as read-only. */
    public double[] getFrequenciesIn(int input)
        {
        return inputs[input].frequencies[inputIndexes[input]];
        }
                
    /** Gets the amplitudes array of the partials provided by the Unit attached to Unit Input Port INPUT.
        This array should be treated as read-only. */
    public double[] getAmplitudesIn(int input)
        {
        return inputs[input].amplitudes[inputIndexes[input]];
        }

    /** Gets the orders array of the partials provided by the Unit attached to Unit Input Port INPUT.
        This array should be treated as read-only. */
    public byte[] getOrdersIn(int input)
        {
        return inputs[input].orders[inputIndexes[input]];
        }

    /** Sets the frequencies array of Unit Output Port OUTPUT to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array should then be treated as read-only. */
    public void pushFrequencies(int input, int output)
        {
        frequencies[output] = inputs[input].frequencies[inputIndexes[input]];
        }
                
    /** Sets the amplitudes array of Unit Output Port OUTPUT to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array should then be treated as read-only. */
    public void pushAmplitudes(int input, int output)
        {
        amplitudes[output] = inputs[input].amplitudes[inputIndexes[input]];
        }

    /** Sets the orders array of Unit Output Port OUTPUT to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array should then be treated as read-only. */
    public void pushOrders(int input, int output)
        {
        orders[output] = inputs[input].orders[inputIndexes[input]];
        }
                
    /** Copies the frequencies array of Unit Output Port OUTPUT from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyFrequencies(int input, int output)
        {
        System.arraycopy(inputs[input].frequencies[inputIndexes[input]], 0, frequencies[output], 0, frequencies[output].length);
        }

    /** Copies the amplitudes array of Unit Output Port OUTPUT from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyAmplitudes(int input, int output)
        {
        System.arraycopy(inputs[input].amplitudes[inputIndexes[input]], 0, amplitudes[output], 0, amplitudes[output].length);
        }
                
    /** Copies the orders array of Unit Output Port OUTPUT from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyOrders(int input, int output)
        {
        System.arraycopy(inputs[input].orders[inputIndexes[input]], 0, orders[output], 0, orders[output].length);
        }

    /** Sets the frequencies array of Unit Output Port #0 to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void pushFrequencies(int input)
        {
        pushFrequencies(input, 0);
        }
                
    /** Sets the amplitudes array of Unit Output Port #0 to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void pushAmplitudes(int input)
        {
        pushAmplitudes(input, 0);
        }

    /** Sets the orders array of Unit Output Port #0 to 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void pushOrders(int input)
        {
        pushOrders(input, 0);
        }
                
    /** Copies the frequencies array of Unit Output Port #0 from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyFrequencies(int input)
        {
        copyFrequencies(input, 0);
        }

    /** Copies the amplitudes array of Unit Output Port #0 from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyAmplitudes(int input)
        {
        copyAmplitudes(input, 0);
        }

    /** Copies the orders array of Unit Output Port #0 from 
        the partials provided by the Unit attached to Unit Input Port INPUT.
        The output array may then be written to. */
    public void copyOrders(int input)
        {
        copyOrders(input, 0);
        }

       
    ////// INPUT UTILITIES
                
    /** Sets the orders of the partials of all unit output ports such that order[i] = i. */
    public void setOrders()
        {
        for(int j = 0; j < orders.length; j++)
            {
            byte[] o = orders[j];
            for(int i = 0; i < o.length; i++)
                o[i] = (byte)i;
            }
        }
    
    /** Sets the frequencies of the partials of all unit output ports such that
        frequency[i] is equal to i + 1.    */    
    public void standardizeFrequencies()
        {
        for(int i = 0; i < frequencies.length; i++)
            standardizeFrequencies(i);
        }

    /** Sets the frequencies of the partials of unit output port J such that
        frequency[i] is equal to i + 1.    */    
    public void standardizeFrequencies(int j)
        {
        double[] f = frequencies[j];
        for(int i = 0; i < f.length; i++)
            f[i] = i + 1;
        }
        
    /** Maximizes the amplitudes of the partials of all unit output ports, that is, scales them
        so that the largest one is 1.  If the amplitudes are all zero, they are left as zero. */
    public void maximizeAmplitudes()
        {
        for(int i = 0; i < amplitudes.length; i++)
            maximizeAmplitudes(i);
        }

    /** Maximizes the amplitudes of the partials of unit output port J, that is, scales them
        so that the largest one is 1.  If the amplitudes are all zero, they are left as zero. */
    public void maximizeAmplitudes(int j)
        {
        double[] a = amplitudes[j];
        double max = Math.abs(a[j]);
                
        for(int i = 1; i < a.length; i++)
            {
            if (a[i] > max)
                {
                max = a[i];
                }
            }
            
        if (max > 0)
            {
            double scale = 1.0 / max;
            for(int i = 0; i < a.length; i++)
                {
                a[i] *= scale;
                }
            }
        }

    /** Normalizes the amplitudes of the partials of all unit output ports, that is, scales them
        so that they sum to 1.  If the amplitudes are all zero, they are left as zero. */
    public void normalizeAmplitudes()
        {
        for(int i = 0; i < amplitudes.length; i++)
            normalizeAmplitudes(i);
        }

    /** Normalizes the amplitudes of the partials of unit output port J, that is, scales them
        so that they sum to 1.  If the amplitudes are all zero, they are left as zero. */
    public void normalizeAmplitudes(int j)
        {
        double total = 0;
        double[] a = amplitudes[j];
        for(int i = 0; i < a.length; i++)
            {
            total += Math.abs(a[i]);
            }
        if (total != 0)
            {
            double invTotal = 1.0 / total;
            for(int i = 0; i < a.length; i++)
                {
                a[i] *= invTotal;
                }
            }
        }

    /** Returns the partial from output unit port J whose frequency is lowest. */
    public int getLowestPartial(int j)
        {
        double[] f = frequencies[j];
        double minimum = f[0];
        int val = 0;
        for(int i = 1; i < f.length; i++)
            if (f[i] < minimum)
                {
                minimum = f[i];
                val = i;
                }
        return val;
        }

    /** Returns the partial from output unit port J whose amplitude is highest. */
    public int getLoudestPartial(int j)
        {
        double[] a = amplitudes[j];
        double maximum = a[0];
        int val = 0;
        for(int i = 1; i < a.length; i++)
            if (a[i] > maximum)
                {
                maximum = a[i];
                val = i;
                }
        return val;
        }


    // Swaps partials i and j
    void swap(int i, int j, double[] f, double[] a, byte[] o)
        {
        double d = f[i];
        f[i] = f[j];
        f[j] = d;
        d = a[i];
        a[i] = a[j];
        a[j] = d;
        byte e = o[i];
        o[i] = o[j];
        o[j] = e;
        }

    // standard insertion sort
    void insertionSort(double freq[], double[] amp, byte[] order) 
        {
        for (int i=1; i<freq.length; i++) // Insert i'th record
            for (int j=i; (j>0) && (freq[j] < freq[j-1]); j--)
                {
                swap(j, j-1, freq, amp, order);
                }
        }
    
    // quicksort partition
    int quicksortPartition(double freq[], double[] amp, byte[] order, int low, int high)
        {
        double pivot = freq[high];
         
        // index of smaller element
        int i = (low-1); 
        for (int j = low; j <= high-1; j++)
            {
            // If current element is smaller than or
            // equal to pivot
            if (freq[j] <= pivot)
                {
                i++;
                swap(i, j, freq, amp, order);
                }
            }
 
        swap(i+1, high, freq, amp, order); 
        return i+1;
        }

    int[] quickSortStack = null;
    void quickSort (double freq[], double[] amp, byte[] order, int l, int h)
        {
        // Create an auxiliary stack
        if (quickSortStack == null) quickSortStack = new int[h-l+1];
  
        // initialize top of stack
        int top = -1;
  
        // push initial values of l and h to stack
        quickSortStack[++top] = l;
        quickSortStack[++top] = h;
  
        // Keep popping from stack while is not empty
        while (top >= 0)
            {
            // Pop h and l
            h = quickSortStack[top--];
            l = quickSortStack[top--];
  
            // Set pivot element at its correct position
            // in sorted array
            int p = quicksortPartition(freq, amp, order, l, h);
  
            // If there are elements on left side of pivot,
            // then push left side to stack
            if (p-1 > l)
                {
                quickSortStack[++top] = l;
                quickSortStack[++top] = p - 1;
                }
  
            // If there are elements on right side of pivot,
            // then push right side to stack
            if (p+1 < h)
                {
                quickSortStack[++top] = p + 1;
                quickSortStack[++top] = h;
                }
            }
        }

// standard quicksort
    void quickSort(double freq[], double amp[], byte order[])
        {
        quickSort(freq, amp, order, 0, freq.length - 1);
        }

    /** Sorts the partials of Unit Output port by frequency. Returns TRUE if any elements potentially changed positions. 
        Orders are always copied.  Frequencies are never copied.   Amplitudes are only copied if indicated.  */
    public boolean bigSort(int j, boolean copyAmplitudes)
        {
        if (copyAmplitudes)
            {
            this.amplitudes[j] = this.amplitudes[j].clone();
            }

        this.orders[j] = this.orders[j].clone();

        double[] frequencies = this.frequencies[j];
        double[] amplitudes = this.amplitudes[j];
        byte[] orders = this.orders[j];
        
        quickSort(frequencies, amplitudes, orders);
        return true;
        }





    // This is basically a somewhat better bubble sort.  I think bubble sort
    // will likely be the best option in most cases because we won't have big
    // frequency randomization.
    //
    // Modified from https://www.geeksforgeeks.org/cocktail-sort/
        
    /** Sorts the partials of Unit Output port by frequency. Returns TRUE if any elements potentially changed positions.
        You might find this method more efficient than bigSort(...) when the partials are almost in sorted order, only
        differing by one or two positions. Orders are always copied.  Frequencies are never copied.   Amplitudes are
        only copied if indicated.  */
    public boolean simpleSort(int j, boolean copyAmplitudes) 
        {
        boolean swapped = true;
        boolean everSwapped = false;
        
        if (copyAmplitudes)
            {
            this.amplitudes[j] = this.amplitudes[j].clone();
            }
        
        this.orders[j] = this.orders[j].clone();

        double[] frequencies = this.frequencies[j];
        double[] amplitudes = this.amplitudes[j];
        byte[] orders = this.orders[j];
        
        int start = 0;
        int end = frequencies.length;
 
        while (swapped==true)
            {
            // reset the swapped flag on entering the 
            // loop, because it might be true from a 
            // previous iteration.
            swapped = false;
 
            // loop from bottom to top same as
            // the bubble sort
            for (int i = start; i < end-1; ++i)
                {
                if (frequencies[i] > frequencies[i + 1])
                    {
                    swap(i, i+1, frequencies, amplitudes, orders);
                    swapped = true;
                    }
                }
 
            // if nothing moved, then array is sorted.
            if (swapped==false)
                break;
 
            // otherwise, reset the swapped flag so that it
            // can be used in the next stage
            swapped = false;
 
            // move the end point back by one, because
            // item at the end is in its rightful spot
            end = end-1;
 
            // from top to bottom, doing the
            // same comparison as in the previous stage
            for (int i = end-1; i >=start; i--)
                {
                if (frequencies[i] > frequencies[i+1])
                    {
                    swap(i, i+1, frequencies, amplitudes, orders);
                    swapped = true;
                    }
                }
 
            // increase the starting point, because
            // the last stage would have moved the next
            // smallest number to its rightful spot.
            start = start+1;
        
            everSwapped = everSwapped || swapped;
            }
        return everSwapped;
        }
    
    /*
    // OrderStatistics [Cormen, p. 187]:
    // Find the ith smallest element of the array between indices p and r inclusive 
    // i starts at 1 (for "first").
        
    public double randomizedSelect(double[] array, int p, int r, int i, java.util.Random rng)
    {
    if (p==r) return array[p];
    int q = randomizedPartition(array, p, r, rng);
    int k = q-p+1;
    if (i <= k)
    return randomizedSelect(array, p, q, i, rng);
    else
    return randomizedSelect(array, q+1, r, i-k, rng);
    }
                
                
    // [Cormen, p. 162]
    int randomizedPartition(double[] array, int p, int r, java.util.Random rng)
    {
    int i = rng.nextInt(r-p+1)+p;
                
    //exchange array[p]<->array[i]
    double tmp = array[i];
    array[i]=array[p];
    array[p]=tmp;
    return partition(array,p,r);
    }
                
                
    // [Cormen, p. 154]
    int partition(double[] array, int p, int r)
    {
    double x = array[p];
    int i = p-1;
    int j = r+1;
    while(true)
    {
    do j--; while(array[j]>x);
    do i++; while(array[i]<x);
    if ( i < j )
    {
    //exchange array[i]<->array[j]
    double tmp = array[i];
    array[i]=array[j];
    array[j]=tmp;
    }
    else
    return j;
    }
    }
    */     



//// CONSTRAINTS

    /** This tells a UI that it should (or should not) include a constraints menu.
        By default the heuristic is: include a menu if there is at least one input.
        Override this to customize further. */
    public boolean isConstrainable() { return inputs.length > 0; }  
        
    public static final int CONSTRAINT_NONE = 0;
    public static final int CONSTRAINT_FUNDAMENTAL = 1;
    public static final int CONSTRAINT_EVEN = 2;
    public static final int CONSTRAINT_ODD = 3;
    public static final int CONSTRAINT_FIRST_THIRD = 4;
    public static final int CONSTRAINT_SECOND_THIRD = 5;
    public static final int CONSTRAINT_THIRD_THIRD = 6;
    public static final int CONSTRAINT_OCTAVES = 7;
    public static final int CONSTRAINT_FIFTHS = 8;
    public static final int CONSTRAINT_MAJOR_THIRDS = 9;
    public static final int CONSTRAINT_MINOR_SEVENTHS = 10;
    public static final int CONSTRAINT_MAJOR_SECONDS = 11;
    public static final int CONSTRAINT_MAJOR_SEVENTHS = 12;
    public static final int CONSTRAINT_MINOR_SECONDS = 13;
    public static final int CONSTRAINT_MINOR_THIRDS = 14;
    public static final int CONSTRAINT_MAJOR_SIXTHS = 15;
    public static final int CONSTRAINT_FIRST_TWO = 16;
    public static final int CONSTRAINT_FIRST_FOUR = 17;
    public static final int CONSTRAINT_FIRST_EIGHT = 18;
    public static final int CONSTRAINT_FIRST_SIXTEEN = 19;
    public static final int CONSTRAINT_FIRST_THIRTY_TWO = 20;
    public static final int CONSTRAINT_FIRST_SIXTY_FOUR = 21;
        
    public static final String[] constraintNames = new String[]
    { "None", "Fund", "Even", "Odd", "1st 3rd",
      "2nd 3rd", "3rd 3rd", "Octaves", "Fifths",
      "M. 3rds", "m. 7ths", "M. 2nds", "M. 7ths",
      "m. 2nds", "m. 3rds", "M. 6ths", "Low 2", "Low 4", "Low 8",
      "Low 16", "Low 32", "Low 64"};
        
    // Notice this is all 1-based here, but they'll get converted to 0-based
    static final int[] NONE_HARMONICS = new int[NUM_PARTIALS];
    static final int[] FUNDAMENTAL_HARMONICS = { 1 };
    static final int[] EVEN_HARMONICS; // = new int[64];
    static final int[] ODD_HARMONICS; //  = new int[64];
    static final int[] FIRST_THIRD_HARMONICS ; //  = new int[43];
    static final int[] SECOND_THIRD_HARMONICS ; // = new int[43];
    static final int[] THIRD_THIRD_HARMONICS ; //  = new int[42];
    static final int[] OCTAVE_HARMONICS; //  = new int[] { 1, 2, 4, 8, 16, 32, 64, 64 * 2 };
    static final int[] FIFTH_HARMONICS; // = new int[] { 3, 6, 12, 24, 48, 96, 96 * 2 }; 
    static final int[] MAJOR_THIRD_HARMONICS; // = new int[] { 5, 10, 20, 40, 80, 80 * 2 };
    static final int[] MINOR_SEVENTH_HARMONICS; // = new int[] { 7, 14, 28, 56, 112, 112 * 2 };
    static final int[] MAJOR_SECOND_HARMONICS; // = new int[] { 9, 18, 36, 72, 72 * 2 };
    static final int[] MAJOR_SEVENTH_HARMONICS; // = new int[] { 15, 30, 60, 120, 120 * 2 };
    static final int[] MINOR_SECOND_HARMONICS; // = new int[] { 17, 34, 68, 68 * 2 };
    static final int[] MINOR_THIRD_HARMONICS; // = new int[] { 19, 38, 76, 76 * 2 };
    static final int[] MAJOR_SIXTH_HARMONICS; // = new int[] { 27, 54, 108, 108 * 2 };
    static final int[] FIRST_TWO_HARMONICS = new int[] { 1, 2 };
    static final int[] FIRST_FOUR_HARMONICS = new int[] { 1, 2, 3, 4 };
    static final int[] FIRST_EIGHT_HARMONICS = new int[] { 1, 2, 3, 4, 5, 6, 7, 8 };
    static final int[] FIRST_SIXTEEN_HARMONICS = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };
    static final int[] FIRST_THIRTY_TWO_HARMONICS = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32 };
    static final int[] FIRST_SIXTY_FOUR_HARMONICS = new int[64];
    
    /* All harmonics by harmonics option */
    static final int[][] HARMONICS;
    
    /* All partials EXCEPT harmonics by harmonics option */
    static final int[][] ANTI_HARMONICS;
        
    Unit constraintIn;
    int constraintIndex;
    int constraint;  // only used if constraint is Unit.NIL
    boolean invertConstraints = false;
    double[] invertConstrainedAmplitudes = null;
    double[] invertConstrainedFrequencies = null;
    int[] constraintInPartials = null;
        

    /** Sets the constraints to the non-zero amplitude harmonics in the Unit attached to the given Unit Input Port whose output port is index.
        Setting to Unit.NIL indicates that the constraints are not based on these harmonics.  */
    public void setConstraintIn(Unit constraintIn, int index) { this.constraintIn = constraintIn; this.constraintIndex = index; }
    /** Returns the unit defining the constraints, if any, or Unit.NIL. */
    public Unit getConstraintIn() { return constraintIn; }
    /** Returns the output port of the Unit defining the constraints, if any. */
    public int getConstraintIndex() { return constraintIndex; }
    /** Sets the constraints to the given constraint type.  If you have called setConstraintIn(...),
        it takes precedence over these constraints. */
    public void setConstraint(int constraint) { this.constraint = constraint; }
    /** Returns the constraint type.  If you have called setConstraintIn(...),
        it takes precedence over these constraints. */
    public int getConstraint() { return constraint; }
    /** Inverts the constraints (or not). */
    public void setInvertConstraints(boolean invertConstraints) { this.invertConstraints = invertConstraints; }
    /** Returns whether the constraints are inverted or not. */
    public boolean getInvertConstraints() { return invertConstraints; }

    /** Return a list of harmonic constraints.   This list consists of indexes into the frequencies, amplitudes, and orders arrays. */
    public int[] getConstrainedPartials()
        {
        // pick a partial
        if (invertConstraints)
            {
            if (!(constraintIn instanceof Nil))
                {
                // costly...
                                
                double[] c = getConstraintIn().amplitudes[getConstraintIndex()];
                int count = 0;
                for(int i = 0; i < c.length; i++)
                    {
                    if (c[i] == 0)
                        count++;
                    }
                                        
                if (constraintInPartials == null || constraintInPartials.length != count)
                    constraintInPartials = new int[count];

                count = 0;
                for(int i = 0; i < c.length; i++)
                    {
                    if (c[i] == 0)
                        constraintInPartials[count++] = i;
                    }
                return constraintInPartials;
                }
                                
            else return ANTI_HARMONICS[constraint];
            }
        else
            {
            if (!(constraintIn instanceof Nil))
                {
                // costly...
                                
                double[] c = getConstraintIn().amplitudes[getConstraintIndex()];
                int count = 0;
                for(int i = 0; i < c.length; i++)
                    {
                    if (c[i] != 0)
                        count++;
                    }
                                        
                if (constraintInPartials == null || constraintInPartials.length != count)
                    constraintInPartials = new int[count];

                count = 0;
                for(int i = 0; i < c.length; i++)
                    {
                    if (c[i] > 0)
                        constraintInPartials[count++] = i;
                    }
                return constraintInPartials;
                }

            else return HARMONICS[constraint];
            }
        }
                
                

    /** Resets (to partials from the Unit attached to Unit Input port 0) those partials that are not part of the constraint. 
        Returns true if something was (or was likely) reset. */
    public boolean constrain()
        {
        double[] frequencies = getFrequencies(0);
        double[] amplitudes = getAmplitudes(0);
        byte[] orders = getOrders(0);
        
        if (inputs == null || inputs.length == 0)
            return false;
                
        Unit source = getInput(0);
        int index = getInputIndex(0);
        
        double[] sourcefrequencies = getFrequenciesIn(index);
        double[] sourceamplitudes = getAmplitudesIn(index);
        byte[] sourceorders = getOrdersIn(index);
                
        if (invertConstraints)
            {
            // We want to constrain to only the partials NOT given.  So here we're resetting
            // if the partials ARE part of the constraint
                
            if (!(getConstraintIn() instanceof Nil))
                {
                double[] constraintAmplitudes = getConstraintIn().amplitudes[getConstraintIndex()];
                for(int i = 0; i < frequencies.length; i++)
                    {
                    if (constraintAmplitudes[i] != 0)
                        {
                        frequencies[i] = sourcefrequencies[i];
                        amplitudes[i] = sourceamplitudes[i];
                        orders[i] = sourceorders[i];
                        }
                    }
                }
            else 
                {
                int[] harmonics = HARMONICS[constraint];
                for(int i = 0; i < harmonics.length; i++)
                    {
                    frequencies[harmonics[i]] = sourcefrequencies[harmonics[i]];
                    amplitudes[harmonics[i]] = sourceamplitudes[harmonics[i]];
                    orders[harmonics[i]] = sourceorders[harmonics[i]];
                    }
                }
            }
        else
            {
            if (constraint == CONSTRAINT_NONE) return false;

            // We want to constrain to only the given partials.  So here we're resetting
            // if the partials are NOT part of the constraint, that, is, if the "anti-partials"
            // ARE part of the constraint

            if (!(getConstraintIn() instanceof Nil))
                {
                double[] constraintAmplitudes = getConstraintIn().amplitudes[getConstraintIndex()];
                for(int i = 0; i < frequencies.length; i++)
                    {
                    if (constraintAmplitudes[i] == 0)
                        {
                        frequencies[i] = sourcefrequencies[i];
                        amplitudes[i] = sourceamplitudes[i];
                        orders[i] = sourceorders[i];
                        }
                    }
                }
            else 
                {
                int[] antiharmonics = ANTI_HARMONICS[constraint];
                for(int i = 0; i < antiharmonics.length; i++)
                    {
                    frequencies[antiharmonics[i]] = sourcefrequencies[antiharmonics[i]];
                    amplitudes[antiharmonics[i]] = sourceamplitudes[antiharmonics[i]];
                    orders[antiharmonics[i]] = sourceorders[antiharmonics[i]];
                    }
                }
            }
            
        return true;
        }


    ////// CONSTRAINTS
        
    static int[] buildHarmonics(int initial)
        {
        int count = 0;
        for(int i = initial; i < NUM_PARTIALS; i*=2)
            {
            count++;
            }
        int[] harmonics = new int[count];
        count = 0;
        for(int i = initial; i < NUM_PARTIALS; i*=2)
            {
            harmonics[count++] = i;
            }
        return harmonics;
        }


    static
        {
        //// We need to build the harmonics.  First things first, let's allocate
        //// the right amount for the unalocated EVEN, ODD, FIRST ... THIRD_THIRD harmonics
        
        EVEN_HARMONICS = new int[NUM_PARTIALS / 2];
        ODD_HARMONICS = new int[NUM_PARTIALS / 2];
        int remainder = NUM_PARTIALS % 3;
        FIRST_THIRD_HARMONICS = new int[NUM_PARTIALS / 3 + (remainder > 0 ? 1 : 0)];
        SECOND_THIRD_HARMONICS = new int[NUM_PARTIALS / 3  + (remainder > 1 ? 1 : 0)];
        THIRD_THIRD_HARMONICS = new int[NUM_PARTIALS / 3];
                
        /// Now we construct the harmonics for the various standard harmonics 
                
        OCTAVE_HARMONICS = buildHarmonics(1);
        FIFTH_HARMONICS = buildHarmonics(3);
        MAJOR_THIRD_HARMONICS = buildHarmonics(5);
        MINOR_SEVENTH_HARMONICS = buildHarmonics(7);
        MAJOR_SECOND_HARMONICS = buildHarmonics(9);
        MAJOR_SEVENTH_HARMONICS = buildHarmonics(15);
        MINOR_SECOND_HARMONICS = buildHarmonics(17);
        MINOR_THIRD_HARMONICS = buildHarmonics(19);
        MAJOR_SIXTH_HARMONICS = buildHarmonics(27);
                
                
        /// Now we can build the HARMONICS and ANTI_HARMONICS arrays

        HARMONICS = new int[][] { 
            NONE_HARMONICS, 
            FUNDAMENTAL_HARMONICS, 
            EVEN_HARMONICS, 
            ODD_HARMONICS, 
            FIRST_THIRD_HARMONICS, 
            SECOND_THIRD_HARMONICS, 
            THIRD_THIRD_HARMONICS, 
            OCTAVE_HARMONICS,
            FIFTH_HARMONICS, 
            MAJOR_THIRD_HARMONICS, 
            MINOR_SEVENTH_HARMONICS, 
            MAJOR_SECOND_HARMONICS,
            MAJOR_SEVENTH_HARMONICS, 
            MINOR_SECOND_HARMONICS, 
            MINOR_THIRD_HARMONICS, 
            MAJOR_SIXTH_HARMONICS,
            FIRST_TWO_HARMONICS, 
            FIRST_FOUR_HARMONICS, 
            FIRST_EIGHT_HARMONICS, 
            FIRST_SIXTEEN_HARMONICS, 
            FIRST_THIRTY_TWO_HARMONICS,
            FIRST_SIXTY_FOUR_HARMONICS 
            };

        ANTI_HARMONICS = new int[HARMONICS.length][];

        // All the harmonics are 1-based in the constants, but we need them to be 0-based:
        for(int j = 0; j < HARMONICS.length; j++)       
            {
            for (int i = 0; i < HARMONICS[j].length; i++)
                --HARMONICS[j][i];
            }


        // Now we handle NONE, FIRXT_SIXTY_FOUR, EVEN, ODD, and the various THIRDs

        for(int i = 0; i < NONE_HARMONICS.length; i++)
            {
            NONE_HARMONICS[i] = i;
            }
                        
        for(int i = 0; i < FIRST_SIXTY_FOUR_HARMONICS.length; i++)
            {
            FIRST_SIXTY_FOUR_HARMONICS[i] = i;
            }
                        
        for(int i = 0; i < NUM_PARTIALS; i += 2)
            {
            EVEN_HARMONICS[i / 2] = i;
            ODD_HARMONICS[i / 2] = i + 1;
            }
                        
        for(int i = 0; i < NUM_PARTIALS; i += 3)
            {
            FIRST_THIRD_HARMONICS[i / 3] = i;
            if ((i / 3) < SECOND_THIRD_HARMONICS.length)
                SECOND_THIRD_HARMONICS[i / 3] = i + 1;
            if ((i / 3) < THIRD_THIRD_HARMONICS.length)
                THIRD_THIRD_HARMONICS[i / 3] = i + 2;
            }
                        
        // build anti-harmonics
        
        for(int j = 0; j < HARMONICS.length; j++)
            {
            ANTI_HARMONICS[j] = new int[NUM_PARTIALS - HARMONICS[j].length];
            int index = 0;
            int antiIndex = 0;
            for(int i = 0; i < NUM_PARTIALS; i++)
                {
                if (index >= HARMONICS[j].length || HARMONICS[j][index] != i)
                    { 
                    ANTI_HARMONICS[j][antiIndex] = i;
                    antiIndex++; 
                    }
                else
                    {
                    index++;
                    }
                }
            }
        }
        
    public void printStats()
        {
        super.printStats();
        for(int i = 0; i < getNumInputs(); i++)                 
            System.err.println("" + i + " INPUT: " + getInput(i));
        for(int i = 0; i < getNumOutputs(); i++)                 
            System.err.println("" + i + " OUTPUT: " + getOutputName(i));
        System.err.println("CONSTRAINT: " + constraintIn + " " + constraint + " " + invertConstraints);
        }

    public static String getName() { return "Unit"; }

    public Object clone()
    	{
    	Unit obj = (Unit)(super.clone());
    	
    	// ---- Copy over unit inputs.  We retain pointers to the old inputs. ----
    	// Input Names
    	obj.inputNames = (String[])(inputNames.clone());
    	// Defaults
    	obj.defaultInputs = (Unit[])(defaultInputs.clone());
    	obj.inputs = (Unit[])(inputs.clone());
    	// unit indexes.  We're setting them all to zero because we reset to defaults (which are constants)
    	obj.inputIndexes = (int[])(inputIndexes.clone());	


		// ---- Copy over unit outputs ----
		// Output Names
		obj.outputNames = (String[])(outputNames.clone());
		// Amplitudes
		obj.amplitudes = (double[][])(amplitudes.clone());
		for(int i = 0; i < obj.amplitudes.length; i++)
			obj.amplitudes[i] = (double[])(obj.amplitudes[i].clone());
		// Frequencies
		obj.frequencies = (double[][])(frequencies.clone());
		for(int i = 0; i < obj.frequencies.length; i++)
			obj.frequencies[i] = (double[])(obj.frequencies[i].clone());
		// Orders
		obj.orders = (byte[][])(orders.clone());
		for(int i = 0; i < obj.orders.length; i++)
			obj.orders[i] = (byte[])(obj.orders[i].clone());

		
		// ---- Copy over constraints.  We retain a pointer to the old constraint input if necessary.  So we just copy over the cached info. ----
		if (obj.invertConstrainedAmplitudes != null)
			obj.invertConstrainedAmplitudes = (double[])(obj.invertConstrainedAmplitudes.clone());
		if (obj.invertConstrainedFrequencies != null)
			obj.invertConstrainedFrequencies = (double[])(obj.invertConstrainedFrequencies.clone());
		if (obj.constraintInPartials != null)
			obj.constraintInPartials = (int[])(obj.constraintInPartials.clone());

    	return obj;
    	}



    ///// JSON Serialization
    /** Called to return a String to be used as a JSON key for the given unit input. 
    	This must be unique within unit inputs in the unit.  By default the name of the unit input is used.  */
    public String getKeyForInput(int input) { return "" + getInputName(input); }
    /** Called to return a String to be used as a JSON key for the given unit input. 
    	This must be unique within unit outputs in the unit.  By default the name of the unit input is used.  */
    public String getKeyForOutput(int output) { return "" + getOutputName(output); }

    /** Returns the unit output number for a given JSON Key, or SERIALIZATION_NOT_FOUND if the key is not found.
      This method is the inverse of getKeyForOutput(...)  */
	public int getOutputForKey(String key)
		{
		// Terribly inefficient but it'll suffice
		for(int i = 0; i < getNumOutputs(); i++)
			{
			if (getKeyForOutput(i).equals(key)) return i;
			}
		return SERIALIZATION_NOT_FOUND;
		}
	
	public JSONObject save() throws JSONException
		{
		JSONObject obj = super.save();
				
		// inputs
		JSONObject inputs = new JSONObject();
		for(int i = 0; i < getNumInputs(); i++)
			{
			Unit input = getInput(i);
			int inputIndex = getInputIndex(i);
			JSONObject m = new JSONObject();
			m.put("id", input.getID());
			m.put("at", input.getKeyForOutput(inputIndex));
			inputs.put(getKeyForInput(i), m);
			}
		obj.put("unit", inputs);

		return obj;
		}

    /** Loads unit inputs from the given JSON Object representing the modulation storage. */
	public void loadUnits(JSONObject inputs, HashMap<String, Modulation> ids, int moduleVersion, int patchVersion)
		{
		for(int i = 0; i < getNumInputs(); i++)
			{
			JSONObject m = inputs.optJSONObject(getKeyForInput(i));
			if (m == null)
				{
					System.err.println("WARNING: Could not load unit " + getKeyForInput(i) + " in " + this);
				}
			else if (m.getString("id").equals("null"))
				{
				setInput(Unit.NIL, i, 0);
				} 
			else
				{
				Unit unit = (Unit)(ids.get(m.getString("id")));
				setInput(unit, i, unit.getOutputForKey(m.getString("at")));
				}
			}	
		}
		
	public void load(JSONObject obj, HashMap<String, Modulation> ids, int patchVersion) throws Exception
		{
		super.load(obj, ids, patchVersion);

		// version
		int moduleVersion = obj.getInt("v");
		
		// inputs
		JSONObject inputs = obj.getJSONObject("unit");
		loadUnits(inputs, ids, moduleVersion, patchVersion);
		}
    }

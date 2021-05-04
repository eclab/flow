// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;


public class XORShift32
    {
    int seed;
        
    public XORShift32(int val)
        {
        reseed(val);
        }
                
    public void reseed(int val)
        {
        if (val == 0) val = -1;
        seed = val;
        }
        
    public int next()
        {
        seed ^= (seed << 13);
        seed ^= (seed >>> 17);
        seed ^= (seed << 5);
        return seed;
        }
                
    public int nextInt(int n)
        {
        if (n<=0)
            throw new IllegalArgumentException("n must be positive, got: " + n);
        
        if ((n & -n) == n)
            return (int)((n * (long)(next() & 0x7FFFFFFF)) >> 31);
        
        int bits, val;
        do 
            {
            bits = (next() & 0x7FFFFFFF);
            val = bits % n;
            } 
        while(bits - val + (n-1) < 0);
        return val;
        }

    public float nextFloat()
        {
        return (next() & 0x00FFFFFF) / ((float)(1 << 24));
        }

    public boolean nextBoolean (float probability)
        {
        if (probability < 0.0f || probability > 1.0f)
            throw new IllegalArgumentException ("probability must be between 0.0 and 1.0 inclusive.");
        if (probability==0.0f) return false;            // fix half-open issues
        else if (probability==1.0f) return true;        // fix half-open issues
        return nextFloat() < probability; 
        }
    }

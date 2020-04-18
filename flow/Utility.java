// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow;

/**
   Various utility methods, primarily fast approximations of mathematical functions.
**/


public class Utility 
{
    static double[] sqrtTable = new double[65536];

    /** A faster (~1.5x) approximation of Square Root.  Uses Math.sqrt for values >= 1,
        else uses a lookup table 64K in size. */
    // about twice as fast
    public static double fastSqrt(final double a)
    {
        if (a >= 1) return Math.sqrt(a);
        return sqrtTable[(int)(a * 65536)];
    }

    /** A very fast (53x) but poor approximation of a^b. */
    // about 53 times faster
    public static double fasterpow(final double a, final double b) 
    {
        final int tmp = (int) (Double.doubleToLongBits(a) >> 32);
        final int tmp2 = (int) (b * (tmp - 1072632447) + 1072632447);
        return Double.longBitsToDouble(((long) tmp2) << 32);
    }

    /** A fast (3.5x) approximation of a^b. */
    // About 3.5 times faster
    public static double fastpow(final double a, final double b) 
    {
        if (b == 0)
            {
                return 1.0;
            }
        else if (b == 1)
            {
                return a;
            }
        else if (b < 0)
            {
                return 1.0 / fastpow(a, -b);
            }
        else if ( b <= 10 && b == (int) b)
            {
                double res = a;
                for(int i = 1; i < b; i++)
                    {
                        res = res * a;
                    }
                return res;
            }
        else
            {       
                double r = 1.0;
                double base = a;
                int exp = (int) b;

                // exponentiation by squaring
                while (exp != 0) 
                    {
                        if ((exp & 1) != 0) 
                            {
                                r *= base;
                            }
                        base *= base;
                        exp >>= 1;
                    }

                // use the IEEE 754 trick for the fraction of the exponent
                final double b_faction = b - (int) b;
                final long tmp = Double.doubleToLongBits(a);
                final long tmp2 = (long) (b_faction * (tmp - 4606921280493453312L)) + 4606921280493453312L;
                return r * Double.longBitsToDouble(tmp2);
            }
    }

    /** An approximation of a^b which uses Math.pow for values of b < 1,
        and uses fastpow for values >= 1.  We presently use this in filters
        but its implementation may change in the future.
    */
    // This should remove some of the weirdness in the LPF sounds
    public static double hybridpow(final double a, final double b) 
    {
        if (Math.abs(b) < 1.0)
            return Math.pow(a, b);
        return fastpow(a, b);
    }


    //// FastSin and FastCos are from
    //// https://github.com/Bukkit/mc-dev/blob/master/net/minecraft/server/MathHelper.java
    
    // The problem is that to get decent looking sawtooth and square and triangle waves etc., you need to have all
    // your partials running at the same phase -- but making changes in frequency will change this.  I had thought
    // the problem we were having was that SIN_TABLE_LENGTH was too short, but that's not it.
    static final int SIN_TABLE_LENGTH = 65536; //  * 16;
    static final double SIN_MULTIPLIER = SIN_TABLE_LENGTH / Math.PI / 2;
    final static int SIN_TABLE_LENGTH_MINUS_1 = SIN_TABLE_LENGTH - 1;
    final static int SIN_TABLE_LENGTH_DIV_4 = SIN_TABLE_LENGTH / 4;
    final static double[] sinTable = new double[SIN_TABLE_LENGTH];

    /** A fast approximation of Sine using a lookup table.  40x the speed of Math.sin. */
    public static final double fastSin(double f) 
    {
        return sinTable[((int) (f * SIN_MULTIPLIER)) & (SIN_TABLE_LENGTH - 1)];
    }

    /** A fast approximation of Cosine using a lookup table. */
    public static final double fastCos(double f) 
    {
        return sinTable[((int) (f * SIN_MULTIPLIER + SIN_TABLE_LENGTH_DIV_4)) & (SIN_TABLE_LENGTH - 1)];
    }


    /**
     * A fast approximation of Sine using a lookup table and Catmull-Rom cubic spline interpolation.
     16x the speed of Math.sin.
    */
    public static final double fastIntSin(double f) {
        double v = f * SIN_MULTIPLIER;
        int conv = (int) v;
        double alpha = v - conv;
        
        int slot1 = conv & SIN_TABLE_LENGTH_MINUS_1;
        int slot0 = (slot1 - 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot2 = (slot1 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot3 = (slot2 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        
        double f0 = sinTable[slot0];
        double f1 = sinTable[slot1];
        double f2 = sinTable[slot2];
        double f3 = sinTable[slot3];
        
        return alpha * alpha * alpha * (-0.5 * f0 + 1.5 * f1 - 1.5 * f2 + 0.5 * f3) +
            alpha * alpha * (f0 - 2.5 * f1 + 2 * f2 - 0.5 * f3) +
            alpha * (-0.5 * f0 + 0.5 * f2) +
            f1;
    }


    /** A fast approximation of Cosine using a lookup table and Catmull-Rom cubic spline interpolation. */
    public static final double fastIntCos(double f) 
    {
        double v = f * SIN_MULTIPLIER + SIN_TABLE_LENGTH_DIV_4;
        int conv = (int) v;
        double alpha = v - conv;
        
        int slot1 = conv & SIN_TABLE_LENGTH_MINUS_1;
        int slot0 = (slot1 - 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot2 = (slot1 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        int slot3 = (slot2 + 1) & SIN_TABLE_LENGTH_MINUS_1;
        
        double f0 = sinTable[slot0];
        double f1 = sinTable[slot1];
        double f2 = sinTable[slot2];
        double f3 = sinTable[slot3];
        
        return alpha * alpha * alpha * (-0.5 * f0 + 1.5 * f1 - 1.5 * f2 + 0.5 * f3) +
            alpha * alpha * (f0 - 2.5 * f1 + 2 * f2 - 0.5 * f3) +
            alpha * (-0.5 * f0 + 0.5 * f2) +
            f1;
    }



    static 
    {
        for (int i = 0; i < SIN_TABLE_LENGTH; ++i) 
            {
                sinTable[i] = (double)Math.sin((double) i * Math.PI * (2.0 / SIN_TABLE_LENGTH));
            }
                
        for (int i = 0; i < 65536; ++i) 
            {
                sqrtTable[i] = Math.sqrt(i / 65536.0);
            }
    }


    public static void main(String[] args)
    {
        double sum = 0;
        for(double i = 0; i < 10000; i+= 0.000005)
            {
                sum += Utility.fastSin(i);
            }
        System.err.println(sum);
    }

    /*
      public static void main(String[] args)
      {
      double sum = 0;
      for(double i = 0; i < 1; i+= 0.0000000002)
      {
      //sum += Utility.fastSqrt(i);
      sum += Math.sqrt(i);
      }
      System.err.println(sum);
      }
    */


    /*
      static public function pow2_v4( p:Number ):Number
      {   
      // idea from http://nic.schraudolph.org/pubs/Schraudolph99.pdf
      //var magic1:Number = (1048576/Math.LN2) / 1.442695040;
      //var magic2:Number = (1072693248 - 60801)
      // p * magic1 + magic2;
             
      fastmem.fastSetI32(p * 1048576.0006461141 + 1072632447,4);
      return fastmem.fastGetDouble(0);
      }
 
      static public function pow2_v4Initialize():void
      {
      fastmem.fastSetI32(0,0);    
      }
    */

    /*
      public static void main(String[] args) 
      {
      double d = 0;
      long time = System.nanoTime();
      for (int j = 0; j < 1000; j++) 
      {
      for (double i = 0; i < 22500; i += 0.323236123) 
      {
      //d += Math.pow(0.06234, i);
      //d += Utility.fastpow(0.06234, i);
      //d += Utility.fasterpow(0.06234,i);
      d += Utility.hybridpow(0.06234, i);
      }
      }
      long end = System.nanoTime();
      System.out.println((end - time)/1000000);
      }
    */

}
        

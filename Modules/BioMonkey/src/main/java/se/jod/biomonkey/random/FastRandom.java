/*
 * Copyright (c) 2011, Andreas Olofsson
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 * Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package se.jod.biomonkey.random;

/**
 * This class uses George Marsaglia's XORShift algorithm to generate 
 * a sequence of pseudo-random numbers from a seed. The period of this sequence 
 * is 2^64 - 1, provided the numbers in the algorithm aren't changed. 0 is
 * not part of the sequence. The random numbers are of "medium quality" 
 * (not cryptographically secure).
 * <br/><br/>
 * Whitepaper: http://www.jstatsoft.org/v08/i14/paper
 * <br/><br/>
 * Abstract: <em>Description of a class of simple, extremely fast random number 
 * generators (RNGs) with periods 2^k −1 for k = 32, 64, 96, 128, 160, 192. 
 * These RNGs seem to pass tests of randomness very well.</em>
 *
 * @author Andreas
 */
public class FastRandom {
    //Values used when generating floats and doubles.
    protected static final int fB = (1 << 24) - 1;
    protected static final float fMult = 1/(float)fB;
    
    protected static final long dB = (1L << 53) - 1L;
    protected static final double dMult = 1/(double)dB;
    //The current value.
    protected long x;
    
    /**
     * Creates a new FastRandom instance using the current system
     * nanotime as seed.
     */
    public FastRandom(){
        x = System.nanoTime();
    }
    /**
     * Creates a new FastRandom instance using the provided long value
     * as seed.
     * 
     * @param seed The long value used to seed the generator.
     */
    public FastRandom(long seed){
        x = seed;
    }
    
    /**
     * XORShift algorithm, a = 21, b = 35, c = 4.
     * 
     * @return The next long.
     */
    protected long next()
    {
        x ^= (x << 21); 
        x ^= (x >>> 35);
        x ^= (x << 4);
        return x;
    }
    
    /**
     * Re-seeds the generator with System.nanoTime().
     */
    public void reSeed()
    {
        reSeed(System.nanoTime());
    }
    /**
     * Re-seeds the generator with a new long value.
     * 
     * @param seed The long value used to seed the generator.
     */
    public void reSeed(long seed){
        x = seed;
    }
    
    /**
     * Long:
     * 
     * @return A signed long integer (0 not included).
     */
    public long nextLong()
    {
        return (next() & Long.MAX_VALUE);
    }
    
    /**
     * Integer:
     * 
     * @param n The upper limit.
     * @return A random integer in the range [0,n].
     */
    public int nextInt(int n)
    {
        assert(n >= 0);
        
        int used = n;
        //This is from Paged Geometry.
	used |= used >> 1;
	used |= used >> 2;
	used |= used >> 4;
	used |= used >> 8;
	used |= used >> 16;
	
	// Draw numbers and mask until one is found in [0,n]
	long i;
	do {
            i = nextLong() & used;
        } while( i > n );
	return (int)i;
    }
    
    /**
     * Boolean: 
     * 
     * @return A random boolean (true or false).
     */
    public boolean nextBoolean()
    {
        return ((next() & 0x1) != 0);
    }
    
    /**
     * Float:
     * 
     * @return A random float in the range (0,1]
     */
    public float unitRandom()
    {
        return (next() & fB) * fMult;
    }
    
    /**
     * Double:
     * 
     * @return A random double in the range (0,1]
     */
    public double unitRandomD()
    {
        return (next() & dB) * dMult;
    }
    
}//FastRandom

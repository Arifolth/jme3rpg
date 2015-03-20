/*
 * Copyright (c) 2012, Andreas Olofsson
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
 * Normal distribution random numbers using the Ziggurat algorithm.
 * Modified to work with FastRandom instead of java Random.
 * 
 * NOTE: Beta testing
 * 
 * @author Andreas
 * 
 * Original copyright notice from the Ziggurat code. Note
 * it's not the same Andreas.
 * 
 * @author Andreas Schouten (andreas@schouten-blog.de)
 * This class implements the ziggurat algorithm for normal 
 * distributed random numbers as provided by George Marsaglia 
 * and Wai Wan Tsang in 2000.
 */
public class RNORGenerator extends FastRandom {

    private int jsr = 123456768;
    private static int[] kn;
    private static double[] wn, fn;
    private static boolean initialized = false;

    public RNORGenerator() {
        super();
        zigset();
    }

    public RNORGenerator(long seed) {
        super(seed);
        zigset();
    }

    public double getGaussian() {
        return RNOR();
    }

    private double RNOR() {
        int hz = SHR3();
        int iz = hz & 127;
        return (Math.abs(hz) < kn[iz]) ? hz * wn[iz] : nfix(hz, iz);
    }

    private int SHR3() {
        int jz = jsr;
        int jzr = jsr;
        jzr ^= (jzr << 13);
        jzr ^= (jzr >>> 17);
        jzr ^= (jzr << 5);
        jsr = jzr;
        return jz + jzr;
    }

    private double nfix(int hz, int iz) {
        double r = 3.442619855899;     /* The start of the right tail */
        double r1 = 1 / r;
        double x, y;
        for (;;) {
            x = hz * wn[iz];      /* iz==0, handles the base strip */
            if (iz == 0) {
                do {
                    x = (-Math.log(UNI()) * r1);
                    y = -Math.log(UNI());
                } while (y + y < x * x);
                return (hz > 0) ? r + x : -r - x;
            }
            /* iz>0, handle the wedges of other strips */
            if (fn[iz] + UNI() * (fn[iz - 1] - fn[iz]) < Math.exp(-.5 * x * x)) {
                return x;
            }

            /* initiate, try to exit for(;;) for loop*/
            hz = SHR3();
            iz = hz & 127;

            if (Math.abs(hz) < kn[iz]) {
                return (hz * wn[iz]);
            }
        }

    }

    private double UNI() {
        return 0.5 * (1 + (double) SHR3() / (double) Integer.MIN_VALUE);
    }

    private synchronized void zigset() {
        if (initialized) {
            return;
        }
        initialized = true;

        wn = new double[128];
        fn = new double[128];
        kn = new int[128];

        double m1 = 2147483648.0;
        double dn = 3.442619855899, tn = dn, vn = 9.91256303526217e-3, q;
        int i;


        /* Set up tables for RNOR */
        q = vn / Math.exp(-.5 * dn * dn);
        kn[0] = (int) ((dn / q) * m1);
        kn[1] = 0;

        wn[0] = q / m1;
        wn[127] = dn / m1;

        fn[0] = 1.;
        fn[127] = Math.exp(-.5 * dn * dn);

        for (i = 126; i >= 1; i--) {
            dn = Math.sqrt(-2. * Math.log(vn / dn + Math.exp(-.5 * dn * dn)));
            kn[i + 1] = (int) ((dn / tn) * m1);
            tn = dn;
            fn[i] = Math.exp(-.5 * dn * dn);
            wn[i] = dn / m1;
        }
    }
}

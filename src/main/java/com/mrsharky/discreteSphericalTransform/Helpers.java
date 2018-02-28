/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.discreteSphericalTransform;

import org.apache.commons.math3.complex.Complex;

/**
 * 
 * @author Julien Pierret
 */
public class Helpers {
    public static SphericalHarmonic GenerateRandomSphericalHarmonic(int q) throws Exception {
        SphericalHarmonic sh = new SphericalHarmonic(q);
        for (int k = 0; k <= q; k++) {
            for (int l = 0; l <= k; l++) {

                double realSign = (Math.random() > 0.75 ? -1.0 : 1.0)*2;
                double imagSign = (Math.random() > 0.75 ? -1.0 : 1.0)*2;
                double real = Math.random()*realSign;
                double imag = Math.random()*imagSign;

                // No i's on l = 0
                if (l == 0) {
                    imag = 0.0;
                }
                sh.SetHarmonic(k, l, new Complex(real, imag));
            }
        }
        return sh;
    }
}

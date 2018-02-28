/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.discreteSphericalTransform.tests;

import com.google.common.base.Stopwatch;
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import static com.mrsharky.discreteSphericalTransform.Helpers.GenerateRandomSphericalHarmonic;
import org.apache.commons.math3.complex.Complex;

/**
 * 
 * @author Julien Pierret
 */
public class HighRandomHarmonicTest {

    
    
    
    public static void main(String[] args) throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        
        int q = 100;
        int latCount = q + 1;
        int lonCount = latCount*2;
        System.out.println("Performing harmonic test to Q=" + q);
        
        SphericalHarmonic sh = GenerateRandomSphericalHarmonic(q);
        
        Stopwatch timer = new Stopwatch();
        timer.start();  
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        invShTrans.SetMultiThreading(true);
        
        System.out.println("Performing Inv DST Calculation");
        double[][] spatial = invShTrans.ProcessGaussianDoubleArray(latCount, lonCount);

        System.out.println("Performing DST Calculation");
        DiscreteSphericalTransform dst2 = new DiscreteSphericalTransform(spatial, q);
        timer.stop();

        System.out.println("Performed the calculation in: " + timer);

        SphericalHarmonic sh2 = dst2.GetSpectra();
 
        for (int k=0; k <= q; k++) {
            for (int l=0; l <= k; l++) {
                Complex original = sh.GetHarmonic(k, l);
                Complex rebuilt = sh2.GetHarmonic(k, l);
                Complex diff = original.add(rebuilt.multiply(-1.0));

                if (Math.abs(diff.getReal()) > 10e-12 || Math.abs(diff.getImaginary()) > 10e-12) {
                    System.out.println("K=" + k + ", L=" + l);
                    System.out.println("Original:" + original + ", Rebuilt: " + rebuilt + ", Diff" + diff);
                    throw new Exception("Mismatch");
                }
            }
        }
        
        System.out.println("Process ran successfully!");
        
    }
    
}

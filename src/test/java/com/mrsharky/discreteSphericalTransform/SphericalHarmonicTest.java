/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import static com.mrsharky.discreteSphericalTransform.SphericalHarmonicYTest.GenerateRandomSphericalHarmonic;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import org.apache.commons.math3.complex.Complex;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonicTest {
    
    public SphericalHarmonicTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testSphericalMult() throws Exception {
        int latPoints = 10;
        int lonPoints = 20;
        int q = 4;
        
        // Harmonic 1
        SphericalHarmonic sh1 = GenerateRandomSphericalHarmonic(q-1);
        InvDiscreteSphericalTransform invShTrans1 = new InvDiscreteSphericalTransform(sh1);
        double[][] spatial1 = invShTrans1.ProcessGaussianDoubleArray(latPoints, lonPoints);
           
        // Harmonic 2
        SphericalHarmonic sh2 = GenerateRandomSphericalHarmonic(q+1);
        InvDiscreteSphericalTransform invShTrans2 = new InvDiscreteSphericalTransform(sh2);
        double[][] spatial2 = invShTrans2.ProcessGaussianDoubleArray(latPoints, lonPoints);
        
        // Baseline
        double[][] baseline = DoubleArray.Multiply(spatial1, spatial2);
        int newQ = (q-1) + (q+1);
        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(baseline, newQ, true);
        dst.GetSpectra().PrintHarmonic();
        
        // Multiplication
        SphericalHarmonic sh3 = sh1.Multiply(sh2);
        sh3.PrintHarmonic();
        
        // Compare harmonics
        Complex[] multInSpatial = dst.GetSpectra().GetHalfCompressedSpectra();
        Complex[] multInSpectral = sh3.GetHalfCompressedSpectra();
        
        // Check the new harmonic has the correct length
        if (multInSpatial.length != multInSpectral.length) {
            Assert.fail("Harmonic lengths between spherical harmonics multiplied in spatial vs spectral should be the same");
        }
        
        // Check the harmonic values are nearly identical
        for (int i = 0; i < multInSpatial.length; i++) {
            Complex diff = multInSpatial[i].add(multInSpectral[i].multiply(-1.0));
            if (Math.abs(diff.getReal()) > 1e-12 || Math.abs(diff.getImaginary()) > 1e-12) {
                Assert.fail("Harmonic values differ by too much");
            }
        }
        
        // Check the spatial values are nearly identical
        InvDiscreteSphericalTransform invShTrans3 = new InvDiscreteSphericalTransform(sh3);
        double[][] spatial3 = invShTrans3.ProcessGaussianDoubleArray(latPoints, lonPoints);
        double[][] multDiff = DoubleArray.Add(baseline, DoubleArray.Multiply(spatial3, -1.0));
        
        for (int i = 0; i < multDiff.length; i++) {
            for (int j = 0; j < multDiff[0].length; j++) {
                if (Math.abs(multDiff[i][j]) > 1e-12 ) {
                    Assert.fail("Spatial values differ by too much");
                }
            }
        }
    }
    

    /**
     * Test of CalculateNumPointsFromQ method, of class SphericalHarmonic.
     */
    @Test
    public void testCalculateNumPointsFromQ() {
        
    }

    /**
     * Test of Clone method, of class SphericalHarmonic.
     */
    @Test
    public void testClone() {
        
    }

    /**
     * Test of Multiply method, of class SphericalHarmonic.
     */
    @Test
    public void testMultiply() throws Exception {
       
    }

    /**
     * Test of Conjugate method, of class SphericalHarmonic.
     */
    @Test
    public void testConjugate() throws Exception {
        int q = 5;
        
        
        
        // Test for oscilating sign (l = odd)
        {
            int k = 3;
            int l = 3;
            double value = 2;
            SphericalHarmonic sh = new SphericalHarmonic(q);
            sh.SetHarmonic(k, l, new Complex(value, value));
            
            SphericalHarmonic shConj = sh.Conjugate();
            double conjReal = shConj.GetHarmonic(k, l).getReal();
            
            if (value*-1.0 != conjReal) {
                Assert.fail("Real part of conjugate failed");
            }
        }
        
        // Test for no oscilation (l = even)
        {
            int k = 3;
            int l = 2;
            double value = 2;
            SphericalHarmonic sh = new SphericalHarmonic(q);
            sh.SetHarmonic(k, l, new Complex(value, value));
            
            SphericalHarmonic shConj = sh.Conjugate();
            double conjReal = shConj.GetHarmonic(k, l).getReal();
            
            if (value != conjReal) {
                Assert.fail("Real part of conjugate failed");
            }
        }
    }

    /**
     * Test of SetHalfCompressedSpectra method, of class SphericalHarmonic.
     */
    @Test
    public void testSetHalfCompressedSpectra() throws Exception {
        
    }

    /**
     * Test of GetQ method, of class SphericalHarmonic.
     */
    @Test
    public void testGetQ() {
       
    }

    /**
     * Test of GetFullCompressedSpectra method, of class SphericalHarmonic.
     */
    @Test
    public void testGetFullCompressedSpectra() throws Exception {
       
    }

    /**
     * Test of GetFullSpectral method, of class SphericalHarmonic.
     */
    @Test
    public void testGetFullSpectral() throws Exception {
        
    }

    /**
     * Test of GetHalfCompressedSpectra method, of class SphericalHarmonic.
     */
    @Test
    public void testGetHalfCompressedSpectra() {
        
    }

    /**
     * Test of GetHarmonic method, of class SphericalHarmonic.
     */
    @Test
    public void testGetHarmonic() throws Exception {
       
    }

    /**
     * Test of SetHarmonic method, of class SphericalHarmonic.
     */
    @Test
    public void testSetHarmonic() throws Exception {
       
    }

    /**
     * Test of main method, of class SphericalHarmonic.
     */
    @Test
    public void testMain() throws Exception {
        
    }
    
}

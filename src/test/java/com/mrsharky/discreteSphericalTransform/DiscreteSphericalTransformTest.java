/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.GenerateGaussianGriddedPoints;
import com.mrsharky.helpers.LegendreGausWeights;
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
public class DiscreteSphericalTransformTest {
    
    public DiscreteSphericalTransformTest() {
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

    public static double[][] GenerateL2M1(int lmax, int nlat, int nphi) throws Exception {
        double[][] data = new double[1][1];

        LegendreGausWeights lgw = new LegendreGausWeights(nlat,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED

        double[] phi2 = DoubleArray.Cos(legZerosRad);                            // TESTED

        data = new double[nlat][nphi];

        // generate some gridded data:
        for (int ip=0; ip<nphi; ip++) {		// loop on longitude
            double phi = (2.*Math.PI/(nphi)) * ip;
            double cos_phi = Math.cos(phi);
            for (int it=0; it<nlat; it++) {		// loop on latitude
                //double cos_theta = shtns->ct[it];
                double cos_theta = phi2[it];
                double sin_theta = Math.sqrt(1.0 - cos_theta*cos_theta);
                data[it][ip] = cos_phi*cos_theta*sin_theta;
                //Sh[ip*nlat + it] = cos_phi*cos_theta*sin_theta;	// this is an m=1, l=2 harmonic [mres==1 is thus required]
            }
        }
        return data;
    }
    
    /**
     * Spectral to spatial and back
     * @throws Exception 
     */
    @Test
    public void testInvTransform() throws Exception {
        int q = 5;
        SphericalHarmonic sh = new SphericalHarmonic(q);
        sh.SetHarmonic(2, 1, new Complex(1.0, 0.0));
        
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        double[][] spatial = invShTrans.ProcessGaussianDoubleArray(32, 10);

        DiscreteSphericalTransform dst2 = new DiscreteSphericalTransform(spatial, q, true);

        SphericalHarmonic sh2 = dst2.GetSpectra();
        
        Complex original = sh.GetHarmonic(2, 1);
        Complex rebuilt = sh2.GetHarmonic(2, 1);

        Complex diff = original.add(rebuilt.multiply(-1.0));

        if (Math.abs(diff.getReal()) > 10e-12 || Math.abs(diff.getImaginary()) > 10e-12) {
            Assert.fail("Error too large between originala and rebuilt");
        }
        
        System.out.println("Original");
        ComplexArray.Print(sh.GetFullSpectral());
        
        System.out.println("Rebuilt");
        ComplexArray.Print(sh2.GetFullSpectral());
        
        {
            GenerateGaussianGriddedPoints gggp = new GenerateGaussianGriddedPoints(32, 10);
            double[] lat = gggp.GetLat();
            double[] lon = gggp.GetLon();
            
            double[] spatial2 = invShTrans.ProcessPoints(lat, lon);
            
            SphericalHarmonicY Y = new SphericalHarmonicY (lat, lon, q);
            Complex[] harmonics = Y.GetHarmonic(2, 1);
            
            ComplexArray.Print(harmonics);
            
        }
        
        
    }
    
    /**
     * Test Spatial to spectral and back
     * @throws Exception 
     */
    @Test
    public void testTransform() throws Exception {
        int lmax = 3;		// maximum degree of spherical harmonics
        int nlat = 5;		// number of points in the latitude direction  (constraint: nlat >= lmax+1)
        int nphi = 5;   
        double[][] data = GenerateL2M1(lmax, nlat, nphi);

        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(data, lmax, true);
        InvDiscreteSphericalTransform idst = new InvDiscreteSphericalTransform(dst.GetSpectra());
        double[][] rebuilt = idst.ProcessGaussianDoubleArray(dst.GetM(), dst.GetN());

        double[][] diff = DoubleArray.Add(data, DoubleArray.Multiply(rebuilt, -1.0));
        diff = DoubleArray.Power(diff, 2.0);
        double error = DoubleArray.SumArray(diff);

        if (error > 10e-12) {
            Assert.fail("Error too large between originala and rebuilt");
        }  
    }
    
}

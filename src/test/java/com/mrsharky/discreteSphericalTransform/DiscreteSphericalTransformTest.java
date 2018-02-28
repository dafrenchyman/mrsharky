/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import static com.mrsharky.discreteSphericalTransform.Helpers.GenerateRandomSphericalHarmonic;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.GenerateGaussianGriddedPoints;
import com.mrsharky.helpers.LegendreGausWeights;
import com.mrsharky.helpers.Utilities;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
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
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        int q = 5;
        int k = q-1;
        int l = q-1;
        int latCount = q + 1;
        int lonCount = latCount*2;
        Complex value = new Complex(2.0, 3.0);
        SphericalHarmonic sh = new SphericalHarmonic(q);
        sh.SetHarmonic(k, l, value);
        
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        double[][] spatial = invShTrans.ProcessGaussianDoubleArray(latCount, lonCount);

        DiscreteSphericalTransform dst2 = new DiscreteSphericalTransform(spatial, q, true);

        SphericalHarmonic sh2 = dst2.GetSpectra();
        
        // The particular harmonic is good
        {
            Complex original = sh.GetHarmonic(k, l);
            Complex rebuilt = sh2.GetHarmonic(k, l);

            Complex diff = original.add(rebuilt.multiply(-1.0));

            System.out.println("Original:" + original + ", Rebuilt: " + rebuilt + ", Diff" + diff);
            if (Math.abs(diff.getReal()) > 10e-12 || Math.abs(diff.getImaginary()) > 10e-12) {
                Assert.fail("Error too large between originala and rebuilt");
            }
        }
        
        // Check all the other harmonics are zero
        {
            for (int K=0; K <= q; K++) {
                for (int L=0; L <= K; L++) {
                    Complex original = sh.GetHarmonic(K, L);
                    Complex rebuilt = sh2.GetHarmonic(K, L);
                    Complex diff = original.add(rebuilt.multiply(-1.0));

                    if (Math.abs(diff.getReal()) > 10e-12 || Math.abs(diff.getImaginary()) > 10e-12) {
                        System.out.println("K=" + K + ", L=" + L);
                        System.out.println("Original:" + original + ", Rebuilt: " + rebuilt + ", Diff" + diff);
                        Assert.fail("Error too large between originala and rebuilt");
                    }
                }
            }  
        }
        
        if (q <= 10 && false) {
            System.out.println("Original");
            ComplexArray.Print(sh.GetFullSpectral());

            System.out.println("Rebuilt");
            ComplexArray.Print(sh2.GetFullSpectral());
        }
        
        if (false) {
            GenerateGaussianGriddedPoints gggp = new GenerateGaussianGriddedPoints(latCount, lonCount);
            double[] lat = gggp.GetLat();
            double[] lon = gggp.GetLon();
            
            double[] spatial2 = invShTrans.ProcessPoints(lat, lon);
            
            SphericalHarmonicY Y = new SphericalHarmonicY (lat, lon, q);
            Complex[] harmonics = Y.GetHarmonic(k, l);
            
            ComplexArray.Print(harmonics);
            
        }  
    }
    
    
    @Test
    public void testGridboxVsCoordinate() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        
        // Generate Spatial data
        int q = 15;
        int latCount = q + 1;
        int lonCount = latCount*2;     
        SphericalHarmonic sh = GenerateRandomSphericalHarmonic(q);
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        
        // Compare different lats from different methods
        AreasForGrid areasForGrid = new AreasForGrid(latCount, lonCount, 1.0);
        double[] lats_area = Utilities.LatitudeToRadians(areasForGrid.GetLatitude());
        double[] lons_area = Utilities.LatitudeToRadians(areasForGrid.GetLongitude());
        
        double[] lats_dst = DiscreteSphericalTransform.GetLatitudeCoordinates(latCount);
        double[] lons_dst = DiscreteSphericalTransform.GetLongitudeCoordinates(lonCount);
        
        // Check results
        for (int i = 0; i < lats_area.length; i++) {
            double ratio = lats_area[i] / lats_dst[i];
            if (Math.abs(1.0 - ratio) >= 0.00001) {
                throw new Exception("latitude values do not match");
            }
        }
        
        for (int i = 0; i < lons_area.length; i++) {
            double ratio = lons_area[i] / lons_dst[i];
            if (Math.abs(1.0 - ratio) >= 0.00001) {
                throw new Exception("longitude values do not match");
            }
        }
        
        
        Pair<double[], double[]> invDistPairs = InvDiscreteSphericalTransform.GenerateCoordinatePoints(latCount, lonCount);

        
    }
    
    /**
     * Spectral to spatial and back
     * @throws Exception 
     */
    @Test
    public void testTransformRandom() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        
        int q = 15;
        int latCount = q + 1;
        int lonCount = latCount*2;
        System.out.println("Performing random harmonic test to Q=" + q);
        
        SphericalHarmonic sh = GenerateRandomSphericalHarmonic(q);
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        invShTrans.SetMultiThreading(true);
        double[][] spatial = invShTrans.ProcessGaussianDoubleArray(latCount, lonCount);

        DiscreteSphericalTransform dst2 = new DiscreteSphericalTransform(spatial, q);

        SphericalHarmonic sh2 = dst2.GetSpectra();
 
        for (int k=0; k <= q; k++) {
            for (int l=0; l <= k; l++) {
                Complex original = sh.GetHarmonic(k, l);
                Complex rebuilt = sh2.GetHarmonic(k, l);
                Complex diff = original.add(rebuilt.multiply(-1.0));

                if (Math.abs(diff.getReal()) > 10e-12 || Math.abs(diff.getImaginary()) > 10e-12) {
                    System.out.println("K=" + k + ", L=" + l);
                    System.out.println("Original:" + original + ", Rebuilt: " + rebuilt + ", Diff" + diff);
                    Assert.fail("Error too large between originala and rebuilt");
                }
            }
        }  
    }
    
    
    
    
    
    /**
     * Test Spatial to spectral and back
     * @throws Exception 
     */
    @Test
    public void testTransform() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
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

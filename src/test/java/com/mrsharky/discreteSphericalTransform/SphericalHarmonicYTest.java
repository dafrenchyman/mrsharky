/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import static com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform.GenerateL2M1;
import static com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform.GenerateSinCosData;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.GenerateGaussianGriddedPoints;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.javatuples.Pair;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonicYTest {
    
    public SphericalHarmonicYTest() {
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

    /**
     * Test of GetData method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetData() {
       
    }

    /**
     * Test of Size method, of class SphericalHarmonicY.
     */
    @Test
    public void testSize() {
      
    }

    /**
     * Test of GetQ method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetQ() {
       
    }

    /**
     * Test of Clone method, of class SphericalHarmonicY.
     */
    @Test
    public void testClone() throws Exception {
     
    }
    
    /**
     * Test of Conjugate method, of class SphericalHarmonicY.
     */
    @Test
    public void testConjugate() throws Exception {
        
        
        
        AreasForGrid areasForGrid = new AreasForGrid(94,192,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        double totalArea = DoubleArray.SumArray(areaFraction);
        System.out.println(totalArea);
        
        int q = 5;
        int k = 3;
        int l = 3;
        double value = 2;
        SphericalHarmonic sh = new SphericalHarmonic(q);
        sh.SetHarmonic(k, l, new Complex(value, value));
        SphericalHarmonic[] sphericals = new SphericalHarmonic[2];
        sphericals[0] = sh;
        sphericals[1] = sh.Clone();
        
        SphericalHarmonicY shy = new SphericalHarmonicY(sphericals);
        
        SphericalHarmonicY shyConj = shy.Conjugate();
        
        Complex[] newValue = shyConj.GetHarmonic(k, l);
        
        System.out.println();
        
    }
    
    
    @Test
    public void testFirstHarmonic1() throws Exception {
        System.out.println("Starting Program");
        int latPoints = 94;
        int lonPoints = 192;
        double[][] DATA = GenerateSinCosData(latPoints, lonPoints);
        
        int q = 25;	
        
        double[][] randomStdDev = new double[latPoints][lonPoints];
        for (int i = 0; i < latPoints; i++) {
            for (int j = 0; j < lonPoints; j++) {
                randomStdDev[i][j] = Math.random();
            }
        }
        
        AreasForGrid areasForGrid = new AreasForGrid(latPoints, lonPoints,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));

        
        //int Q = 5;
        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(DATA, q, true);
        InvDiscreteSphericalTransform invShTrans1 = new InvDiscreteSphericalTransform(dst.GetSpectra());
        double[][] spatial1 = invShTrans1.ProcessGaussianDoubleArray(latPoints, lonPoints);
           
        double baseline = DoubleArray.SumArray(DoubleArray.Multiply(DoubleArray.Multiply(spatial1, areaFraction), randomStdDev));
        
        SphericalHarmonic newHarm = new SphericalHarmonic(0);
        newHarm.SetHarmonic(0, 0, dst.GetSpectra().GetHarmonic(0, 0));

        InvDiscreteSphericalTransform invShTrans2 = new InvDiscreteSphericalTransform(newHarm);
        double[][] rebuiltSpatial = invShTrans2.ProcessGaussianDoubleArray(latPoints, lonPoints);

        double rebuilt = DoubleArray.SumArray(DoubleArray.Multiply(DoubleArray.Multiply(rebuiltSpatial, areaFraction), randomStdDev));

        System.out.println("" + baseline + "\t" + rebuilt);
        
        

    }
    
    @Test
    public void testFirstHarmonic2() throws Exception {
        int q = 10;

        int obs = 1;
        
        SphericalHarmonic[] sphericals = new SphericalHarmonic[obs];
        
        for (int i = 0; i < obs; i++) {
            SphericalHarmonic sh = new SphericalHarmonic(q);
            for (int k = 0; k <= q; k++) {
                for (int l = 0; l <= k; l++) {

                    double realSign = (Math.random() > 0.75 ? -1.0 : 1.0) * 10;
                    double imagSign = (Math.random() > 0.75 ? -1.0 : 1.0) * 10;
                    double real = Math.random()*realSign;
                    double imag = Math.random()*imagSign;
                    
                    // No i's on l = 0
                    if (l == 0) {
                        imag = 0.0;
                    }
                    sh.SetHarmonic(k, l, new Complex(real, imag));
                }
            }
            sphericals[i] = sh;
        }
        
        // Generate gridpoints
        int latPoints = 94;
        int lonPoints = 192;
        GenerateGaussianGriddedPoints gggp = new GenerateGaussianGriddedPoints(latPoints, lonPoints);
        double[] lat = gggp.GetLat();
        double[] lon = gggp.GetLon();
        
        Map<Pair<Integer, Integer>, double[]> values = new HashMap<Pair<Integer, Integer>, double[]>();
        Map<Integer, double[][]> finalObs = new HashMap<Integer, double[][]>();
        for (int o = 0; o <obs; o++) {
            SphericalHarmonic sh = sphericals[o];
            InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
            double[][] spatial = invShTrans.ProcessGaussianDoubleArray(latPoints, lonPoints);
            finalObs.put(o, spatial);
            
            for (int i = 0; i < latPoints; i++) {
                for (int j = 0; j < lonPoints; j++) {
                    Pair<Integer, Integer> key = Pair.with(i, j);
                    if (!values.containsKey(key)) {
                        values.put(key, new double[obs]);
                    }
                    double[] currValues = values.get(key);
                    currValues[o] = spatial[i][j];
                    values.put(key, currValues);
                }
            }
        }
        
        // Now, generate mean and variance values
        Map<Pair<Integer, Integer>, Double> means = new HashMap<Pair<Integer, Integer>, Double>();
        Map<Pair<Integer, Integer>, Double> vars = new HashMap<Pair<Integer, Integer>, Double>();
        for (Pair<Integer, Integer> key : values.keySet()) {
            double[] currValues = values.get(key);
            DescriptiveStatistics ds = new DescriptiveStatistics(currValues);
            double mean = ds.getMean();
            double var = ds.getVariance();
            means.put(key, mean);
            vars.put(key, var);
        }
        
        // Subtract mean from obs
        boolean useMean = false;
        boolean useVar = false;
        for (int o = 0; o <obs; o++) {
            double[][] spatial = finalObs.get(o);
            for (int i = 0; i < latPoints; i++) {
                for (int j = 0; j < lonPoints; j++) {
                    Pair<Integer, Integer> key = Pair.with(i, j);
                    double mean = 0.0;
                    if (useMean) {
                        mean = means.get(key);
                    }
                    double var = 1.0;
                    if (useVar) {
                        var = vars.get(key);
                    }
                    
                    spatial[i][j] = (spatial[i][j] - mean) / Math.sqrt(var);
                }
            }
        }
        
        AreasForGrid areasForGrid = new AreasForGrid(latPoints, lonPoints,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Convert data from spatial to spectral 
        for (int o = 0; o <obs; o++) {
            double[][] spatial = finalObs.get(o);
            
            // Get the "baseline" value (all Qs)
            double baseline = DoubleArray.SumArray(DoubleArray.Multiply(spatial, areaFraction));
            
            // Convert the data to spectral
            DiscreteSphericalTransform dst = new DiscreteSphericalTransform(spatial, q, true);
            SphericalHarmonic harm = dst.GetSpectra();
            
            // Generate a new harmonic based only from 0,0
            SphericalHarmonic newHarm = new SphericalHarmonic(0);
            //newHarm.SetHarmonic(0, 0, sphericals[o].GetHarmonic(0, 0));
            newHarm.SetHarmonic(0, 0, harm.GetHarmonic(0, 0));
            
            InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(newHarm);
            double[][] rebuiltSpatial = invShTrans.ProcessGaussianDoubleArray(latPoints, lonPoints);
            
            double rebuilt = DoubleArray.SumArray(DoubleArray.Multiply(rebuiltSpatial, areaFraction));
            
            System.out.println("" + baseline + "\t" + rebuilt);
        }
            
        System.out.println();
        
    }
    
    @Test
    public void testFirstHarmonic3() throws Exception {
        int q = 1;
        SphericalHarmonic sh = new SphericalHarmonic(q);
       
        sh.SetHarmonic(0, 0, new Complex(-0.3188681016673894, 0.0));
        sh.SetHarmonic(1, 0, new Complex(-0.3582412638832888, 0.0));
        sh.SetHarmonic(1, 1, new Complex(-0.10221721984461112, -0.02064337562030474));
        //sh.SetHarmonic(0, 0, new Complex(real, imag));
        
        int latPoints = 94;
        int lonPoints = 192;
        AreasForGrid areasForGrid = new AreasForGrid(latPoints, lonPoints,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
      
        InvDiscreteSphericalTransform invShTrans1 = new InvDiscreteSphericalTransform(sh);
        double[][] spatial1 = invShTrans1.ProcessGaussianDoubleArray(latPoints, lonPoints);   
        double baseline = DoubleArray.SumArray(DoubleArray.Multiply(spatial1, areaFraction));
        
        SphericalHarmonic newHarm = new SphericalHarmonic(0);
        newHarm.SetHarmonic(0, 0, sh.GetHarmonic(0, 0));

        InvDiscreteSphericalTransform invShTrans2 = new InvDiscreteSphericalTransform(newHarm);
        double[][] rebuiltSpatial = invShTrans2.ProcessGaussianDoubleArray(latPoints, lonPoints);

        double rebuilt = DoubleArray.SumArray(DoubleArray.Multiply(rebuiltSpatial, areaFraction));

        System.out.println("" + baseline + "\t" + rebuilt);
        
    }
    

    /**
     * Test of SetHalfCompressedSpectra method, of class SphericalHarmonicY.
     */
    @Test
    public void testSetHalfCompressedSpectra() throws Exception {
        
    }

    /**
     * Test of GetFullCompressedSpectra method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetFullCompressedSpectra() throws Exception {
        
    }

    /**
     * Test of GetFullSpectral method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetFullSpectral() throws Exception {
        
    }

    /**
     * Test of GetHalfCompressedSpectra method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetHalfCompressedSpectra() {
        
    }

    /**
     * Test of GetHarmonic method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetHarmonic_3args() throws Exception {
        
    }

    /**
     * Test of GetHarmonic method, of class SphericalHarmonicY.
     */
    @Test
    public void testGetHarmonic_int_int() throws Exception {
        
    }

    /**
     * Test of SetHarmonic method, of class SphericalHarmonicY.
     */
    @Test
    public void testSetHarmonic() throws Exception {
       
    }

    /**
     * Test of main method, of class SphericalHarmonicY.
     */
    @Test
    public void testMain() throws Exception {
        
    }
    
}

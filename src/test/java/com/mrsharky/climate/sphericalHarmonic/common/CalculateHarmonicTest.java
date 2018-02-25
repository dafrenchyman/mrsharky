/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.common;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import static com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec.CutoffVarExplained;
import com.mrsharky.dataAnalysis.PcaCovJBlas;
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLatitudeCoordinates;
import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLongitudeCoordinates;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import static com.mrsharky.helpers.Utilities.RadiansToLongitude;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.RadiansToLatitude;
import com.mrsharky.stations.netcdf.AngellKorshoverNetwork;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.jblas.ComplexDoubleMatrix;
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
public class CalculateHarmonicTest {
    
    public CalculateHarmonicTest() {
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

    private static Complex GenerateRandomHarmonic(int l) {
        double realSign = (Math.random() > 0.75 ? -1.0 : 1.0);
        double imagSign = (Math.random() > 0.75 ? -1.0 : 1.0);
        double real = Math.random()*realSign;
        double imag = Math.random()*imagSign;
        // No i's on l = 0
        if (l == 0) {
            imag = 0.0;
        }
        return new Complex(real, imag);
    }
    
    @Test
    public void testNonHarmonicFriendlyPoints() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        int q = 7;
        int latNum = 32;
        int lonNum = 10;
        
        AreasForGrid areasForGrid = new AreasForGrid(latNum, lonNum, 1.0);
        double[] preLat = Utilities.LatitudeToRadians(areasForGrid.GetLatitude());
        double[] preLon = Utilities.LongitudeToRadians(areasForGrid.GetLongitude());
        
        double[] lat = new double[latNum * lonNum];
        double[] lon = new double[latNum * lonNum];
        
        {
            int counter = 0;
            for (int i = 0; i < preLat.length; i++) {
                for (int j = 0; j < preLon.length; j++) {
                    lat[counter] = preLat[i];
                    lon[counter] = preLon[j];
                    counter++;
                }
            }
        }
        
        SphericalHarmonic sh = new SphericalHarmonic(q);
        double originalFirstHarmValue = 20.0;
        sh.SetHarmonic(0, 0, new Complex(originalFirstHarmValue, 0.0));
        for (int k = 1; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                sh.SetHarmonic(k, l, GenerateRandomHarmonic(l));
            }
        }
        
        InvDiscreteSphericalTransform invSh = new InvDiscreteSphericalTransform(sh);
        double[][] spatial = invSh.ProcessGaussianDoubleArray(latNum, lonNum);
        double[] spatialVec = new double[latNum*lonNum];
        int counter = 0;
        for (int i = 0; i < spatial.length; i++) {
            for (int j = 0; j < spatial[0].length; j++) {
                spatial[i][j] = spatial[i][j] * Math.random();
                spatialVec[counter] = spatial[i][j];
                counter++;
            }
        }
        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(spatial, q, true);
        sh = dst.GetSpectra();
        
        // Generate PCA
        SphericalHarmonic[] timeseries = new SphericalHarmonic[]{ sh };
        Triplet<Complex[][], Complex[], double[]> eigens = generatePca(timeseries);
        Complex[][] eigenVectors = eigens.getValue0();
        Complex[] eigenValues = eigens.getValue1();
               
        CalculateHarmonic harmonic = new CalculateHarmonic(q, false, eigenVectors, eigenValues);
        SphericalHarmonic shRebuilt = new SphericalHarmonic(q);
        for (int k = 0; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                Pair<Complex, double[]> values = harmonic.Process(k, l, lat, lon, spatialVec);
                Complex S_kl = values.getValue0();
                double[] weights = values.getValue1();
                shRebuilt.SetHarmonic(k, l, S_kl);
            }
        }
        
        // Make sure first harmonic is within 10% of original value
        double originalFirstHarmonicValue = sh.GetHarmonic(0, 0).getReal();
        double rebuiltFirstHarmonicValue  = shRebuilt.GetHarmonic(0, 0).getReal();
        
        double ratio = rebuiltFirstHarmonicValue/originalFirstHarmonicValue;
        if (Math.abs(ratio-1.0) > 0.30 ) {
            Assert.fail("Error too large between originala and rebuilt");
        }
        
        // Get the area
        double[] gauslat = RadiansToLatitude(DoubleArray.Add(GetLatitudeCoordinates(latNum), -(Math.PI/2.0)));
        double[] gauslon = RadiansToLongitude(DoubleArray.Add(GetLongitudeCoordinates(lonNum), - Math.PI));
        AreasForGrid gausAreasForGrid = new AreasForGrid(gauslat, gauslon, 1.0);
        double[][] areaFraction = DoubleArray.Multiply(gausAreasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Original Area
        InvDiscreteSphericalTransform invOrigSh = new InvDiscreteSphericalTransform(sh);
        double[][] orig = invOrigSh.ProcessGaussianDoubleArray(latNum, lonNum);
        double origValue = DoubleArray.SumArray(DoubleArray.Multiply(orig, areaFraction));
        
        // Rebuilt Area
        InvDiscreteSphericalTransform invShRebuilt = new InvDiscreteSphericalTransform(shRebuilt);
        double[][] rebuilt = invShRebuilt.ProcessGaussianDoubleArray(latNum, lonNum);
        double value = DoubleArray.SumArray(DoubleArray.Multiply(rebuilt, areaFraction));
        
        System.out.println("Original: " + origValue + ", Rebuilt: " + value);
    }
    
    @Test
    public void testIncreasingFirstHarmonic() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        int q = 7;
        int latNum = 32;
        int lonNum = 10;
        int timePoints = 30;
        
        // Get Lat/Lon & gridbox areas
        double[] lat = RadiansToLatitude(DoubleArray.Add(GetLatitudeCoordinates(latNum), -(Math.PI/2.0)));
        double[] lon = RadiansToLongitude(DoubleArray.Add(GetLongitudeCoordinates(lonNum), - Math.PI));
        AreasForGrid areasForGrid = new AreasForGrid(lat, lon, 1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Generate the baseline
        SphericalHarmonic[] timeseries = new SphericalHarmonic[timePoints];
        double[] origAverage = new double[timePoints];
        for (int i = 0; i < timePoints; i++) {
            SphericalHarmonic sh = new SphericalHarmonic(q);
            double originalFirstHarmValue = 20.0 + 1.0*i*Math.abs(Math.random());
            sh.SetHarmonic(0, 0, new Complex(originalFirstHarmValue, 0.0));
            for (int k = 1; k <= q; k++) {
                for (int l = 0; l <= k; l++) {
                    sh.SetHarmonic(k, l, GenerateRandomHarmonic(l));
                }
            }
            timeseries[i] = sh;
            
            InvDiscreteSphericalTransform invSh = new InvDiscreteSphericalTransform(timeseries[i]);
            double[][] rebuilt = invSh.ProcessGaussianDoubleArray(latNum, lonNum);
            double value = DoubleArray.SumArray(DoubleArray.Multiply(rebuilt, areaFraction));
            origAverage[i] = value;
        }
        
        // Generate PCA
        Triplet<Complex[][], Complex[], double[]> eigens = generatePca(timeseries);
        Complex[][] eigenVectors = eigens.getValue0();
        Complex[] eigenValues = eigens.getValue1();
        
        // Recreate the data
        CalculateHarmonic harmonic = new CalculateHarmonic(q, false, eigenVectors, eigenValues);
        SphericalHarmonic[] rebuiltTimeseries = new SphericalHarmonic[timePoints];
        for (int i = 0; i < timePoints; i++) {
            InvDiscreteSphericalTransform invSh = new InvDiscreteSphericalTransform(timeseries[i]);
            double[] spatial = invSh.ProcessGaussian(latNum, lonNum);
            Pair<double[], double[]> coords = InvDiscreteSphericalTransform.GenerateCoordinatePoints(latNum, lonNum);
            double[] currLat = coords.getValue0();
            double[] currLon = coords.getValue1();
            SphericalHarmonic shRebuilt = new SphericalHarmonic(q);
            for (int k = 0; k <= q; k++) {
                for (int l = 0; l <= k; l++) {
                    Pair<Complex, double[]> values = harmonic.Process(k, l, currLat, currLon, spatial);
                    Complex S_kl = values.getValue0();
                    double[] weights = values.getValue1();
                    shRebuilt.SetHarmonic(k, l, S_kl);
                }
            }
            rebuiltTimeseries[i] = shRebuilt;
        }
        
        // get the rebuilt
        double[] rebuiltAverage = new double[timePoints];
        for (int i = 0; i < timePoints; i++) {
            InvDiscreteSphericalTransform invSh = new InvDiscreteSphericalTransform(rebuiltTimeseries[i]);
            double[][] rebuilt = invSh.ProcessGaussianDoubleArray(latNum, lonNum);
            double value = DoubleArray.SumArray(DoubleArray.Multiply(rebuilt, areaFraction));
            rebuiltAverage[i] = value;
        }
        
        DoubleArray.Print(origAverage);
        DoubleArray.Print(rebuiltAverage);
    }
    
    /**
     * Test of Process method, of class CalculateHarmonic.
     */
    @Test
    public void testStrongFirstHarmRandomOther() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        int q = 7;
        int latNum = 32;
        int lonNum = 10;
        SphericalHarmonic sh = new SphericalHarmonic(q);
        double originalFirstHarmValue = 20.0;
        sh.SetHarmonic(0, 0, new Complex(originalFirstHarmValue, 0.0));
        for (int k = 1; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                sh.SetHarmonic(k, l, GenerateRandomHarmonic(l));
            }
        }

        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        double[] spatial = invShTrans.ProcessGaussian(latNum, lonNum);
        Pair<double[], double[]> coordinates = invShTrans.GenerateCoordinatePoints(latNum, lonNum);
        
        double[] lat = coordinates.getValue0();
        double[] lon = coordinates.getValue1();
        
        // Generate PCA
        SphericalHarmonic[] timeseries = new SphericalHarmonic[]{ sh };
        Triplet<Complex[][], Complex[], double[]> eigens = generatePca(timeseries);
        Complex[][] eigenVectors = eigens.getValue0();
        Complex[] eigenValues = eigens.getValue1();
        
        CalculateHarmonic harmonic = new CalculateHarmonic(q, false, eigenVectors, eigenValues);
        SphericalHarmonic shRebuilt = new SphericalHarmonic(q);
        for (int k = 0; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                Pair<Complex, double[]> values = harmonic.Process(k, l, lat, lon, spatial);
                Complex S_kl = values.getValue0();
                double[] weights = values.getValue1();
                shRebuilt.SetHarmonic(k, l, S_kl);
            }
        }
        
        // Make sure first harmonic is within 10% of original value
        double rebuiltFirstHarmonicValue = shRebuilt.GetHarmonic(0, 0).getReal();
        double ratio = rebuiltFirstHarmonicValue/originalFirstHarmValue;
        if (Math.abs(ratio-1.0) > 0.10 ) {
            Assert.fail("Error too large between originala and rebuilt");
        }
    }
    
    private static Triplet<Complex[][], Complex[], double[]> generatePca(SphericalHarmonic[] timeseriesSpherical) throws Exception {
        
        int timeseriesLength = timeseriesSpherical.length;
        int numOfQpoints = timeseriesSpherical[0].GetFullCompressedSpectra().length;
        Complex[][] qRealizations = new Complex[numOfQpoints][timeseriesLength];
        
        for (int j = 0; j < timeseriesLength; j++) {
            Complex[] spectral = timeseriesSpherical[j].GetFullCompressedSpectra();        
            for (int i = 0; i < numOfQpoints; i++) {
                qRealizations[i][j] = spectral[j];
            }
        }
        
        ComplexDoubleMatrix qRealizationsMatrix = ApacheMath3ToJblas(qRealizations);
        ComplexDoubleMatrix qRealizations_trans = qRealizationsMatrix.transpose().conj();
        ComplexDoubleMatrix R_hat_s = (qRealizationsMatrix.mmul(qRealizations_trans)).mul(1/(timeseriesLength + 0.0));
        PcaCovJBlas pca = new PcaCovJBlas(R_hat_s);

        // Get the eigen values & eigen vectors
        Complex[] eigenValues = pca.GetEigenValuesMath3();
        Complex[][] eigenVectors = pca.GetEigenVectorsMath3();                
        double[] varExplained = pca.GetSumOfVarianceExplained();
        
        // Truncate to variance explained
        Triplet<Complex[][], Complex[], double[]> eigens = CutoffVarExplained(0.9, eigenVectors, eigenValues, varExplained);  
        return eigens;
    }
    
    /**
     * Test of Process method, of class CalculateHarmonic.
     */
    @Test
    public void testAngellKorshoverNetworkLocations() throws Exception {
        System.out.println("Testing: " + Thread.currentThread().getStackTrace()[1].getMethodName());
        int q = 7;
        
        SphericalHarmonic sh = new SphericalHarmonic(q);
        double originalFirstHarmValue = 20.0;
        sh.SetHarmonic(0, 0, new Complex(originalFirstHarmValue, 0.0));
        InvDiscreteSphericalTransform invShTrans = new InvDiscreteSphericalTransform(sh);
        for (int k = 1; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                sh.SetHarmonic(k, l, GenerateRandomHarmonic(l));
            }
        }
        
        AngellKorshoverNetwork akNetwork = new AngellKorshoverNetwork();
        double[] lat = akNetwork.GetLats();
        double[] lon = akNetwork.GetLons();
        double[] value = invShTrans.ProcessPoints(lat, lon);
                
        // Generate PCA
        SphericalHarmonic[] timeseries = new SphericalHarmonic[]{ sh };
        Triplet<Complex[][], Complex[], double[]> eigens = generatePca(timeseries);
        Complex[][] eigenVectors = eigens.getValue0();
        Complex[] eigenValues = eigens.getValue1();
        
        CalculateHarmonic harmonic = new CalculateHarmonic(q, false, eigenVectors, eigenValues);
        
        SphericalHarmonic shRebuilt = new SphericalHarmonic(q);
        for (int k = 0; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                Pair<Complex, double[]> currHarm = harmonic.Process(k, l, lat, lon, value);
                Complex S_kl = currHarm.getValue0();
                double[] weights = currHarm.getValue1();
                shRebuilt.SetHarmonic(k, l, S_kl);
            }
        }
        
        // Make sure first harmonic is within 30% of original value
        double rebuiltFirstHarmonicValue = shRebuilt.GetHarmonic(0, 0).getReal();
        double ratio = rebuiltFirstHarmonicValue/originalFirstHarmValue;
        if (Math.abs(ratio-1.0) > 0.30 ) {
            Assert.fail("Error too large between originala and rebuilt");
        }
        
        sh.PrintHarmonic();
        shRebuilt.PrintHarmonic();
    }
    
    
    
}

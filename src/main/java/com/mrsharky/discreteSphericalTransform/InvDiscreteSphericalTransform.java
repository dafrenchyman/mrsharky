/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_ass1;
import com.mrsharky.helpers.LegendreGausWeights;
import com.mrsharky.helpers.ComplexArray;
import org.apache.commons.math3.complex.Complex;
import static com.mrsharky.helpers.Utilities.linspace;
import com.mrsharky.helpers.DoubleArray;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 *
 * @author dafre
 */
public class InvDiscreteSphericalTransform implements Serializable {
    
    private final SphericalHarmonic _spectral;
    private boolean _multiThreaded = false;
    
    public void SetMultiThreading(boolean multi) {
        _multiThreaded = multi;
    }
    
    /**
     * Constructor - Using compressed spectral data
     * @param spectral Takes in compressed spectral data generated from {@link DiscreteSphericalTransform_ass1}
     */
    public InvDiscreteSphericalTransform(SphericalHarmonic spectral) {
        _spectral = spectral;
    }
    
    private Complex[][] ExponentialHelper(int L, double[] theta) {
        Complex[][] output = new Complex[L+1][theta.length];
        for (int l = 0; l <=L; l++) {
            for (int t = 0; t < theta.length; t++) {
                Complex calc = new Complex(0,1);
                calc = calc.multiply(l).multiply(theta[t]).exp();
                output[l][t] = calc;
            }
        }
        return output;
    }
    
    public static Pair<double[], double[]> GenerateSingleCoordinatePoints(int M, int N) throws Exception {
        double[] phi   = LatPointsGaussianQuadrature(M);
        double[] theta = LonPoints(N);
        return Pair.with(phi, theta);
    }
    
    public static Pair<double[], double[]> GenerateCoordinatePoints(int M, int N) throws Exception {
        Pair<double[], double[]> genPoints = GenerateSingleCoordinatePoints(M, N);
        double[] phi = genPoints.getValue0();
        double[] theta = genPoints.getValue1();
        double[] lat   = new double[phi.length*theta.length];
        double[] lon   = new double[phi.length*theta.length];
        int counter = 0;
        for (int i = 0; i < phi.length; i++) {
            for (int j = 0; j < theta.length; j++) {
                lat[counter] = phi[i];
                lon[counter] = theta[j];
                counter++;
            }
        }
        Pair<double[], double[]> returnValue = Pair.with(lat, lon);
        return returnValue;
    }
    
    private static double[] LatPointsGaussianQuadrature(int M) throws Exception {
        LegendreGausWeights lgw = new LegendreGausWeights(M,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] legZerosRad = DoubleArray.ArcSin(legZerosM);
        return legZerosRad;
    }
    
    private static double[] LonPoints(int N) {
        double lonPoints[] = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);
        lonPoints = DoubleArray.Add(lonPoints, -Math.PI);
        return lonPoints;
    }
    
    /**
     * Generate data using Gaussian quadrature on the latitude on even spacing on the longitude
     * @param M Number of latitude points to have
     * @param N Number of longitude points to have
     * @return Spatial values for given inputs
     * @throws Exception 
     */
    public double[] ProcessGaussian(int M, int N) throws Exception {
        Pair<double[], double[]> points = GenerateCoordinatePoints(M, N);
        return ProcessPoints(points.getValue0(), points.getValue1());
    }
    
    public double[][] ProcessGaussianDoubleArray(int M, int N) throws Exception {
        double[] values = ProcessGaussian(M,N);
        double[][] newValues = new double[M][N];
        int counter = 0;
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                newValues[i][j] = values[counter];
                counter++;
            }
        }
        return newValues;
    }
    
    //private Map<Pair<Integer, double[]>, SphericalAssociatedLegendrePolynomials> _p_k_l;
    
    /**
     * Process arbitrary points
     * @param latPoints Latitude values in radian (-pi/2, pi/2)
     * @param lonPoints Longitude values in radian (-pi, pi)
     * @return Spatial values for given points
     * @throws Exception 
     */
    public double[] ProcessPoints(double[] latPoints, double[] lonPoints) throws Exception {
        
        // Check the point counts are ok
        if (latPoints.length != lonPoints.length) {
            throw new Exception("number of latPoints and number of lonPoints should be the same");
        }
        
        double[] latPoints_c = new double[latPoints.length];
        double[] lonPoints_c = new double[lonPoints.length];
        
        // Check lat/lon points are valid
        for (int i = 0; i < latPoints.length; i++) {
            double currLat = latPoints[i];
            double currLon = lonPoints[i];
            if (currLat < -Math.PI/2.0 -(10e-10)  || currLat > Math.PI/2.0 +(10e-10)) {
                throw new Exception("Lat points need to be between -pi/2 & pi/2");
            }
            
            if (currLon < -Math.PI -(10e-10) || currLon > Math.PI + (10e-10) ) {
                throw new Exception("Lon points need to be between -pi & pi");
            }
            
            // Adjust lat so its in ranges 0 < lat < pi 
            latPoints_c[i] = currLat + Math.PI/2.0;
            
            // Adjust lon so its in ranges 0 < lon < 2pi
            lonPoints_c[i] = currLon + Math.PI;
        }
        
        
        double[] DATAREBUILT = new double[latPoints_c.length];
        
        if (_multiThreaded) {
            DATAREBUILT = ProcessPointsMultiThreaded(latPoints_c, lonPoints_c);
        } else { 
            Complex[][] SPECTRA = _spectral.GetFullSpectral();
            int Q = SPECTRA.length-1;

            double[] phi = DoubleArray.Cos(latPoints_c);
            SphericalAssociatedLegendrePolynomials P_k_l = new SphericalAssociatedLegendrePolynomials(Q,phi);
            double[] theta = lonPoints_c;
            Complex[][] expHelp = this.ExponentialHelper(Q, theta);

            for (int k = 0; k <= Q; k++) {
                for (int l = 0; l <= k; l++) {
                    double oscil = Math.pow(-1, l);
                    double[] pkl = P_k_l.GetAsDouble(k, l);
                    Complex[] thetaExp  = ComplexArray.GetRow(expHelp, l);
                    Complex   currX     = SPECTRA[k][l+Q]; 
                    Complex[] currY     = ComplexArray.Multiply(pkl, thetaExp);
                    Complex[] currSum   = ComplexArray.Multiply(currY, currX);

                    // If it's not the l=0, then generate both the +l & -l parts and add them togethar at the same time
                    if (l != 0) {
                        Complex   currXConj   = SPECTRA[k][Q-l];
                        Complex[] currYConj   = ComplexArray.Multiply(ComplexArray.Conjugate(currY),oscil);
                        Complex[] currSumConj = ComplexArray.Multiply(currYConj, currXConj);
                        currSum = ComplexArray.Add(currSum, currSumConj);
                    }
                    DATAREBUILT = DoubleArray.Add(DATAREBUILT, ComplexArray.Real(currSum));
                }
            }
        }
        return DATAREBUILT;
    }
    
    
    public class DataRebuilt {
        private double[] _DATAREBUILT;
        public DataRebuilt(int size) {
            _DATAREBUILT = new double[size];
        }

        public synchronized void addValues (double[] newSum) throws Exception {
            _DATAREBUILT = DoubleArray.Add(_DATAREBUILT, newSum);
        }
        
        public double[] getValue(){ return _DATAREBUILT; }
    }
    
    public double[] ProcessPointsMultiThreaded(double[] latPoints, double[] lonPoints) throws Exception {
        if (latPoints.length != lonPoints.length) {
            throw new Exception("number of latPoints and number of lonPoints should be the same");
        }
        Complex[][] SPECTRA = _spectral.GetFullSpectral();
        int Q = SPECTRA.length-1;

        double[] phi = DoubleArray.Cos(latPoints);
        SphericalAssociatedLegendrePolynomials P_k_l = new SphericalAssociatedLegendrePolynomials(Q,phi);
        double[] theta = lonPoints;
        Complex[][] expHelp = this.ExponentialHelper(Q, theta);
        DataRebuilt rebuiltData = new DataRebuilt(latPoints.length);
        
        int threads = Runtime.getRuntime().availableProcessors();
        //threads = 1;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Future<Void>> futures = new ArrayList<Future<Void>>();

        for (int k = 0; k <= Q; k++) {
            for (int l = 0; l <= k; l++) {

                final int k_f = k;
                final int l_f = l;

                // Process the spherical harmonics multi-threaded
                Callable<Void> callable = new Callable<Void>() {
                    public Void call() throws Exception {

                        double oscil = Math.pow(-1, l_f);
                        double[] pkl = P_k_l.GetAsDouble(k_f, l_f);
                        Complex[] thetaExp  = ComplexArray.GetRow(expHelp, l_f);
                        Complex   currX     = SPECTRA[k_f][l_f+Q]; 
                        Complex[] currY     = ComplexArray.Multiply(pkl, thetaExp);
                        Complex[] currSum   = ComplexArray.Multiply(currY, currX);

                        // If it's not the l=0, then generate both the +l & -l parts and add them togethar at the same time
                        if (l_f != 0) {
                            Complex   currXConj   = SPECTRA[k_f][Q-l_f];
                            Complex[] currYConj   = ComplexArray.Multiply(ComplexArray.Conjugate(currY),oscil);
                            Complex[] currSumConj = ComplexArray.Multiply(currYConj, currXConj);
                            currSum = ComplexArray.Add(currSum, currSumConj);
                        }

                        rebuiltData.addValues(ComplexArray.Real(currSum));
                        return null;
                    }
                };
                futures.add(service.submit(callable));
            }
        }

        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        return rebuiltData.getValue();
    }
    
    
    
    public static double[][] GenerateSinCosData (int xSize, int ySize) {
        double xLength = 4*Math.PI;
        double[] x = linspace(0,xLength,xSize+1);

        double yLength = 4*Math.PI;
        double[] y = linspace(0,yLength,ySize+1);

        MeshGrid mesh = new MeshGrid(x,y);
        double[][] X = mesh.GetX();
        double[][] Y = mesh.GetY();

        double[][] data = new double[y.length][x.length];

        for (int row = 0; row < y.length; row++) {
            for (int col = 0; col < x.length; col++) {
                data[row][col] = Math.sin(X[row][col]) + Math.cos(Y[row][col]);
            }
        }
        return data;
    }
    
    public static double[][] GenerateL2M1(int lmax, int nlat, int nphi) throws Exception {
        double[][] data = new double[1][1];

        LegendreGausWeights lgw = new LegendreGausWeights(nlat,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED

        double[] phi2 = DoubleArray.Cos(legZerosRad);                            // TESTED
        //AssociatedLegendrePolynomials_ass P_k_l = new AssociatedLegendrePolynomials_ass(Q,phi);
        //double[] theta = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);  // TESTED

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
    
    
    public static void main(String args[]) throws Exception {
        System.out.println("Starting Program");
        double[][] DATA = GenerateSinCosData(200, 100);
        
        int lmax = 3;		// maximum degree of spherical harmonics
        int nlat = 32;		// number of points in the latitude direction  (constraint: nlat >= lmax+1)
        int nphi = 10;   
        double[][] DATA2 = GenerateL2M1(lmax, nlat, nphi);
        int M = nlat;
        int N = nphi;
        int Q = lmax;
        
        //int Q = 5;
        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(DATA2, lmax, true);
        
        SphericalHarmonic spectra = dst.GetSpectra();
        ComplexArray.Print(spectra.GetHalfCompressedSpectra());


        final long startTime = System.currentTimeMillis();
        System.out.println("Rebuilt Data - new ass method");
        InvDiscreteSphericalTransform idst = new InvDiscreteSphericalTransform(spectra);
        final long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) );

        double[] lats = dst.GetLatitudeCoordinates();
        double[] lons = dst.GetLongitudeCoordinates();

        // First Test
        {
            double[] newLats = new double[lats.length*lons.length];
            double[] newLons = new double[lats.length*lons.length];
            int counter = 0;
            for (int i = 0; i < lats.length; i++) {
                for (int j = 0; j < lons.length; j++) {
                    newLats[counter] = lats[i];
                    newLons[counter] = lons[j];
                    counter++;
                }
            }
            double[] newTest = idst.ProcessPoints(newLats, newLons);
            DoubleArray.Print(newTest);
        }

        // Second Test
        {
            double[] newLats = new double[lats.length];
            double[] newLons = new double[lats.length];
            int counter = 0;
            for (int i = 0; i < lats.length; i++) {
                int j = 4;
                newLats[counter] = lats[i];
                newLons[counter] = lons[j];
                counter++;
            }
            double[] newTest = idst.ProcessPoints(newLats, newLons);
            DoubleArray.Print(newTest);
        }
        
        // Third Test
        {
            double[][] newTest = idst.ProcessGaussianDoubleArray(M, N);
            DoubleArray.Print(newTest);
        }


        // Error calculation with sin
        if (true) {
            double[][] rebuilt = idst.ProcessGaussianDoubleArray(M, N);
            double error = 0;
            double[][] Error = new double[rebuilt.length][rebuilt[0].length];
            for (int i = 0; i < rebuilt.length; i++) {
                for (int j = 0; j < rebuilt[0].length; j++) {
                    double currError = Math.pow(DATA2[i][j] - rebuilt[i][j], 2.0);
                    Error[i][j] = currError;
                    double currErrorSin = currError * Math.sin(lats[i]);
                    error += currErrorSin;
                }
                System.out.println(error);
            }

            // Error calculation looking at L = 0, M = 0 of the squared diff
            DiscreteSphericalTransform_ass1 dstError = new DiscreteSphericalTransform_ass1(Error, Q, true);
            Complex spectraError = dstError.GetSpectraCompressed()[0];
            System.out.println("Sin Error: " + error + ", M = 0, L = 0 Error: " + spectraError);

            double[][] errors = DoubleArray.Power(DoubleArray.Add(rebuilt, DoubleArray.Multiply(DATA2, -1.0)
                    ), 2.0);
            System.out.println("Total Error: " + DoubleArray.SumArray(errors) );
        }
    }
}

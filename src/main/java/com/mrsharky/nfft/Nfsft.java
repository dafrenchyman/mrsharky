/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.nfft;

import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.LegendreGausWeights;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import static com.mrsharky.helpers.Utilities.linspace;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author mrsharky
 */
public class Nfsft {
    static {
        //System.out.println(System.getProperty("java.library.path"));
        //System.setProperty("java.library.path", "/home/mrsharky/NetBeansProjects/mavenproject1/");
        //System.load("/media/dropbox/PhD/Reboot/Projects/Ncep20thCenturyReanalysisV2c/Java/DataProcessor/libJniDynamicTest.so");
        //System.loadLibrary("JniDynamicTest"); // Load native library at runtime
        System.load("/media/dropbox/PhD/Reboot/Projects/Ncep20thCenturyReanalysisV2c/C/nfftJni/dist/Debug/GNU-Linux/libnfftJni.so");
        // hello.dll (Windows) or libhello.so (Unixes)
                                       
    }
    
    private final long _plan;
    private final int _N;
    
    public Nfsft() {
        _plan = 2;
        _N = 2;
    }
    
    private double[] ConvertLat(double[] lat) {
        double[] newLat = new double[lat.length];
        for (int i = 0; i < lat.length; i++) {
            newLat[i] = lat[i]/(2*Math.PI); // Convert Lat from 0 to pi -> 0 to 0.5
        }
        return newLat;
    }
    
    private double[] ConvertLon(double[] lon) throws Exception {
        double[] newLon = new double[lon.length];
        for (int i = 0; i < lon.length; i++) {
            
            // Convert Lon from 0 to 2pi to -0.5 to 0.5
            if (lon[i] >= 0 && lon[i] < Math.PI) {
                newLon[i] = lon[i]/(2*Math.PI);
            } else if (lon[i] >= Math.PI && lon[i] < 2*Math.PI) {
                newLon[i] = lon[i]/(2*Math.PI) - 1.0;
            } else {
                throw new Exception ("Lon (x) Coordinates are not within the range: 0 <= x <= 2PI");
            }
        }
        return newLon;
    }
    
    public Nfsft(int N, double[] lat, double[] lon) throws Exception { 
        this._plan = getPlan();
        this._N = N;
        Initialize(N, lat, lon);
    }
    
    
    public Nfsft(int N, double[][] latDoubleArray, double[][] lonDoubleArray) throws Exception {
        this._plan = getPlan();
        this._N = N;
        
        // Convert the data from a double array to a single array
        double[] lat = new double[latDoubleArray.length*latDoubleArray[0].length];
        double[] lon = new double[lonDoubleArray.length*lonDoubleArray[0].length];
        for (int i = 0; i < latDoubleArray.length; i++) {
            for (int j = 0; j < latDoubleArray[0].length; j++) {
                int location = i*latDoubleArray.length + j*latDoubleArray[0].length;
                lat[location] = latDoubleArray[i][j];
                lon[location] = lonDoubleArray[i][j];
            }
        }
        Initialize(N, lat, lon);
    }
    
    private void Initialize(int N, double[] lat, double[] lon) throws Exception {
        
        this.precompute(_N);
        this.initAdvanced(this._plan, _N, lat.length);
        
        double[] newLat = ConvertLat(lat);
        double[] newLon = ConvertLon(lon);
        
        for (int i = 0; i < lon.length; i++) {
            
            // Convert Lon from 0 to 2pi to -0.5 to 0.5
            if (lon[i] >= 0 && lon[i] < Math.PI) {
                newLon[i] = lon[i]/(2*Math.PI);
            } else if (lon[i] >= Math.PI && lon[i] < 2*Math.PI) {
                newLon[i] = lon[i]/(2*Math.PI) - 1.0;
            } else {
                throw new Exception ("Lon (x) Coordinates are not within the range: 0 <= x <= 2PI");
            }
            
            // Convert Lat from 0 to pi to 0 to 0.5
            newLat[i] = lat[i]/(2*Math.PI);
        }
        this.setX(this._plan, newLat, newLon);
    }
    
    public void Close() {
        this.finalize(this._plan);
    }
    
    public Complex[][] SpatialToSpectralDirect(Complex[] values) {
        double[] real = new double[values.length];
        double[] imaginary = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            real[i] = values[i].getReal();
            imaginary[i] = values[i].getImaginary();
        }
        return Nfsft.this.SpatialToSpectralDirect(real, imaginary);
    }
    
    public Complex[] SpectralToSpatialDirect(Complex[][] values) {
        int arraySize = getIndex(_N,-1*_N,_N)+1;
        
        //int arraySize = NFSFT_INDEX(nfsftPlan->N,-1*nfsftPlan->N,nfsftPlan)+1;
        double[] real = new double[arraySize];
        double[] imaginary = new double[arraySize];
        
        for (int k = 0; k <= _N; k++) {
            for (int n = -k; n <= k; n++) {
                real[getIndex(k,n,_N)]      = values[this._N+n][k].getReal();
                imaginary[getIndex(k,n,_N)] = values[this._N+n][k].getImaginary();
            }
        }
        this.setFHat(_plan, real, imaginary);
        double[] spatial = this.trafoDirect(_plan);
        
        Complex[] spatialComplex = new Complex[spatial.length/2];
        
        for (int k = 0; k < spatial.length/2; k++) {
            spatialComplex[k] = new Complex(spatial[2*k], spatial[2*k+1]);
        }
        return spatialComplex;
    }
    
    public Complex[][] SpatialToSpectral(Complex[] values) {
        double[] real = new double[values.length];
        double[] imaginary = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            real[i] = values[i].getReal();
            imaginary[i] = values[i].getImaginary();
        }
        return Nfsft.this.SpatialToSpectral(real, imaginary);
    }   
    
    public Complex[][] SpatialToSpectralDirect(double[] real, double[] imaginary) {
        this.setF(this._plan, real, imaginary);
        double[] spectral = this.adjointDirect(this._plan);
        return NfsftToComplexDoubleArray(spectral);
    }
    
    public Complex[][] SpatialToSpectral(double[] real, double[] imaginary) {
        this.setF(this._plan, real, imaginary);       
        double[] spectral = this.adjoint(this._plan);
        return NfsftToComplexDoubleArray(spectral);
    }
    
    private Complex[][] NfsftToComplexDoubleArray(double[] spectral) {
        Complex[][] spectralComplex = ComplexArray.CreateComplex(2*(this._N+1)-1, this._N+1);
        for (int k = 0; k <= _N; k++) {
            for (int n = -k; n <= k; n++) {
                spectralComplex[this._N+n][k] = new Complex(spectral[getIndexReal(k,n,_N)], spectral[getIndexImag(k,n,_N)]);
            }
        }
        return spectralComplex;
    }
    
    private static int getIndex(int k,int n,int N) {
        return ((2*N+2)*(N-n+1)+N+k+1);
    }
    
    private static int getIndexReal(int k,int n,int N) {
        return 2*((2*N+2)*(N-n+1)+N+k+1);
    }
    
    private static int getIndexImag(int k,int n,int N) {
        return 2*((2*N+2)*(N-n+1)+N+k+1)+1;
    }
        
    // Native Methods
    private native double[] toSpectral(int q, int n, int m, double[] lat, double[] lon, double[] real, double [] imaginary);
    private native long getPlan();
    private native void init(long plan, int N, int M);
    private native void initAdvanced(long plan, int N, int M);
    private native void precompute(int N);
    private native void setX(long plan, double[] lat, double[] lon);
    private native void setF(long plan, double[] real, double[] imaginary);
    private native void setFHat(long plan, double[] real, double[] imaginary); 
    private native double[] trafoDirect(long plan);
    private native double[] trafo(long plan);
    private native double[] adjointDirect(long plan);
    private native double[] adjoint(long plan);
    private native void finalize(long plan);
    
    // Test Driver
    public static void main(String[] args) throws Exception { 
      
        int Q = 100;
        int N = Q+1; // Y (lat)
        int M = N*2; // X (lon)
        
        double[] xlon = new double[1];
        
        double[] lat = new double[N*M];
        double[] lon = new double[N*M];
        double[] real = new double[N*M];
        double[] imaginary = new double[N*M];
        double[] legGausWeights = new double[N*M];
        
        double[][] DATA = new double[1][1];
        
        //LegendreGausWeights lgwt = new LegendreGausWeights(N, -0.5, 0.5);        
        LegendreGausWeights lgwt = new LegendreGausWeights(N, -1.0, 1.0);
        
        {
            double xLength = 2*Math.PI;
            int xSize = M;
            double[] x = linspace(0,xLength-xLength/xSize,xSize);
            
            xlon = linspace(0,2*Math.PI-2*Math.PI/xSize,xSize);

            double yLength = 5*Math.PI;
            int ySize = N;
            double[] y = linspace(0,yLength,ySize);

            MeshGrid mesh = new MeshGrid(x,y);
            double[][] X = mesh.GetX();
            double[][] Y = mesh.GetY();

            DATA = new double[y.length][x.length];
            
            int counter = 0;
            for (int col = 0; col < x.length; col++) {
                for (int row = 0; row < y.length; row++) {
                    DATA[row][col] = Math.sin(Y[row][col]) + Math.cos(X[row][col]);
                    
                    // Populate the stuff
                    real[counter] = DATA[row][col];
                    //System.out.println(real[counter]);
                    imaginary[counter] = 0.0;
                    legGausWeights[counter] = Math.PI/N*lgwt.GetWeights()[row];
                    
                    lat[counter] = Math.acos(lgwt.GetValues()[row]);
                    lon[counter] = xlon[col];
                    counter++;
                }
            }
        }
        
        Nfsft nfsft = new Nfsft(Q, lat, lon);
        
        Complex[][] spectral = nfsft.SpatialToSpectralDirect(real, imaginary);
        Complex[] rebuiltSpatial = nfsft.SpectralToSpatialDirect(spectral);
        ComplexArray.Print(spectral);
        //PrintComplexDoubleArray(nfsft.SpatialToSpectral(real, imaginary));
                  
        double totalError = 0;
        for (int counter = 0; counter < rebuiltSpatial.length; counter++) {
            double orig = real[counter];
            double rebu = rebuiltSpatial[counter].getReal() * legGausWeights[counter];
            totalError += Math.pow((orig-rebu), 2.0);
            System.out.println("Orig: " + orig + ", Rebuilt: " + rebu + ", Error: " + (orig-rebu));
        }
        System.out.println("Total Sum Sq Error: " + totalError);
        
        
        
        nfsft.Close();

        
        if (false) {
            double[] newLat = nfsft.ConvertLat(lat);
            double[] newLon = nfsft.ConvertLon(lon);
            double[] values = new Nfsft().toSpectral(Q, newLat.length, newLon.length, newLat , newLon, real, imaginary);

            // Fancier version
            double[] spectrala = new double[5];
            {
                Nfsft currNfsft = new Nfsft();
                long ptrPlan = currNfsft.getPlan();

                currNfsft.precompute(Q);
                currNfsft.init(ptrPlan, Q, lat.length);

                currNfsft.setX(ptrPlan, newLat, newLon);

                currNfsft.setF(ptrPlan, real, imaginary);

                spectrala = currNfsft.adjointDirect(ptrPlan);

                for (int k = 0; k <= Q; k++) {
                    for (int n = -k; n <= k; n++) {
                        System.out.println("Real["+ k + "," + n + "]: " + values[getIndexReal(k,n,Q)] + " : " + spectrala[getIndexReal(k,n,Q)]);
                    }
                }

                for (int k = 0; k <= Q; k++) {
                    for (int n = -k; n <= k; n++) {
                        System.out.println("Imag["+ k + "," + n + "]: " + values[getIndexImag(k,n,Q)] + " : " + spectrala[getIndexImag(k,n,Q)]);
                    }
                }
            }
        }     
        
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.helpers.LegendreGausWeights;
import com.mrsharky.helpers.ComplexArray;
import org.apache.commons.math3.complex.Complex;
import static com.mrsharky.helpers.Utilities.linspace;
import com.mrsharky.helpers.DoubleArray;

/**
 *
 * @author dafre
 */
public class InvDiscreteSphericalTransform_log {
    
    private Complex[][] _spatial;
    private int _m;
    private int _n;
    private int _q;
    
    public Complex[][] GetSpatialAsComplex(){
        return this._spatial;
    }
    
    public double[][] GetSpatial(){
        double[][] output = new double[_spatial.length][_spatial[0].length];
        for (int i = 0; i < _spatial.length; i++) {
            for (int j = 0; j < _spatial[0].length; j++) {
                output[i][j] = _spatial[i][j].getReal();
            }
        }
        return output;
    }
    
    
    public Complex[] GetSpectraCompressed() {
        int size = (int) Math.pow(_q+1.0, 2.0);
        Complex[] compressedSpectra = new Complex[size];
        int counter = 0;
        for (int k = 0; k <= _q; k++) {
            for (int l = -k; l <= k; l++) {
                compressedSpectra[counter++] = _spatial[k][l+(_q)];
            }
        }
        return compressedSpectra;
    }
    
    private Complex[][] UnCompressSpectral(Complex[] compressedSpectral, int Q) {
        ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        Complex[][] uncompressedSpectral = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        int counter = 0;
        for (int k = 0; k <= Q; k++) {
            for (int l = -k; l <= k; l++) {
                uncompressedSpectral[k][l+(Q)] = compressedSpectral[counter++];
            }
        }
        return uncompressedSpectral;
    }
    
    public int GetM() {
        return this._m;
    }
    
    public int GetN() {
        return this._n;
    }
    
    private Complex[][] ThetaImaginarylHelper(int L, double[] theta) {
        Complex[][] output = new Complex[L+1][theta.length];
        for (int l = 0; l <=L; l++) {
            for (int t = 0; t < theta.length; t++) {
                Complex calc = new Complex(0,1);
                calc = calc.multiply(l).multiply(theta[t]);
                output[l][t] = calc;
            }
        }
        return output;
    }
    
    private double SummationHelper(int K, int L) throws Exception {
        if (L > K) {
            throw new Exception("L > K!!!");
        }
        double left = 0;
        double right = 0;
        // (k-l)!
        for (int i = 1; i <= K-L; i++) {
            left = left + Math.log(i);
        }
        // (k+l)!
        for (int i = 1; i <= K+L; i++) {
            right = right + Math.log(i);
        }
        return (left - right)*0.5;
    }
    
    public InvDiscreteSphericalTransform_log(Complex[] SPECTRA, int M, int N, int Q, boolean GAUSQUADon) throws Exception {
        Complex[][] unCompressedSpectral = UnCompressSpectral(SPECTRA, Q);
        Initialize(unCompressedSpectral, M, N, GAUSQUADon);
    }
    
    public InvDiscreteSphericalTransform_log(Complex[][] SPECTRA, int M, int N, boolean GAUSQUADon) throws Exception {
        Initialize(SPECTRA, M, N, GAUSQUADon);
    }
    
    
    private boolean _verbose = false;
    
    private void Initialize(Complex[][] SPECTRA, int M, int N, boolean GAUSQUADon) throws Exception {
        int Q = SPECTRA.length-1;
        if (_verbose) {
            System.out.println("Begin Spectal to Spatial Conversion (Q = " + Q + ")");
        }
        
        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));
        
        double[] phi = DoubleArray.Cos(legZerosRad);
        AssociatedLegendrePolynomials_log P_k_l = new AssociatedLegendrePolynomials_log(Q,phi);
        double[] theta = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);
        Complex[][] expHelp = this.ThetaImaginarylHelper(Q, theta);
        Complex[][] DATAREBUILT = ComplexArray.CreateComplex(M-1,N);
        
        double constants = Math.sqrt(1.0/(4.0*Math.PI));
        for (int k = 0; k <= Q; k++) {
            for (int l = 0; l <= k; l++) {
                double oscil = Math.pow(-1, l);
                double constants2 = 0.5 * Math.log(2*k+1.0);
                Complex factorials = new Complex(SummationHelper(k,l), 0.0);
                Complex constant2_factorials = factorials.add(constants2);
                Complex[] pklLog = P_k_l.GetAsDouble(k, l);
                Complex[] thetaExp = ComplexArray.GetRow(expHelp, l);
                Complex     currX   = SPECTRA[k][l+(Q)];
                
                Complex[][] currY = ComplexArray.Add(ComplexArray.MeshAdd(pklLog, thetaExp), constant2_factorials);
                currY = ComplexArray.Exp(currY);
                //currY = ComplexHelpers.Conjugate(currY);
                Complex[][] currSum = ComplexArray.Multiply(currY, currX);
                
                // If it's not the l=0, then generate both the +l & -l parts and add them togethar at the same time
                if (l != 0) {
                    Complex     currXConj = SPECTRA[k][Q-l];
                    Complex[][] currYConj = ComplexArray.Multiply(ComplexArray.Conjugate(currY),oscil);
                    Complex[][] currSumConj = ComplexArray.Multiply(currYConj, currXConj);
                    currSum = ComplexArray.Add(currSum, currSumConj);
                }
                //ComplexHelpers.Print(currSum);
                DATAREBUILT = ComplexArray.Add(DATAREBUILT, currSum);
            }
        }
        DATAREBUILT = ComplexArray.Multiply(DATAREBUILT, constants);
        
        // Loop through the SPECTRA and the Ys and get rid of NaN and Infinity (not really an issue in log space version)
        this._spatial = ComplexArray.RemoveNaNInf(DATAREBUILT);
        this._m = M;
        this._n = N;
        this._q = Q;
    }
    
    public static void main(String args[]) throws Exception {
        System.out.println("Starting Program");
        double[][] DATA = new double[1][1];
        {
            double xLength = 4*Math.PI;
            int xSize = 100;
            double[] x = linspace(0,xLength,xSize+1);

            double yLength = 4*Math.PI;
            int ySize = 50;
            double[] y = linspace(0,yLength,ySize+1);

            MeshGrid mesh = new MeshGrid(x,y);
            double[][] X = mesh.GetX();
            double[][] Y = mesh.GetY();

            DATA = new double[y.length][x.length];
            
            for (int row = 0; row < y.length; row++) {
                for (int col = 0; col < x.length; col++) {
                    DATA[row][col] = Math.sin(X[row][col]) + Math.cos(Y[row][col]);
                }
            }
        }
        
        int Q = 1000;
        DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(DATA, Q, true);
        
        //Complex[][] spectra = dst.GetSpectra();
        Complex[] spectraCompr = dst.GetSpectraCompressed();
        
        
        
        
        System.out.println("Original Data");
        DoubleArray.Print(DATA);
        
        //ComplexHelpers.Print(dst.GetSpectra());
        
        if (false) {
            System.out.println("Rebuilt Data - old method");
            InvDiscreteSphericalTransform invdst = new InvDiscreteSphericalTransform(spectraCompr, dst.GetM(), dst.GetN(), Q, true);
            Complex[][] spatial = invdst.GetSpatial();
            ComplexArray.Print(spatial);
        }
        
        System.out.println("Rebuilt Data - new method");
        InvDiscreteSphericalTransform_log idst = new InvDiscreteSphericalTransform_log(spectraCompr, dst.GetM(), dst.GetN(), Q, true);
        ComplexArray.Print(idst.GetSpatialAsComplex());
        
    }
}

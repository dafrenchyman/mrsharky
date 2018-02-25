/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.discreteSphericalTransform.SphericalAssociatedLegendrePolynomials;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.LegendreGausWeights;
import com.mrsharky.helpers.ComplexArray;
import org.apache.commons.math3.complex.Complex;
import static com.mrsharky.helpers.Utilities.linspace;
import com.mrsharky.helpers.DoubleArray;

/**
 *
 * @author dafre
 */
public class DiscreteSphericalTransform_ass1 {
    
    private Complex[][] _spectra;
    private Complex[][][] _y;
    private int _m;
    private int _n;
    private int _q;
    
    public Complex[][] GetSpectra(){
        return this._spectra;
    }
    
    public Complex[] GetSpectraCompressed() {
        int size = (int) Math.pow(_q+1.0, 2.0);
        Complex[] compressedSpectra = new Complex[size];
        int counter = 0;
        for (int k = 0; k <= _q; k++) {
            for (int l = -k; l <= k; l++) {
                compressedSpectra[counter++] = _spectra[k][l+(_q)];
            }
        }
        return compressedSpectra;
    }
    
    public void PrintSpectraCompressed() {
        for (int k = 0; k <= _q; k++) {
            for (int l = -k; l <= k; l++) {
                System.out.println("K = " + k + " L = " + l + " Value: " + _spectra[k][l+(_q)]);
            }
        }
    }
    
    public Complex[][][] GetY() {
        return this._y;
    }
    
    public int GetM() {
        return this._m;
    }
    
    public int GetN() {
        return this._n;
    }
    
    public double[] GetLatitudeCoordinates() throws Exception {   
        LegendreGausWeights lgw = new LegendreGausWeights(_m,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));
        
        double [] coordinates = new double[_m];
        for (int i = 0; i < _m; i++) {
            coordinates[i] = legZerosRad[i];
        }
        return coordinates;
    }
    
    public double[] GetLongitudeCoordinates() throws Exception {   
        double[] theta = DoubleArray.Multiply(linspace(1.0,_n,_n), 2*Math.PI/_n);
        
        double [] coordinates = new double[_n];
        for (int j = 0; j <_n; j++) {
            coordinates[j] = theta[j];
        }
        return coordinates;
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

    public DiscreteSphericalTransform_ass1(double[][] DATA, int Q, boolean GAUSQUADon) throws Exception {
        int M = DATA.length;
        int N = DATA[0].length;
        
        LegendreGausWeights lgw = new LegendreGausWeights(M,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED
        
        double[] phi = DoubleArray.Cos(legZerosRad);                            // TESTED
        SphericalAssociatedLegendrePolynomials P_k_l = new SphericalAssociatedLegendrePolynomials(Q,phi);
        double[] theta = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);  // TESTED
        Complex[][] expHelp = this.ExponentialHelper(Q, theta);                 // TESTED        
        Complex[][] SPECTRA = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
         
        double constants = 2.0 * Math.PI / N;
        for (int k = 0; k <= Q; k++) {
            for (int l = 0; l <= k; l++) {
                double oscil = Math.pow(-1, l);
                double[] pkl = P_k_l.GetAsDouble(k, l);
                Complex[] totalSum = ComplexArray.CreateComplex(phi.length);                
                for (int t = 0; t < theta.length; t++) {
                    Complex currThetaExp = expHelp[l][t];
                    double[] dataSub = DoubleArray.GetColumn(DATA, t);
                    Complex[] value = ComplexArray.Multiply(dataSub, currThetaExp);
                    totalSum = ComplexArray.Add(totalSum, value);
                }

                totalSum = ComplexArray.Multiply(totalSum, gausWeights);
                totalSum = ComplexArray.Multiply(totalSum, pkl);
                totalSum = ComplexArray.Multiply(totalSum, oscil);
                
                Complex value_KL = ComplexArray.Sum(totalSum);
                SPECTRA[k][Q+l] = value_KL.conjugate().multiply(oscil);
                SPECTRA[k][Q-l] = value_KL;
            }
        }
        SPECTRA = ComplexArray.Multiply(SPECTRA, constants);
            
        // Loop through the SPECTRA and the Ys and get rid of NaN and Infinity
        this._spectra = ComplexArray.RemoveNaNInf(SPECTRA);
        this._m = M;
        this._n = N;
        this._q = Q;
        //System.out.println();
    }
    
    public static void main(String args[]) throws Exception {
        
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
        
        int Q = 2000;
        DiscreteSphericalTransform_ass1 dst = new DiscreteSphericalTransform_ass1(DATA, Q, true);
        
        Complex[][] spectra = dst.GetSpectra();
        //Utilities.PrintComplexDoubleArray(spectra);
        //Complex[] spectraCompr = dst.GetSpectraCompressed();
        //Complex[][][] y = dst.GetY();
        //int m = dst.GetM();
        //int n = dst.GetN();
        
        //System.out.println("Spectra");
        //Utilities.PrintComplexDoubleArray(spectra);
        
        System.out.println("Spectra Compressed");
        dst.PrintSpectraCompressed();
        //Utilities.PrintComplexDoubleArray(spectraCompr);
        
        //System.out.println("Y");
        //Utilities.PrintComplexDoubleArray(y);
    }
}

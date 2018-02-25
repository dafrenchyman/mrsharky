/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.LegendreGausWeights;
import com.google.common.math.BigIntegerMath;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.ComplexArray;
import org.apache.commons.math3.complex.Complex;
import static com.mrsharky.helpers.Utilities.linspace;
import com.mrsharky.helpers.DoubleArray;

/**
 *
 * @author dafre
 */
public class DiscreteSphericalTransform_log {
    
    private final Complex[][] _spectra;
    private final int _m;
    private final int _n;
    private final int _q;
    
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
    
    public int GetM() {
        return this._m;
    }
    
    public int GetN() {
        return this._n;
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
        for (int i = 1; i <= K-L; i++) {
            left = left + Math.log(i);
        }
        for (int i = 1; i <= K+L; i++) {
            right = right + Math.log(i);
        }
        return (left - right)*0.5;
    }
    
    private boolean _verbose = true;

    public DiscreteSphericalTransform_log(double[][] DATA, int Q, boolean GAUSQUADon) throws Exception {
        if (_verbose) {
            System.out.println("Begin Spatial to Spectal Conversion (Q = " + Q + ")");
        }
        int M = DATA.length+1;
        int N = DATA[0].length;
        
        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] gausWeightsLog = DoubleArray.Log(gausWeights);
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED
        
        double[] phi = DoubleArray.Cos(legZerosRad);                            // TESTED
        AssociatedLegendrePolynomials_log P_k_l = new AssociatedLegendrePolynomials_log(Q,phi);
        double[] theta = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);  // TESTED
        Complex[][] expHelp = this.ThetaImaginarylHelper(Q, theta);                 // TESTED        
        Complex[][] SPECTRA = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        
        Complex[][] logDATA = ComplexArray.Log(DATA);
        
        for (int k = 0; k <= Q; k++) {
            //System.out.print("\rk=" + k + " of " + Q + "");
            double constants = Math.sqrt(Math.PI * (2*k+ 1))/N;
            for (int l = 0; l <= k; l++) {
                double oscil = Math.pow(-1, l);
                double factorials = SummationHelper(k,l);
                Complex[] pklLog = P_k_l.GetAsDouble(k, l);
                Complex[] pklLog_factorial = ComplexArray.Add(pklLog, factorials);
                Complex[] pklLog_factorial_gausWeightsLog = ComplexArray.Add(pklLog_factorial, gausWeightsLog);
                Complex[] totalSum = ComplexArray.CreateComplex(phi.length);                
                for (int t = 0; t < theta.length; t++) {
                    Complex currThetaExp = expHelp[l][t];
                    Complex[] dataSubLog = ComplexArray.GetColumn(logDATA, t);
                    Complex[] value = ComplexArray.Add(dataSubLog, pklLog_factorial_gausWeightsLog);
                    value = ComplexArray.Add(value, currThetaExp);
                    value = ComplexArray.Exp(value);
                    totalSum = ComplexArray.Add(totalSum, value);
                }
                
                Complex[] combining = ComplexArray.Multiply(totalSum, constants);
                combining = ComplexArray.Multiply(combining, oscil);
                
                Complex value_KL = ComplexArray.Sum(combining);
                /*if (l == 0) {
                    value_KL = new Complex (value_KL.getReal(), 0.0);
                }*/
                SPECTRA[k][Q+l] = value_KL.conjugate().multiply(oscil);
                SPECTRA[k][Q-l] = value_KL;
            }
        }
            
        // Loop through the SPECTRA and the Ys and get rid of NaN and Infinity
        this._spectra = ComplexArray.RemoveNaNInf(SPECTRA);
        this._m = M;
        this._n = N;
        this._q = Q;
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
        DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(DATA, Q, true);
        
        Complex[][] spectra = dst.GetSpectra();
        //Utilities.PrintComplexDoubleArray(spectra);
        //Complex[] spectraCompr = dst.GetSpectraCompressed();
        //int m = dst.GetM();
        //int n = dst.GetN();
        
        //System.out.println("Spectra");
        //Utilities.PrintComplexDoubleArray(spectra);
        
        System.out.println("Spectra Compressed");
        //Utilities.PrintComplexDoubleArray(spectraCompr);
        dst.PrintSpectraCompressed();
        
        //System.out.println("Y");
        //Utilities.PrintComplexDoubleArray(y);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.Utilities;
import com.mrsharky.helpers.LegendreGausWeights;
import com.google.common.math.BigIntegerMath;
import com.mrsharky.discreteSphericalTransform.AssociatedLegendrePolynomials;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.ComplexArray;
import static com.mrsharky.helpers.Utilities.minToMaxByOne;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import static com.mrsharky.helpers.Utilities.linspace;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import com.mrsharky.helpers.DoubleArray;
import java.math.BigInteger;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;

/*******************************
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 * @author Julien Pierret
 *******************************/
public class DiscreteSphericalTransform_new {
    
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
    
    public Complex[][][] GetY() {
        return this._y;
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
    
    private double SummationHelper_old(int K, int L) throws Exception {
        if (L > K) {
            throw new Exception("L > K!!!");
        }
        double output = 0;
        if (K-L == 0) {
            return 1;
        }
        for (int i = K-L+1; i <= K+L; i++) {
            output = output + i;
        }
        return output + 2*K+1;
    }
    
    private double SummationHelper2(int K, int L) throws Exception {
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
        return Math.exp(left - right);
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
        return Math.exp((left - right)*0.5);
    }
    
    private double SummationHelper3(int K, int L) throws Exception {
        if (L > K) {
            throw new Exception("L > K!!!");
        }        
        double top = BigIntegerMath.factorial(K-L).doubleValue();
        double bot = BigIntegerMath.factorial(K+L).doubleValue();
        double output = top / bot;
        return output;
    }
    

    public DiscreteSphericalTransform_new(double[][] DATA, int Q, boolean GAUSQUADon) throws Exception {
        int M = DATA.length+1;
        int N = DATA[0].length;
        
        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED
        
        double[] phi = DoubleArray.Cos(legZerosRad);                            // TESTED
        AssociatedLegendrePolynomials P_k_l = new AssociatedLegendrePolynomials(Q,phi);
        double[] theta = DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);  // TESTED
        Complex[][] expHelp = this.ExponentialHelper(Q, theta);                 // TESTED        
        Complex[][] SPECTRA = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
         
        for (int k = 0; k <= Q; k++) {
            double constants = Math.sqrt(Math.PI * (2*k+ 1))/N;
            for (int l = 0; l <= k; l++) {
                double oscil = Math.pow(-1, l);
                //double factorials = Math.sqrt(SummationHelper(k,l));
                double factorials = SummationHelper(k,l);
                double[] pkl = P_k_l.GetAsDouble(k, l);
                Complex[] totalSum = ComplexArray.CreateComplex(phi.length);                
                for (int t = 0; t < theta.length; t++) {
                    Complex currThetaExp = expHelp[l][t];
                    double[] dataSub = DoubleArray.GetColumn(DATA, t);
                    double[] currValue = DoubleArray.Multiply(dataSub, gausWeights);
                    currValue = DoubleArray.Multiply(currValue, pkl);
                    Complex[] value = ComplexArray.Multiply(currValue, currThetaExp);
                    totalSum = ComplexArray.Add(totalSum, value);
                }
                
                /*for (int t = 0; t < theta.length; t++) {
                    Complex currThetaExp = expHelp[l][t];
                    double[] dataSub = DoubleArray.GetColumn(DATA, t);
                    Complex[] dataSubLog = ComplexHelpers.Log(dataSub);
                    double[] gausWeights2 = DoubleArray.Log(gausWeights);
                    Complex[] currValue = ComplexHelpers.Add(dataSubLog, gausWeights2);
                    Complex[] pkl2 = ComplexHelpers.Log(pkl);
                    currValue = ComplexHelpers.Add(currValue, pkl2);
                    Complex currThetaExp2 = currThetaExp.log();
                    Complex[] value = ComplexHelpers.Add(currValue, currThetaExp2);
                    value = ComplexHelpers.Exp(value);
                    totalSum = ComplexHelpers.Add(totalSum, value);
                    //ComplexHelpers.Print(totalSum);
                }*/
                
                Complex[] combining = ComplexArray.Multiply(totalSum, constants);
                combining = ComplexArray.Multiply(combining, oscil);
                combining = ComplexArray.Multiply(combining, factorials);
                
                Complex value_KL = ComplexArray.Sum(combining);
                SPECTRA[k][Q+l] = value_KL.conjugate().multiply(oscil);
                SPECTRA[k][Q-l] = value_KL;
            }
        }
            
        // Loop through the SPECTRA and the Ys and get rid of NaN and Infinity
        this._spectra = ComplexArray.RemoveNaNInf(SPECTRA);
        this._m = M;
        this._n = N;
        this._q = Q;
        System.out.println();
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
        
        int Q = 160;
        DiscreteSphericalTransform_new dst = new DiscreteSphericalTransform_new(DATA, Q, true);
        
        Complex[][] spectra = dst.GetSpectra();
        ComplexArray.Print(spectra);
        Complex[] spectraCompr = dst.GetSpectraCompressed();
        Complex[][][] y = dst.GetY();
        int m = dst.GetM();
        int n = dst.GetN();
        
        //System.out.println("Spectra");
        //Utilities.PrintComplexDoubleArray(spectra);
        
        System.out.println("Spectra Compressed");
        ComplexArray.Print(spectraCompr);
        
        //System.out.println("Y");
        //Utilities.PrintComplexDoubleArray(y);
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.MatrixUtilities;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.discreteSphericalTransform.old.LegendreGausWeights;
import static com.mrsharky.helpers.Utilities.minToMaxByOne;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import static com.mrsharky.helpers.Utilities.linspace;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import com.mrsharky.helpers.DoubleArray;

/**
 *
 * @author dafre
 */
public class DiscreteSphericalTransformLogDiv_old {
    
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

    public DiscreteSphericalTransformLogDiv_old(double[][] DATA, int Q, boolean GAUSQUADon) throws Exception {
        int M = DATA.length+1;
        int N = DATA[0].length;
        FieldMatrix<Complex> DATA_comp = MatrixUtilities.ToFieldMatrixComplex(
                new Array2DRowRealMatrix(DATA)
                );
        
        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);
        RealMatrix legZerosM = lgw.GetValues();
        RealMatrix gausWeights = lgw.GetWeights().transpose();
        RealMatrix legZerosRad = MatrixUtilities.ArcSin(legZerosM).scalarAdd(Math.PI/2.0);
        
        Complex[][][] Y = ComplexArray.CreateComplex(2*(Q+1)+1,N,Q+1);
        Complex[][][] SPECTRABUILDING = ComplexArray.CreateComplex(2*(Q+1)+1,N,Q+1);
        Complex[][] SPECTRA = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        System.out.println("Begin Spatial to Spectal Conversion (Q = " + Q + ")");
        for (int k = 0; k < Q+1; k++) {
            System.out.print("\rk=" + k + " of " + Q + "");
            for (int counterM = 1; counterM < M; counterM++) {
                double phi = legZerosRad.getEntry(counterM-1, 0);
                
                RealMatrix legTemp = (new AssociatedLegendrePolynomials(k,Math.cos(phi))).GetAsRealMatrix();
                RealMatrix counterN = DoubleArray.ToRealMatrix(linspace(1.0,N,N));
                RealMatrix theta = counterN.scalarMultiply(2*Math.PI/N);
                RealMatrix l = DoubleArray.ToRealMatrix(minToMaxByOne(0.0,k));
                
                RealMatrix KminusL = l.scalarMultiply(-1.0).scalarAdd(k);
                RealMatrix KplusL = l.scalarAdd(k);
                int numberOfFactorials = KminusL.getRowDimension();
                RealMatrix divFactorial = (MatrixUtils.createRealMatrix(1, numberOfFactorials));
                
                for (int counter = 1; counter < numberOfFactorials+1; counter++ ) {
                    double left = DoubleArray.SumArray(DoubleArray.Log(minToMaxByOne(1.0,(int) KminusL.getEntry(counter-1, 0))));
                    double right = DoubleArray.SumArray(DoubleArray.Log(minToMaxByOne(1.0,(int) KplusL.getEntry(counter-1, 0))));
                    //double both = Math.exp(left - right);
                    double both = (left - right);
                    divFactorial.setEntry(0, counter-1, both);
                }
                
                // Ys
                FieldMatrix<Complex> YposL;
                FieldMatrix<Complex> YnegL;
                {
                    FieldMatrix<Complex> theta_comp = MatrixUtilities.ToFieldMatrixComplex(theta);
                    FieldMatrix<Complex> l_comp = MatrixUtilities.ToFieldMatrixComplex(l);
                    FieldMatrix<Complex> middle = MatrixUtilities.ToComplexFieldMatrix(
                                MatrixUtilities.Tile(legTemp,1,N));

                    // Note we are carrying over the log trick over to the left part of the equation,
                    // So "left" is where we back out of it
                    double inLogs = Math.log(2*k+1) + 
                            -1.0*Math.log(4.0) + 
                            -1.0*Math.log(Math.PI);
                    FieldMatrix<Complex> left = MatrixUtilities.ToFieldMatrixComplex(
                            MatrixUtilities.ElementWiseExponential(
                                divFactorial.scalarAdd(inLogs).scalarMultiply(0.5))).transpose();

                    // YposL
                    {
                        FieldMatrix<Complex> exponential = MatrixUtilities.ElementWiseExponential(
                                l_comp.scalarMultiply(new Complex(0.0, 1.0))
                                .multiply(theta_comp.transpose()));
                        YposL = MatrixUtilities.ElementwiseMultiplication(
                                MatrixUtilities.ElementwiseMultiplication(
                                    MatrixUtilities.Tile(left, 1,N), middle),
                                exponential);
                    }

                    // YnegL
                    {
                        FieldMatrix<Complex> osc = MatrixUtilities.ToComplexFieldMatrix(MatrixUtilities.ElementwiseMatrixPower(MatrixUtilities.Tile(DoubleArray.ToRealMatrix(new double[]{-1}),1,k+1)
                                    , l.transpose()));
                        osc = MatrixUtilities.ElementwiseMultiplication(osc.transpose(), left);

                        FieldMatrix<Complex> exponential = MatrixUtilities.ElementWiseExponential(
                                l_comp.scalarMultiply(new Complex(0.0, -1.0))
                                .multiply(theta_comp.transpose()));

                        YnegL = MatrixUtilities.ElementwiseMultiplication(
                                MatrixUtilities.ElementwiseMultiplication(
                                    MatrixUtilities.Tile(osc, 1,N), middle),
                                exponential);
                    }
                }
                
                int[] lplusQ = ComplexArray.ToInt(l.scalarAdd(Q).getColumn(0));
                int[] negLplusQ = ComplexArray.ToInt(l.scalarMultiply(-1.0).scalarAdd(Q).getColumn(0));
                int[] vec2 = ComplexArray.ToInt(minToMaxByOne(0.0,N-1));
                ComplexArray.SetData(Y, YposL.getData(), lplusQ, vec2, k);
                ComplexArray.SetData(Y, YnegL.getData(), negLplusQ, vec2, k);
                
                double gausWeightMN;
                if (GAUSQUADon) {
                    gausWeightMN = 2*Math.PI/N * gausWeights.getEntry(0,counterM-1);
                } else {
                    gausWeightMN = 4*Math.PI/Math.pow(N, 2.0);
                }
                
                // Spectra Building
                {
                    FieldMatrix<Complex> middle = MatrixUtilities.Tile(DATA_comp.getRowMatrix(counterM-1),k+1,1);
                    // Spectra Building Positive
                    {
                        FieldMatrix<Complex> osc = MatrixUtilities.Tile(MatrixUtilities.ToComplexFieldMatrix(MatrixUtilities.ElementwiseMatrixPower(MatrixUtilities.Tile(DoubleArray.ToRealMatrix(new double[]{-1}),1,k+1)
                                        , l.transpose())).transpose(),1,N);
                        FieldMatrix<Complex> spectraLeft = MatrixUtilities.ElementwiseMultiplication(
                                MatrixUtilities.ElementwiseMultiplication(osc, middle),
                                YnegL).scalarMultiply(new Complex(gausWeightMN, 0.0));

                        ComplexArray.SetData(SPECTRABUILDING, spectraLeft.getData(), lplusQ, vec2, k);
                    }
                    // Spectra Building Negative
                    {
                        FieldMatrix<Complex> osc = MatrixUtilities.Tile(MatrixUtilities.ToComplexFieldMatrix(MatrixUtilities.ElementwiseMatrixPower(MatrixUtilities.Tile(DoubleArray.ToRealMatrix(new double[]{-1}),1,k+1)
                                        , l.scalarMultiply(-1.0).transpose())).transpose(),1,N);
                        FieldMatrix<Complex> spectraRight = MatrixUtilities.ElementwiseMultiplication(
                                MatrixUtilities.ElementwiseMultiplication(osc, middle),
                                YposL).scalarMultiply(new Complex(gausWeightMN, 0.0));

                        ComplexArray.SetData(SPECTRABUILDING, spectraRight.getData(), negLplusQ, vec2, k);
                    }
                }

                RealMatrix l2 = DoubleArray.ToRealMatrix(minToMaxByOne(-k + 0.0,k + 0.0));
                int[] l2plusQ = ComplexArray.ToInt(l2.scalarAdd(Q).getColumn(0));
                Complex[][] currSpectra = ComplexArray.GetData(SPECTRABUILDING, l2plusQ, vec2, k);
                Complex[] newSpectra = ComplexArray.RowSum(currSpectra);
                Complex[] oldSpectra = ComplexArray.GetData(SPECTRA, k, l2plusQ);
                ComplexArray.SetData(SPECTRA, ComplexArray.Add(newSpectra,oldSpectra), k, l2plusQ);
            }
        }
        
        // Loop through the SPECTRA and the Ys and get rid of NaN and Infinity
        this._spectra = ComplexArray.RemoveNaNInf(SPECTRA);
        this._y = ComplexArray.RemoveNaNInf(Y);
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
        
        int Q = 50;
        DiscreteSphericalTransformLogDiv_old dst = new DiscreteSphericalTransformLogDiv_old(DATA, Q, true);
        
        Complex[][] spectra = dst.GetSpectra();
        Complex[] spectraCompr = dst.GetSpectraCompressed();
        Complex[][][] y = dst.GetY();
        int m = dst.GetM();
        int n = dst.GetN();
        
        System.out.println("Spectra");
        ComplexArray.Print(spectra);
        
        System.out.println("Spectra Compressed");
        ComplexArray.Print(spectraCompr);
        
        System.out.println("Y");
        //Utilities.PrintComplexDoubleArray(y);
    }
}

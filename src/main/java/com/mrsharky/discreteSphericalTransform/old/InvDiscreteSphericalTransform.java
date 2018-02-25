/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.MatrixUtilities;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.discreteSphericalTransform.old.AssociatedLegendrePolynomials;
import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransformLogDiv;
import static com.mrsharky.helpers.MatrixUtilities.ToComplexFieldMatrix;
import static com.mrsharky.helpers.Utilities.arange;
import static com.mrsharky.helpers.Utilities.linspace;
import static com.mrsharky.helpers.Utilities.minToMaxByOne;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.RealMatrix;
import com.mrsharky.discreteSphericalTransform.old.LegendreGausWeights;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;

/**
 *
 * @author dafre
 */
public class InvDiscreteSphericalTransform {
    
    private Complex[][] _spatial;
    
    public Complex[][] GetSpatial(){
        return this._spatial;
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
    
    public InvDiscreteSphericalTransform(Complex[] SPECTRA, int M, int N, int Q, boolean GAUSQUADon) throws Exception {
        Complex[][] unCompressedSpectral = UnCompressSpectral(SPECTRA, Q);
        Initialize(unCompressedSpectral, M, N, GAUSQUADon);
    }
    
    public InvDiscreteSphericalTransform(Complex[][] SPECTRA, int M, int N, boolean GAUSQUADon) throws Exception {
        Initialize(SPECTRA, M, N, GAUSQUADon);
    }
    
    private void Initialize(Complex[][] SPECTRA, int M, int N, boolean GAUSQUADon) throws Exception {

        int Q = SPECTRA.length-1;

        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);

        RealMatrix legZerosM;
        if (GAUSQUADon == true) {
            legZerosM = lgw.GetValues();
            RealMatrix gausWeights = lgw.GetWeights().transpose();
        } else {
            double distanceBetween = 2.0/M;
            legZerosM = DoubleArray.ToRealMatrix(arange(-1+distanceBetween, 1, distanceBetween));
        }
        RealMatrix legZerosRad = MatrixUtilities.ArcSin(legZerosM).scalarAdd(Math.PI/2.0);

        Complex[][][] Y = ComplexArray.CreateComplex(2*(Q+1)+1,N,Q+1);
        Complex[][] DATAREBUILT = ComplexArray.CreateComplex(M-1,N);
        
        // Common Objects
        RealMatrix counterN = DoubleArray.ToRealMatrix(linspace(1.0,N,N));
        RealMatrix theta = counterN.scalarMultiply(2*Math.PI/N);

        System.out.println("Begin Spectral to Spatial Conversion (Q = " + Q + ")");
        for (int k = 0; k < Q+1; k++) {
            // Common
            RealMatrix l = DoubleArray.ToRealMatrix(minToMaxByOne(0.0,k));
            RealMatrix KminusL = l.scalarMultiply(-1.0).scalarAdd(k);
            RealMatrix KplusL = l.scalarAdd(k);
            int numberOfFactorials = KminusL.getRowDimension();
            FieldMatrix<Complex> divFactorial = ToComplexFieldMatrix(MatrixUtils.createRealMatrix(1, numberOfFactorials));
            int[] lplusQ = ComplexArray.ToInt(l.scalarAdd(Q).getColumn(0));
            int[] negLplusQ = ComplexArray.ToInt(l.scalarMultiply(-1.0).scalarAdd(Q).getColumn(0));
            int[] vec2 = ComplexArray.ToInt(minToMaxByOne(0.0,N-1));
            RealMatrix l2 = DoubleArray.ToRealMatrix(minToMaxByOne(-k + 0.0,k + 0.0));
            int[] l2plusQ = ComplexArray.ToInt(l2.scalarAdd(Q).getColumn(0));
            int[] counterNminus1 = linspace(0,N-1,N);
            
            // @ counter == 90, numbers get too small
            for (int counter = 1; counter < numberOfFactorials+1; counter++ ) {
                double left = DoubleArray.SumArray(DoubleArray.Log(minToMaxByOne(1.0,(int) KminusL.getEntry(counter-1, 0))));
                double right = DoubleArray.SumArray(DoubleArray.Log(minToMaxByOne(1.0,(int) KplusL.getEntry(counter-1, 0))));
                double both = Math.exp(left - right);
                divFactorial.setEntry(0, counter-1, new Complex(both, 0.0));
            }
            
            // Ys
            FieldMatrix<Complex> theta_comp = MatrixUtilities.ToFieldMatrixComplex(theta);
            FieldMatrix<Complex> l_comp = MatrixUtilities.ToFieldMatrixComplex(l);
            FieldMatrix<Complex> Yleft = MatrixUtilities.ElementWiseSqrt(
                                    divFactorial.scalarMultiply(new Complex((2*k+1)/(4*Math.PI),0.0))
                                ).transpose();
            FieldMatrix<Complex> exponentialYposL = MatrixUtilities.ElementWiseExponential(
                                l_comp.scalarMultiply(new Complex(0.0, 1.0))
                                .multiply(theta_comp.transpose()));
            FieldMatrix<Complex> oscYnegL = MatrixUtilities.ToComplexFieldMatrix(MatrixUtilities.ElementwiseMatrixPower(MatrixUtilities.Tile(DoubleArray.ToRealMatrix(new double[]{-1}),1,k+1)
                            , l.transpose()));
            oscYnegL = MatrixUtilities.ElementwiseMultiplication(oscYnegL.transpose(), Yleft);
            FieldMatrix<Complex> exponentialYnegL = MatrixUtilities.ElementWiseExponential(
                        l_comp.scalarMultiply(new Complex(0.0, -1.0))
                        .multiply(theta_comp.transpose()));
            FieldMatrix<Complex> left = (new Array2DRowFieldMatrix<Complex>(ComplexArray.GetData(SPECTRA, k, l2plusQ))).transpose();
            
            System.out.print("\rk=" + k + " of " + Q + "");
            for (int counterM = 1; counterM < M; counterM++) {
                double phi = legZerosRad.getEntry(counterM-1, 0);
                // build the associated legendre values needed for sphereical harmonics function
                RealMatrix legTemp = (new AssociatedLegendrePolynomials(k,Math.cos(phi))).GetAsRealMatrix();

                // Ys
                FieldMatrix<Complex> middle = MatrixUtilities.ToComplexFieldMatrix(MatrixUtilities.Tile(legTemp,1,N));
                FieldMatrix<Complex> YposL = MatrixUtilities.ElementwiseMultiplication(MatrixUtilities.ElementwiseMultiplication(MatrixUtilities.Tile(Yleft, 1,N), middle),
                        exponentialYposL);
                FieldMatrix<Complex> YnegL = MatrixUtilities.ElementwiseMultiplication(MatrixUtilities.ElementwiseMultiplication(MatrixUtilities.Tile(oscYnegL, 1,N), middle),
                        exponentialYnegL);

                ComplexArray.SetData(Y, YposL.getData(), lplusQ, vec2, k);
                ComplexArray.SetData(Y, YnegL.getData(), negLplusQ, vec2, k);

                /*Complex[][] currSpectra = ComplexHelpers.GetData(SPECTRABUILDING, l2plusQ, vec2, k);
                Complex[] newSpectra = ComplexHelpers.RowSum(currSpectra);
                Complex[] oldSpectra = ComplexHelpers.GetData(SPECTRA, k, l2plusQ);
                ComplexHelpers.SetData(SPECTRA, ComplexHelpers.Add(newSpectra,oldSpectra), k, l2plusQ);*/

                FieldMatrix<Complex> right = new Array2DRowFieldMatrix<Complex>(ComplexArray.GetData(Y, l2plusQ, counterNminus1, k));
                Complex[] dotProduct = left.multiply(right).getRow(0);
                Complex[] oldRebuilt = ComplexArray.GetData(DATAREBUILT, counterM-1, counterNminus1);
                ComplexArray.SetData(DATAREBUILT, ComplexArray.Add(oldRebuilt, dotProduct) ,counterM-1, counterNminus1);         

                /*
                DATAREBUILT[counterM-1, counterN-1] = (np.array(DATAREBUILT[counterM-1, counterN-1], ndmin=2) + np.dot(np.array(SPECTRA[k, l2+(Q)], ndmin=2), Y[l2+(Q),:,k])).flatten()
                */
            }
            //print("\rk = %d of %d" % (k, Q), end="")
        }
        //print("\n")
        this._spatial = DATAREBUILT;        
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
        
        int Q = 5;
        DiscreteSphericalTransformLogDiv dst = new DiscreteSphericalTransformLogDiv(DATA, Q, true);
        
        Complex[][] spectra = dst.GetSpectra();
        Complex[] spectraCompr = dst.GetSpectraCompressed();
        Complex[][][] y = dst.GetY();
        int m = dst.GetM();
        int n = dst.GetN();
  
        InvDiscreteSphericalTransform invdst = new InvDiscreteSphericalTransform(spectraCompr, m, n, Q, true);
        Complex[][] spatial = invdst.GetSpatial();
        ComplexArray.Print(spatial);

    }
    
}

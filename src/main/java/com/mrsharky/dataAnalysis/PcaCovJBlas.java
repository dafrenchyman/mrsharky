/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataAnalysis;

import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import org.javatuples.Triplet;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import static com.mrsharky.helpers.JblasMatrixHelpers.Print;
import static com.mrsharky.helpers.JblasMatrixHelpers.Abs;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import static com.mrsharky.helpers.JblasMatrixHelpers.RowMultiplyExpansion;
import static com.mrsharky.helpers.JblasMatrixHelpers.GetRowColValues;
import static com.mrsharky.helpers.JblasMatrixHelpers.ColumnMax;
import static com.mrsharky.helpers.JblasMatrixHelpers.JblasToApacheMath3;
import static com.mrsharky.helpers.JblasMatrixHelpers.JblasToVectorApacheMath3;
import org.apache.commons.math3.complex.Complex;
import static org.jblas.Singular.fullSVD;

/**
 *
 * @author mrsharky
 */
public class PcaCovJBlas {
    
    private ComplexDoubleMatrix _eigenValues;
    private ComplexDoubleMatrix _eigenVectors;
    private double[] _varianceExplained;
    
    public ComplexDoubleMatrix GetEigenValues() {
        return this._eigenValues;
    }
    
    public Complex[] GetEigenValuesMath3() throws Exception {
        return JblasToVectorApacheMath3(this._eigenValues);
    }
    
    public ComplexDoubleMatrix GetEigenVectors() {
        return this._eigenVectors;
    }
    
    public Complex[][] GetEigenVectorsMath3() {
        return JblasToApacheMath3(this._eigenVectors);
    }
    
    public double[] GetVarianceExplained() {
        return this._varianceExplained;
    }
    
    public double[] GetSumOfVarianceExplained() {
        double total = 0.0;
        double[] result = new double[_varianceExplained.length];
        for (int i = 0; i < _varianceExplained.length; i++) {
            total += _varianceExplained[i];
            result[i] = total;
        }
        return result;
    }
    
    public PcaCovJBlas(Complex[][] X_cov) {
        ProcessPcaCov(ApacheMath3ToJblas(X_cov));
    }
    
    public PcaCovJBlas(ComplexDoubleMatrix X_cov) {
        ProcessPcaCov(X_cov);
    }
    
    private void ProcessPcaCov(ComplexDoubleMatrix X_cov) {
        // Get eigenvectors and eigenvalues from SVD
        ComplexDoubleMatrix[] SVD = fullSVD(X_cov);
        ComplexDoubleMatrix V = SVD[0];
        ComplexDoubleMatrix S = SVD[1];
        ComplexDoubleMatrix U = SVD[2];
        
        // Get the Maximum Absolute value per column of V
        ComplexDoubleMatrix V_abs = Abs(V);        
        Triplet<ComplexDoubleMatrix, int[], int[]> V_absMax = ColumnMax(V_abs);
        ComplexDoubleMatrix test = V_absMax.getValue0();
        int[] V_absMaxRowLocation = V_absMax.getValue1();
        int[] V_absMaxColLocation = V_absMax.getValue2();
        ComplexDoubleMatrix maxPerColumn = GetRowColValues(V, V_absMaxRowLocation, V_absMaxColLocation);
       
        // Get the new Column sign 
        ComplexDoubleMatrix absMaxPerColumn = Abs(maxPerColumn);            
        ComplexDoubleMatrix colSign = maxPerColumn.divi(absMaxPerColumn);
        ComplexDoubleMatrix coeff = RowMultiplyExpansion(V, colSign);      
        
        // Calculate variance Explained
        double eigenValueTotal = S.sum().real();
        double[] varExplained = new double[S.length];

        for (int row = 0; row < S.length; row++) {
            varExplained[row] = S.get(row,0).real() / eigenValueTotal;
        }
        
        // Now save all the variables
        this._eigenValues = S;
        this._eigenVectors = coeff;
        this._varianceExplained = varExplained;
    }
    
    
    
    public static void main(String args[]) throws Exception {

        double[][] dbl_real = new double[][] {
            {30.02,     2.83,       3.46,       10.83},
            {5.2,       6.1,        1.4,        3.9},
            {12.312,    13.809,     14.674,     3.263},
            {103.1,     23.2,       74.82,      1.86},
            {2.98,      15.36,      16.3,       22.72}
        };
        
        double[][] dbl_imag = new double[][] {
            {3.1,       1.93,       19.72,      4.6},
            {26.91,     27.1,       6.7,        13.82},
            {1.72,      2.71,       62.7,       13.6},
            {13.5,      3.2,        7.2,        8.6},
            {8,         1,          6.7,        2.72}
        };
        
        ComplexDoubleMatrix X = new ComplexDoubleMatrix(
                new DoubleMatrix(dbl_real),
                new DoubleMatrix(dbl_imag));
        
        // Get Column Means
        ComplexDoubleMatrix colMeans = X.columnMeans();
        X.subRowVector(colMeans);
        //X = RowSubtractExpansion(X, colMeans);
        
        // Create the Covarience Matrix (Should divide by number of dates)
        ComplexDoubleMatrix X_conjTrans = X.conj().transpose();
        ComplexDoubleMatrix X_cov = (X_conjTrans.mmul(X)).mul(1/(X.columns + 0.0));
        
        PcaCovJBlas pca = new PcaCovJBlas(X_cov);
        
        Print(pca.GetEigenValues());
        ComplexArray.Print(pca.GetEigenValuesMath3());
        Print(pca.GetEigenVectors());
        ComplexArray.Print(pca.GetEigenVectorsMath3());
        
        DoubleArray.Print(pca.GetVarianceExplained());
    }
}

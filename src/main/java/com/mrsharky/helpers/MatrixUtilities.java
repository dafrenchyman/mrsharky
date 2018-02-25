/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.helpers.DoubleArray;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author dafre
 */
public class MatrixUtilities {
    
    public static FieldMatrix<Complex> ToFieldMatrixComplex(RealMatrix mat) {
        Complex[][] compMat = new Complex[mat.getRowDimension()][mat.getColumnDimension()];
        for (int row = 0; row < compMat.length; row++) {
            for (int col = 0; col < compMat[0].length; col++) {
                compMat[row][col] = new Complex(mat.getEntry(row, col), 0);
            }
        }
        return new Array2DRowFieldMatrix<Complex>(compMat); 
    }
    
    public static RealMatrix Cos(RealMatrix mat) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, Math.cos(mat.getEntry(row, col)));
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> Cos(FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, ret.getEntry(row, col).cos());
            }
        }
        return ret;
    }
    
    public static RealMatrix Sin(RealMatrix mat) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, Math.sin(mat.getEntry(row, col)));
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> Sin(FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, ret.getEntry(row, col).sin());
            }
        }
        return ret;
    }
    
    public static RealMatrix ArcSin (RealMatrix mat) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, Math.asin(mat.getEntry(row, col)));
            }
        }
        return ret;
    }
    
    public static RealMatrix ElementwiseMatrixPower(RealMatrix mat, double exponent) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                double val = mat.getEntry(row, col);
                ret.setEntry(row, col, Math.pow(val, exponent));
            }
        }
        return ret;
    }
    
    public static RealMatrix ElementwiseMatrixPower(RealMatrix mat1, RealMatrix mat2) throws Exception {
        RealMatrix ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    double val1 = mat1.getEntry(row, col);
                    double val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, Math.pow(val1, val2));
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match");
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> ElementwiseMatrixPower(FieldMatrix<Complex> mat1, FieldMatrix<Complex> mat2) throws Exception {
        FieldMatrix<Complex> ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    Complex val1 = mat1.getEntry(row, col);
                    Complex val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, val1.pow(val2));
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match");
        }
        return ret;
    }
    
    public static RealMatrix ElementWiseExponential(RealMatrix mat) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                double val = mat.getEntry(row, col);
                ret.setEntry(row, col, Math.exp(val));
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> Tile(FieldMatrix<Complex> mat, int rowRep, int colRep) {
        Complex[][] retComp = new Complex[mat.getRowDimension()*rowRep][mat.getColumnDimension()*colRep];
        for (int rowCounter = 0; rowCounter < mat.getRowDimension()*rowRep; rowCounter++) {
            int row = rowCounter % mat.getRowDimension();
            for (int colCounter = 0; colCounter < mat.getColumnDimension()*colRep; colCounter++) {
                int col = colCounter % mat.getColumnDimension();
                retComp[rowCounter][colCounter] = mat.getEntry(row, col);
            }
        }
        return new Array2DRowFieldMatrix<Complex>(retComp); 
    }
    
    public static RealMatrix Tile(RealMatrix mat, int rowRep, int colRep) {
        double[][] retComp = new double[mat.getRowDimension()*rowRep][mat.getColumnDimension()*colRep];
        for (int rowCounter = 0; rowCounter < mat.getRowDimension()*rowRep; rowCounter++) {
            int row = rowCounter % mat.getRowDimension();
            for (int colCounter = 0; colCounter < mat.getColumnDimension()*colRep; colCounter++) {
                int col = colCounter % mat.getColumnDimension();
                retComp[rowCounter][colCounter] = mat.getEntry(row, col);
            }
        }
        return new Array2DRowRealMatrix(retComp); 
    }

    public static FieldMatrix<Complex> ElementWiseExponential(FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, mat.getEntry(row, col).exp());
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> ElementWiseLn(FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, mat.getEntry(row, col).log());
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> ElementWiseSqrt (FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, mat.getEntry(row, col).sqrt());
            }
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> RowSums (FieldMatrix<Complex> mat) {
        FieldMatrix<Complex> ret = mat.getColumnMatrix(0);
        for (int row = 0; row < mat.getRowDimension(); row++) {
            Complex total = new Complex(0.0, 0.0);
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                total = total.add(mat.getEntry(row, col));
            }
            ret.setEntry(row, 0, total);
        }
        return ret;
    }
    
    public static RealMatrix ElementwiseMultiplication (RealMatrix mat1, RealMatrix mat2) throws Exception {
        RealMatrix ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    double val1 = mat1.getEntry(row, col);
                    double val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, val1*val2);
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match");
        }
        return ret;
    }
    
    public static RealMatrix ElementwiseAdd (RealMatrix mat1, RealMatrix mat2) throws Exception {
        RealMatrix ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    double val1 = mat1.getEntry(row, col);
                    double val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, val1+val2);
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match");
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> ElementwiseMultiplication (FieldMatrix<Complex> mat1, FieldMatrix<Complex> mat2) throws Exception {
        FieldMatrix<Complex> ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    Complex val1 = mat1.getEntry(row, col);
                    Complex val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, val1.multiply(val2));
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match:\n" +
                    "Mat1: "+ mat1.getRowDimension() + "X" +mat1.getColumnDimension() +"\n" +
                    "Mat2: "+ mat2.getRowDimension() + "X" +mat2.getColumnDimension());
        }
        return ret;
    }
    
    public static FieldMatrix<Complex> ElementwiseMultiplicationTile (FieldMatrix<Complex> mat1, FieldMatrix<Complex> mat2) throws Exception {
        int numRows = Math.max(mat1.getRowDimension(),    mat2.getRowDimension());
        int numCols = Math.max(mat1.getColumnDimension(), mat2.getColumnDimension());
        Complex[][] compMat = new Complex[numRows][numCols];        
        if ((mat1.getColumnDimension() % mat2.getColumnDimension() == 0 || mat2.getColumnDimension() % mat1.getColumnDimension() == 0 ) &&
            (mat1.getRowDimension()    % mat2.getRowDimension() == 0    || mat1.getRowDimension()    % mat2.getRowDimension() == 0)) {
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    Complex val1 = mat1.getEntry(row % mat1.getRowDimension(), col % mat1.getColumnDimension());
                    Complex val2 = mat2.getEntry(row % mat2.getRowDimension(), col % mat2.getColumnDimension());
                    compMat[row][col] = val1.multiply(val2);
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match:\n" +
                    "Mat1: "+ mat1.getRowDimension() + "X" +mat1.getColumnDimension() +"\n" +
                    "Mat2: "+ mat2.getRowDimension() + "X" +mat2.getColumnDimension());
        }
        return new Array2DRowFieldMatrix<Complex>(compMat); 
    }
    
    
    
    public static FieldMatrix<Complex> ElementwiseAdd (FieldMatrix<Complex> mat1, FieldMatrix<Complex> mat2) throws Exception {
        FieldMatrix<Complex> ret = mat1.copy();
        if (mat1.getColumnDimension() == mat2.getColumnDimension() &&
                mat1.getRowDimension() == mat2.getRowDimension()) {    
            for (int row = 0; row < mat1.getRowDimension(); row++) {
                for (int col = 0; col < mat1.getColumnDimension(); col++) {
                    Complex val1 = mat1.getEntry(row, col);
                    Complex val2 = mat2.getEntry(row, col);
                    ret.setEntry(row, col, val1.add(val2));
                }
            }
        } else {
            throw new Exception("Matrix dimensions do not match:\n" +
                    "Mat1: "+ mat1.getRowDimension() + "X" +mat1.getColumnDimension() +"\n" +
                    "Mat2: "+ mat2.getRowDimension() + "X" +mat2.getColumnDimension());
        }
        return ret;
    }
    
    public static RealMatrix MatrixAbs (RealMatrix mat) {
        RealMatrix ret = mat.copy();
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                ret.setEntry(row, col, Math.abs(mat.getEntry(row, col)));
            }
        }
        return ret;
    }
    
    public static double MatrixMax (RealMatrix mat) {
        double maxValue = -Double.MAX_VALUE;
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                double currDouble = mat.getEntry(row, col);
                if (currDouble > maxValue) {
                    maxValue = currDouble;
                }
            }
        }
        return maxValue;
    }
    
    public static double MatrixMin (RealMatrix mat) {
        double minValue = Double.MAX_VALUE;
        for (int row = 0; row < mat.getRowDimension(); row++) {
            for (int col = 0; col < mat.getColumnDimension(); col++) {
                double currDouble = mat.getEntry(row, col);
                if (currDouble < minValue) {
                    minValue = currDouble;
                }
            }
        }
        return minValue;
    }
    
    public static void PrintMatrix(RealMatrix mat) {
        double[][] values = mat.getData();
        DoubleArray.Print(values);
    }
    
    public static void PrintMatrix(FieldMatrix<Complex> mat) {
        Complex[][] values = mat.getData();
        ComplexArray.Print(values);
    }

    public static FieldMatrix<Complex> ToComplexFieldMatrix(RealMatrix mat) {
        double[][] rawData = mat.getData();
        Complex[][] compMat = new Complex[mat.getRowDimension()][mat.getColumnDimension()];
        for (int row = 0; row < compMat.length; row++) {
            for (int col = 0; col < compMat[0].length; col++) {
                compMat[row][col] = new Complex(rawData[row][col], 0);
            }
        }
        return new Array2DRowFieldMatrix<Complex>(compMat); 
    }
    
    
}

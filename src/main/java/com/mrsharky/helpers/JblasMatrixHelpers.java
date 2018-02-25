package com.mrsharky.helpers;

import org.apache.commons.math3.complex.Complex;
import org.javatuples.Triplet;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.ojalgo.matrix.store.ComplexDenseStore;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.scalar.ComplexNumber;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mrsharky
 */
public class JblasMatrixHelpers {
    
    public static ComplexDoubleMatrix OjalgoToJblas(MatrixStore<ComplexNumber> input) {
        DoubleMatrix real = DoubleMatrix.zeros((int) input.countRows(), (int) input.countColumns());
        DoubleMatrix imag = DoubleMatrix.zeros((int) input.countRows(), (int) input.countColumns());
        for (int row = 0; row < input.countRows(); row++) {
            for (int col =0; col < input.countColumns(); col++) {
                real.put(row,col, input.get(row,col).getReal());
                imag.put(row,col, input.get(row,col).getImaginary());
            }
        }
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(real, imag);
        return output;
    }
    
    public static ComplexDenseStore JblasToOjalgo(ComplexDoubleMatrix input) {
        PhysicalStore.Factory<ComplexNumber, ComplexDenseStore> storeFactory = ComplexDenseStore.FACTORY;
        ComplexDenseStore output = storeFactory.makeZero(input.rows, input.columns);
        for (int row = 0; row < input.rows; row++) {
            for (int col = 0; col < input.columns; col++) {
                ComplexDouble complexDouble = input.get(row,col);
                output.set(row, col, ComplexNumber.of(complexDouble.real(), complexDouble.imag()));
            }
        }
        return output;
    }
    
    public static Complex[] JblasToVectorApacheMath3(ComplexDoubleMatrix input) throws Exception {
        Complex[] output = null;
        if (input.columns == 1) {
            output = new Complex[input.rows];
            for (int row = 0; row < input.rows; row++) {
                ComplexDouble complexDouble = input.get(row,0);
                output[row] = new Complex(complexDouble.real(), complexDouble.imag());
            }
        } else if (input.rows == 1) {
            output = new Complex[input.columns];
            for (int col = 0; col < input.columns; col++) {
                ComplexDouble complexDouble = input.get(0,col);
                output[col] = new Complex(complexDouble.real(), complexDouble.imag());
            }
        } else {
            throw new Exception("One of the dimensions shoud be equal to 1; rows = " + input.rows + ", columns: " + input.columns);
        }
        return output;
    }
    
    public static Complex[][] JblasToApacheMath3(ComplexDoubleMatrix input) {
        Complex[][] output = new Complex[input.rows][input.columns];
        for (int row = 0; row < input.rows; row++) {
            for (int col = 0; col < input.columns; col++) {
                ComplexDouble complexDouble = input.get(row,col);
                output[row][col] = new Complex(complexDouble.real(), complexDouble.imag());
            }
        }
        return output;
    }
    
    public static ComplexDoubleMatrix ApacheMath3ToJblas(Complex[][] input) {
        DoubleMatrix real = DoubleMatrix.zeros( input.length, input[0].length);
        DoubleMatrix imag = DoubleMatrix.zeros( input.length, input[0].length);
        for (int row = 0; row < input.length; row++) {
            for (int col =0; col < input[0].length; col++) {
                real.put(row,col, input[row][col].getReal());
                imag.put(row,col, input[row][col].getImaginary());
            }
        }
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(real, imag);
        return output;
    }
    
    public static void Print(ComplexDoubleMatrix input) {
        System.out.print("\t");
        for (int col = 0; col < input.columns; col++) {
            System.out.print("\t" + (col+1));
        }
        System.out.println();
        
        for (int row = 0; row < input.rows; row++) {
            System.out.print((row+1));
            for (int col = 0; col < input.columns; col++) {
                System.out.print("\t(" + input.get(row, col).real() + ", " + input.get(row,col).imag() + ")");
            }
            System.out.println();
        }
    }
    
    public static ComplexDoubleMatrix Abs(ComplexDoubleMatrix input) {
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(input.rows, input.columns);
        for (int row = 0; row < input.rows; row++) {
            for (int col = 0; col < input.columns; col++) {
                double absValue = Math.sqrt(
                                    Math.pow(input.get(row, col).real(), 2.0) + 
                                    Math.pow(input.get(row, col).imag(), 2.0) 
                                  );
                output.put(row, col, absValue);
            }
        }
        return output;
    }
    
    public static ComplexDoubleMatrix RowMultiplyExpansion(ComplexDoubleMatrix input, ComplexDoubleMatrix rowMult) {
        // Check that rowMult only has 1 row
        
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(input.rows, input.columns);
        for (int row = 0; row < input.rows; row++) {
            for (int col = 0; col < input.columns; col++) {
                ComplexDouble value = input.get(row, col).mul(rowMult.get(0,col));
                output.put(row, col, value);
            }
        }
        return output;
    }
    
    public static ComplexDoubleMatrix RowSubtractExpansion(ComplexDoubleMatrix input, ComplexDoubleMatrix rowSub) {
        // Check that rowMult only has 1 row
        
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(input.rows, input.columns);
        for (int row = 0; row < input.rows; row++) {
            for (int col = 0; col < input.columns; col++) {
                ComplexDouble value = input.get(row, col).add(rowSub.get(0,col).mul(-1)
                );
                output.put(row, col, value);
            }
        }
        return output;
    }
    
    
    
    public static ComplexDoubleMatrix GetRowColValues(ComplexDoubleMatrix input, int[] rows, int[] cols) {
        // Check that the length of row, col are the same
        
        ComplexDoubleMatrix output = new ComplexDoubleMatrix(1, rows.length);
        for (int counter = 0; counter < rows.length; counter++) {
            ComplexDouble value = input.get(rows[counter], cols[counter]);
            output.put(0, counter, value);
        }
        return output;
    }
    
    public static Triplet<ComplexDoubleMatrix, int[], int[]> ColumnMax(ComplexDoubleMatrix input) {
        ComplexDoubleMatrix output_value = new ComplexDoubleMatrix(1, input.columns);
        int output_colLocation[] = new int[input.columns];
        int output_rowLocation[] = new int[input.columns];
        for (int col = 0; col < input.columns; col++) {
            double currMax = Double.MIN_VALUE;
            int currLoc = Integer.MIN_VALUE;
            for (int row = 0; row < input.rows; row++) {
                double value = input.get(row, col).real();
                if (value > currMax) {
                    currMax = value;
                    currLoc = row;
                }
            }
            output_value.put(0, col, currMax);
            output_rowLocation[col] = currLoc;
            output_colLocation[col] = col;
        }
        Triplet output = Triplet.with(output_value, output_rowLocation, output_colLocation);
        return output;
    }
}

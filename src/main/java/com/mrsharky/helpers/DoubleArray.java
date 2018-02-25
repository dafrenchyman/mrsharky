/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author mrsharky
 */
public class DoubleArray {

    public static double[] Multiply(double[] mat, double scaler) {
        double[] ret = new double[mat.length];
        for (int row = 0; row < mat.length; row++) {
            ret[row] = mat[row] * scaler;
        }
        return ret;
    }

    public static double[][] Multiply(double[][] mat1, double[][] mat2) throws Exception {
        if (mat1.length != mat2.length || mat1[0].length != mat2[0].length) {
            throw new Exception("Array dimensions do not agree");
        }
        double[][] ret = new double[mat1.length][mat1[0].length];
        for (int row = 0; row < mat1.length; row++) {
            for (int col = 0; col < mat1[0].length; col++) {
                double val1 = mat1[row][col];
                double val2 = mat2[row][col];
                ret[row][col] = val1 * val2;
            }
        }
        return ret;
    }
    
    public static double[][] Multiply(double[][] mat1, double scaler) {
        double[][] ret = new double[mat1.length][mat1[0].length];
        for (int row = 0; row < mat1.length; row++) {
            for (int col = 0; col < mat1[0].length; col++) {
                ret[row][col] = mat1[row][col] * scaler;
            }
        }
        return ret;
    }
    
    public static double[][] MeshMultiply (double[] comp1, double[] comp2) throws Exception {
        double[][] output = new double[comp1.length][comp2.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp2.length; j++) {
                output[i][j] = comp1[i] * comp2[j];
            }
        }
        return output;
    }
    
    public static double[][] Exp(double[][] mat1) {
        double[][] ret = new double[mat1.length][mat1[0].length];
        for (int row = 0; row < mat1.length; row++) {
            for (int col = 0; col < mat1[0].length; col++) {
                double val1 = Math.exp(mat1[row][col]);
                ret[row][col] = val1;
            }
        }
        return ret;
    }
    
    public static double[] Multiply(double[] mat1, double[] mat2) throws Exception {
        if (mat1.length != mat2.length) {
            throw new Exception("Array dimensions do not agree");
        }
        double[] ret = new double[mat1.length];
        for (int i = 0; i < mat1.length; i++) {
            ret[i] = mat1[i] * mat2[i];
        }
        return ret;
    }

    public static double[] Cos(double[] mat) {
        double[] ret = new double[mat.length];
        for (int i = 0; i < mat.length; i++) {
            ret[i] = Math.cos(mat[i]);
        }
        return ret;
    }

    public static double SumArray(double[] array) {
        double total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
        }
        return total;
    }
    
    public static double SumArray(double[][] array) {
        double total = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[0].length; j++) {
                total += array[i][j];
            }
        }
        return total;
    }

    public static double Max(double[][] mat) {
        double maxValue = -Double.MAX_VALUE;
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[row].length; col++) {
                double currDouble = mat[row][col];
                if (currDouble > maxValue) {
                    maxValue = currDouble;
                }
            }
        }
        return maxValue;
    }

    public static double Max(double[] mat) {
        double maxValue = -Double.MAX_VALUE;
        for (int row = 0; row < mat.length; row++) {
            double currDouble = mat[row];
            if (currDouble > maxValue) {
                maxValue = currDouble;
            }
        }
        return maxValue;
    }

    public static RealMatrix ToRealMatrix(double[] mat) {
        RealMatrix ret = MatrixUtils.createRealMatrix(mat.length, 1);
        for (int row = 0; row < mat.length; row++) {
            ret.setEntry(row, 0, mat[row]);
        }
        return ret;
    }

    public static double[] Log(double[] array) {
        double[] ret = new double[array.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = Math.log(array[i]);
        }
        return ret;
    }
    
    public static double[][] Log(double[][] array) {
        double[][] ret = new double[array.length][array[0].length];
        for (int i = 0; i < ret.length; i++) {
            for (int j = 0; j < ret[0].length; j++) {
                ret[i][j] = Math.log(array[i][j]);
            }
        }
        return ret;
    }

    public static double[][] SetColumn(double[][] mat, int column, double value) {
        double[][] ret = new double[mat.length][mat[0].length];
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[row].length; col++) {
                if (col == column) {
                    ret[row][col] = value;
                } else {
                    ret[row][col] = mat[row][col];
                }
            }
        }
        return ret;
    }

    public static double[][] SetColumn(double[][] mat, int column, double[] value) {
        double[][] ret = new double[mat.length][mat[0].length];
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[row].length; col++) {
                if (col == column) {
                    ret[row][col] = value[row];
                } else {
                    ret[row][col] = mat[row][col];
                }
            }
        }
        return ret;
    }
    
    public static double[] ArcSin (double[] mat) {
        double[] output = new double[mat.length];
        for (int i = 0; i < mat.length; i++) {
            output[i] = Math.asin(mat[i]);
        }
        return output;
    }

    public static void Print(double[][] values) {
        System.out.println(PrintToString(values));
    }
    
    public static String PrintToString(double[][] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int col = 0; col < values[0].length; col++) {
            sb.append("\t" + (col + 1));
        }
        sb.append("\n");
        for (int row = 0; row < values.length; row++) {
            sb.append((row + 1) + ":\t");
            for (int col = 0; col < values[0].length; col++) {
                sb.append(values[row][col] + "\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public static String PrintToString(double[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("\t1\n");
        for (int row = 0; row < values.length; row++) {
            sb.append((row + 1) + ":\t");
            sb.append(values[row] + "\t\n");
        }
        return sb.toString();
    }
    

    public static void Print(double[] values) {
        System.out.println(PrintToString(values));
        /*System.out.println();
        System.out.println("\t" + 1);
        for (int row = 0; row < values.length; row++) {
            System.out.print((row + 1) + ":\t");
            System.out.println(values[row] + "\t");
        }*/
    }

    public static double[] Sin(double[] mat) {
        double[] ret = new double[mat.length];
        for (int row = 0; row < mat.length; row++) {
            ret[row] = Math.sin(mat[row]);
        }
        return ret;
    }

    

    public static double[][] Power(double[][] mat, double exponent) {
        double[][] ret = new double[mat.length][mat[0].length];
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[0].length; col++) {
                double val = mat[row][col];
                ret[row][col] = Math.pow(val, exponent);
            }
        }
        return ret;
    }
    
    public static double[][] Power(double[][] mat, double[][] exponent) {
        double[][] ret = new double[mat.length][mat[0].length];
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[0].length; col++) {
                double val = mat[row][col];
                double exp = exponent[row][col];
                ret[row][col] = Math.pow(val, exp);
            }
        }
        return ret;
    }

    public static double[] Power(double[] mat, double exponent) {
        double[] ret = new double[mat.length];
        for (int row = 0; row < mat.length; row++) {
            double val = mat[row];
            ret[row] = Math.pow(val, exponent);
        }
        return ret;
    }

    public static double[] GetColumn(double[][] mat1, int column) {
        double[] ret = new double[mat1.length];
        for (int row = 0; row < mat1.length; row++) {
            ret[row] = mat1[row][column];
        }
        return ret;
    }

    public static double[] Add(double[] mat, double scaler) {
        double[] ret = new double[mat.length];
        for (int row = 0; row < mat.length; row++) {
            ret[row] = mat[row] + scaler;
        }
        return ret;
    }
    
    public static double[] Add(double[] mat1, double[] mat2) throws Exception {
        if (mat1.length != mat2.length) {
            throw new Exception("Array dimensions do not agree");
        }
        double[] ret = new double[mat1.length];
        for (int i = 0; i < mat1.length; i++) {
            ret[i] = mat1[i] + mat2[i];
        }
        return ret;
    }
    
    public static double[][] Add(double[][] mat1, double[][] mat2) throws Exception {
        if (mat1.length != mat2.length || mat1[0].length != mat2[0].length) {
            throw new Exception("Array dimensions do not agree");
        }
        double[][] ret = new double[mat1.length][mat1[0].length];
        for (int i = 0; i < mat1.length; i++) {
            for (int j = 0; j < mat1[0].length; j++) {
                ret[i][j] = mat1[i][j] + mat2[i][j];
            }
        }
        return ret;
    }
    
    public static double[][] Add(double[][] mat1, double value) throws Exception {
        double[][] ret = new double[mat1.length][mat1[0].length];
        for (int i = 0; i < mat1.length; i++) {
            for (int j = 0; j < mat1[0].length; j++) {
                ret[i][j] = mat1[i][j] + value;
            }
        }
        return ret;
    }
    
    public static double[][] Resize(double[][] orig, int row, int col) {
        double[][] ret = new double[row][col];
        
        int rowMax = Math.min(row, orig.length);
        int colMax = Math.min(col, orig[0].length);
        
        for (int i = 0; i < rowMax; i++) {
            for (int j = 0; j < colMax; j++) {
                ret[i][j] = orig[i][j];
            }
        }
        return ret;
    }
    
    public static double[] Resize(double[] orig, int length) {
        double[] ret = new double[length];
        int lengthMax = Math.min(length, orig.length);
        for (int i = 0; i < lengthMax; i++) {
            ret[i] = orig[i];
        }
        return ret;
    }

    public static double[] GetRow(double[][] mat1, int row) {
        double[] ret = new double[mat1[0].length];
        for (int col = 0; col < mat1[0].length; row++) {
            ret[row] = mat1[row][col];
        }
        return ret;
    }

    public static double[] Abs(double[] mat) {
        double[] ret = new double[mat.length];
        for (int row = 0; row < mat.length; row++) {
            ret[row] = Math.abs(mat[row]);
        }
        return ret;
    }

    public static double[][] Abs(double[][] mat) {
        double[][] ret = new double[mat.length][mat[0].length];
        for (int row = 0; row < mat.length; row++) {
            for (int col = 0; col < mat[row].length; col++) {
                ret[row][col] = Math.abs(mat[row][col]);
            }
        }
        return ret;
    }
    
    public static double[] Subset (double[] comp1, int rowStart, int rowEnd) throws Exception {
        // If the start/end were submitted backwards, flip them
        if (rowStart > rowEnd) {
            int tmp = rowStart;
            rowStart = rowEnd;
            rowEnd = tmp;
        }
        
        // Check that the "end" is bigger than the size
        if (rowEnd > comp1.length) {
            throw new Exception("Not a valid subset.");
        }
        
        double[] comp = new double[rowEnd-rowStart+1];
        for (int i = rowStart; i <= rowEnd; i++) {
            comp[i-rowStart] = comp1[i];
        }  
        return comp;
    }
    
}

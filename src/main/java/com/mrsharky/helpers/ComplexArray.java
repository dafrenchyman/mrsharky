/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Triplet;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.ojalgo.matrix.store.ComplexDenseStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.scalar.ComplexNumber;

/**
 *
 * @author dafre
 */
public class ComplexArray {
    
    public static Complex[] GetColumn(Complex[][] mat1, int column) {
        Complex[] ret = new Complex[mat1.length];
        for (int row = 0; row < mat1.length; row++) {
            Complex curr = mat1[row][column];
            ret[row] = new Complex(curr.getReal(), curr.getImaginary());
        }
        return ret;
    }
    
    public static Complex[] GetRow(Complex[][] mat1, int row) {
        Complex[] ret = new Complex[mat1[0].length];
        for (int col = 0; col < mat1[0].length; col++) {
            Complex curr = mat1[row][col];
            ret[col] = new Complex(curr.getReal(), curr.getImaginary());
        }
        return ret;
    }
    
    public static Complex[][] ComplexDenseStoreToComplex(ComplexDenseStore comp) {
        int numRows = (int) comp.countRows();
        int numCols = (int) comp.countColumns();
        Complex[][] output = new Complex[numRows][numCols];
        for (int row = 0; row < numRows; row++) {
            for (int col = 0; col < numCols; col++) {
                double real = comp.get(row, col).getReal();
                double imag = comp.get(row, col).getImaginary();
                output[row][col] = new Complex(real,imag);
            }
        }
        return output;
    }
    
    public static Complex[][] ComplexNumberToComplex(ComplexNumber[][] comp) {
        Complex[][] output = new Complex[comp.length][comp[0].length];
        for (int row = 0; row < comp.length; row++) {
            for (int col = 0; col < comp[0].length; col++) {
                if (comp[row][col] == null) {
                    output[row][col] = new Complex(0.0, 0.0);
                } else {
                    double real = comp[row][col].getReal();
                    double imag = comp[row][col].getImaginary();
                    output[row][col] = new Complex(real, imag);
                }
            }
        }
        return output;
    }
    
    public static ComplexDenseStore ComplexToComplexDenseStore(Complex[][] comp) {
        PhysicalStore.Factory<ComplexNumber, ComplexDenseStore> storeFactory = ComplexDenseStore.FACTORY;
        ComplexDenseStore output = storeFactory.makeZero(comp.length, comp[0].length);
        for (int row = 0; row < comp.length; row++) {
            for (int col = 0; col < comp[0].length; col++) {
                double real = comp[row][col].getReal();
                double imag = comp[row][col].getImaginary();
                output.set(row, col, ComplexNumber.of(real, imag));
            }
        }
        return output;
    }
    
    public static Complex[][] RemoveNaNInf(Complex [][] comp) {
        Complex[][] out = new Complex[comp.length][comp[0].length];
        for (int i = 0; i < comp.length; i++) {
            for (int j = 0; j < comp[0].length; j++) {
                Complex curr = comp[i][j];
                out[i][j] = curr.isNaN() || curr.isInfinite() ? new Complex(0.0, 0.0) : new Complex(curr.getReal(), curr.getImaginary());
            }
        }
        return out;
    }
    
    public static Complex[][][] RemoveNaNInf(Complex [][][] comp) {
        Complex[][][] out = new Complex[comp.length][comp[0].length][comp[0][0].length];
        for (int i = 0; i < comp.length; i++) {
            for (int j = 0; j < comp[0].length; j++) {
                for (int k = 0; k < comp[0][0].length; k++) {
                    Complex curr = comp[i][j][k];
                    out[i][j][k] = curr.isNaN() || curr.isInfinite() ? new Complex(0.0, 0.0) : new Complex(curr.getReal(), curr.getImaginary());
                }
            }
        }
        return out;
    }
    
    public static Complex[][] Subset (Complex[][] comp1, int rowStart, int rowEnd, int colStart, int colEnd) throws Exception {
 
        // If the start/end were submitted backwards, flip them
        if (rowStart > rowEnd) {
            int tmp = rowStart;
            rowStart = rowEnd;
            rowEnd = tmp;
        }
        
        if (colStart > colEnd) {
            int tmp = colStart;
            colStart = colEnd;
            colEnd = tmp;
        }
        
        Complex[][] comp = new Complex[rowEnd-rowStart+1][colEnd-colStart+1];
        
        // Check that the "end" is bigger than the size
        if (rowEnd >= comp1.length || colEnd >= comp1[0].length) {
            throw new Exception("Not a valid subset.");
        }
        
        for (int i = rowStart; i <= rowEnd; i++) {
            for (int j = colStart ; j <= colEnd; j++) {
                Complex curr = comp1[i][j];
                comp[i-rowStart][j-colStart] = new Complex(curr.getReal(), curr.getImaginary());
            }
        }  
        return comp;
    }
    
    public static Complex[] Subset (Complex[] comp1, int rowStart, int rowEnd) throws Exception {

        // If the start/end were submitted backwards, flip them
        if (rowStart > rowEnd) {
            int tmp = rowStart;
            rowStart = rowEnd;
            rowEnd = tmp;
        }

        // Check that the "end" is bigger than the size
        if (rowEnd >= comp1.length) {
            throw new Exception("Not a valid subset.");
        }
        
        Complex[] comp = new Complex[rowEnd-rowStart+1];
        
        for (int i = rowStart; i <= rowEnd; i++) {
            Complex curr = comp1[i];
            comp[i-rowStart] = new Complex(curr.getReal(), curr.getImaginary());
        }  
        return comp;
    }
    
    public static Complex[] Add (Complex[] comp1, Complex[] comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                comp[i] = comp1[i].add(comp2[i]);
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + "\n" +
                    "Comp2: " + comp2.length);
        }
        return comp;
    }
    
    public static Complex[] Add (Complex[] comp1, double[] comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                comp[i] = comp1[i].add(comp2[i]);
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + "\n" +
                    "Comp2: " + comp2.length);
        }
        return comp;
    }
    
    public static Complex[] Add (double comp1, Complex[] comp2) throws Exception {
        return Add(comp2, comp1);
    }
    
    public static Complex[] Add (Complex[] comp1, double comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].add(comp2);
        }    
        return comp;
    }
    
    public static Complex[][] Add (double[][] comp1, Complex[][] comp2) throws Exception {
        return Add(comp2, comp1);
    }
    
    public static Complex[][] Add (Complex[][] comp1, double[][] comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                for (int j =0 ; j < comp1[0].length; j++) {
                    comp[i][j] = comp1[i][j].add(comp2[i][j]);
                }
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + ", " + comp1[0].length + "\n" +
                    "Comp2: " + comp2.length + ", " + comp2[0].length + "\n");
        }
        return comp;
    }
    
    public static Complex[][] Add (Complex[][] comp1, Complex[][] comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                for (int j =0 ; j < comp1[0].length; j++) {
                    comp[i][j] = comp1[i][j].add(comp2[i][j]);
                }
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + ", " + comp1[0].length + "\n" +
                    "Comp2: " + comp2.length + ", " + comp2[0].length + "\n");
        }
        return comp;
    }
    
    public static Complex[][] Add (Complex[][] comp1, Complex comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j =0 ; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].add(comp2);
            }
        }  
        return comp;
    }
    
    public static Complex[][] Add (Complex[][] comp1, double comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j =0 ; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].add(comp2);
            }
        }  
        return comp;
    }
    
    public static Complex[] Add (Complex[] comp1, Complex comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].add(comp2);
        }
        return comp;
    }
    
    public static Complex[] Multiply (Complex[] comp1, Complex comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].multiply(comp2);
        }
        return comp;
    }
    
    public static Complex[][] Multiply (Complex[][] comp1, Complex[][] comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                for (int j =0 ; j < comp1[0].length; j++) {
                    comp[i][j] = comp1[i][j].multiply(comp2[i][j]);
                }
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + ", " + comp1[0].length + "\n" +
                    "Comp2: " + comp2.length + ", " + comp2[0].length + "\n");
        }
        return comp;
    }
    
    public static Complex[][] Multiply (Complex[][] comp1, double[][] comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];

        if (comp1.length == comp2.length) {
            for (int i = 0; i < comp1.length; i++) {
                for (int j =0 ; j < comp1[0].length; j++) {
                    comp[i][j] = comp1[i][j].multiply(comp2[i][j]);
                }
            }
        } else {
            throw new Exception("Complex[] dimensions do not match:\n" +
                    "Comp1: " + comp1.length + ", " + comp1[0].length + "\n" +
                    "Comp2: " + comp2.length + ", " + comp2[0].length + "\n");
        }
        return comp;
    }
    
    public static Complex[][] Multiply (double[][] comp2, Complex[][] comp1) throws Exception {
        return Multiply (comp1, comp2);
    }
    
    
    public static Complex[] Multiply (double[] comp1, Complex comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp2.multiply(comp1[i]);
        }
        return comp;
    }
    
    public static Complex[] Multiply (double[] dbl, Complex[] comp) throws Exception {
        return Multiply( comp, dbl);
    }
    
    public static Complex[] Multiply (Complex[] comp, double[] dbl) throws Exception {
        if (comp.length != dbl.length) {
            throw new Exception("Dimensions missmatch");
        }
        Complex[] output = new Complex[comp.length];
        for (int i = 0; i < comp.length; i++) {
            output[i] = comp[i].multiply(dbl[i]);
        }
        return output;
    }
    
    public static Complex[] Multiply (Complex[] comp, Complex[] dbl) throws Exception {
        if (comp.length != dbl.length) {
            throw new Exception("Dimensions missmatch");
        }
        Complex[] output = new Complex[comp.length];
        for (int i = 0; i < comp.length; i++) {
            output[i] = comp[i].multiply(dbl[i]);
        }
        return output;
    }
    
    public static Complex[] Multiply (Complex comp, double[] dbl) throws Exception {
        Complex[] output = new Complex[dbl.length];
        for (int i = 0; i < dbl.length; i++) {
            output[i] = comp.multiply(dbl[i]);
        }
        return output;
    }
    
    public static Complex[][] MeshMultiply (Complex[] comp1, Complex[] comp2) throws Exception {
        Complex[][] output = new Complex[comp1.length][comp2.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp2.length; j++) {
                output[i][j] = comp1[i].multiply(comp2[j]);
            }
        }
        return output;
    }
    
    public static Complex[][] MeshAdd (Complex[] comp1, Complex[] comp2) throws Exception {
        Complex[][] output = new Complex[comp1.length][comp2.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp2.length; j++) {
                output[i][j] = comp1[i].add(comp2[j]);
            }
        }
        return output;
    }
    
    public static Complex[][] MeshAdd (double[] comp1, Complex[] comp2) throws Exception {
        Complex[][] output = new Complex[comp1.length][comp2.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp2.length; j++) {
                output[i][j] = comp2[j].add(comp1[i]);
            }
        }
        return output;
    }
    
    public static Complex[][] MeshMultiply (double[] comp1, Complex[] comp2) throws Exception {
        Complex[][] output = new Complex[comp1.length][comp2.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp2.length; j++) {
                output[i][j] = comp2[j].multiply(comp1[i]);
            }
        }
        return output;
    }
    
    
    public static Complex[] Add (double[] comp1, Complex comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp2.add(comp1[i]);
        }
        return comp;
    }
    
    public static Complex[] Exp (Complex[] comp1) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].exp();
        }
        return comp;
    }
    
    public static Complex[][] Exp (Complex[][] comp1) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].exp();
            }
        }
        return comp;
    }
    
    
    public static Complex[] Log (Complex[] comp1) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].log();
        }
        return comp;
    }
    
    public static Complex[][] Log (Complex[][] comp1) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].log();
            }
        }
        return comp;
    }
    
    public static Complex[] Log (double[] comp1) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            Complex currValue = new Complex(comp1[i]);
            comp[i] = currValue.log();
        }
        return comp;
    }
    
    public static Complex[][] Log (double[][] comp1) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                Complex currValue = new Complex(comp1[i][j]);
                comp[i][j] = currValue.log();
            }
        }
        return comp;
    }
    
    public static Complex[] Multiply (Complex[] comp1, double comp2) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].multiply(comp2);
        }
        return comp;
    }
    
    public static Complex[][] Multiply (Complex[][] comp1, double comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].multiply(comp2);
            }
        }
        return comp;
    }
    
    public static Complex[][] Multiply (Complex[][] comp1, Complex comp2) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].multiply(comp2);
            }
        }
        return comp;
    }
    
    public static Complex[] Conjugate (Complex[] comp1) throws Exception {
        Complex[] comp = new Complex[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].conjugate();  
        }
        return comp;
    }
    
    public static Complex[][] Conjugate (Complex[][] comp1) throws Exception {
        Complex[][] comp = new Complex[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].conjugate();
            }
        }
        return comp;
    }
    
    public static Complex[][] Transpose (Complex[][] comp1) throws Exception {
        Complex[][] comp = new Complex[comp1[0].length][comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                Complex curr = comp1[i][j];
                comp[j][i] = new Complex(curr.getReal(), curr.getImaginary());
            }
        }
        return comp;
    }
    
    public static double[] Real (Complex[] comp1) throws Exception {
        double[] comp = new double[comp1.length];
        for (int i = 0; i < comp1.length; i++) {
            comp[i] = comp1[i].getReal();
        }
        return comp;
    }
    
    public static double[][] Real (Complex[][] comp1) throws Exception {
        double[][] comp = new double[comp1.length][comp1[0].length];
        for (int i = 0; i < comp1.length; i++) {
            for (int j = 0; j < comp1[0].length; j++) {
                comp[i][j] = comp1[i][j].getReal();
            }
        }
        return comp;
    }
    
    public static void Print(Complex[][][] values) {
        System.out.println();
        for (int set = 0; set < values[0][0].length; set++) {
            System.out.println("Set=" + set);
            for (int col = 0; col <values[0].length; col++) { 
                System.out.print("\t" + (col+1));
            }
            System.out.println();
            for (int row = 0; row < values.length; row++) {
                System.out.print((row+1) +":\t");
                for (int col = 0; col <values[0].length; col++) {
                    System.out.print(values[row][col][set] + "\t");
                }
                System.out.println();
            }
        }
    }
    
    public static void Print(Complex[][] values) {
        System.out.println();
        for (int col = 0; col < values[0].length; col++) {
            System.out.print("\t" + (col + 1));
        }
        System.out.println();
        for (int row = 0; row < values.length; row++) {
            System.out.print((row + 1) + ":\t");
            for (int col = 0; col < values[0].length; col++) {
                Complex curr = values[row][col];
                System.out.print("(" + curr.getReal() + ", " + curr.getImaginary() +  ")\t");
            }
            System.out.println();
        }
    }
    
    public static void Print(Complex[] values) {
        System.out.println();
        for (int col = 0; col < 1; col++) {
            System.out.print("\t" + (col + 1));
        }
        System.out.println();
        for (int row = 0; row < values.length; row++) {
            System.out.print((row + 1) + ":\t");
            Complex curr = values[row];
            System.out.print("(" + curr.getReal() + ", " + curr.getImaginary() +  ")\n");
        }
        System.out.println();
    }
    
    public static Complex[] CreateComplex (int vec1) {
        Complex[] comp = new Complex[vec1];
        for (int i = 0; i < vec1; i++) {
            comp[i] = new Complex(0.0,0.0);
        }
        return comp;
    }
    
    public static Complex[] CreateComplex (double[] vec1) {
        Complex[] comp = new Complex[vec1.length];
        for (int i = 0; i < vec1.length; i++) {
            comp[i] = new Complex(vec1[i],0.0);
        }
        return comp;
    }
    
    public static Complex[][] CreateComplex (int vec1, int vec2) {
        Complex[][] comp = new Complex[vec1][vec2];
        for (int i = 0; i < vec1; i++) {
            for (int j = 0; j < vec2; j++) {
                comp[i][j] = new Complex(0.0,0.0);
            }
        }
        return comp;
    }
    
    public static Complex[][][] CreateComplex (int vec1, int vec2, int vec3) {
        Complex[][][] comp = new Complex[vec1][vec2][vec3];
        for (int i = 0; i < vec1; i++) {
            for (int j = 0; j < vec2; j++) {
                for (int k = 0; k < vec3; k++) {
                    comp[i][j][k] = new Complex(0.0,0.0);
                }
            }
        }
        return comp;
    }
    
    public static Complex[] RowSum(Complex[][] origData) {
        Complex[] ret = new Complex[origData.length];
        for (int row = 0; row < origData.length; row++) {
            Complex total = new Complex(0.0, 0.0);
            for (int col = 0; col < origData[0].length; col++) {
                total = total.add(origData[row][col]);
            }
            ret[row] = total;
        }
        return ret;
    }
    
    public static Complex Sum(Complex[] origData) {
        Complex total = new Complex(0,0);
        for (int row = 0; row < origData.length; row++) {
            total = total.add(origData[row]);
        }
        return total;
    }
    
    public static Complex[][][] SetData(Complex[][][] origData, Complex[][] newData, int[] vec1, int[] vec2, int vec3) {
        for (int vec1Counter = 0; vec1Counter < vec1.length; vec1Counter++) {
            int currVec1 = vec1[vec1Counter];
            for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
                int currVec2 = vec2[vec2Counter];
                origData[currVec1][currVec2][vec3] = newData[vec1Counter][vec2Counter];
            }
        }
        return origData;
    }
    
    public static Complex[][] SetData(Complex[][] origData, Complex[] newData, int vec1, int[] vec2) {
        for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
            int currVec2 = vec2[vec2Counter];
            origData[vec1][currVec2] = newData[vec2Counter];
        }
        return origData;
    }
    
    public static Complex[][] GetData(Complex[][][] origData, int[] vec1, int[] vec2, int vec3) {
        Complex[][] newData = new Complex[vec1.length][vec2.length];
        for (int vec1Counter = 0; vec1Counter < vec1.length; vec1Counter++) {
            int currVec1 = vec1[vec1Counter];
            for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
                int currVec2 = vec2[vec2Counter];
                Complex curr = origData[currVec1][currVec2][vec3];
                newData[vec1Counter][vec2Counter] = new Complex(curr.getReal(), curr.getImaginary());
            }
        }
        return newData;
    }
    
    public static ComplexNumber[][] GetData(ComplexNumber[][][] origData, int[] vec1, int[] vec2, int vec3) {
        ComplexNumber[][] newData = new ComplexNumber[vec1.length][vec2.length];
        for (int vec1Counter = 0; vec1Counter < vec1.length; vec1Counter++) {
            int currVec1 = vec1[vec1Counter];
            for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
                int currVec2 = vec2[vec2Counter];
                newData[vec1Counter][vec2Counter] = origData[currVec1][currVec2][vec3];
            }
        }
        return newData;
    }
    
    public static Complex[][] GetData(Complex[][][] origData, int vec1, int[] vec2, int[] vec3) {
        Complex[][] newData = new Complex[vec2.length][vec3.length];
        for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
            int currVec2 = vec2[vec2Counter];
            for (int vec3Counter = 0; vec3Counter < vec3.length; vec3Counter++) {
                int currVec3 = vec3[vec3Counter];
                newData[vec2Counter][vec3Counter] = origData[vec1][currVec2][currVec3];
            }
        }
        return newData;
    }
    
    public static ComplexNumber[][] GetData(ComplexNumber[][][] origData, int vec1, int[] vec2, int[] vec3) {
        ComplexNumber[][] newData = new ComplexNumber[vec2.length][vec3.length];
        for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
            int currVec2 = vec2[vec2Counter];
            for (int vec3Counter = 0; vec3Counter < vec3.length; vec3Counter++) {
                int currVec3 = vec3[vec3Counter];
                newData[vec2Counter][vec3Counter] = origData[vec1][currVec2][currVec3];
            }
        }
        return newData;
    }
    
    public static Complex[] GetData(Complex[][] origData, int vec1, int[] vec2) {
        Complex[] newData = new Complex[vec2.length];
        for (int vec2Counter = 0; vec2Counter < vec2.length; vec2Counter++) {
            int currVec2 = vec2[vec2Counter];
            Complex curr = origData[vec1][currVec2];
            newData[vec2Counter] = new Complex(curr.getReal(), curr.getImaginary());
        }
        return newData;
    }
    
    public static int[] ToInt (double[] input) {
        int[] output = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (int) Math.round(input[i]);
        }
        return output;
    }
}

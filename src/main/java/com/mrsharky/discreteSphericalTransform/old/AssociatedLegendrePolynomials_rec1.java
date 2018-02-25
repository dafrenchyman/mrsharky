/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import static com.mrsharky.helpers.MatrixUtilities.ToFieldMatrixComplex;
import static com.mrsharky.helpers.MatrixUtilities.PrintMatrix;

/**
 *
 * @author dafre
 */
public class AssociatedLegendrePolynomials_rec1 {

    private Map<Integer,Map<Integer,Double>> _calculatedValues;
    private double[][] _legendre;
    
    public double[][] GetAsDouble() {
        return _legendre;
    }
    
    /*public RealMatrix GetAsRealMatrix() {
        RealMatrix mat =  MatrixUtils.createRealMatrix(_legendre.length, 1);
        for (int k = 0; k < mat.getRowDimension(); k++) {
            mat.setEntry(k, 0, _legendre[k][0]);
        }
        return mat;
    }*/
    
    public RealMatrix GetAsRealMatrix() {
        RealMatrix mat =  MatrixUtils.createRealMatrix(_legendre.length, _legendre[0].length);
        for (int k = 0; k < mat.getRowDimension(); k++) {
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                mat.setEntry(k, j, _legendre[k][j]);
            }
            
        }
        return mat;
    }
    
    public FieldMatrix<Complex> GetAsFieldMatrixComplex() {
        RealMatrix mat =  MatrixUtils.createRealMatrix(_legendre.length, 1);
        for (int k = 0; k < mat.getRowDimension(); k++) {
            mat.setEntry(k, 0, _legendre[k][0]);
        }
        return ToFieldMatrixComplex(mat);
    }
   
    public double AssLegFunc2(int l, int m, double x) {
        boolean alreadyCalculated = this._calculatedValues.get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(l).get(m);
        } else if (l == m) {
            double right = Math.sqrt((1.0-x)*(1.0+x));
            double doubleFactorial=1.0;
            double value = 1.0;
            for (int i=1; i <= m; i++) {
                value *= -doubleFactorial*right;
                doubleFactorial += 2.0;
            }
            this._calculatedValues.get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            double value = x*(2*m +1) * AssLegFunc2(m,m,x);
            this._calculatedValues.get(l).put(m, value);
            return value;
        } else {
            double top = (-2*(m+1)*x*AssLegFunc2(l,m+1,x) / Math.sqrt(1-Math.pow(x, 2))) - AssLegFunc2(l,m+2,x);
            double bottom = (l+m+1)*(l-m-2);
            double value = top/bottom;
            
            //double value = (x*(2*l -1) * AssLegFunc2(l-1,m,x) - (l+m-1)*AssLegFunc2(l-2,m,x))/(l-m);
            this._calculatedValues.get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials_rec1(int N, double X) {
        this._calculatedValues = new HashMap<Integer,Map<Integer,Double>>();
        for (int k = 0; k <= N; k++) {
            this._calculatedValues.put(k, new HashMap<Integer,Double>());
        }
        
        double[][] output = new double[N+1][1];
        for (int k = 0; k <= N; k++) {
            output[k][0] = AssLegFunc2(N,k,X);
        }
        
        this._legendre = output;
    }
    
    public AssociatedLegendrePolynomials_rec1(int N, double[] X) {
        this._calculatedValues = new HashMap<Integer,Map<Integer,Double>>();
        
        
        double[][] output = new double[N+1][X.length];
        for (int k = 0; k <= N; k++) {
            for (int j = 0; j < X.length; j++) {
                for (int k1 = 0; k1 <= N; k1++) {
                    this._calculatedValues.put(k1, new HashMap<Integer,Double>());
                }
                
                for (int i = 0; i <=N; i++) {
                    AssLegFunc2(i,i,X[j]);
                }
                for (int i = 1; i < N; i++) {
                    AssLegFunc2(i+1,i,X[j]);
                }
                
                output[k][j] = AssLegFunc2(N,k,X[j]);
            }
            
        }
        
        this._legendre = output;
    }
    
    public static void main(String args[]) throws Exception {
        
        //double x = 0;
        
        double[] x = new double[] {-0.998909990849, 0, 0.998909990849};
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        
        AssociatedLegendrePolynomials_rec1 leg = new AssociatedLegendrePolynomials_rec1(10,x);
        PrintMatrix(leg.GetAsRealMatrix());
        
        /*System.out.println(AssLegFuncRecursive(2,2,x));   
        System.out.println(AssLegFuncRecursive(2,1,x));   
        System.out.println(AssLegFuncRecursive(2,0,x));   
        
        
        
        System.out.println(AssLegFuncRecursive(1,0,x));
        System.out.println(x);
        
        System.out.println(AssLegFuncRecursive(1,1,x));   
        System.out.println((Math.pow(1-Math.pow(x,2.0), 0.5))*(-1.0));
        
        System.out.println(AssLegFuncRecursive(4,4,x));
        System.out.println((Math.pow(1-Math.pow(x,2.0), 2.0))*(105.0));
        
                

        System.out.println(LegFuncRecursive(2,x));
        System.out.println(LegFuncRecursiveBig(2,x).doubleValue());
        System.out.println((3*Math.pow(x, 2.0)-1.0)/2.0);
        

        System.out.println(LegFuncRecursive(3,x));
        System.out.println(LegFuncRecursiveBig(3,x).doubleValue());
        System.out.println((5*Math.pow(x, 3.0)-3.0*x)/2.0);
        
        int M = 52;
        LegendreGausWeights lgw = new LegendreGausWeights(M-1,-1,1);
        RealMatrix legZerosM = lgw.GetValues();
        RealMatrix gausWeights = lgw.GetWeights().transpose();
        RealMatrix legZerosRad = ArcSineOfMatrixValues(legZerosM).scalarAdd(Math.PI/2.0);
        
        double phi = legZerosRad.getEntry(1, 0);*/
        
    }
    
}

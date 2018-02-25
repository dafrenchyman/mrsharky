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
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author dafre
 */
public class AssociatedLegendrePolynomials {

    private Map<Integer,Map<Integer,Double>> _calculatedValues;
    private double[][] _legendre;
    
    public double[][] GetAsDouble() {
        return _legendre;
    }
    
    public void PrintCalculatedValues () {
        List<Integer> L = this._calculatedValues.keySet().stream().sorted().collect(Collectors.toList());
        for (int l : L) {
            List<Integer> M = this._calculatedValues.get(l).keySet().stream().sorted().collect(Collectors.toList());
            for (int m : M) {
                double value = this._calculatedValues.get(l).get(m);
                System.out.println("L = " + l + " M = " + m + " Value: " + (value));
                
            }
        }   
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
            double value = (x*(2*l -1) * AssLegFunc2(l-1,m,x) - (l+m-1)*AssLegFunc2(l-2,m,x))/(l-m);
            this._calculatedValues.get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials(int N, double X) {
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
    
    public AssociatedLegendrePolynomials(int N, double[] X) {
        this._calculatedValues = new HashMap<Integer,Map<Integer,Double>>();
        
        
        double[][] output = new double[N+1][X.length];
        for (int k = 0; k <= N; k++) {
            for (int j = 0; j < X.length; j++) {
                for (int k1 = 0; k1 <= N; k1++) {
                    this._calculatedValues.put(k1, new HashMap<Integer,Double>());
                }
                output[k][j] = AssLegFunc2(N,k,X[j]);
            }
            
        }
        
        this._legendre = output;
    }
    
    public static void main(String args[]) throws Exception {
        
        //double x = 0;
        
        //double[] x = new double[] {-0.998909990849, -0.998909990849};
        double x = -0.998909990849;
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        
        
        
        AssociatedLegendrePolynomials leg = new AssociatedLegendrePolynomials(10,x);
        leg.PrintCalculatedValues();
        
        PrintMatrix(leg.GetAsRealMatrix());
        
        
    }
    
}

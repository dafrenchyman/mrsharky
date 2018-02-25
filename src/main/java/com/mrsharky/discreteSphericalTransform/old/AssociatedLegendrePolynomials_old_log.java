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
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author dafre
 */
public class AssociatedLegendrePolynomials_old_log {

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
                System.out.println("L = " + l + " M = " + m + " Value: " + Math.log(Math.abs(value)));
                
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
    
    /*public RealMatrix GetAsRealMatrix() {
        RealMatrix mat =  MatrixUtils.createRealMatrix(_legendre.length, _legendre[0].length);
        for (int k = 0; k < mat.getRowDimension(); k++) {
            for (int j = 0; j < mat.getColumnDimension(); j++) {
                mat.setEntry(k, j, _legendre[k][j].getReal());
            }
            
        }
        return mat;
    }*/
    
    /*public FieldMatrix<Complex> GetAsFieldMatrixComplex() {
        RealMatrix mat =  MatrixUtils.createRealMatrix(_legendre.length, 1);
        for (int k = 0; k < mat.getRowDimension(); k++) {
            mat.setEntry(k, 0, _legendre[k][0].getReal());
        }
        return ToFieldMatrixComplex(mat);
    }*/
    
    boolean _log = true;
   
    public Double AssLegFunc2(int l, int m, double x, boolean negativeX) {
        if (x < 0) {
            x = x*-1.0;
            negativeX = false;
        }
        boolean alreadyCalculated = this._calculatedValues.get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(l).get(m);
        } else if (l == m) {
            double value = 1.0;
            if (!_log) {
                double right = Math.sqrt((1.0-x)*(1.0+x));
                double doubleFactorial=1.0;
                for (int i=1; i <= m; i++) {
                    value *= -doubleFactorial*right;
                    doubleFactorial += 2.0;
                }
            } else {
                Complex left = (new Complex(1.0, 0)).add(-x).log().multiply(0.5);
                Complex right = (new Complex(1.0, 0)).add(x).log().multiply(0.5);
                Complex combined = left.add(right);

                Complex doubleFactorial = new Complex(1.0, 0.0);
                Complex value1 = new Complex(0.0, 0.0);           // log(1) = 0
                Complex logNegOne = (new Complex(-1.0, 0.0)).log();
                for (int i=1; i <= m; i++) {
                    value1 = (value1.add(doubleFactorial.log()).add(logNegOne).add(combined));
                    doubleFactorial = doubleFactorial.add(2.0);
                }
                value = value1.exp().getReal();
            }
            if (negativeX) {
                double power = Math.pow(-1, m+l);
                value = value*power;
            }
            this._calculatedValues.get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            double value = 0;
            if (!_log) {
                value = x*(2*m +1) * AssLegFunc2(m,m,x, negativeX);
                this._calculatedValues.get(l).put(m, value);
            } else {
                Complex comp1 = (new Complex(x, 0.0)).log();
                double  comp2 = Math.log(2*m+1);
                Complex comp3 = (new Complex(AssLegFunc2(m,m,x, negativeX), 0.0).log());
                Complex value1 = comp1.add(comp2).add(comp3).exp();
                value = value1.getReal();
            }
            
            if (negativeX) {
                double power = Math.pow(-1, m+l);
                value = value*power;
            }
            this._calculatedValues.get(l).put(m, value);
            return value;
        } else {
            double value = 0;
            if (!_log) {
                value = (x*(2*l -1) * AssLegFunc2(l-1,m,x, negativeX) - (l+m-1)*AssLegFunc2(l-2,m,x, negativeX))/(l-m);
            } else {

                // Left
                Complex left1 = (new Complex(x, 0.0)).log();
                double  left2 = Math.log(2*l-1);
                Complex left3 = new Complex(AssLegFunc2(l-1,m,x, negativeX), 0.0).log();
                Complex left  = left1.add(left2).add(left3).exp();

                // Right
                double  right1 = Math.log(l+m-1);
                Complex right2 = new Complex(AssLegFunc2(l-2,m,x, negativeX), 0.0).log();
                Complex right  = right2.add(right1).exp().multiply(-1.0);
                Complex value1 = left.add(right).multiply(1.0/(l-m));
                value = value1.getReal();
            }
            
            if (negativeX) {
                double power = Math.pow(-1, m+l);
                value = value*power;
            }
            this._calculatedValues.get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials_old_log(int N, double X) {
        this._calculatedValues = new HashMap<Integer,Map<Integer,Double>>();
        for (int k = 0; k <= N; k++) {
            this._calculatedValues.put(k, new HashMap<Integer,Double>());
        }
        
        //Complex[][] output = ComplexHelpers.CreateComplex(N+1, 1);
        double[][] output = new double[N+1][1];
        for (int k = 0; k <= N; k++) {
            output[k][0] = AssLegFunc2(N,k,X, false);
        }
        
        this._legendre = output;
    }
    
    public AssociatedLegendrePolynomials_old_log(int N, double[] X) {
        this._calculatedValues = new HashMap<Integer,Map<Integer,Double>>();
        
        
        //Complex[][] output = ComplexHelpers.CreateComplex(N+1, X.length);
        double[][] output = new double[N+1][X.length];
        for (int k = 0; k <= N; k++) {
            for (int j = 0; j < X.length; j++) {
                for (int k1 = 0; k1 <= N; k1++) {
                    this._calculatedValues.put(k1, new HashMap<Integer,Double>());
                }
                output[k][j] = AssLegFunc2(N,k,X[j], false);
            }
            
        }
        
        this._legendre = output;
    }
    
    public static void main(String args[]) throws Exception {
        
        //double x = 0;
        
        double[] x = new double[] {-0.998909990849, 0.998909990849};
        //LegendreGausWeights lgwt = new LegendreGausWeights(30, -1.0, 1.0);     
        //double[] x = lgwt.GetValues();
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        //double x = 0.998909990849;
        AssociatedLegendrePolynomials_old_log leg = new AssociatedLegendrePolynomials_old_log(10,x);
        
        //ComplexHelpers.Print(leg.GetAsDouble());
        //ComplexHelpers.Print(ComplexHelpers.Exp(leg.GetAsDouble())); 
        
        leg.PrintCalculatedValues();
        
        DoubleArray.Print(DoubleArray.Log(DoubleArray.Abs(leg.GetAsDouble())));
        //DoubleArray.Print(leg.GetAsDouble());     
        
        
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.LegendreGausWeights;
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
public class AssociatedLegendrePolynomials_new_log {

    //          Theta       K           L
    private Map<Double, Map<Integer,Map<Integer,Double>>> _calculatedValues;
    private double[] _keys;
    
    public Complex[] GetAsDouble(int k, int l) {
        Complex[] output = new Complex[this._keys.length];
        Complex power = (new Complex(-1.0, 0.0)).log().multiply(k);
        for (int i = 0; i < this._keys.length; i++) {
            double currLookup = this._keys[i];
            if (currLookup >= 0) {
                output[i] = new Complex(this._calculatedValues.get(currLookup).get(k).get(l));
            } else {
                output[i] = (new Complex(this._calculatedValues.get(-1*currLookup).get(k).get(l))).add(power);
            }
        }
        return output;
    }
    
    public void PrintCalculatedValues () {
        List<Double> Theta = this._calculatedValues.keySet().stream().sorted().collect(Collectors.toList());
        for (double theta : Theta) {
            System.out.println("--------------------------------------------------------");
            System.out.println("Theta: " + theta);
            System.out.println("--------------------------------------------------------");
            List<Integer> L = this._calculatedValues.get(theta).keySet().stream().sorted().collect(Collectors.toList());
            
            for (int l : L) {
                List<Integer> M = this._calculatedValues.get(theta).get(l).keySet().stream().sorted().collect(Collectors.toList());
                for (int m : M) {
                    double value = this._calculatedValues.get(theta).get(l).get(m);
                    value = Math.exp(value);
                    System.out.println("L = " + l + " M = " + m + " Value: " + value);
                }
            }
        }
    }
    
    boolean _log = true;
   
    public Double AssLegFunc2(int l, int m, double x) {
        if (x < 0) {
            x = x*-1.0;
        }
        boolean alreadyCalculated = this._calculatedValues.get(x).get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(x).get(l).get(m);
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
                value = value1.getReal();
            }
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            double value = 0;
            if (!_log) {
                value = x*(2*m +1) * AssLegFunc2(m,m,x);
                this._calculatedValues.get(x).get(l).put(m, value);
            } else {
                Complex comp1 = (new Complex(x, 0.0)).log();
                double  comp2 = Math.log(2*m+1);
                Complex comp3 = (new Complex(AssLegFunc2(m,m,x), 0.0));
                Complex value1 = comp1.add(comp2).add(comp3);
                value = value1.getReal();
            }
            
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else {
            double value = 0;
            if (!_log) {
                value = (x*(2*l -1) * Math.exp(AssLegFunc2(l-1,m,x)) - (l+m-1) * Math.exp(AssLegFunc2(l-2,m,x)))/(l-m);
            } else {

                

                // Left
                Complex left1 = (new Complex(x, 0.0)).log();
                double  left2 = Math.log(2*l-1);
                Complex left3 = new Complex(AssLegFunc2(l-1,m,x), 0.0);
                
                // Right
                double  right1 = Math.log(l+m-1);
                Complex right2 = new Complex(AssLegFunc2(l-2,m,x), 0.0);
                
                // Calc A & C
                Complex A = left1.add(left2).add(left3);
                Complex C = right2.add(right1);
                
                if (C.getReal() > A.getReal()) {
                    Complex dummy = C;
                    C = A;
                    A = dummy;
                }
                
                // Left log(l-m) + log(a)
                double divisor = -1.0 * Math.log(l-m);
                Complex left = A.add(divisor);
                
                // Right log(1-explog(C/A))
                double expCA = (C.add(A.multiply(-1.0))).exp().multiply(-1.0).getReal();
                double right = Math.log1p(expCA);
                
                Complex value1 = left.add(right);
                value = value1.getReal();
            }

            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials_new_log(int N, double X) {
        this._calculatedValues = new HashMap<Double, Map<Integer,Map<Integer,Double>>>();
        Map<Integer, Map<Integer, Double>> currDouble = new HashMap<Integer, Map<Integer, Double>>();
        for (int k = 0; k <= N; k++) {
            currDouble.put(k, new HashMap<Integer,Double>());
        }
        this._calculatedValues.put(X, currDouble);
        
        double[][] output = new double[N+1][1];
        for (int k = 0; k <= N; k++) {
            output[k][0] = AssLegFunc2(N,k,X);
        }
    }
    
    public AssociatedLegendrePolynomials_new_log(int K, double[] X) {
        this._calculatedValues = new HashMap<Double, Map<Integer,Map<Integer,Double>>>();
        this._keys = new double[X.length];
        for (int i = 0; i < X.length; i++) {

            Map<Integer, Map<Integer, Double>> currDouble = new HashMap<Integer, Map<Integer, Double>>();
            for (int k = 0; k <= K; k++) {
                currDouble.put(k, new HashMap<Integer,Double>());
            }
            double currValue = X[i] < 0 ? -1*X[i] : X[i];

            this._calculatedValues.put(currValue, currDouble);
            this._keys[i] = X[i];
        }
        
        for (int N = 0; N <= K; N++) {
            for (int k = 0; k <= N; k++) {
                for (int j = 0; j < X.length; j++) {
                    AssLegFunc2(N,k,X[j]);
                }
            }
        }
    }
    
    public static void main(String args[]) throws Exception {
        
        //double x = 0;
        
        LegendreGausWeights lgwt = new LegendreGausWeights(30, -1.0, 1.0);     
        //double[] x = lgwt.GetValues();
        
        double[] x = new double[] {-0.998909990849, 0.998909990849};
        //double x = 0.998909990849;
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        
        AssociatedLegendrePolynomials_new_log leg = new AssociatedLegendrePolynomials_new_log(100,x);
        
        //ComplexHelpers.Print(leg.GetAsDouble());
        //ComplexHelpers.Print(ComplexHelpers.Exp(leg.GetAsDouble())); 
        
        leg.PrintCalculatedValues();
        
        //DoubleArray.Print(leg.GetAsDouble());
        //DoubleArray.Print(DoubleArray.Exp(leg.GetAsDouble()));
        
        
    }
    
}

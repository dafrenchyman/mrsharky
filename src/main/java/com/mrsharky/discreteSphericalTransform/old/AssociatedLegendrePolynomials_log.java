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
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Julien Pierret
 */
public class AssociatedLegendrePolynomials_log {

    //          Theta       K           L
    private Map<Double, Map<Integer,Map<Integer,Complex>>> _calculatedValues;
    private double[] _keys;
    
    public Complex[] GetAsDouble(int k, int l) {
        Complex[] output = new Complex[this._keys.length];
        for (int i = 0; i < this._keys.length; i++) {
            double currLookup = this._keys[i];
            output[i] = (this._calculatedValues.get(currLookup).get(k).get(l));
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
                    Complex value = this._calculatedValues.get(theta).get(l).get(m);
                    value = (value).exp();
                    System.out.println("L = " + l + " M = " + m + " Value: " + value);
                }
            }
        }
    }
    
    boolean _log = true;
   
    public Complex AssLegFunc2(int l, int m, double x) {
        boolean alreadyCalculated = this._calculatedValues.get(x).get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(x).get(l).get(m);
        } else if (l == m) {
            Complex value = new Complex(1.0, 0.0);
            if (!_log) {
               
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
                value = value1;
            }
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            Complex value = new Complex(0.0, 0.0);
            if (!_log) {
               
            } else {
                Complex comp1 = (new Complex(x, 0.0)).log();
                double  comp2 = Math.log(2*m+1);
                Complex comp3 = AssLegFunc2(m,m,x);
                Complex value1 = comp1.add(comp2).add(comp3);
                value = value1;
            }
            
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else {
            Complex value = new Complex(0.0, 0.0);
            if (!_log) {
                //value = (x*(2*l -1) * Math.exp(AssLegFunc2(l-1,m,x)) - (l+m-1) * Math.exp(AssLegFunc2(l-2,m,x)))/(l-m);
            } else {

                

                // Left
                Complex left1 = (new Complex(x, 0.0)).log();
                double  left2 = Math.log(2*l-1);
                Complex left3 = AssLegFunc2(l-1,m,x);
                
                // Right
                double  right1 = Math.log(l+m-1);
                Complex right2 = AssLegFunc2(l-2,m,x);
                
                // Calc A & C
                Complex A = left1.add(left2).add(left3);
                Complex C = right2.add(right1);
                
                /*if ((C.getReal()) > (A.getReal())) {
                    Complex dummy = C;
                    C = A;
                    A = dummy;
                }*/
                
                // Left log(l-m) + log(a)
                double divisor = -1.0 * Math.log(l-m);
                Complex left = A.add(divisor);
                
                // Right log(1-explog(C/A))
                Complex expCA = (C.add(A.multiply(-1.0))).exp().multiply(-1.0);
                Complex right = expCA.add(1.0).log();
                
                Complex value1 = left.add(right);
                value = value1;
            }

            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials_log(int N, double X) {
        this._calculatedValues = new HashMap<Double, Map<Integer,Map<Integer,Complex>>>();
        Map<Integer, Map<Integer, Complex>> currDouble = new HashMap<Integer, Map<Integer, Complex>>();
        for (int k = 0; k <= N; k++) {
            currDouble.put(k, new HashMap<Integer,Complex>());
        }
        this._calculatedValues.put(X, currDouble);
        
        Complex[][] output = new Complex[N+1][1];
        for (int k = 0; k <= N; k++) {
            output[k][0] = AssLegFunc2(N,k,X);
        }
    }
    
    public AssociatedLegendrePolynomials_log(int K, double[] X) {
        this._calculatedValues = new HashMap<Double, Map<Integer,Map<Integer,Complex>>>();
        this._keys = new double[X.length];
        for (int i = 0; i < X.length; i++) {

            Map<Integer, Map<Integer, Complex>> currDouble = new HashMap<Integer, Map<Integer, Complex>>();
            for (int k = 0; k <= K; k++) {
                currDouble.put(k, new HashMap<Integer,Complex>());
            }
            double currValue = X[i];// < 0 ? -1*X[i] : X[i];

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
        
        //double[] x = new double[] {-0.998909990849, -0.998909990849};
        double[] x = new double[] {-0.1, -0.1};
        //double x = 0.998909990849;
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        
        AssociatedLegendrePolynomials_log leg = new AssociatedLegendrePolynomials_log(10,x);
        
        //ComplexHelpers.Print(leg.GetAsDouble());
        //ComplexHelpers.Print(ComplexHelpers.Exp(leg.GetAsDouble())); 
        
        leg.PrintCalculatedValues();
        
        //DoubleArray.Print(leg.GetAsDouble());
        //DoubleArray.Print(DoubleArray.Exp(leg.GetAsDouble()));
        
        
    }
    
}

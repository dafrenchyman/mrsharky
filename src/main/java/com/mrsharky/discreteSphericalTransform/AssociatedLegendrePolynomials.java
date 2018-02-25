/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.helpers.DoubleArray;
import java.util.HashMap;
import java.util.Map;
import static com.mrsharky.helpers.DoubleArray.Print;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Julien Pierret
 */
public class AssociatedLegendrePolynomials {

    //          Theta       K           L
    private Map<Double, Map<Integer,Map<Integer,Double>>> _calculatedValues;
    private double[] _keys;
    
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
                    value = value* Math.pow(-1, m+l);
                    System.out.println("L = " + l + " M = " + m + " Value: " + value);
                }
            }
        }
    }
    
    
    public double[] GetAsDouble(int k, int l) {
        double[] output = new double[this._keys.length];
        double power = Math.pow(-1, k+l);
        for (int i = 0; i < this._keys.length; i++) {
            double currLookup = this._keys[i];
            if (currLookup >= 0) {
                output[i] = this._calculatedValues.get(currLookup).get(k).get(l);
            } else {
                output[i] = this._calculatedValues.get(-1*currLookup).get(k).get(l) * power;
            }
        }
        return output;
    }
   
    public double AssLegFunc2(int l, int m, double x) {
        if (x < 0) {
            x = x*-1.0;
        }
        boolean alreadyCalculated = this._calculatedValues.get(x).get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(x).get(l).get(m);
        } else if (l == m) {
            double right = Math.sqrt((1.0-x)*(1.0+x));
            double doubleFactorial=1.0;
            double value = 1.0;
            for (int i=1; i <= m; i++) {
                value *= -doubleFactorial*right;
                doubleFactorial += 2.0;
            }
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            double value = x*(2*m +1) * AssLegFunc2(m,m,x);
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else {
            double value = (x*(2*l -1) * AssLegFunc2(l-1,m,x) - (l+m-1)*AssLegFunc2(l-2,m,x))/(l-m);
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        }
    }
    
    public AssociatedLegendrePolynomials(int N, double X) {
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
    
    public AssociatedLegendrePolynomials(int K, double[] X) {
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
        
        //double x = -0.998909990849;
        
        
        //double[] x = new double[] {-0.9989099908489036, -0.9989099908489036};
        
        double[] x = new double[] {-0.1};
        
        //System.out.println(AssLegFuncRecursiveBig(50,50,x).doubleValue());
        
        AssociatedLegendrePolynomials leg = new AssociatedLegendrePolynomials(10,x);
        
        leg.PrintCalculatedValues();
        
    }
    
}

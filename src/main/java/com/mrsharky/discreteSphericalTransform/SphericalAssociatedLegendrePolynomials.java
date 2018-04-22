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
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 *
 * @author Julien Pierret
 */
public class SphericalAssociatedLegendrePolynomials {

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
                    //value = value* Math.pow(-1, m+l);
                    System.out.println("L = " + l + " M = " + m + " Value: " + value);
                }
            }
        }
    }
    
    public void PrintCalculatedValuesNotNorm () {
        List<Double> Theta = this._calculatedValues.keySet().stream().sorted().collect(Collectors.toList());
        for (double theta : Theta) {
            System.out.println("--------------------------------------------------------");
            System.out.println("Theta: " + theta);
            System.out.println("--------------------------------------------------------");
            List<Integer> L = this._calculatedValues.get(theta).keySet().stream().sorted().collect(Collectors.toList());
            
            for (int l : L) {
                List<Integer> M = this._calculatedValues.get(theta).get(l).keySet().stream().sorted().collect(Collectors.toList());
                 
                for (int m : M) {
                    
                    // New method
                    if (true) {
                        double[] values = GetAsDoubleNonNormal(l, m);
                        System.out.println("L = " + l + " M = " + m + " Value: " + values[0]);
                    }

                    // old Method
                    if (false) {
                        double prod = 1.0;
                        for (int j = l-m+1; j <= l+m; j++) {
                            prod *= j;
                        }
                        double modifier = Math.sqrt(4.0*Math.PI * prod / (2.0 * l + 1.0));

                        double value = this._calculatedValues.get(theta).get(l).get(m) * modifier;
                        value = value* Math.pow(-1, m+l);
                        System.out.println("L = " + l + " M = " + m + " Value: " + value);
                    }                    
                }
            } 
       }
    }
    
    
    public double[] GetAsDouble(int k, int l) {
        double[] output = new double[this._keys.length];
        double power = Math.pow(-1, k+l);
        for (int i = 0; i < this._keys.length; i++) {
            double currLookup = this._keys[i];
            output[i] = this._calculatedValues.get(currLookup).get(k).get(l);
            /*if (currLookup >= 0) {
                output[i] = this._calculatedValues.get(currLookup).get(k).get(l);
            } else {
                output[i] = this._calculatedValues.get(-1*currLookup).get(k).get(l) * power;
            }*/
        }
        return output;
    }
    
    public double[] GetAsDoubleNonNormal(int k, int l) {
        double[] output = new double[this._keys.length];
        double power = Math.pow(-1, k+l);
        
        double prod = 1.0;
        for (int j = k-l+1; j <= k+l; j++) {
            prod *= j;
        }
        for (int i = 0; i < this._keys.length; i++) {
            double currLookup = this._keys[i];
            double modifier = Math.sqrt(4.0*Math.PI * prod / (2*k+1));
            output[i] = this._calculatedValues.get(currLookup).get(k).get(l) * modifier;
            /*if (currLookup >= 0) {
                output[i] = this._calculatedValues.get(currLookup).get(k).get(l) * modifier;
            } else {
                output[i] = this._calculatedValues.get(currLookup).get(k).get(l) * power * modifier;
            }*/
        }
        return output;
    }
    
    
    
   
    public double AssLegFunc2(int l, int m, double x) {
        /*if (x < 0) {
            x = x*-1.0;
        }*/
        
        
        boolean alreadyCalculated = this._calculatedValues.get(x).get(l).containsKey(m) ? true : false;
        if (alreadyCalculated) {
            return this._calculatedValues.get(x).get(l).get(m);
        } else if (l == m) {
            double value = 1.0;
           
            double right = (1.0-x)*(1.0+x);
            for (int i = 1; i <= m; i++) {
                value *= (2*i - 1.0) * right / (2*i);
            }
            value = Math.sqrt((2.0*m+1.0) *value / (4.0*Math.PI));
            value = value * Math.pow(-1, m);
            
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else if (l == (m+1)) {
            double value = x*Math.sqrt(2.0 * m + 3.0) * AssLegFunc2(m,m,x);            
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        } else {
            double leftTop = 4.0 * Math.pow(l, 2.0) -1.0;
            double leftBot = Math.pow(l, 2.0) - Math.pow(m, 2.0);
            double rightTop = Math.pow(l-1.0, 2.0) - Math.pow(m, 2.0);
            double rightBot = 4.0 * Math.pow(l-1, 2.0) -1.0;
            double value = Math.sqrt(leftTop/leftBot) * (x*AssLegFunc2(l-1,m, x) - Math.sqrt(rightTop/rightBot) * AssLegFunc2(l-2, m,x));
            
            this._calculatedValues.get(x).get(l).put(m, value);
            return value;
        }
    }
    
    public SphericalAssociatedLegendrePolynomials(int N, double X) {
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
    
    public SphericalAssociatedLegendrePolynomials(int K, double[] X) {
        this._calculatedValues = new HashMap<Double, Map<Integer,Map<Integer,Double>>>();
        this._keys = new double[X.length];
        for (int i = 0; i < X.length; i++) {

            Map<Integer, Map<Integer, Double>> currDouble = new HashMap<Integer, Map<Integer, Double>>();
            for (int k = 0; k <= K; k++) {
                currDouble.put(k, new HashMap<Integer,Double>());
            }
            //double currValue = X[i] < 0 ? -1*X[i] : X[i];
            double currValue = X[i];

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
        
        SphericalAssociatedLegendrePolynomials leg = new SphericalAssociatedLegendrePolynomials(10,x);
        
        leg.PrintCalculatedValuesNotNorm();
        //leg.PrintCalculatedValues();
        
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.helpers;

import static com.mrsharky.helpers.Utilities.linspace;

/**
 * 
 * @author Julien Pierret
 */
public class GenerateGaussianGriddedPoints {

    private double[] LonPoints(int N) {
        return DoubleArray.Multiply(linspace(1.0,N,N), 2*Math.PI/N);
    }
    
    private static double[] LatPointsGaussianQuadrature(int M) throws Exception {
        LegendreGausWeights lgw = new LegendreGausWeights(M,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));
        return legZerosRad;
    }
    
    private final double[] _lon;
    private final double[] _lat;
    
    public GenerateGaussianGriddedPoints(int M, int N) throws Exception {
        
        double[] phi   = LatPointsGaussianQuadrature(M);
        double[] theta = LonPoints(N);
        double[] lat   = new double[phi.length*theta.length];
        double[] lon   = new double[phi.length*theta.length];
        int counter = 0;
        for (int i = 0; i < phi.length; i++) {
            for (int j = 0; j < theta.length; j++) {
                lat[counter] = phi[i];
                lon[counter] = theta[j];
                counter++;
            }
        }
        
        _lat = lat;
        _lon = lon;
    }
    
    public double[] GetLat() {
        return this._lat;
    }
    
    public double[] GetLon() {
        return this._lon;
    }
    
}

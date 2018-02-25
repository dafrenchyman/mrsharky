/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import java.util.List;
/**
 *
 * @author dafre
 */
public class MeshGrid {
    
    private double[][] _X;
    private double[][] _Y;
    
    public double[][] GetX(){
        return this._X;
    }
    
    public double[][] GetY(){
        return this._Y;
    }
    
    public MeshGrid(double[] x, double[] y) {
        double[][] X = new double[y.length][x.length];
        double[][] Y = new double[y.length][x.length];
        
        for (int row = 0; row < y.length; row++) {
            for (int col = 0; col < x.length; col++) {
                X[row][col] = x[col];
                Y[row][col] = y[row];
            }
        }
        this._X = X;
        this._Y = Y;
    }
    
    public MeshGrid(List<Double> x, List<Double> y) {
        double[][] X = new double[y.size()][x.size()];
        double[][] Y = new double[y.size()][x.size()];
        
        for (int row = 0; row < y.size(); row++) {
            for (int col = 0; col < x.size(); col++) {
                X[row][col] = x.get(col);
                Y[row][col] = y.get(row);
            }
        }
        this._X = X;
        this._Y = Y;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_ResultsVarPca implements Serializable {
    
    // Normal gridded variables
    private final int _q;
    
    private Map<Integer, double[][]>  _gridBoxAnomalyMean;
    private Map<Integer, double[][]>  _gridBoxAnomalyVariance;
    
    // Global variables
    private Map<Pair<Integer, Integer>, Complex[]>   _eigenValues;
    private Map<Pair<Integer, Integer>, Complex[][]> _eigenVectors;
    private Map<Pair<Integer, Integer>, double[]>    _varianceExplained;
    private Map<Pair<Integer, Integer>, double[][]> _rollingGridBoxAnomVar;
    
    public SphericalHarmonics_ResultsVarPca(int q) {
        _q = q;
        _gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        _gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        _eigenValues = new HashMap<Pair<Integer, Integer>, Complex[]>();
        _eigenVectors = new HashMap<Pair<Integer, Integer>, Complex[][]>();
        _varianceExplained = new HashMap<Pair<Integer, Integer>, double[]>();
        _rollingGridBoxAnomVar = new HashMap<Pair<Integer, Integer>, double[][]>();
    }
    
    public void SetGribBoxAnomalyMean(int month, double[][] gridBoxAnomalyMean) { _gridBoxAnomalyMean.put(month, gridBoxAnomalyMean); }
    public double[][] GetGribBoxAnomalyMean(int month) { return _gridBoxAnomalyMean.get(month); }
    
    public void SetGribBoxAnomalyVariance(int month, double[][] gridBoxAnomalyVariance) { _gridBoxAnomalyVariance.put(month, gridBoxAnomalyVariance); }
    public double[][] GetGribBoxAnomalyVariance(int month) { return _gridBoxAnomalyVariance.get(month); }
    
    public void SetEigenValues(int month, int year, Complex[] eigenValues) { _eigenValues.put(Pair.with(month, year), eigenValues); }
    public Complex[] GetEigenValues(int month, int year) { return _eigenValues.get(Pair.with(month, year)); }
    
    public void SetEigenVectors(int month, int year, Complex[][] eigenVectors) { _eigenVectors.put(Pair.with(month, year), eigenVectors); }
    public Complex[][] GetEigenVectors(int month, int year) { return _eigenVectors.get(Pair.with(month, year)); }
    
    public void SetEigenVarianceExplained(int month, int year, double[] varExplained) { _varianceExplained.put(Pair.with(month, year), varExplained); }
    public double[] GetEigenVarianceExplained(int month, int year) { return _varianceExplained.get(Pair.with(month, year)); }
    
    public void SetRollingGridBoxAnomVar(int month, int year, double[][] rollGridBoxAnomVar) { _rollingGridBoxAnomVar.put(Pair.with(month, year), rollGridBoxAnomVar); }
    public double[][] GetRollingGridBoxAnomVar(int month, int year) { return _rollingGridBoxAnomVar.get(Pair.with(month, year)); }
    
    public int GetQ() { return this._q; }
}

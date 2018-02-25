/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor.old;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
import org.javatuples.Quintet;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_PcaResults implements Serializable {
    
    // Normal gridded variables
    private final int _q;
    private Map<Integer, Complex[]>   _eigenValues;
    private Map<Integer, Complex[][]> _eigenVectors;
    private Map<Integer, double[][]>  _gridBoxAnomalyMean;
    private Map<Integer, double[][]>  _gridBoxAnomalyVariance;
    private Map<Integer, double[]>    _varianceExplained;
    
    // Global variables
    private Map<Integer, Double> _globalMean;
    private Map<Integer, Double> _globalVar;
    private Map<Pair<Integer, Integer>, Double> _globalRollingMean;
    private Map<Pair<Integer, Integer>, Double> _globalRollingVar;
    private Map<Pair<Integer, Integer>, double[][]> _rollingGridBoxAnomalyVariance;
    
    // Original data
    private Map<Date, double[][]> _origData;
    
    public SphericalHarmonics_PcaResults(int q) {
        _q = q;
        _eigenValues = new HashMap<Integer, Complex[]>();
        _eigenVectors = new HashMap<Integer, Complex[][]>();
        _gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        _gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        _varianceExplained = new HashMap<Integer, double[]>();
        
        _globalMean = new HashMap<Integer, Double>();
        _globalVar = new HashMap<Integer, Double>();
        _globalRollingMean = new HashMap<Pair<Integer, Integer>, Double>();
        _globalRollingVar = new HashMap<Pair<Integer, Integer>, Double>();
        _origData = new HashMap<Date, double[][]>();
    }
    
    public void setResults(int month, Complex[] eigenValues, Complex[][] eigenVectors, 
            double[][] gridBoxAnomalyMean, double[][] gribBoxAnomalyVariance, double[] varianceExplained) {
        _eigenValues.put(month, eigenValues);
        _eigenVectors.put(month, eigenVectors);
        _gridBoxAnomalyMean.put(month, gridBoxAnomalyMean);
        _gridBoxAnomalyVariance.put(month, gribBoxAnomalyVariance);
        _varianceExplained.put(month, varianceExplained);
    }
    
    public void setGlobalResults(int month, int year, double globalRollingMean, double globalRollingVar) {
        Pair<Integer, Integer> key = Pair.with(month, year);
        _globalRollingMean.put(key, globalRollingMean);
        _globalRollingVar.put(key, globalRollingVar);
    }
    
    public void SetOneNumberResults(int month, double globalMean, double globalVar) {
        _globalMean.put(month, globalMean);
        _globalVar.put(month,globalVar);
    }
    
    public void AddVariableAnamolyResults(Map<Pair<Integer, Integer>, double[][]> data) {
        _rollingGridBoxAnomalyVariance = data;
    }
    
    public int GetQ() {
        return this._q;
    }
    
    public Quintet GetResults(int month) {
        Quintet results = Quintet.with(_eigenValues.get(month), _eigenVectors.get(month), _gridBoxAnomalyMean.get(month), _gridBoxAnomalyVariance.get(month), _varianceExplained.get(month));
        return results;
    }
    
    public double[][] GetRollingGridBoxAnomaly(int month, int year) {
        Pair<Integer, Integer> key = Pair.with(month, year);
        return _rollingGridBoxAnomalyVariance.get(key);
    }
    
    public Pair GetGlobalResults(int month, int year) {
        Pair<Integer, Integer> key = Pair.with(month, year);
        double mean = _globalRollingMean.get(key);
        double var = _globalRollingVar.get(key);
        Pair<Double, Double> global = Pair.with(mean, var);
        return global;
    }
    
    public Pair GetOneNumberResults(int month) {
        return Pair.with(_globalMean.get(month), _globalVar.get(month));
    }
    
    public double GetGlobalMean(int month) {
        return _globalMean.get(month);
    }
    
    public double GetGlobalVar(int month) {
        return _globalVar.get(month);
    }
    
    public double GetGlobalRollingMean(int month, int year) {
        Pair<Integer, Integer> key = Pair.with(month, year);
        return _globalRollingMean.get(key);
    }
    
    public double GetGlobalRollingVar(int month, int year) {
        Pair<Integer, Integer> key = Pair.with(month, year);
        return _globalRollingVar.get(key);
    }
    
    public Complex[] GetEigenvalues(int month) {
        return _eigenValues.get(month);
    }
    
    public Complex[][] GetEigenVectors(int month) {
        return _eigenVectors.get(month);
    }
    
    public double[][] GetGridBoxAnomalyMean(int month) {
        return _gridBoxAnomalyMean.get(month);
    }
    
    public double[][] GetGridBoxAnomalyVariance(int month) {
        return _gridBoxAnomalyVariance.get(month);
    }
    
    public double[] GetVarianceExplained(int month) {
        return _varianceExplained.get(month);
    }    
}

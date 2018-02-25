/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_Results implements Serializable {
    
    // Normal gridded variables
    private final int _q;
    
    private Map<Integer, double[][]>  _gridBoxAnomalyMean;
    private Map<Integer, double[][]>  _gridBoxAnomalyVariance;
    
    // Global variables
    private Map<Integer, Complex[]>   _eigenValues;
    private Map<Integer, Complex[][]> _eigenVectors;
    private Map<Integer, double[]>    _varianceExplained;
    private Map<Pair<Integer, Integer>, double[][]> _rollingGridBoxAnomVar;
    
    // Original Data
    Map<Date, double[][]> _yearlyData = new HashMap<Date, double[][]>();
    Map<Date, double[][]> _monthlyData = new HashMap<Date, double[][]>();
    
    public SphericalHarmonics_Results(int q) {
        _q = q;
        _gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        _gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        _eigenValues = new HashMap<Integer, Complex[]>();
        _eigenVectors = new HashMap<Integer, Complex[][]>();
        _varianceExplained = new HashMap<Integer, double[]>();
        _rollingGridBoxAnomVar = new HashMap<Pair<Integer, Integer>, double[][]>();
        _yearlyData = new HashMap<Date, double[][]>();
        _monthlyData = new HashMap<Date, double[][]>();
    }
    
    public void setOriginalYearlyData(Map<Date, double[][]> yearlyData) { _yearlyData = yearlyData; }
    public Map<Date, double[][]> getOriginalYearlyData() { return _yearlyData; }
    
    public void setOriginalMonthlyData(Map<Date, double[][]> monthlyData) { _monthlyData = monthlyData; }
    public Map<Date, double[][]> getOriginalMonthlyData() { return _monthlyData; }
    
    public void setGridBoxAnomalyMean(int month, double[][] gridBoxAnomalyMean) { _gridBoxAnomalyMean.put(month, gridBoxAnomalyMean); }
    public double[][] getGridBoxAnomalyMean(int month) { return _gridBoxAnomalyMean.get(month); }
    
    public void setGridBoxAnomalyVariance(int month, double[][] gridBoxAnomalyVariance) { _gridBoxAnomalyVariance.put(month, gridBoxAnomalyVariance); }
    public double[][] getGridBoxAnomalyVariance(int month) { return _gridBoxAnomalyVariance.get(month); }
    
    public void setEigenValues(int month, Complex[] eigenValues) { _eigenValues.put(month, eigenValues); }
    public Complex[] getEigenValues(int month) { return _eigenValues.get(month); }
    
    public void setEigenVectors(int month, Complex[][] eigenVectors) { _eigenVectors.put(month, eigenVectors); }
    public Complex[][] getEigenVectors(int month) { return _eigenVectors.get(month); }
    
    public void setEigenVarianceExplained(int month, double[] varExplained) { _varianceExplained.put(month, varExplained); }
    public double[] getEigenVarianceExplained(int month) { return _varianceExplained.get(month); }
    
    public void setRollingGridBoxAnomVar(int month, int year, double[][] rollGridBoxAnomVar) { _rollingGridBoxAnomVar.put(Pair.with(month, year), rollGridBoxAnomVar); }
    public double[][] getRollingGridBoxAnomVar(int month, int year) { return _rollingGridBoxAnomVar.get(Pair.with(month, year)); }
    
    public int getQ() { return this._q; }
}

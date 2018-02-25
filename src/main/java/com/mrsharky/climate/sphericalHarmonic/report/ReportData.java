/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.climate.sphericalHarmonic.report;

import java.util.Map;
import java.util.TreeMap;
import org.javatuples.Pair;

/**
 * 
 * @author Julien Pierret
 */
public class ReportData {
    public String Filename;
    public boolean Normalized;
    public double PcaVarianceExplained;
    public String PcaDataset;
    public int PcaQ;
    public String PcaDates;
    public double GhcnMinimumDistance;
    public int GhcnMinYears;
    
    private Map<Integer, Map<Integer, Pair<Double, Integer>>> _data;
    
    public Map<Integer, Map<Integer, Pair<Double, Integer>>> GetData() {
        return _data;
    }
    
    public ReportData() {
        _data = new TreeMap<Integer, Map<Integer, Pair<Double, Integer>>>();
    }
    
    public void SetData(int year, int month, double value, int stationCount) {
        if (!_data.containsKey(year)) {
            _data.put(year, new TreeMap<Integer, Pair<Double, Integer>>());
        }
        _data.get(year).put(month, Pair.with(value, stationCount));
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.common;

import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.Utilities.SaveStringToFile;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 *
 * @author Julien Pierret
 */
public class TimeseriesResults {
    private Map<Integer, Map<Integer, Triplet<SphericalHarmonic, Double, Double>>> _data;
    
    public TimeseriesResults() {
        _data = new HashMap<Integer, Map<Integer, Triplet<SphericalHarmonic, Double, Double>>>();
    }
    
    public void Set(int year, int month, double value, double spatialError, SphericalHarmonic harmonic) {
        if (!_data.containsKey(year)) {
            _data.put(year, new HashMap<Integer, Triplet<SphericalHarmonic, Double, Double>>());
        }
        _data.get(year).put(month, Triplet.with(harmonic, value, spatialError));
    }

    public Triplet<SphericalHarmonic, Double, Double> Get(int year, int month) {
        return _data.get(year).get(month);
    }
    
    private List<Integer> GetYears() {
        return _data.keySet().stream().sorted().collect(Collectors.toList());
    }
    
    private List<Integer> GetMonths() {
        List<Integer> years = GetYears();
        List<Integer> months = new ArrayList<Integer>();
        for (int year : years) {
            months.addAll(_data.get(year).keySet());
        }
        months = months.stream().distinct().sorted().collect(Collectors.toList());
        return months;
    }
    
    private String GetOverallResults() {
        List<Integer> years = GetYears();
        List<Integer> months = GetMonths();
        StringBuilder sb = new StringBuilder();

        {
            sb.append("Temperature Anomaly\n");
            sb.append("Year,");
            for (int m = 0; m < months.size(); m++) {
                int currMonth = months.get(m);
                sb.append(currMonth + ",");
            }
            sb.append("\n");
            for (int y = 0; y < years.size(); y++) {
                int currYear = years.get(y);
                sb.append(currYear + ",");
                for (int m = 0; m < months.size(); m++) {
                    int currMonth = months.get(m);
                    if (_data.get(currYear).containsKey(currMonth)) {
                        Triplet<SphericalHarmonic, Double, Double> values = _data.get(currYear).get(currMonth);
                        double currValue = (double) values.getValue1();
                        sb.append(currValue);
                    }
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        
        {
            sb.append("Spatial Error (or station count)\n");
            sb.append("Year,");
            for (int m = 0; m < months.size(); m++) {
                int currMonth = months.get(m);
                sb.append(currMonth + ",");
            }
            sb.append("\n");
            for (int y = 0; y < years.size(); y++) {
                int currYear = years.get(y);
                sb.append(currYear + ",");
                for (int m = 0; m < months.size(); m++) {
                    int currMonth = months.get(m);
                    if (_data.get(currYear).containsKey(currMonth)) {
                        Triplet<SphericalHarmonic, Double, Double> values = _data.get(currYear).get(currMonth);
                        double currValue = values.getValue2();
                        sb.append(currValue);
                    }
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    public void Print() {
        String results = GetOverallResults();
        System.out.println(results);
    }
    
    public String GetGriddedData(double[][] gridBoxAnomSd) throws Exception {
        List<Integer> years = GetYears();
        List<Integer> months = GetMonths();
        StringBuilder sb = new StringBuilder();

        sb.append("Temperature Anomaly\n");
        for (int year : years) {
            for (int month : months) {
                sb.append("Year = " + year + ",Month = " + month);
                SphericalHarmonic harmonic =  _data.get(year).get(month).getValue0();
                
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(harmonic);
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                double[][] values = DoubleArray.Multiply(stationHarmonics_spatial, gridBoxAnomSd);
                                        
                sb.append(DoubleArray.PrintToString(values));
            }
        }
        return sb.toString();
    }
    
    public void SaveGriddedDataToCsv(double[][] gridBoxAnomSd, String fileLocation) throws Exception {
        String results = GetGriddedData(gridBoxAnomSd);
        SaveStringToFile(results, fileLocation);
    }
    
    public void SaveOverallResultsToCsv(String fileLocation) {
        String results = GetOverallResults();
        SaveStringToFile(results, fileLocation);
    }
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.dataprocessor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * 
 * @author Julien Pierret
 */
public class NetCdfAnomaly {
    
    private Map<Date, double[][]> _yearlyData;
    private Map<Date, double[][]> _monthlyData;
    
    private Map<Integer, double[][]> _gridBoxAnomalyMean;
    private Map<Integer, double[][]> _gridBoxAnomalyVariance;
    
    public Map<Date, double[][]> GetYearlyData() {
        return this._yearlyData;
    }
    
    public Map<Date, double[][]> GetMonthlyData() {
        return this._monthlyData;
    }
    
    public Map<Integer, double[][]> GetGridBoxAnomalyMean() {
        return this._gridBoxAnomalyMean;
    }
    
    public Map<Integer, double[][]> GetGridBoxAnomalyVariance() {
        return this._gridBoxAnomalyVariance;
    }
    
    
    public NetCdfAnomaly(Map<Date, double[][]> allData, Date baselineLower, Date baselineUpper, boolean normalize) {
        
        _yearlyData = new HashMap<Date, double[][]>();
        _monthlyData = new HashMap<Date, double[][]>();
        
        _gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        _gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        
        // Get the numLans & numLons
        Date anyDate = allData.keySet().iterator().next();
        double[][] anyData = allData.get(anyDate);
        int numLats = anyData.length;
        int numLons = anyData[0].length;
        
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Processing Anomalies");
        System.out.println("------------------------------------------------------------------------");
        for (int month = 0; month <= 12; month++) {
            System.out.println("Processing Anomalies for month: " + month);

            
            // Getting baseline anomaly data
            double[][] gridBoxAnomalyMean3 = new double[numLats][numLons];
            double[][] gridBoxAnomalyVariance3 = new double[numLats][numLons];
            final int currMonth = month;
            
            List<Date> baselinedates = null;
            if (month == 0) { // All months
                baselinedates = allData.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper)).sorted()
                        .collect(Collectors.toList());
            } else {
                baselinedates = allData.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth-1).sorted()
                        .collect(Collectors.toList());
            }
            System.out.println("Number of Baseline Dates: " + baselinedates.size());
            for (int latCounter = 0; latCounter < numLats; latCounter++) {
                for (int lonCounter = 0; lonCounter < numLons; lonCounter++) {
                    double[] baselineValues = new double[baselinedates.size()];

                    for (int dateCounter = 0; dateCounter < baselinedates.size(); dateCounter++) {
                        Date currDate = baselinedates.get(dateCounter);
                        baselineValues[dateCounter] = allData.get(currDate)[latCounter][lonCounter];
                    }

                    DescriptiveStatistics ds = new DescriptiveStatistics(baselineValues);
                    gridBoxAnomalyMean3[latCounter][lonCounter] = ds.getMean();
                    if (normalize) {
                        gridBoxAnomalyVariance3[latCounter][lonCounter] = ds.getVariance();
                    } else {
                        gridBoxAnomalyVariance3[latCounter][lonCounter] = 1.0;
                    }
                }
            }
            _gridBoxAnomalyMean.put(month, gridBoxAnomalyMean3);
            _gridBoxAnomalyVariance.put(month, gridBoxAnomalyVariance3);
            
            // Modify the gridbox values by subtracting out the anomaly and normalizing
            List<Date> dataDates = null;
            if (month == 0) {
                dataDates = allData.keySet().stream().sorted()
                        .filter(data -> data.getMonth() == 0)
                        .collect(Collectors.toList());
                for (int dateCounter = 0; dateCounter < dataDates.size(); dateCounter++) {
                    int year = dataDates.get(dateCounter).getYear();

                    List<Date> yearDates = allData.keySet().stream().sorted()
                            .filter(data -> data.getYear() == year)
                            .collect(Collectors.toList());                    

                    double[][] newValues = new double[numLats][numLons];
                    for (int latCounter = 0; latCounter < numLats; latCounter++) {
                        for (int lonCounter = 0; lonCounter < numLons; lonCounter++) {

                            double[] gridBoxValues = new double[yearDates.size()];
                            for (int yearCounter = 0; yearCounter < yearDates.size(); yearCounter++) {
                                Date currDate = yearDates.get(yearCounter);
                                gridBoxValues[yearCounter] = allData.get(currDate)[latCounter][lonCounter];
                            }
                            DescriptiveStatistics ds = new DescriptiveStatistics(gridBoxValues);
                            double anomaly = gridBoxAnomalyMean3[latCounter][lonCounter];
                            double variance = gridBoxAnomalyVariance3[latCounter][lonCounter];
                            double oldValue = ds.getMean();
                            newValues[latCounter][lonCounter] = (oldValue - anomaly)/Math.sqrt(variance);
                        }
                    }
                    _yearlyData.put(dataDates.get(dateCounter), newValues);
                }
            } else {
                dataDates = allData.keySet().stream()
                        .filter(data -> data.getMonth() == currMonth-1)
                        .collect(Collectors.toList());
                for (int dateCounter = 0; dateCounter < dataDates.size(); dateCounter++) {
                    Date currDate = dataDates.get(dateCounter);
                    double[][] newValues = new double[numLats][numLons];
                    double[][] oldValues = allData.get(currDate);
                    for (int latCounter = 0; latCounter < numLats; latCounter++) {
                        for (int lonCounter = 0; lonCounter < numLons; lonCounter++) {
                            double anomaly = gridBoxAnomalyMean3[latCounter][lonCounter];
                            double variance = gridBoxAnomalyVariance3[latCounter][lonCounter];
                            double oldValue = oldValues[latCounter][lonCounter];
                            newValues[latCounter][lonCounter] = (oldValue - anomaly)/Math.sqrt(variance);
                        }
                    }
                    _monthlyData.put(currDate, newValues);
                }
            }
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
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
public class SphericalHarmonics_LongTermStations_FullSpectral_SaveSource {
    
    public SphericalHarmonics_LongTermStations_FullSpectral_SaveSource(int q, String input, String variable, String time
            , Date lowerDateCutoff, Date upperDateCutoff, Date startDate, Date endDate, boolean normalize, String output) throws Exception {
        
        // Load the NetCDF data
        int Q = q;
        
        // Gridbox level values
        Map<Integer, double[][]> gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        Map<Integer, double[][]> gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        
        // Object that will store the final results
        SphericalHarmonics_Results pcaResults = new SphericalHarmonics_Results(q);
        
        // Generate anomalies and final datasets
        {
            NetCdfLoader loader = new NetCdfLoader(input, variable, time);
            Map<Date, double[][]> allData = loader.LoadData();

            Date minDate = allData.keySet().stream().min((a, b) -> a.compareTo(b)).get();
            Date maxDate = allData.keySet().stream().max((a, b) -> a.compareTo(b)).get();
            System.out.println("Min Date: " + minDate);
            System.out.println("Max Date: " + maxDate);

            Date baselineLower = lowerDateCutoff;
            Date baselineUpper = upperDateCutoff;

            int numLats = loader.GetLats().length;
            int numLons = loader.GetLons().length;
            
            // Final dataset to take PCA of
            Map<Date, double[][]> yearlyData = new HashMap<Date, double[][]>();
            Map<Date, double[][]> monthlyData = new HashMap<Date, double[][]>();

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
                gridBoxAnomalyMean.put(month, gridBoxAnomalyMean3);
                gridBoxAnomalyVariance.put(month, gridBoxAnomalyVariance3);

                // Save the results
                pcaResults.setGridBoxAnomalyMean(month, gridBoxAnomalyMean3);
                pcaResults.setGridBoxAnomalyVariance(month, gridBoxAnomalyVariance3);

                // Modify the gridbox values by subtracting out the anomaly and normalizing
                List<Date> dataDates = null;
                if (month == 0) {
                    dataDates = allData.keySet().stream()
                            .filter(data -> data.getMonth() == 0)
                            .collect(Collectors.toList());
                    for (int dateCounter = 0; dateCounter < dataDates.size(); dateCounter++) {
                        int year = dataDates.get(dateCounter).getYear();

                        List<Date> yearDates = allData.keySet().stream()
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
                        Date currDate = dataDates.get(dateCounter);

                        yearlyData.put(currDate, newValues);
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
                        monthlyData.put(currDate, newValues);
                    }
                }
            }

            for (int month = 0; month <= 12; month++) {
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Saving data to CSV: " + month);
                System.out.println("------------------------------------------------------------------------");
                final int currMonth = month;

                List<Date> dates = null;
                if (month == 0) {
                    dates = yearlyData.keySet().stream()
                        .filter(date -> date.after(startDate) && date.before(endDate)).sorted()
                        .filter(d -> d.getYear()+1900 == 1980)
                        .collect(Collectors.toList());
                } else {
                    dates = monthlyData.keySet().stream()
                        .filter(date -> date.after(startDate) && date.before(endDate) && date.getMonth() == currMonth-1).sorted()
                        .filter(d -> d.getYear()+1900 == 1980)
                        .collect(Collectors.toList());
                }
                final List<Date> finalDates = dates;

                for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                    final int finalDateCounter = dateCounter;

                    
                    Date currDate = finalDates.get(finalDateCounter);               
                    
                    System.out.println("Processing date: " + currDate);
                    double[][] currData2 = null;
                    if (currMonth == 0) {
                        currData2 = yearlyData.get(currDate);
                    } else {
                        currData2 = monthlyData.get(currDate);
                    }

                    // Save results to disk
                    String results = DoubleArray.toCsvString(currData2);
                    int year = currDate.getYear() + 1900;
                    String csvFile = output + "/year=" + year + "_month=" + currMonth + ".serialized";
                    Utilities.SaveStringToFile(results, csvFile);  
                }
            }
        }

    }
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser in = new SphericalHarmonics_InputParser(args, SphericalHarmonics_LongTermStations_FullSpectral_SaveSource.class.getName());
        if (in.InputsCorrect()) {
            SphericalHarmonics_LongTermStations_FullSpectral_SaveSource s = new SphericalHarmonics_LongTermStations_FullSpectral_SaveSource(
                    in.q, in.input, in.variable, in.time
                    , in.lowerDateCutoff, in.upperDateCutoff
                    , in.startDate, in.endDate, in.normalize
                    , in.output);
        }
    }
}

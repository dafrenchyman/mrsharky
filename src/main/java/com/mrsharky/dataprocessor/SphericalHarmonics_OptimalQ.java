/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_log;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_log;
import com.mrsharky.helpers.DoubleArray;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_OptimalQ {
        
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser inputParser = new SphericalHarmonics_InputParser(args, SphericalHarmonics_OptimalQ.class.getName());
        if (inputParser.InputsCorrect()) {
            int Q = inputParser.q;
            NetCdfLoader loader = new NetCdfLoader(inputParser.input, inputParser.variable, inputParser.time);
            Map<Date, double[][]> allData = loader.LoadData();
                                   
            Date baselineLower = inputParser.lowerDateCutoff;
            Date baselineUpper = inputParser.upperDateCutoff;
            
            int numLats = loader.GetLats().length;
            int numLons = loader.GetLons().length;
            
            boolean normalize = true;
            for (int month = 1; month <= 12; month++) {
                System.out.println("Processing Anomalies for month: " + month);
                
                // Getting baseline anomaly data
                double[][] gridBoxAnomalyMean3 = new double[numLats][numLons];
                double[][] gridBoxAnomalyVariance3 = new double[numLats][numLons];
                int currMonth = month -1;
                List<Date> baselinedates = allData.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth).sorted()
                        .collect(Collectors.toList());
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
                        gridBoxAnomalyVariance3[latCounter][lonCounter] = ds.getVariance();
                    }
                }

                // Modify the gridbox values by subtracting out the anomaly and normalizing
                List<Date> dataDates = allData.keySet().stream()
                        .filter(data -> data.getMonth() == currMonth)
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
                            if (normalize) {
                                newValues[latCounter][lonCounter] = (oldValue - anomaly)/Math.sqrt(variance);
                            } else {
                                newValues[latCounter][lonCounter] = oldValue - anomaly;
                            }
                        }
                    }
                    allData.put(currDate, newValues);
                }
            }
            
            // Find the optimal Q value for all the dates
            double[] qError = new double[Q+1];
            List<Date> dataDates = allData.keySet().stream()
                        .filter(data -> data.after(baselineLower) && data.before(baselineUpper)).sorted()
                        .collect(Collectors.toList());
            for (int currQ = 0; currQ <= Q; currQ++) {
                System.out.println("-------------------------------------------------------");
                System.out.println("Processing Q = " + currQ);
                System.out.println("-------------------------------------------------------");
                double currError = 0.0;
                for (Date currDate : dataDates) {
                    double[][] currDateData = allData.get(currDate);
                    DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(currDateData, currQ, true);
                    
                    InvDiscreteSphericalTransform_log invDst = new InvDiscreteSphericalTransform_log(dst.GetSpectraCompressed(), dst.GetM(), dst.GetN(), currQ, true);
                    double[][] currDateRebuilt = DoubleArray.Multiply(invDst.GetSpatial(), -1.0);
                    double[][] error = DoubleArray.Power(DoubleArray.Add(currDateData, currDateRebuilt), 2.0);
                    currError += DoubleArray.SumArray(error);
                }
                System.out.println("Finished Processing Q = " + currQ);
                System.out.println("Total Error: " + currError);
                qError[currQ] = currError;
            }
            DoubleArray.Print(qError); 
        }
    }
}

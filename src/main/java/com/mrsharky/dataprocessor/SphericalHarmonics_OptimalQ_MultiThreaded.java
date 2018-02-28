/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_ass1;
import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_log;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_ass1;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_log;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.shtns.Shtns;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_OptimalQ_MultiThreaded {
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_OptimalQ_InputParser inputParser = new SphericalHarmonics_OptimalQ_InputParser(args, SphericalHarmonics_OptimalQ_MultiThreaded.class.getName());
        if (inputParser.InputsCorrect()) {
            int qLower = inputParser.qLower;
            int qUpper = inputParser.qUpper;
            NetCdfLoader loader = new NetCdfLoader(inputParser.input, inputParser.variable, inputParser.time);
            Map<Date, double[][]> allData = loader.LoadData();
                                   
            Date baselineLower = inputParser.lowerDateCutoff;
            Date baselineUpper = inputParser.upperDateCutoff;
            
            int numLats = loader.GetLats().length;
            int numLons = loader.GetLons().length;
            
            boolean normalize = inputParser.normalize;
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
            double[][] qError = new double[qUpper+1-qLower][2];
            List<Date> dataDates = allData.keySet().stream()
                        .filter(data -> data.after(baselineLower) && data.before(baselineUpper)).sorted()
                        .collect(Collectors.toList());
            for (int currQ = qLower; currQ <= qUpper; currQ++) {
                System.out.println("-------------------------------------------------------");
                System.out.println("Processing Q = " + currQ);
                System.out.println("-------------------------------------------------------");
                double currError = 0.0;
                
                boolean useShtns = true;
                int threads = 1;
                if (!useShtns) {
                    threads = Runtime.getRuntime().availableProcessors();
                }
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Double>> futures = new ArrayList<Future<Double>>();
                final int q = currQ;
                
                if (useShtns) {
                    System.out.println("Setting up SHTns");
                    Shtns shtns = new Shtns(q, q, 1, numLats, numLons, 0);
                    for (final Date currDate : dataDates) {
                        Callable<Double> callable = new Callable<Double>() {
                            public Double call() throws Exception {
                                double[][] currDateData = allData.get(currDate);

                                shtns.SpatialToSpectral(currDateData);
                                Complex [] spectral = shtns.GetSpectralCompressed();

                                shtns.SpectralToSpatial(spectral);
                                double[][] currDateRebuilt = DoubleArray.Multiply(shtns.GetSpatial(), -1.0);
                                double[][] error = DoubleArray.Power(DoubleArray.Add(currDateData, currDateRebuilt), 2.0);

                                // process your input here and compute the output
                                System.out.print("Finished: " + currDate + "\r");
                                return DoubleArray.SumArray(error);
                            }
                        };
                        futures.add(service.submit(callable));

                    }
                } else {
                    for (final Date currDate : dataDates) {    
                        Callable<Double> callable = new Callable<Double>() {
                            public Double call() throws Exception {
                                double[][] currDateData = allData.get(currDate);
                                /*DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(currDateData, q, true);
                                InvDiscreteSphericalTransform_log invDst = new InvDiscreteSphericalTransform_log(dst.GetSpectraCompressed(), dst.GetM(), dst.GetN(), q, true);*/
                                DiscreteSphericalTransform_ass1 dst = new DiscreteSphericalTransform_ass1(currDateData, q, true);
                                InvDiscreteSphericalTransform_ass1 invDst = new InvDiscreteSphericalTransform_ass1(dst.GetSpectraCompressed());
                                double[][] rebuilt = invDst.ProcessGaussianDoubleArray(dst.GetM(), dst.GetN());
                                double[] latCoordinates = dst.GetLatitudeCoordinates();

                                // Error calculation with sin
                                double error = 0;
                                double[][] Error = new double[rebuilt.length][rebuilt[0].length];
                                for (int i = 0; i < rebuilt.length; i++) {
                                    for (int j = 0; j < rebuilt[0].length; j++) {
                                        double currError = Math.pow(currDateData[i][j] - rebuilt[i][j], 2.0);
                                        Error[i][j] = currError;
                                        error += currError * Math.sin(latCoordinates[i]);
                                    }
                                }

                                // Error calculation looking at L = 0, M = 0 of the squared diff
                                DiscreteSphericalTransform_ass1 dstError = new DiscreteSphericalTransform_ass1(Error, q, true);
                                Complex spectraError = dstError.GetSpectraCompressed()[0];
                                System.out.println("Sin Error: " + error + ", M = 0, L = 0 Error: " + spectraError);
                                
                                if (false) {
                                    double[][] currDateRebuilt = DoubleArray.Multiply(rebuilt, -1.0);
                                    double[][] currError = DoubleArray.Power(DoubleArray.Add(currDateData, currDateRebuilt), 2.0);

                                    // process your input here and compute the output
                                    System.out.print("Finished: " + currDate + "\r");

                                    error =  DoubleArray.SumArray(currError);
                                }
                                return spectraError.getReal();
                            }
                        };
                        futures.add(service.submit(callable));
                    }
                }

                // Wait for the threads to finish and add all the data up
                service.shutdown();
                for (Future<Double> future : futures) {
                    currError += future.get();
                }
                qError[currQ-qLower][0] = currQ;
                qError[currQ-qLower][1] = currError;
                System.out.println("Finished Processing Q = " + currQ);
                System.out.println("Total Error: " + currError);
            }
            DoubleArray.Print(qError); 
        }
    }
}

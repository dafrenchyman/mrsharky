/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_ass1;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_ass1;
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
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_OptimalQ_MultiThreaded_Compare {
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_OptimalQ_InputParser inputParser = new SphericalHarmonics_OptimalQ_InputParser(args);
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
            double[][] qError = new double[qUpper+1-qLower][5];
            List<Date> dataDates = allData.keySet().stream()
                        .filter(data -> data.after(baselineLower) && data.before(baselineUpper)).sorted()
                        .collect(Collectors.toList());
            for (int currQ = qLower; currQ <= qUpper; currQ++) {
                System.out.println("-------------------------------------------------------");
                System.out.println("Processing Q = " + currQ);
                System.out.println("-------------------------------------------------------");
                
                int threads = Runtime.getRuntime().availableProcessors();
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Pair<Double, Double>>> futures = new ArrayList<Future<Pair<Double, Double>>>();
                final int q = currQ;
                
                qError[currQ-qLower][0] = currQ;
                
                // Process via SHTns first
                if (true) {
                    System.out.println("Shtns - Start");
                    System.out.println("Setting up SHTns");
                    Shtns shtns = new Shtns(q, q, 1, numLats, numLons, 0);
                    for (final Date currDate : dataDates) {

                        double[][] currDateData = allData.get(currDate);

                        shtns.SpatialToSpectral(currDateData);
                        Complex [] spectral = shtns.GetSpectralCompressed();

                        shtns.SpectralToSpatial(spectral);
                        double[][] rebuilt = shtns.GetSpatial();

                        double[] latCoordinates = shtns.GetLats();

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
                        shtns.SpatialToSpectral(Error);
                        Complex spectraError = shtns.GetSpectralCompressed()[0];
                        System.out.println("Date: " + currDate + ", Sin Error: " + error + ", M = 0, L = 0 Error: " + spectraError.getReal());
                        qError[currQ-qLower][1] += error;
                        qError[currQ-qLower][3] += spectraError.getReal();
                    }
                    System.out.println("Shtns - Finish");
                }
                
                System.out.println("DST - Start");
                for (final Date currDate : dataDates) {    
                    Callable<Pair<Double, Double>> callable = new Callable<Pair<Double, Double>>() {
                        public Pair<Double, Double> call() throws Exception {
                            double[][] currDateData = allData.get(currDate);
                            /*DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(currDateData, q, true);
                            InvDiscreteSphericalTransform_log invDst = new InvDiscreteSphericalTransform_log(dst.GetSpectraCompressed(), dst.GetM(), dst.GetN(), q, true);*/
                            /*DiscreteSphericalTransform_ass1 dst = new DiscreteSphericalTransform_ass1(currDateData, q, true);
                            InvDiscreteSphericalTransform_ass1 invDst = new InvDiscreteSphericalTransform_ass1(dst.GetSpectraCompressed());*/
                            DiscreteSphericalTransform dst = new DiscreteSphericalTransform(currDateData, q, true);
                            InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(dst.GetSpectra());
                            
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
                            DiscreteSphericalTransform dstError = new DiscreteSphericalTransform(Error, q, true);
                            Complex spectraError = dstError.GetSpectra().GetHarmonic(0, 0);
                            System.out.println("Date: " + currDate + ", Sin Error: " + error + ", M = 0, L = 0 Error: " + spectraError.getReal());

                            if (false) {
                                double[][] currDateRebuilt = DoubleArray.Multiply(rebuilt, -1.0);
                                double[][] currError = DoubleArray.Power(DoubleArray.Add(currDateData, currDateRebuilt), 2.0);

                                // process your input here and compute the output
                                System.out.print("Finished: " + currDate + "\r");

                                error =  DoubleArray.SumArray(currError);
                            }
                            Pair<Double, Double> results = Pair.with(error, spectraError.getReal());
                            return results;
                        }
                    };
                    futures.add(service.submit(callable));
                }


                // Wait for the threads to finish and add all the data up
                service.shutdown();
                for (Future<Pair<Double, Double>> future : futures) {
                    qError[currQ-qLower][2] += future.get().getValue0();
                    qError[currQ-qLower][4] += future.get().getValue1();
                }  
                System.out.println("DST - Finish");
                System.out.println("Finished Processing Q = " + currQ);
            }
            DoubleArray.Print(qError); 
        }
    }
}

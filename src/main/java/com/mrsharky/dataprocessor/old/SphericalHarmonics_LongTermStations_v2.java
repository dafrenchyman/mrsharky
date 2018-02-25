/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor.old;

import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_ass1;
import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_log;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_ass1;
import com.mrsharky.dataAnalysis.LoadData_svdTest;
import com.mrsharky.dataAnalysis.PcaCovJBlas;
import com.mrsharky.dataprocessor.NetCdfLoader;
import com.mrsharky.dataprocessor.SphericalHarmonics_InputParser;
import com.mrsharky.stations.ghcn.GhcnV3_Helpers;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import static com.mrsharky.helpers.JblasMatrixHelpers.Print;
import static com.mrsharky.helpers.Utilities.SerializeObject;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
import org.jblas.ComplexDoubleMatrix;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_LongTermStations_v2 {
    
    public SphericalHarmonics_LongTermStations_v2(int q, String input, String variable, String time
            , Date lowerDateCutoff, Date upperDateCutoff, String output) throws Exception {
        
        // Load the NetCDF data
        int Q = q;
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        Map<Date, double[][]> allData = loader.LoadData();

        Date baselineLower = lowerDateCutoff;
        Date baselineUpper = upperDateCutoff;

        int numLats = loader.GetLats().length;
        int numLons = loader.GetLons().length;

        boolean normalize = true;
        Map<Integer, double[][]> gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        Map<Integer, double[][]> gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
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
            gridBoxAnomalyMean.put(month, gridBoxAnomalyMean3);
            gridBoxAnomalyVariance.put(month, gridBoxAnomalyVariance3);
            
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

        SphericalHarmonics_PcaResults pcaResults = new SphericalHarmonics_PcaResults(q);
        for (int month = 1; month <= 12; month++) {
            System.out.println("Converting from Spatial to Spectral for month: " + month);
            int currMonth = month -1;
            List<Date> dates = allData.keySet().stream()
                    .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth).sorted()
                    .collect(Collectors.toList());

            // Convert data from spatial to spectral
            int numOfQpoints = (int) Math.pow(Q+1, 2.0);
            Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
            
            int threads = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(threads);
            List<Future<Pair<Integer, Complex[]>>> futures = new ArrayList<Future<Pair<Integer, Complex[]>>>();
            
            for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                final int finalDateCounter = dateCounter;
                
                // Process the spherical harmonics multi-threaded
                Callable<Pair<Integer, Complex[]>> callable = new Callable<Pair<Integer, Complex[]>>() {
                    public Pair<Integer, Complex[]> call() throws Exception {
                        Date currDate = dates.get(finalDateCounter);
                        
                        System.out.println("Processing date: " + currDate);
                        double[][] currData2 = allData.get(currDate);
                        
                        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(currData2, Q, true);
                        Complex[] spectral = dst.GetSpectra().GetFullCompressedSpectra();
                        
                        Pair<Integer, Complex[]> results = Pair.with(finalDateCounter, spectral);
                        return results;
                    }};
                futures.add(service.submit(callable));
            }
            
            // Wait for the threads to finish and then save all the dates
            service.shutdown();
            for (Future<Pair<Integer, Complex[]>> future : futures) {
                int dateCounter = future.get().getValue0();
                Complex[] spectral = future.get().getValue1();
                for (int rowCounter = 0; rowCounter < numOfQpoints; rowCounter++) {
                    qRealizations[rowCounter][dateCounter] = spectral[rowCounter];
                }
            }

            ComplexDoubleMatrix qRealizationsMatrix = ApacheMath3ToJblas(qRealizations);

            System.out.println("Processing Complex Conjugate of Qrealizations");
            ComplexDoubleMatrix Qrealizations_trans = qRealizationsMatrix.transpose().conj();
            System.out.println("qRealizationsMatrix:" + qRealizationsMatrix.rows + "," + qRealizationsMatrix.columns);
            System.out.println("Qrealizations_trans:" + Qrealizations_trans.rows + "," + Qrealizations_trans.columns);

            System.out.println("Processing R_hat_s");
            ComplexDoubleMatrix R_hat_s = (qRealizationsMatrix.mmul(Qrealizations_trans)).mul(1/(dates.size() + 0.0));

            System.out.println("R_hat_s:" + R_hat_s.rows + "," + R_hat_s.columns);

            System.out.println("Processing PCA");
            PcaCovJBlas pca = new PcaCovJBlas(R_hat_s);

            // We now have our eigenValues and eigenVectorss
            double varExpCutoff = 0.9;
            Complex[] eigenValues = pca.GetEigenValuesMath3();
            Complex[][] eigenVectors = pca.GetEigenVectorsMath3();                
            double[] varExplained = pca.GetSumOfVarianceExplained();
            

            // If we are only getting a certain number of PCAs
            if (varExpCutoff <= 1.0) {
                // find where the cutoff is
                int cutOffLocation = 0;
                for (int i = 0; i < varExplained.length; i++) {
                    cutOffLocation = i;
                    if (varExplained[i] >= varExpCutoff) {
                        System.out.println("    cuttoff: " + cutOffLocation + " of " + (varExplained.length-1));
                        break;
                    }
                }
                eigenVectors = ComplexArray.Subset(eigenVectors, 0, eigenVectors.length-1, 0, cutOffLocation);
                eigenVectors = ComplexArray.Transpose(eigenVectors);
                eigenValues = ComplexArray.Subset(eigenValues, 0, cutOffLocation);
                varExplained = DoubleArray.Subset(varExplained, 0, cutOffLocation);
            }

            // Save results
            pcaResults.setResults(month, eigenValues, eigenVectors, gridBoxAnomalyMean.get(month), gridBoxAnomalyVariance.get(month), varExplained);
            
            // Convert data back to spatial
            /*Map<Integer, double[][]> spatial = new HashMap<Integer, double[][]>();
            for (int eigenCounter = 0; eigenCounter < eigenVectors[0].length; eigenCounter++) {
                Complex[] currEigenVector = ComplexArray.GetColumn(eigenVectors, eigenCounter);
                InvDiscreteSphericalTransform_ass invDst = new InvDiscreteSphericalTransform_ass(currEigenVector);
                double[][] currSpatial = invDst.ProcessGaussianDoubleArray(M, N);
                spatial.put(eigenCounter, currSpatial);
            }*/
        }
        SerializeObject(pcaResults, output);
    }
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser in = new SphericalHarmonics_InputParser(args, SphericalHarmonics_LongTermStations_v2.class.getName());
        if (in.InputsCorrect()) {
            SphericalHarmonics_LongTermStations_v2 s = new SphericalHarmonics_LongTermStations_v2(
                    in.q, in.input, in.variable, in.time
                    , in.lowerDateCutoff, in.upperDateCutoff, in.output);
        }
    }
}

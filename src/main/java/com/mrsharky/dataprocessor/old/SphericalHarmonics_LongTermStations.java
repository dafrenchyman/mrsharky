/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor.old;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransform_log;
import com.mrsharky.dataAnalysis.LoadData_svdTest;
import com.mrsharky.dataAnalysis.PcaCovJBlas;
import com.mrsharky.dataprocessor.NetCdfLoader;
import com.mrsharky.dataprocessor.SphericalHarmonics_InputParser;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import static com.mrsharky.helpers.JblasMatrixHelpers.Print;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.complex.Complex;
import org.jblas.ComplexDoubleMatrix;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_LongTermStations {
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser inputParser = new SphericalHarmonics_InputParser(args, SphericalHarmonics_LongTermStations.class.getName());
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
            
            
            for (int month = 1; month <= 12; month++) {
                System.out.println("Converting from Spatial to Spectral for month: " + month);
                int currMonth = month -1;
                List<Date> dates = allData.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth).sorted()
                        .collect(Collectors.toList());
                
                // Convert data from spatial to spectral
                
                int M = 0;
                int N = 0;
                int numOfQpoints = (int) Math.pow(Q+1, 2.0);
                Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
                for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                    Date currDate = dates.get(dateCounter);

                    System.out.println("Processing date: " + currDate);
                    double[][] currData2 = allData.get(currDate);

                    try {
                        DiscreteSphericalTransform_log dst = new DiscreteSphericalTransform_log(currData2, Q, true);
                        Complex[] spectral = dst.GetSpectraCompressed();
                        M = dst.GetM();
                        N = dst.GetN();
                        for (int rowCounter = 0; rowCounter < numOfQpoints; rowCounter++) {
                            qRealizations[rowCounter][dateCounter] = spectral[rowCounter];
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(LoadData_svdTest.class.getName()).log(Level.SEVERE, null, ex);
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
                
                Print(pca.GetEigenValues());
                Print(pca.GetEigenVectors());
                DoubleArray.Print(pca.GetVarianceExplained());
                
                double[] varExplained = pca.GetVarianceExplained();
            }
            
        }
    }
}

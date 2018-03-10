/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.dataAnalysis.PcaCovJBlas;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import com.mrsharky.stations.netcdf.GridBoxVariance;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
import org.jblas.ComplexDoubleMatrix;
import static com.mrsharky.helpers.Utilities.SerializeObjectLocal;

/**
 *
 * @author Julien Pierret
 */
public class SphericalHarmonics_LongTermStations {
    
    public SphericalHarmonics_LongTermStations(int q, String input, String variable, String time
            , Date lowerDateCutoff, Date upperDateCutoff, Date startDate, Date endDate, boolean normalize, String output) throws Exception {
        
        // Load the NetCDF data
        int Q = q;
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        Map<Date, double[][]> allData = loader.LoadData();
        
        Date minDate = allData.keySet().stream().min((a, b) -> a.compareTo(b)).get();
        Date maxDate = allData.keySet().stream().max((a, b) -> a.compareTo(b)).get();
        System.out.println("Min Date: " + minDate);
        System.out.println("Max Date: " + maxDate);
        
        Map<Date, double[][]> yearlyData = new HashMap<Date, double[][]>();
        Map<Date, double[][]> monthlyData = new HashMap<Date, double[][]>();

        Date baselineLower = lowerDateCutoff;
        Date baselineUpper = upperDateCutoff;

        int numLats = loader.GetLats().length;
        int numLons = loader.GetLons().length;

        // Calculate gridbox area
        AreasForGrid areasForGrid = new AreasForGrid(numLats, numLons,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
       
        // Object that will store the final results
        SphericalHarmonics_PcaResults pcaResults = new SphericalHarmonics_PcaResults(q);
        
        // Gridbox level values
        Map<Integer, double[][]> gridBoxAnomalyMean = new HashMap<Integer, double[][]>();
        Map<Integer, double[][]> gridBoxAnomalyVariance = new HashMap<Integer, double[][]>();
        
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
            
            // Calculate an overall mean and variance (at the global level, not gridbox level)
            {    
                // Calculate a one number mean and variance
                {
                    System.out.println("Calculating global mean/variance");
                    double[] globalValues = new double[baselinedates.size()];
                    for (int dateCounter = 0; dateCounter < baselinedates.size(); dateCounter++) {
                        Date currDate = baselinedates.get(dateCounter);
                        globalValues[dateCounter] = DoubleArray.SumArray(DoubleArray.Multiply(allData.get(currDate), areaFraction));
                    }
                    DescriptiveStatistics ds = new DescriptiveStatistics(globalValues);          
                    pcaResults.SetOneNumberResults(month, ds.getMean(), ds.getVariance());
                }
                
                // Calculate a "rolling" variance
                {
                    int varianceNumYears = 30;
                    int yearsToAdd = varianceNumYears / 2;
                    System.out.println("Calculating global rolling mean/variance");

                    Date currDate = (Date) minDate.clone();
                    while (currDate.getYear() <= maxDate.getYear()) {
                        Calendar calendar = Calendar.getInstance();
                        System.out.println("Processing Rolling Anomalies for month: " + month + ", date: " + currDate);

                        // Get lower & upper cut-offs
                        Date lowerCutOff = null;
                        Date upperCutOff = null;
                        {
                            calendar.setTime(currDate);
                            calendar.add(Calendar.YEAR, -1*yearsToAdd);
                            calendar.add(Calendar.HOUR, -24);
                            lowerCutOff = calendar.getTime();

                            calendar.setTime(currDate);
                            calendar.add(Calendar.YEAR,    yearsToAdd);
                            //calendar.add(Calendar.HOUR,  24);
                            upperCutOff = calendar.getTime();
                        }
                        final Date lowerCutOff_f = lowerCutOff;
                        final Date upperCutOff_f = upperCutOff;

                        // Getting baseline anomaly data
                        List<Date> rollingBaselinedates = null;
                        if (month == 0) { // All months
                            rollingBaselinedates = allData.keySet().stream()
                                    .filter(date -> date.after(lowerCutOff_f) && date.before(upperCutOff_f)).sorted()
                                    .collect(Collectors.toList());
                        } else {
                            rollingBaselinedates = allData.keySet().stream()
                                    .filter(date -> date.after(lowerCutOff_f) && date.before(upperCutOff_f) && date.getMonth() == currMonth-1).sorted()
                                    .collect(Collectors.toList());
                        }

                        System.out.println("Number of Rolling Baseline Dates: " + rollingBaselinedates.size());
                        double[] globalValues = new double[rollingBaselinedates.size()];
                        for (int dateCounter = 0; dateCounter < rollingBaselinedates.size(); dateCounter++) {
                            Date rollDate = rollingBaselinedates.get(dateCounter);
                            globalValues[dateCounter] = DoubleArray.SumArray(DoubleArray.Multiply(allData.get(rollDate), areaFraction));
                        }
                        DescriptiveStatistics ds = new DescriptiveStatistics(globalValues);  
                        pcaResults.setGlobalResults(month, currDate.getYear() + 1900, ds.getMean(), ds.getVariance());

                        // Add year to date
                        calendar.setTime(currDate);
                        calendar.add(Calendar.YEAR, 1);
                        currDate = calendar.getTime();
                    }
                }
            }
            
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
                    yearlyData.put(dataDates.get(dateCounter), newValues);
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
        
        // Process variable Gridbox data
        GridBoxVariance gbv = new GridBoxVariance(allData, numLats, numLons, 30);
        pcaResults.AddVariableAnamolyResults(gbv.GetGridBoxAnomVar());
        
        for (int month = 0; month <= 12; month++) {
            System.out.println("------------------------------------------------------------------------");
            System.out.println("Converting from Spatial to Spectral for month: " + month);
            System.out.println("------------------------------------------------------------------------");
            final int currMonth = month;
            
            List<Date> dates = null;
            if (month == 0) {
                dates = yearlyData.keySet().stream()
                    .filter(date -> date.after(startDate) && date.before(endDate)).sorted()
                    .collect(Collectors.toList());
            } else {
                dates = monthlyData.keySet().stream()
                    .filter(date -> date.after(startDate) && date.before(endDate) && date.getMonth() == currMonth-1).sorted()
                    .collect(Collectors.toList());
            }
            final List<Date> finalDates = dates;
            // Convert data from spatial to spectral
            
            int numOfQpoints = SphericalHarmonic.CalculateNumPointsFromQ(Q);
            Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
            
            int threads = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(threads);
            List<Future<Pair<Integer, Complex[]>>> futures = new ArrayList<Future<Pair<Integer, Complex[]>>>();
            
            for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                final int finalDateCounter = dateCounter;
                
                // Process the spherical harmonics multi-threaded
                Callable<Pair<Integer, Complex[]>> callable = new Callable<Pair<Integer, Complex[]>>() {
                    public Pair<Integer, Complex[]> call() throws Exception {
                        Date currDate = finalDates.get(finalDateCounter);
                        
                        System.out.println("Processing date: " + currDate);
                        double[][] currData2 = null;
                        if (currMonth == 0) {
                            currData2 = yearlyData.get(currDate);
                        } else {
                            currData2 = monthlyData.get(currDate);
                        }
                        
                        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(currData2, Q, true);
                        Complex[] spectral = dst.GetSpectra().GetHalfCompressedSpectra();
                        
                        Pair<Integer, Complex[]> results = Pair.with(finalDateCounter, spectral);
                        return results;
                    }};
                futures.add(service.submit(callable));
            }
            
            // Wait for the threads to finish and then save all the dates
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for (Future<Pair<Integer, Complex[]>> future : futures) {
                int dateCounter = future.get().getValue0();
                Complex[] spectral = future.get().getValue1();
                for (int rowCounter = 0; rowCounter < numOfQpoints; rowCounter++) {
                    qRealizations[rowCounter][dateCounter] = spectral[rowCounter];
                }
            }

            ComplexDoubleMatrix qRealizationsMatrix = ApacheMath3ToJblas(qRealizations);

            System.out.println("Processing Complex Conjugate of Qrealizations");
            ComplexDoubleMatrix qRealizations_trans = qRealizationsMatrix.transpose().conj();
            System.out.println("qRealizationsMatrix:" + qRealizationsMatrix.rows + "," + qRealizationsMatrix.columns);
            System.out.println("qRealizations_trans:" + qRealizations_trans.rows + "," + qRealizations_trans.columns);

            System.out.println("Processing R_hat_s");
            ComplexDoubleMatrix R_hat_s = (qRealizationsMatrix.mmul(qRealizations_trans)).mul(1/(dates.size() + 0.0));

            System.out.println("R_hat_s:" + R_hat_s.rows + "," + R_hat_s.columns);

            System.out.println("Processing PCA");
            PcaCovJBlas pca = new PcaCovJBlas(R_hat_s);

            // We now have our eigenValues and eigenVectorss
            double varExpCutoff = 0.99;
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
        SerializeObjectLocal(pcaResults, output);
    }
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser in = new SphericalHarmonics_InputParser(args, SphericalHarmonics_LongTermStations.class.getName());
        if (in.InputsCorrect()) {
            SphericalHarmonics_LongTermStations s = new SphericalHarmonics_LongTermStations(
                    in.q, in.input, in.variable, in.time
                    , in.lowerDateCutoff, in.upperDateCutoff
                    , in.startDate, in.endDate, in.normalize
                    , in.output);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

import com.mrsharky.climate.sphericalHarmonic.common.CalculateHarmonic;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import static com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec.CutoffVarExplained;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.dataprocessor.SphericalHarmonics_Results;
import com.mrsharky.stations.StationSelectionResults;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import com.mrsharky.stations.StationResults;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

/**
 *
 * @author Julien Pierret
 */
public class ClimateFromStations1_FullSpectra {
    
    private boolean _verbose;
    
    public ClimateFromStations1_FullSpectra(String pcaDataLocation, String stationDataLocation, double varExpCutoff, int q, boolean normalized, String output) throws Exception {
        
        // Check for system properties           
        _verbose = false;
        String property = System.getProperty("verbose");
        if (property != null) {
            _verbose = property.toUpperCase().equals("TRUE") ? true : false;
        }
                 
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_Results pcaData = (SphericalHarmonics_Results) LoadSerializedObject(pcaDataLocation);
        
        // Create Array that will hold all the results     
        TimeseriesResults finalResults = new TimeseriesResults();

        
        if (q == -1) {
            q = pcaData.getQ();
        }
        
        final int q_f = q;
        
        // Calculate the gridbox area
        double[][] gridBox = pcaData.getGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));

        for (int month = 0; month <= 12; month++) {
            System.out.println("Processing month: " + month);
            final int currMonth = (month == 0 ? 0 : month-1); 
            
            Triplet<Complex[][], Complex[], double[]> Eigens = CutoffVarExplained(varExpCutoff, 
                    pcaData.getEigenVectors(month),
                    pcaData.getEigenValues(month),
                    pcaData.getEigenVarianceExplained(month));
            
            final Complex[][] eigenVectors_f = Eigens.getValue0();
            final Complex[] eigenValues_f = Eigens.getValue1();
                 
            int numEigen = eigenValues_f.length;
            List<Date> monthDates = stationData.GetDates(month).stream()
                    .filter(d -> d.getMonth() == currMonth)
                    //.filter(d -> d.getYear()+1900 > 1880)
                    .sorted()
                    .collect(Collectors.toList());
            
            // Pre process some eigenvalue and eigenvector stuff
            Map<Integer, SphericalHarmonic> eigenSpherical = new HashMap<Integer, SphericalHarmonic>();
            Map<Integer, InvDiscreteSphericalTransform> eigenInvDst = new HashMap<Integer, InvDiscreteSphericalTransform>();
            for (int e = 0; e < numEigen; e++) {
                Complex[] currEigenVector = eigenVectors_f[e];
                SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, false);
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
                eigenSpherical.put(e, currEigenSpherical);
                eigenInvDst.put(e, invDst);
            }      
            
            double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.getGridBoxAnomalyVariance(month), 0.5);
            
            CalculateHarmonic CalcHarm = new CalculateHarmonic(q_f, normalized, eigenVectors_f, eigenValues_f);
            
            for (Date currDate : monthDates) {
                
                int threads = Runtime.getRuntime().availableProcessors();
                //threads = 1;
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Quartet<Integer, Integer, Complex, Integer>>> futures = new ArrayList<Future<Quartet<Integer, Integer, Complex, Integer>>>();
                
                final Date currDate_f = currDate;
                List<StationResults> stations = stationData.GetDate(month, currDate_f);
                
                for (int k = 0; k <= q; k++) {
                    for (int l = 0; l <= k; l++) {

                        final int k_f = k;
                        final int l_f = l;
                
                        // Process the spherical harmonics multi-threaded
                        Callable<Quartet<Integer, Integer, Complex, Integer>> callable = new Callable<Quartet<Integer, Integer, Complex, Integer>>() {
                            public Quartet<Integer, Integer, Complex, Integer> call() throws Exception {

                                Pair<Complex, double[]> values = CalcHarm.Process(k_f, l_f, stations);
                                
                                Complex S_kl = values.getValue0();
                                double[] weights = values.getValue1();
                                int stationCount = weights.length;
                                Quartet<Integer, Integer, Complex, Integer> results = Quartet.with(k_f, l_f, S_kl, stationCount);
                                
                                if (_verbose) {
                                    System.out.println(currDate + ", k: " + k_f  + ", l:" + l_f + ", harmonic: " + S_kl +", stationCount: " + stationCount );
                                }
                                return results;
                            }
                        };
                        futures.add(service.submit(callable));
                    }
                }
             
                service.shutdown();
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(q);
                int stnCount = 0;
                for (Future<Quartet<Integer, Integer, Complex, Integer>> future : futures) {
                    int k = future.get().getValue0();
                    int l = future.get().getValue1();
                    Complex S_kl = future.get().getValue2();
                    stnCount = future.get().getValue3();
                    finalStationHarmonic.SetHarmonic(k, l, S_kl);
                }
                
                //finalStationHarmonic.PrintHarmonic();

                // Now that we have the final harmonic. Convert to spatial
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                double[][] gridbox_values = DoubleArray.Multiply(stationHarmonics_spatial, gridBoxAnomSd);
                
                double[][] gridbox_weighted = DoubleArray.Multiply(gridbox_values, areaFraction);
                double value = DoubleArray.SumArray(gridbox_weighted);
                
                // Get a non-area-weighted version of the errors for each gridbox (to show that the individual error is going down,.. hopefully
                gridbox_values = DoubleArray.Multiply(gridbox_values, -1.0);
                double[][] baseline = month == 0 ? pcaData.getOriginalYearlyData().get(currDate): pcaData.getOriginalMonthlyData().get(currDate);
                double[][] spatialError = DoubleArray.Power(DoubleArray.Add(baseline, gridbox_values),  2.0);
                double totalSpatialError = DoubleArray.SumArray(spatialError);

                //DoubleArray.Print(gridBoxAnomSd);       
                
                int currYear = currDate_f.getYear() + 1900;
                System.out.println(currYear + "\t" + value + "\t" + stnCount);

                finalResults.Set(currYear, month, value, totalSpatialError, finalStationHarmonic);
            }
        }
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(output);
    }
    
    public static void main(String args[]) throws Exception {   
        ClimateFromStations_InputParser in = new ClimateFromStations_InputParser(args, ClimateFromStations1_FullSpectra.class.getName());
        if (in.InputsCorrect()) {
            ClimateFromStations1_FullSpectra c = new ClimateFromStations1_FullSpectra(
                    in.dataEof, in.dataStations, in.varExplained, in.q, in.normalized, in.output);
        }
    }
}

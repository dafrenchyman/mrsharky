/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.old;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.climate.sphericalHarmonic.common.Helpers;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonicY;
import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Quartet;
import org.javatuples.Triplet;

/**
 *
 * @author Julien Pierret
 */
public class ClimateFromStations1 {
    
    private boolean _verbose = false;
    
    public ClimateFromStations1(String pcaDataLocation, String stationDataLocation, double varExpCutoff, int q, boolean normalized, String output) throws Exception {
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_PcaResults pcaData = (SphericalHarmonics_PcaResults) LoadSerializedObject(pcaDataLocation);
        
        // Create Array that will hold all the results     
        TimeseriesResults finalResults = new TimeseriesResults();

        if (q == -1) {
            q = pcaData.GetQ();
        }
        
        final int q_f = q;
        
        // Calculate the gridbox area
        double[][] gridBox = pcaData.GetGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Trim the eigenValues & vectorns to "varExpCutoff"
        Pca_EigenValVec Eigens = new Pca_EigenValVec(pcaData, varExpCutoff);
        
        for (int month = 0; month <= 12; month++) {
            System.out.println("Processing month: " + month);
            final int month_f = month;
            final int currMonth = (month == 0 ? 0 : month-1); 
            final Complex[][] eigenVectors_f = Eigens.GetEigenVectors(month);
            final Complex[] eigenValues_f = Eigens.GetEigenValues(month);
                 
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
                SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, true);
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
                eigenSpherical.put(e, currEigenSpherical);
                eigenInvDst.put(e, invDst);
            }      
            
            // Key to store all the weights
            Map<Triplet<Integer, Integer, List<Long>>, double[]> weightsMap = new ConcurrentHashMap<Triplet<Integer, Integer, List<Long>>, double[]>();
            
            double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.GetGridBoxAnomalyVariance(month), 0.5);
            
            for (Date currDate : monthDates) {
                
                int threads = Runtime.getRuntime().availableProcessors();
                //threads = 1;
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Quartet<Integer, Integer, Complex, Integer>>> futures = new ArrayList<Future<Quartet<Integer, Integer, Complex, Integer>>>();
                
                final Date currDate_f = currDate;
                
                for (int k = 0; k <= q; k++) {
                    for (int l = 0; l <= k; l++) {

                        final int k_f = k;
                        final int l_f = l;
                
                        // Process the spherical harmonics multi-threaded
                        Callable<Quartet<Integer, Integer, Complex, Integer>> callable = new Callable<Quartet<Integer, Integer, Complex, Integer>>() {
                            public Quartet<Integer, Integer, Complex, Integer> call() throws Exception {

                                List<StationResults> stations = stationData.GetDate(month_f, currDate_f);
                                stations = stations.stream().sorted((a,b) -> Long.compare(b.StationId, a.StationId)).collect(Collectors.toList());

                                double[] lats = new double[stations.size()];
                                double[] lons = new double[stations.size()];
                                double[] stationMean = new double[stations.size()];
                                double[] stationSd = new double [stations.size()];
                                double[] stationValue = new double [stations.size()];
                                long[] stationId = new long[stations.size()];

                                List<Long> stationList = new ArrayList<Long>();
                                for (int i = 0; i < stations.size(); i++) {
                                    StationResults currStation = stations.get(i);
                                    stationId[i] = currStation.StationId;                     
                                    lats[i] = Utilities.LatitudeToRadians(currStation.Lat);
                                    lons[i] = Utilities.LongitudeToRadians(currStation.Lon);
                                    stationMean[i] = currStation.BaselineMean;
                                    stationValue[i] = currStation.Value;
                                    stationSd[i] =  Math.sqrt(currStation.BaselineVariance);
                                    stationList.add(currStation.StationId);
                                }

                                SphericalHarmonicY stationHarmonics = new SphericalHarmonicY(lats, lons, q_f); 

                                boolean uniformWeight = false;

                                Complex S_lm = null;

                                // Calculate the weights
                                double[] weights = new double[stationId.length];
                                if (uniformWeight) {
                                    for (int i = 0; i < weights.length; i++) {
                                        weights[i] = (1.0/(weights.length + 0.0)) * (4.0*Math.PI);
                                    }
                                } else {

                                    Triplet<Integer, Integer, List<Long>> weightKey = Triplet.with(k_f, l_f, stationList);
                                    if (weightsMap.containsKey(weightKey)) {
                                        weights = weightsMap.get(weightKey);                                   
                                    } else {
                                        weights = Helpers.GenerateWeights(k_f, l_f, lats, lons, stationHarmonics, eigenVectors_f, eigenValues_f, eigenSpherical, eigenInvDst);
                                        weightsMap.put(weightKey, weights);
                                    }
                                }

                                // Now that we have the weights for given k,l, calculate the harmonic
                                double[] S_n = DoubleArray.Add(stationValue, DoubleArray.Multiply(stationMean, -1.0));

                                if (normalized) {
                                    S_n = DoubleArray.Multiply(S_n, DoubleArray.Power(stationSd, -1.0));
                                }
                                Complex[] S_n_Y_conj = ComplexArray.Multiply(S_n, stationHarmonics.Conjugate().GetHarmonic(0, 0));
                                Complex[] S_n_Y_conj_w = ComplexArray.Multiply(S_n_Y_conj, weights);

                                S_lm = ComplexArray.Sum(S_n_Y_conj_w);
                                
                                Quartet<Integer, Integer, Complex, Integer> results = Quartet.with(k_f, l_f, S_lm, weights.length-1);
                                
                                if (_verbose) {
                                    System.out.println(currDate + ", k: " + k_f  + ", l:" + l_f + ", harmonic: " + S_lm +", stationCount: " + lats.length );
                                    //System.out.println("Weights");
                                    //DoubleArray.Print(weights);
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
                    Complex S_km = future.get().getValue2();
                    stnCount = future.get().getValue3();
                    finalStationHarmonic.SetHarmonic(k, l, S_km);
                }

                // Now that we have the final harmonic. Convert to spatial
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                double value = DoubleArray.SumArray(
                        DoubleArray.Multiply(
                                DoubleArray.Multiply(stationHarmonics_spatial, areaFraction/*areasForGrid.GetAreas()*/),
                                gridBoxAnomSd));

                int currYear = currDate_f.getYear() + 1900;
                System.out.println(currYear + "\t" + value + "\t" + stnCount);

                finalResults.Set(currYear, month, value, stnCount, finalStationHarmonic);
            }
        }
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(output);
    }
    
    public static void main(String args[]) throws Exception {   
        ClimateFromStations_InputParser in = new ClimateFromStations_InputParser(args, ClimateFromStations1.class.getName());
        if (in.InputsCorrect()) {
            ClimateFromStations1 c = new ClimateFromStations1(
                    in.dataEof, in.dataStations, in.varExplained, in.q, in.normalized, in.output);
        }
    }
}

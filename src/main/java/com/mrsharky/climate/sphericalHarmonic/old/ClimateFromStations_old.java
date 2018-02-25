/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.old;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.climate.sphericalHarmonic.common.*;
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonicY;
import static com.mrsharky.discreteSphericalTransform.SphericalHelpers.UnCompressSpectral;
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
public class ClimateFromStations_old {
    
    public ClimateFromStations_old(String pcaDataLocation, String stationDataLocation, double varExpCutoff) throws Exception {
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_PcaResults pcaData = (SphericalHarmonics_PcaResults) LoadSerializedObject(pcaDataLocation);
        
        // Create Array that will hold all the results
        TimeseriesResults finalResults = new TimeseriesResults();

        int q = pcaData.GetQ();
        q = 0;
        
        // Calculate the gridbox area
        double[][] gridBox = pcaData.GetGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Trim the eigenValues & vectorns to "varExpCutoff"
        Pca_EigenValVec Eigens = new Pca_EigenValVec(pcaData, varExpCutoff);
        
        int month = 1;
        {
        //for (int month = 0; month <= 12; month++) {
            System.out.println("Processing month: " + month);
            final int currMonth = (month == 0 ? 0 : month-1); 
            final Complex[][] eigenVectors_f = Eigens.GetEigenVectors(month);
            final Complex[] eigenValues_f = Eigens.GetEigenValues(month);
                 
            int numEigen = eigenValues_f.length;
            List<Date> monthDates = stationData.GetDates(month).stream()
                    .filter(d -> d.getMonth() == currMonth)
                    .filter(d -> d.getYear() + 1900 > 1880)
                    .filter(d -> d.getYear() + 1900 == 1931)
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
            
            for (Date currDate : monthDates) {
                // Get the current System time
                long startTime = System.currentTimeMillis();
                
                List<StationResults> stations = stationData.GetDate(month, currDate);
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
                
                //double[][] gridBoxAnomMean = pcaData.GetGridBoxAnomalyMean(month);
                double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.GetGridBoxAnomalyVariance(month), 0.5);               
                
                SphericalHarmonicY stationHarmonics = new SphericalHarmonicY(lats, lons, q); 
                
                int threads = Runtime.getRuntime().availableProcessors();
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Quartet<Integer, Integer, Complex, double[]>>> futures = new ArrayList<Future<Quartet<Integer, Integer, Complex, double[]>>>();
                
                for (int k = 0; k <= q; k++) {
                    for (int l = 0; l <= k; l++) {

                        final int k_f = k;
                        final int l_f = l;
                        
                        // Process the spherical harmonics multi-threaded
                        Callable<Quartet<Integer, Integer, Complex, double[]>> callable = new Callable<Quartet<Integer, Integer, Complex, double[]>>() {
                            public Quartet<Integer, Integer, Complex, double[]> call() throws Exception {
                                
                                boolean uniformWeight = false;
                                
                                Complex S_lm = null;
                                
                                // Calculate the weights
                                double[] weights = new double[stationId.length];
                                if (uniformWeight) {
                                    for (int i = 0; i < weights.length; i++) {
                                        weights[i] = 1.0 / (weights.length + 0.0);
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

                                boolean normalize = false;
                                if (normalize) {
                                    S_n = DoubleArray.Multiply(S_n, DoubleArray.Power(stationSd, -1.0));
                                }
                                Complex[] S_n_Y_conj = ComplexArray.Multiply(S_n, stationHarmonics.Conjugate().GetHarmonic(k_f, l_f));
                                Complex[] S_n_Y_conj_w = ComplexArray.Multiply(S_n_Y_conj, weights);
                                
                                S_lm = ComplexArray.Sum(S_n_Y_conj_w);

                                Quartet<Integer, Integer, Complex, double[]> results = Quartet.with(k_f, l_f, S_lm, weights);
                                System.out.println("Completed: k=" + k_f + ", l=" + l_f + ", harmonic=" + S_lm);
                                return results;
                            }};
                        futures.add(service.submit(callable));
                    }
                }
                
                service.shutdown();
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(q);
                for (Future<Quartet<Integer, Integer, Complex, double[]>> future : futures) {
                    int k = future.get().getValue0();
                    int l = future.get().getValue1();
                    Complex S_lm = future.get().getValue2();
                    double[] weights = future.get().getValue3();
                    finalStationHarmonic.SetHarmonic(k, l, S_lm);
                }
                
                // Now that we have the final harmonic. Convert to spatial
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                            
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                double value = DoubleArray.SumArray(
                        DoubleArray.Multiply(
                                DoubleArray.Multiply(stationHarmonics_spatial, areaFraction/*areasForGrid.GetAreas()*/),
                                gridBoxAnomSd));
                
                long endTime   = System.currentTimeMillis();
                double totalTime = (endTime - startTime)/1000.0;
                double harmonic = finalStationHarmonic.GetHarmonic(0, 0).getReal();
                

                int currYear = (currDate.getYear()+1900);
                finalResults.Set(currYear, month, value, stations.size(), finalStationHarmonic);
                
                System.out.println(currYear + "\t" + value + "\t" + stations.size() + "\t" + totalTime );
            }
        }
        finalResults.Print();
        finalResults.GetGriddedData(gridBox);   
    }
    
    public static void main(String args[]) throws Exception {   
        ClimateFromStations_InputParser in = new ClimateFromStations_InputParser(args, ClimateFromStations_old.class.getName());
        if (in.InputsCorrect()) {
            ClimateFromStations_old c = new ClimateFromStations_old(
                    in.dataEof, in.dataStations, in.varExplained);
        }
    }
}

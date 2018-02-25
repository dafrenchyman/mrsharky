/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

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
import org.javatuples.Triplet;

/**
 *
 * @author Julien Pierret
 */
public class ClimateFromStations_Global {
    
    private boolean _verbose = false;
    
    public ClimateFromStations_Global(String pcaDataLocation, String stationDataLocation, double varExpCutoff, int q, boolean normalized, String output) throws Exception {
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
            
            int threads = Runtime.getRuntime().availableProcessors();
            //threads = 1;
            ExecutorService service = Executors.newFixedThreadPool(threads);
            List<Future<Triplet<Date, Double, double[]>>> futures = new ArrayList<Future<Triplet<Date, Double, double[]>>>();
            
            for (Date currDate : monthDates) {
                
                final Date currDate_f = currDate;
                final int k_f = 0;
                final int l_f = 0;

                // Process the spherical harmonics multi-threaded
                Callable<Triplet<Date, Double, double[]>> callable = new Callable<Triplet<Date, Double, double[]>>() {
                    public Triplet<Date, Double, double[]> call() throws Exception {

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

                        SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(q_f);
                        finalStationHarmonic.SetHarmonic(k_f, l_f, S_lm);

                        if (_verbose) {
                            System.out.println(currDate + ", k: " + k_f  + ", l:" + l_f + ", harmonic: " + S_lm +", stationCount: " + lats.length );
                            //System.out.println("Weights");
                            //DoubleArray.Print(weights);
                        }

                        // Now that we have the final harmonic. Convert to spatial
                        InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                        double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                        double value = DoubleArray.SumArray(
                                DoubleArray.Multiply(
                                        DoubleArray.Multiply(stationHarmonics_spatial, areaFraction/*areasForGrid.GetAreas()*/),
                                        gridBoxAnomSd));

                        int currYear = currDate_f.getYear() + 1900;
                        System.out.println(currYear + "\t" + value + "\t" + (weights.length-1));

                        Triplet<Date, Double, double[]> results = Triplet.with(currDate_f, value, weights);
                        return results;
                    }
                };
                futures.add(service.submit(callable));

            }

            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            
            for (Future<Triplet<Date, Double, double[]>> future : futures) {
                Date date = future.get().getValue0();
                double value = future.get().getValue1();
                double[] weights = future.get().getValue2();
                int currYear = (date.getYear()+1900);
                finalResults.Set(currYear, month, value, weights.length-1, null);
            }
        }
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(output);
    }
    
    public static void main(String args[]) throws Exception {   
        ClimateFromStations_InputParser in = new ClimateFromStations_InputParser(args, ClimateFromStations_Global.class.getName());
        if (in.InputsCorrect()) {
            ClimateFromStations_Global c = new ClimateFromStations_Global(
                    in.dataEof, in.dataStations, in.varExplained, in.q, in.normalized, in.output);
        }
    }
}

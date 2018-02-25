/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.nearestNeighbor;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.climate.sphericalHarmonic.common.NearestStationKey;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.HaversineDistance;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.javatuples.Triplet;


/**
 *
 * @author Julien Pierret
 */
public class NearestNeighbor {
    
    public NearestNeighbor(String pcaDataLocation, String stationDataLocation, String output) throws Exception {
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_PcaResults pcaData = (SphericalHarmonics_PcaResults) LoadSerializedObject(pcaDataLocation);
        
        // Create Array that will hold all the results
        
        TimeseriesResults finalResults = new TimeseriesResults();
        //int q = pcaData.GetQ();
        int q = 0;
        
        // Calculate the gridbox area
        double[][] gridBox = pcaData.GetGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        double[] latitudes = areasForGrid.GetLatitude();
        double[] longitudes = areasForGrid.GetLongitude();
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        for (int month = 0; month <= 12; month++) {
            System.out.println("Processing month: " + month);
            final int currMonth;
            final int month_f = month;
            if (month == 0 ) {
                currMonth = 0;
            } else {
                currMonth = month-1;
            } 
     
            NearestStationKey key = new NearestStationKey();
            
            List<Date> monthDates = stationData.GetDates(month).stream()
                    .filter(d -> d.getMonth() == currMonth)
                    //.filter(d -> d.getYear()+1900 > 1880)
                    .sorted()
                    .collect(Collectors.toList());
                   
            int threads = Runtime.getRuntime().availableProcessors();
             
            for (Date currDate : monthDates) {
                
                int currYear = currDate.getYear() + 1900;
                List<StationResults> stations = stationData.GetDate(month_f, currDate);
                final List<StationResults> stations_f = stations.stream().sorted((a,b) -> Long.compare(b.StationId, a.StationId)).collect(Collectors.toList());
                
                List<Long> stationList = new ArrayList<Long>();
                for (int i = 0; i < stations.size(); i++) {
                    StationResults currStation = stations.get(i);
                    stationList.add(currStation.StationId);
                }
                
                double[][] gridBoxValues = new double[latitudes.length][longitudes.length];
                
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Triplet<Integer, Integer, Double>>> futures = new ArrayList<Future<Triplet<Integer, Integer, Double>>>();
                
                for (int lat = 0; lat < latitudes.length; lat++) {
                    for (int lon = 0; lon < longitudes.length; lon++) {
                        final int lat_f = lat;
                        final int lon_f = lon;
                        double lat1 = latitudes[lat];
                        double lon1 = longitudes[lon];
                        
                        // Process the spherical harmonics multi-threaded
                        Callable<Triplet<Integer, Integer, Double>> callable = new Callable<Triplet<Integer, Integer, Double>>() {
                            public Triplet<Integer, Integer, Double> call() throws Exception {
                                double bestValue = 0;
                                if (!key.ContainsKey(lat_f, lon_f, stationList)) {
                                    double bestDistance = 100000000.0;
                                    int bestStation = -1;
                                    for (int i = 0; i < stations_f.size(); i++) {
                                        StationResults currStation = stations_f.get(i);
                                        double lat2 = currStation.Lat;
                                        double lon2 = currStation.Lon;
                                        double currDistance = HaversineDistance(lat1, lat2, lon1, lon2, 0.0, 0.0)/1000.0;
                                        if (currDistance < bestDistance) {
                                            bestDistance = currDistance;
                                            bestValue = currStation.Value - currStation.BaselineMean;
                                            bestStation = i;
                                        }
                                    }
                                    key.Set(lat_f, lon_f, stationList, bestStation);
                                } else {
                                    int i = key.Get(lat_f, lon_f, stationList);
                                    StationResults currStation = stations_f.get(i);
                                    bestValue = currStation.Value - currStation.BaselineMean;
                                }
                                Triplet<Integer, Integer, Double> results = Triplet.with(lat_f, lon_f, bestValue);
                                return results;
                            }
                        };
                        futures.add(service.submit(callable));
                    }
                }
                
                service.shutdown();
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                for (Future<Triplet<Integer, Integer, Double>> future : futures) {
                    int lat = future.get().getValue0();
                    int lon = future.get().getValue1();
                    double value = future.get().getValue2();
                    gridBoxValues[lat][lon] = value;
                }

                double value = DoubleArray.SumArray( DoubleArray.Multiply(gridBoxValues, areaFraction) );

                System.out.println(currYear + "\t" + value + "\t" + stations.size());
                finalResults.Set(currYear, month, value, stations.size(), null);
            }
        }
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(pcaDataLocation);
    }
    
    public static void main(String args[]) throws Exception {   
        NearestNeighbor_InputParser in = new NearestNeighbor_InputParser(args, NearestNeighbor.class.getName());
        if (in.InputsCorrect()) {
            NearestNeighbor c = new NearestNeighbor(
                    in.dataEof, in.dataStations, in.output);
        }
    }
}

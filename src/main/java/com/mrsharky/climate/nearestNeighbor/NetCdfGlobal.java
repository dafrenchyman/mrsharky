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
import com.mrsharky.dataprocessor.NetCdfAnomaly;
import com.mrsharky.dataprocessor.NetCdfAnomaly_NoDiff;
import com.mrsharky.dataprocessor.NetCdfLoader;
import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.HaversineDistance;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
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
import org.javatuples.Triplet;


/**
 *
 * @author Julien Pierret
 */
public class NetCdfGlobal {
    
    public NetCdfGlobal(String input, String variable, String time, Date baselineLower, Date baselineUpper, String output) throws Exception {
         
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        
        Map<java.util.Date, double[][]> allData = loader.LoadData();
        double[] lats = loader.GetLats();
        double[] lons = loader.GetLons();
        int numGridPoints = lats.length * lons.length;
        
        NetCdfAnomaly_NoDiff anomalies = new NetCdfAnomaly_NoDiff(allData, baselineLower, baselineUpper);
        
        Map<Date, double[][]> yearlyData = anomalies.GetYearlyData();
        Map<Date, double[][]> monthlyData = anomalies.GetMonthlyData();
        
        TimeseriesResults finalResults = new TimeseriesResults();
        
        // Calculate the gridbox area
        AreasForGrid areasForGrid = new AreasForGrid(lats, lons, 1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        for (int month = 0; month <= 12; month++) {
            System.out.println("------------------------------------------------------------------------");
            System.out.println("Converting from Spatial to Spectral for month: " + month);
            System.out.println("------------------------------------------------------------------------");
            final int currMonth = month;
            
            List<Date> dates = null;
            if (month == 0) {
                dates = yearlyData.keySet().stream().sorted()
                    .collect(Collectors.toList());
            } else {
                dates = monthlyData.keySet().stream()
                    .filter(date -> date.getMonth() == currMonth-1).sorted()
                    .collect(Collectors.toList());
            }
             
            for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                int finalDateCounter = dateCounter;
                Date currDate = dates.get(finalDateCounter);
                int currYear = currDate.getYear() + 1900;

                System.out.println("Processing date: " + currDate);
                double[][] gridBoxValues = null;
                if (currMonth == 0) {
                    gridBoxValues = yearlyData.get(currDate);
                } else {
                    gridBoxValues = monthlyData.get(currDate);
                }

                double value = DoubleArray.SumArray( DoubleArray.Multiply(gridBoxValues, areaFraction) );

                System.out.println(currYear + "\t" + value + "\t" + numGridPoints);
                finalResults.Set(currYear, month, value, numGridPoints, null);
            }
        }
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(output);
    }
    
    public static void main(String args[]) throws Exception {   
        NetCdfGlobalAverage_InputParser in = new NetCdfGlobalAverage_InputParser(args, NetCdfGlobal.class.getName());
        if (in.InputsCorrect()) {
            NetCdfGlobal c = new NetCdfGlobal(in.input, in.variable, in.time, in.baselineLower, in.baselineUpper, in.output);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations.netcdf;

import com.mrsharky.dataprocessor.NetCdfLoader;
import static com.mrsharky.helpers.Utilities.SerializeObject;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class GridBoxVariance {
    
    private Map<Pair<Integer, Integer>, double[][]> _gridBoxAnomalyVariance;
    
    public Map<Pair<Integer, Integer>, double[][]> GetGridBoxAnomVar() {
        return _gridBoxAnomalyVariance;
    }
    
    public GridBoxVariance(Map<Date, double[][]> allData, int numLats, int numLons, int varianceNumYears) {
        Process(allData, numLats, numLons, varianceNumYears);
    }
    
    public GridBoxVariance(String input, String variable, String time, int varianceNumYears) throws Exception {
        
        // Load the NetCDF data
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        Map<Date, double[][]> allData = loader.LoadData();
        
        int numLats = loader.GetLats().length;
        int numLons = loader.GetLons().length; 
        Process(allData, numLats, numLons, varianceNumYears);
    }
        
    private void Process(Map<Date, double[][]> allData, int numLats, int numLons, int varianceNumYears) {
        
        int yearsToAdd = varianceNumYears / 2;
        Date minDate = allData.keySet().stream().min((a, b) -> a.compareTo(b)).get();
        Date maxDate = allData.keySet().stream().max((a, b) -> a.compareTo(b)).get();

        Map<Pair<Integer, Integer>, double[][]> gridBoxAnomalyVariance = new HashMap<Pair<Integer, Integer>, double[][]>();
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Calculating Rolling Gridbox Variances");
        System.out.println("------------------------------------------------------------------------");
        for (int month = 0; month <= 12; month++) {
            Date currDate = (Date) minDate.clone();
            while (currDate.getYear() <= maxDate.getYear()) {
                Calendar calendar = Calendar.getInstance();
                System.out.println("Processing Anomalies for month: " + month + ", date: " + currDate);
                
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
                double[][] gridBoxAnomalyVariance3 = new double[numLats][numLons];
                final int currMonth = month;

                List<Date> baselinedates = null;
                if (month == 0) { // All months
                    baselinedates = allData.keySet().stream()
                            .filter(date -> date.after(lowerCutOff_f) && date.before(upperCutOff_f)).sorted()
                            .collect(Collectors.toList());
                } else {
                    baselinedates = allData.keySet().stream()
                            .filter(date -> date.after(lowerCutOff_f) && date.before(upperCutOff_f) && date.getMonth() == currMonth-1).sorted()
                            .collect(Collectors.toList());
                }
                
                System.out.println("Number of Baseline Dates: " + baselinedates.size());
                for (int latCounter = 0; latCounter < numLats; latCounter++) {
                    for (int lonCounter = 0; lonCounter < numLons; lonCounter++) {
                        double[] baselineValues = new double[baselinedates.size()];

                        for (int dateCounter = 0; dateCounter < baselinedates.size(); dateCounter++) {
                            Date baseDate = baselinedates.get(dateCounter);
                            baselineValues[dateCounter] = allData.get(baseDate)[latCounter][lonCounter];
                        }

                        DescriptiveStatistics ds = new DescriptiveStatistics(baselineValues);
                        gridBoxAnomalyVariance3[latCounter][lonCounter] = ds.getVariance();

                    }
                }
                gridBoxAnomalyVariance.put(Pair.with(month, currDate.getYear() + 1900), gridBoxAnomalyVariance3);
                  
                // Add year to date
                calendar.setTime(currDate);
                calendar.add(Calendar.YEAR, 1);
                currDate = calendar.getTime();
            }
        }
        _gridBoxAnomalyVariance = gridBoxAnomalyVariance;  
    }
    
    public static void main(String args[]) throws Exception {   
        GridBoxVariance_InputParser in = new GridBoxVariance_InputParser(args, GridBoxVariance.class.getName());
        if (in.InputsCorrect()) {
            GridBoxVariance s = new GridBoxVariance(
                    in.input, in.variable, in.time
                    , in.varianceNumYears );
            SerializeObject(s.GetGridBoxAnomVar(), in.output);
        }
    }
}

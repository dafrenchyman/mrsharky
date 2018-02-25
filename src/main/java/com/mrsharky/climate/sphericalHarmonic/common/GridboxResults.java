/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.common;

import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class GridboxResults {
    private Map<Integer, Map<Integer, double[][]>> _data;
    private double[][] _lats;
    private double[][] _lons;
    
    public GridboxResults() {
        _data = new HashMap<Integer, Map<Integer, double[][]>>();
    }
    
    public void Set(int year, int month, double[][] value) {
        if (!_data.containsKey(year)) {
            _data.put(year, new HashMap<Integer, double[][]>());
        }
        _data.get(year).put(month, value);
    }

    public double[][] Get(int year, int month) {
        return _data.get(year).get(month);
    }
    
    private String GetResults() {
        List<Integer> years = _data.keySet().stream().sorted().collect(Collectors.toList());
        List<Integer> months = new ArrayList<Integer>();
        StringBuilder sb = new StringBuilder();

        for (int year : years) {
            months.addAll(_data.get(year).keySet());
        }
        months = months.stream().distinct().sorted().collect(Collectors.toList());
        
        {
            sb.append("Temperature Anomaly\n");
            sb.append("Year,");
            for (int m = 0; m < months.size(); m++) {
                int currMonth = months.get(m);
                sb.append(currMonth + ",");
            }
            sb.append("\n");
            for (int y = 0; y < years.size(); y++) {
                int currYear = years.get(y);
                sb.append(currYear + ",");
                for (int m = 0; m < months.size(); m++) {
                    int currMonth = months.get(m);
                    if (_data.get(currYear).containsKey(currMonth)) {
                        //Pair values = _data.get(currYear).get(currMonth);
                        //double currValue = (double) values.getValue0();
                        //sb.append(currValue);
                    }
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        
        {
            sb.append("Station Count\n");
            sb.append("Year,");
            for (int m = 0; m < months.size(); m++) {
                int currMonth = months.get(m);
                sb.append(currMonth + ",");
            }
            sb.append("\n");
            for (int y = 0; y < years.size(); y++) {
                int currYear = years.get(y);
                sb.append(currYear + ",");
                for (int m = 0; m < months.size(); m++) {
                    int currMonth = months.get(m);
                    if (_data.get(currYear).containsKey(currMonth)) {
                        //Pair values = _data.get(currYear).get(currMonth);
                        //int currValue = (int) values.getValue1();
                        //sb.append(currValue);
                    }
                    sb.append(",");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    public void Print() {
        String results = GetResults();
        System.out.println(results);
    }
    
    
    public void SaveToCsv(String fileLocation) {
        String results = GetResults();
        
        System.out.println("Saving results to: " + fileLocation);
            
        File file = new File(fileLocation);
        File folder = file.getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        try {
            FileUtils.writeStringToFile(new File(fileLocation), results);
        } catch (IOException ex) {
            Logger.getLogger(StationSelectionResults.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    
}

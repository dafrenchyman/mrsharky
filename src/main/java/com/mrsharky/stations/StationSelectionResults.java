/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author mrsharky
 */
public class StationSelectionResults implements Serializable {
       
    private Map<Integer, Map<Date, List<StationResults>>> _results;
    
    public StationSelectionResults () {
        _results = new HashMap<Integer, Map<Date, List<StationResults>>>();
        for (int month = 0; month <= 12; month++) {
            _results.put(month, new HashMap<Date, List<StationResults>>());
        }
    }
    
    public void AddDate(int month, Date date, List<StationResults> results) {
        _results.get(month).put(date, results);
    }
    
    public List<StationResults> GetDate(int month, Date date) {
        return new ArrayList<StationResults>(_results.get(month).get(date));
    }
    
    public List<Integer> GetMonths() {
        List<Integer> returnList = new ArrayList<Integer>();
        returnList.addAll(_results.keySet());
        returnList = returnList.stream().sorted().collect(Collectors.toList());
        return returnList;
    }
    
    public List<Date> GetDates(int month) {
        List<Date> dates = this._results.get(month).keySet().stream().sorted().collect(Collectors.toList());
        return dates;
    }
    
    public void SaveToCsv(String fileLocation) {
        
        StringBuilder sb = new StringBuilder();
        
        List<Integer> months = GetMonths();
        String header = "Month,Year,StationId,Lat,Lon,Value,BaselineMean,BaselineVariance,HistoricalCount\n";
        sb.append(header);
        for (int m : months) {
            List<Date> dates = GetDates(m);
            for (Date d : dates) {
                int year = d.getYear() + 1900;
                List<StationResults> stations = _results.get(m).get(d);
                for (StationResults station : stations) {
                    String currLine = m + "," + year + "," + 
                            station.StationId + "," + station.Lat + "," + station.Lon + "," +
                            station.Value + "," + station.BaselineMean + "," + station.BaselineVariance + "," +
                            station.Cnt + "\n";
                    sb.append(currLine);
                }
            }
        }
        
        try {
            FileUtils.writeStringToFile(new File(fileLocation), sb.toString());
        } catch (IOException ex) {
            Logger.getLogger(StationSelectionResults.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}

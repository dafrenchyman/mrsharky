/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author mrsharky
 */
public class NetCdfDateEntry {
    
    public final String[] date;
    public final List<Date> ddate;
    public final double[] lat;
    public final double[] lon;
    
    public final Map<Double, Integer> lonKey;
    public final Map<Double, Integer> latKey;
    
    // date, lat, lon
    public Map<Date, double[][]> valuesMap;
    public double[][][] values;
    
    public double[][] GetDateData(int date) {
        double[][] result = new double[lat.length][lon.length];
        for (int latCounter = 0; latCounter < this.lat.length; latCounter++) {
            for (int lonCounter = 0; lonCounter < this.lon.length; lonCounter++) {
                result[latCounter][lonCounter] = this.values[date][latCounter][lonCounter];
            }
        }
        return result;
    }
    
    public double[][] GetDateData(Date date) {
        return this.valuesMap.get(date);
    }
    
    public double[][] GetLatMesh(){
        MeshGrid meshGrid = new MeshGrid(this.lon, this.lat);
        return meshGrid.GetY();
    }
    
    public double[][] GetLonMesh(){
        MeshGrid meshGrid = new MeshGrid(this.lon, this.lat);
        return meshGrid.GetX();
    }
    
    public NetCdfDateEntry(List<NetCdfEntry> allData) throws ParseException {
        // Dates
        {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            System.out.println("Getting all the unique dates");
            List<String> dateList = allData.parallelStream()
                    .map(mapper -> mapper.date)
                    .distinct()
                    .sorted().collect(Collectors.toList());
            String[] tempDate = new String[dateList.size()];
            //this.date = new String[dateList.size()];
            tempDate = dateList.toArray(tempDate);
            this.date = tempDate;
            this.ddate = new ArrayList<Date>();
            for (String currDate: this.date) {
                this.ddate.add(format.parse(currDate));        
            }
        }
        
        // Lat
        {
            System.out.println("Getting all the unique Lat");
            Comparator<Double> latComparator = Comparator.comparing(lat -> -lat);
            List<Double> latList = allData.parallelStream()
                    .map(mapper -> mapper.lat)
                    .distinct()
                    .sorted(latComparator).collect(Collectors.toList());
            Double[] tempLat = new Double[latList.size()];
            tempLat = latList.toArray(tempLat);
            this.lat = ArrayUtils.toPrimitive(tempLat);
        }
        
        // Lon
        {
            System.out.println("Getting all the unique Lon");
            List<Double> lonList = allData.parallelStream()
                    .map(mapper -> mapper.lon)
                    .distinct()
                    .sorted().collect(Collectors.toList());
            Double[] tempLon = new Double[lonList.size()];
            tempLon = lonList.toArray(tempLon);
            this.lon = ArrayUtils.toPrimitive(tempLon);
        }
        
        
        this.values = new double[this.date.length][this.lat.length][this.lon.length];
        this.valuesMap = new HashMap<Date, double[][]> ();
        for (int dateCounter = 0; dateCounter < this.date.length; dateCounter++) {
            String currDate = this.date[dateCounter];
            Date currDdate = this.ddate.get(dateCounter);
            System.out.println("Processing Date: " + currDdate);
            List<NetCdfEntry> currDateData = allData.parallelStream().filter(data -> data.date == currDate).collect(Collectors.toList());
            double[][] dateValues = new double[this.lat.length][this.lon.length];
            for (int lonCounter = 0; lonCounter < this.lon.length; lonCounter++) {
                double currLon = this.lon[lonCounter];
                List<NetCdfEntry> currDateLonData = currDateData.parallelStream().filter(data -> data.lon == currLon).collect(Collectors.toList());
                for (int latCounter = 0; latCounter < this.lat.length; latCounter++) {
                    double currLat = this.lat[latCounter];
                    NetCdfEntry currLatData = currDateLonData.stream().filter(data -> data.lat == currLat).findFirst().get();
                    double currValue = (double) currLatData.value;
                    this.values[dateCounter][latCounter][lonCounter] = currValue;
                    dateValues[latCounter][lonCounter] = currValue;
                }
            }
            this.valuesMap.put(currDdate, dateValues);
        }
        
        // Create a lookup, double to lon location in list
        this.lonKey = new HashMap<Double, Integer>();
        for (int location = 0; location < this.lon.length; location++) {
            double value = this.lon[location];
            this.lonKey.put(value, location);
        }
        this.latKey = new HashMap<Double, Integer>();
        for (int location = 0; location < this.lat.length; location++) {
            double value = this.lat[location];
            this.latKey.put(value, location);
        }

        
    }
}

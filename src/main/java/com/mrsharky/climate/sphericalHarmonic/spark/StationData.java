/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.helpers.Utilities;
import com.mrsharky.stations.StationResults;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 
 * @author Julien Pierret
 */
public class StationData implements Serializable {

    public double[] lats;
    public double[] lons;
    public double[] stationMean;
    public double[] stationSd;
    public double[] stationValue;
    public long[] stationId;
    public List<Long> stationList;

    public StationData(List<StationResults> stations) throws Exception {
        stations = stations.stream().sorted((a,b) -> Long.compare(b.StationId, a.StationId)).collect(Collectors.toList());

        lats = new double[stations.size()];
        lons = new double[stations.size()];
        stationMean = new double[stations.size()];
        stationSd = new double [stations.size()];
        stationValue = new double [stations.size()];
        stationId = new long[stations.size()];
        stationList = new ArrayList<Long>();
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
    }
}

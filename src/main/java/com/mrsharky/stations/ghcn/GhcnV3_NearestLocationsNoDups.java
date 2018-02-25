/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.ghcn;


import static com.mrsharky.helpers.Utilities.HaversineDistance;
import com.mrsharky.stations.HolePunchSelection;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.ghcn.GhcnV3_Helpers.QcType;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.HashMap;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;
import org.javatuples.Triplet;

/**
 * 
 * @author jpierret
 */
public class GhcnV3_NearestLocationsNoDups extends HolePunchSelection {
    
    private Dataset<Row> LoadMonthlyData(SparkSession spark, String source, QcType qcType) throws Exception {
        JavaRDD<String> javaRddMonthlyRaw = spark.read().textFile(source).javaRDD();
        GhcnV3_MonthlyDataLoader ghcnMonthlyLoader = new GhcnV3_MonthlyDataLoader(qcType);
        JavaRDD<Row> javaRddData = javaRddMonthlyRaw.mapPartitions(s -> ghcnMonthlyLoader.call(s));
        Dataset<Row> finalData = spark.createDataFrame(javaRddData, ghcnMonthlyLoader.GetSchema());
        return finalData;
    }
    
    private Dataset<Row> LoadInventoryData(SparkSession spark, String source) throws Exception {
        JavaRDD<String> javaRddMonthlyRaw = spark.read().textFile(source).javaRDD();
        GhcnV3_InventoryDataLoader ghcnInventoryLoader = new GhcnV3_InventoryDataLoader();
        JavaRDD<Row> javaRddData = javaRddMonthlyRaw.mapPartitions(s -> ghcnInventoryLoader.call(s));
        Dataset<Row> finalData = spark.createDataFrame(javaRddData, ghcnInventoryLoader.GetSchema());
        return finalData;
    }
    
    private final double[] _lats;
    private final double[] _lons;
    
    public GhcnV3_NearestLocationsNoDups(String monthlyData, String inventoryData, QcType qcType, double[] lats, double[] lons
            , double minDistance, String lowerBaseline, String upperBaseline, int minMonthYears, String destination, boolean createSpark) throws Exception {
        
        super(lowerBaseline, upperBaseline, minDistance, createSpark);
        _lats = lats;
        _lons = lons;
 
        // Load the Monthly Data
        Dataset<Row> MonthlyDataset = LoadMonthlyData(_spark, monthlyData, qcType);
        MonthlyDataset.persist(StorageLevel.MEMORY_ONLY());
        MonthlyDataset.createOrReplaceTempView("MonthlyDataset");
        
        // Load the Inventory Data
        Dataset<Row> InventoryDataset = LoadInventoryData(_spark, inventoryData);
        InventoryDataset.persist(StorageLevel.MEMORY_ONLY());
        InventoryDataset.createOrReplaceTempView("InventoryDataset");
        
        Process(InventoryDataset, MonthlyDataset, minMonthYears, destination, false); 
        Close();
    }
    
    @Override
    protected List<StationResults> GetStations(List<Row> StationData) {
        List<StationResults> finalStations = new ArrayList<StationResults>();
        List<Long> FinalStationIds = new ArrayList<Long>();

        if (StationData.size() > 0) {
            // Figure out which of the available stations are closests to our "good" points 
            for (int i = 0; i < _lats.length; i++) {
                // Find nearest stationId
                double lat1 = _lats[i];
                double lon1 = _lons[i]; 
                double bestDistance = 100000000.0;
                long bestStationId = 0;
                for (int j = 0; j < StationData.size(); j++) {
                    Row currStation = StationData.get(j);
                    long stationId = currStation.getLong(0);
                    double lat2 = currStation.getDouble(2);
                    double lon2 = currStation.getDouble(3);
                    double currDistance = HaversineDistance(lat1, lat2, lon1, lon2, 0.0, 0.0)/1000.0;
                    if (currDistance < bestDistance) {
                        bestDistance = currDistance;
                        bestStationId = stationId;
                    }
                }
                FinalStationIds.add(bestStationId);
            }

            // Create a lookup table so we can fish out the stations easily
            Map<Long, StationResults> stationResultsLookup = new HashMap<Long, StationResults>();
            for (int j = 0; j < StationData.size(); j++) {
                // Get the first station in the list
                Row currRow = StationData.get(j);

                long stationId = currRow.getLong(0);
                //Date currDate = currRow.getDate(1);
                double lat1 = currRow.getDouble(2);
                double lon1 = currRow.getDouble(3);
                long cnt = currRow.getLong(4);
                double value = currRow.getDouble(5);
                double mean = currRow.getDouble(6);
                double var = currRow.getDouble(7);
                
                if (FinalStationIds.contains(stationId)) {
                    StationResults currStation = new StationResults();
                    currStation.Cnt = cnt;
                    currStation.Lat = lat1;
                    currStation.Lon = lon1;
                    currStation.BaselineMean = mean;
                    currStation.StationId = stationId;
                    currStation.Value = value;
                    currStation.BaselineVariance = var;
                    finalStations.add(currStation);
                }
            }
        }
        if (_verbose) {
            System.out.println("ID\tLat\tLon\tValue\tHistorical Count");
            for (int i = 0; i < finalStations.size(); i++) {
                StationResults currStat = finalStations.get(i);
                System.out.println(currStat.StationId + "\t" + currStat.Lat + "\t" + currStat.Lon + "\t" + currStat.Value + "\t" + currStat.Cnt);
            }
        }
        return (finalStations);
    }
    
    public static void main(String[] args) throws Exception {
        
        double[] lons = {-8.4,
            -62.2,
            -119.2,
            -156.5,
            162.2,
            133.2,
            80.1,
            26.4,
            -10.2,
            -58.3,
            -80.4,
            -111.2,
            -131.3,
            -170.1,
            141.4,
            108.1,
            73.1,
            55.1,
            30.3,
            11.4,
            -17.3,
            -66,
            -97.3,
            -117.1,
            -155,
            166.4,
            144.5,
            114.1,
            88.2,
            72.5,
            2.1,
            32.3,
            -3.6,
            -74.1,
            -139,
            171.2,
            160,
            104,
            77,
            37,
            -52.2,
            -43.1,
            -70.3,
            -109.3,
            -194.4,
            177.3,
            146.5,
            118.4,
            96.5,
            47.3,
            28.4,
            -9.5,
            -73.1,
            -176.3,
            138.3,
            77.3,
            37.5,
            -2.2,
            -26.4,
            166.4,
            110.4,
            62.5,
            0
        };
        
        double[] lats = {
            70.6,
            82.3,
            76.1,
            71.2,
            70.4,
            67.3,
            73.3,
            67.2,
            51.6,
            48.3,
            51.2,
            47.3,
            55,
            57.1,
            45.2,
            57.5,
            54.6,
            51.5,
            50.2,
            48.1,
            14.4,
            18.3,
            25.5,
            32.4,
            19.4,
            19.2,
            13.3,
            22.2,
            22.3,
            19.1,
            13.3,
            15.4,
            5.2,
            4.4,
            -9.5,
            7,
            -9.2,
            1.2,
            8.3,
            -1.2,
            4.6,
            -22.5,
            -23.3,
            -27.1,
            -17.3,
            -17.4,
            -19.2,
            -20.2,
            -12.1,
            -18.5,
            -20.1,
            -40.2,
            -41.3,
            -43.6,
            -34.6,
            -37.5,
            -46.5,
            -70.2,
            -75.3,
            -77.5,
            -66.2,
            -67.4,
            -90
        };
        
        GhcnV3_InputParser parser = new GhcnV3_InputParser(args, GhcnV3_NearestLocationsNoDups.class.getName());
        if (parser.InputsCorrect()) {
            GhcnV3_NearestLocationsNoDups ghcn = new GhcnV3_NearestLocationsNoDups(parser.monthlyData, parser.inventoryData, parser.qcType, lats, lons, parser.minDistance, 
                    parser.lowerBaseline, parser.upperBaseline, parser.minMonthYears, parser.destination, parser.createSpark);
        }
    }
}

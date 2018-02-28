/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.netcdf;


import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.stations.HolePunchSelection;
import com.mrsharky.dataprocessor.NetCdfLoader;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.storage.StorageLevel;

/**
 * 
 * @author jpierret
 */
public class NetCdf_NearestLocations extends HolePunchSelection {
    
    private final boolean _verbose = false;
    private final boolean _streamYearly = true;
    private final boolean _streamMonthly = true;
    
    public NetCdf_NearestLocations(String input, String variable, String time, String lowerBaseline, String upperBaseline
            , int latCount, int lonCount, String destination, boolean createSpark , int q) throws Exception {   
        super(lowerBaseline, upperBaseline, 0.0, createSpark);
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        
        Map<java.util.Date, double[][]> allData = loader.LoadData();
        
        List<java.util.Date> dates = new ArrayList<java.util.Date>(allData.keySet());        
        JavaSparkContext jsc = new JavaSparkContext(_spark.sparkContext());
        JavaRDD<java.util.Date> rddDates = jsc.parallelize(dates, 12);
        
        // Get the locations to match to
        double[] lats = null;
        double[] lons = null;
        if (latCount > 0 && lonCount > 0) {
            AreasForGrid areasForGrid = new AreasForGrid(latCount, lonCount, 1.0);
            double[] statLats = areasForGrid.GetLatitude();
            double[] statLons = areasForGrid.GetLongitude();
            
            lats = new double[latCount*lonCount];
            lons = new double[latCount*lonCount];
            
            int counter = 0;
            for (int i = 0; i < statLats.length; i++) {
                for (int j = 0; j < statLons.length; j++) {
                    lats[counter] = statLats[i];
                    lons[counter] = statLons[j];
                    counter++;
                }
            }
        } else {
            AngellKorshoverNetwork akn = new AngellKorshoverNetwork();
            lats = akn.GetLats();
            lons = akn.GetLons();
        }
        
        if (false) {
            DoubleArray.Print(lats);
            DoubleArray.Print(lons);
        }
        
        // Load the Inventory Data
        NetCdf_InventoryDataLoader idl = new NetCdf_InventoryDataLoader(lats, lons);
        Dataset<Row> InventoryDataset = _spark.createDataFrame(idl.GetData(), idl.GetSchema());
        InventoryDataset.persist(StorageLevel.MEMORY_ONLY());
        InventoryDataset.createOrReplaceTempView("InventoryDataset");
        System.out.println("Inventory Data Loaded: " + InventoryDataset.count());

        // Load the Monthly Data
        double[] latsRadians = Utilities.LatitudeToRadians(lats);
        double[] lonsRadians = Utilities.LongitudeToRadians(lons);
        NetCdf_MonthlyDataLoader mdl = new NetCdf_MonthlyDataLoader(allData, latsRadians, lonsRadians, idl.GetStationList(), q);
        JavaRDD<Row> javaRddData = rddDates.mapPartitions(s -> mdl.call(s));
        Dataset<Row> MonthlyDataset = _spark.createDataFrame(javaRddData, mdl.GetSchema());
        MonthlyDataset.persist(StorageLevel.DISK_ONLY());
        MonthlyDataset.createOrReplaceTempView("MonthlyDataset");
        System.out.println("Monthly Data Loaded:" + MonthlyDataset.count());
        
        Process(InventoryDataset, MonthlyDataset, 0, destination, false); 
        Close();
    }
     
    public static void main(String[] args) throws Exception {
        NetCdf_NearestLocations_InputParser parser = new NetCdf_NearestLocations_InputParser(args, NetCdf_NearestLocations.class.getName());
        
        
     
        
        if (parser.InputsCorrect()) {
            NetCdf_NearestLocations netCdf = new NetCdf_NearestLocations(parser.input, parser.variable, parser.time
                    , parser.lowerBaseline, parser.upperBaseline, parser.latCount, parser.lonCount
                    , parser.output, parser.createSpark, parser.q);
        }
    }
}

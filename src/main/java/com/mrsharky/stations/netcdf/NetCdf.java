/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.netcdf;


import com.mrsharky.stations.HolePunchSelection;
import com.mrsharky.dataprocessor.NetCdfLoader;
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
public class NetCdf extends HolePunchSelection {
    
    private final boolean _verbose = false;
    private final boolean _streamYearly = true;
    private final boolean _streamMonthly = true;
    
    public NetCdf(String input, String variable, String time, String lowerBaseline, String upperBaseline
            , int minMonthYears, double minDistance, String destination, boolean createSpark) throws Exception {   
        super(lowerBaseline, upperBaseline, minDistance, createSpark);
        NetCdfLoader loader = new NetCdfLoader(input, variable, time);
        
        Map<java.util.Date, double[][]> allData = loader.LoadData();
        double[] lats = loader.GetLats();
        double[] lons = loader.GetLons();
        
        List<java.util.Date> dates = new ArrayList<java.util.Date>(allData.keySet());        
        JavaSparkContext jsc = new JavaSparkContext(_spark.sparkContext());
        JavaRDD<java.util.Date> rddDates = jsc.parallelize(dates, 12);
        
        // Load the Inventory Data
        NetCdf_InventoryDataLoader_old idl = new NetCdf_InventoryDataLoader_old(loader);
        Dataset<Row> InventoryDataset = _spark.createDataFrame(idl.GetData(), idl.GetSchema());
        InventoryDataset.persist(StorageLevel.MEMORY_ONLY());
        InventoryDataset.createOrReplaceTempView("InventoryDataset");

        // Load the Monthly Data
        NetCdf_MonthlyDataLoader mdl = new NetCdf_MonthlyDataLoader(allData, lats, lons, 102);
        JavaRDD<Row> javaRddData = rddDates.mapPartitions(s -> mdl.call(s));
        Dataset<Row> MonthlyDataset = _spark.createDataFrame(javaRddData, mdl.GetSchema());
        MonthlyDataset.persist(StorageLevel.DISK_ONLY());
        MonthlyDataset.createOrReplaceTempView("MonthlyDataset");
        MonthlyDataset.count();
        
        Process(InventoryDataset, MonthlyDataset, minMonthYears, destination, false); 
        Close();
    }
     
    public static void main(String[] args) throws Exception {
        NetCdf_InputParser parser = new NetCdf_InputParser(args, NetCdf.class.getName());
        if (parser.InputsCorrect()) {
            NetCdf netCdf = new NetCdf(parser.input, parser.variable, parser.time
                    , parser.lowerBaseline, parser.upperBaseline, parser.minMonthYears, parser.minDistance, parser.output, parser.createSpark);
        }
    }
}

/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.ghcn;


import com.mrsharky.stations.HolePunchSelection;
import com.mrsharky.stations.ghcn.GhcnV3_Helpers.QcType;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.storage.StorageLevel;

/**
 * 
 * @author jpierret
 */
public class GhcnV3 extends HolePunchSelection {
    
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
    
    public GhcnV3(String monthlyData, String inventoryData, QcType qcType, double minDistance
            , String lowerBaseline, String upperBaseline, int minMonthYears, String destination, boolean createSpark) throws Exception {
        
        super(lowerBaseline, upperBaseline, minDistance, createSpark);
 
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
    
    
    public static void main(String[] args) throws Exception {
        GhcnV3_InputParser parser = new GhcnV3_InputParser(args, GhcnV3.class.getName());
        if (parser.InputsCorrect()) {
            GhcnV3 ghcn = new GhcnV3(parser.monthlyData, parser.inventoryData, parser.qcType, parser.minDistance, 
                    parser.lowerBaseline, parser.upperBaseline, parser.minMonthYears, parser.destination, parser.createSpark);
        }
    }
}

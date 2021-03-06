/* Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.helpers;

import java.util.List;
import java.util.Map;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.SizeEstimator;
import scala.Tuple2;

/**
 *
 * @author mrsharky
 */
public class SparkUtils {
    
    private static SparkSession.Builder CreateSparkBuilder(String className) {
        SparkSession.Builder builder = SparkSession
                    .builder()
                    .appName("Spark Application - " + className )
                    .config("spark.some.config.option", "some-value")
                    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    //.config("spark.yarn.executor.memoryOverhead", "2048")
                    //.config("spark.shuffle.memoryFraction", "0.5");
                    
                    //.config("spark.dynamicAllocation.enabled", "false")
                    //.config("spark.cores.max", "1")
                    //.config("spark.sql.tungsten.enabled", "true")
                    .config("spark.yarn.executor.memoryOverhead", "5120")
                    .config("spark.driver.maxResultSize","2048");
        return builder;
    }
    
    public static SparkSession CreateDefaultSparkSession(String className) {
        SparkSession.Builder builder = CreateSparkBuilder(className);
        return builder.getOrCreate();
    }
    
    public static SparkSession CreateDefaultSparkSession(String className, String compression) {
        SparkSession.Builder builder = CreateSparkBuilder(className);
        if (compression.toUpperCase().equals("LZO")) {
            builder.config("io.compression.codecs", "com.hadoop.compression.lzo.LzopCodec")
                   .config("io.compression.codec.lzo.class", "com.hadoop.compression.lzo.LzoCodec");
        }
        return builder.getOrCreate();
    }
    
    
    public static void PrintSparkSetting(SparkSession spark) {
        System.out.println("Configuration Settings: "); 
        String[] configs= new String[100];
        spark.conf().getAll().keySet().copyToArray(configs);
        for (String currConfig : configs) {
            if (currConfig != null) {
                String value = spark.conf().get(currConfig);
                System.out.println("Key: " + currConfig + "\tValue: " + value);
            }
        }
    }
    
    public static <T> Dataset<T> OptimalPartitioning(Dataset<T> input, long eachPartitionSize) {
        long totalMemorySize = GetTotalSize2(input);
        int numPartitions = (int) Math.ceil((totalMemorySize + 0.0)/(eachPartitionSize + 0.0));
        int oldPartitions = input.javaRDD().getNumPartitions();
        System.out.println("OptimalPartitioning - Total Number of Partitions Before Repartitioning: " + oldPartitions);
        Dataset<T> output;
        if (numPartitions == 0) {
            output = input.coalesce(1);
        } else if (numPartitions < oldPartitions) {
            output = input.coalesce(numPartitions);
        } else {
            output = input.repartition(numPartitions);
        }
        System.out.println("OptimalPartitioning - Total Number of Partitions After Repartitioning: " + output.javaRDD().getNumPartitions());
        return(output);
    } 
    
    public static <T,U> JavaPairRDD<T, Iterable<U>> OptimalPartitioning(JavaPairRDD<T, Iterable<U>> input, long eachPartitionSize) {       
        long totalMemorySize = GetTotalSize2(input);
        int numPartitions = (int) Math.ceil((totalMemorySize + 0.0)/(eachPartitionSize + 0.0));
        int oldPartitions = input.getNumPartitions();
        System.out.println("OptimalPartitioning - Total Number of Partitions Before Repartitioning: " + oldPartitions);
        JavaPairRDD<T, Iterable<U>> output;
        if (numPartitions == 0) {
            output = input.coalesce(1);
        } else if (numPartitions < oldPartitions) {
            output = input.coalesce(numPartitions);
        } else {
            output = input.repartition(numPartitions);
        }
        System.out.println("OptimalPartitioning - Total Number of Partitions After Repartitioning: " + output.getNumPartitions());
        return(output);
    }
    
    public static <T> JavaRDD<T> OptimalPartitioning(JavaRDD<T> input, long eachPartitionSize) {
        long totalMemorySize = GetTotalSize2(input);
        int numPartitions = (int) Math.ceil((totalMemorySize + 0.0)/(eachPartitionSize + 0.0));
        int oldPartitions = input.getNumPartitions();
        System.out.println("OptimalPartitioning - Total Number of Partitions Before Repartitioning: " + oldPartitions);
        JavaRDD<T> output;
        if (numPartitions == 0) {
            output = input.coalesce(1);
        } else if (numPartitions < oldPartitions) {
            output = input.coalesce(numPartitions);
        } else {
            output = input.repartition(numPartitions);
        }
        System.out.println("OptimalPartitioning - Total Number of Partitions After Repartitioning: " + output.getNumPartitions());
        return(output);
    }
    
    public static <T> JavaRDD<T> OptimalPartitioningByRow(JavaRDD<T> input, long numRowsPerPartition) {
        long totalRows = input.count();
        
        int newPartitions = (int) Math.ceil((totalRows + 0.0)/(numRowsPerPartition + 0.0));
        int oldPartitions = input.getNumPartitions();
        System.out.println("OptimalPartitioningByRow - Total Number of Partitions Before Repartitioning: " + oldPartitions);
        JavaRDD<T> output;
        if (newPartitions == 0) {
            output = input.coalesce(1);
        } else if (newPartitions < oldPartitions) {
            output = input.coalesce(newPartitions);
        } else {
            output = input.repartition(newPartitions);
        }
        System.out.println("OptimalPartitioningByRow - Total Number of Partitions After Repartitioning: " + output.getNumPartitions());
        return(output);
    }
    
    public static <T> long GetTotalSize(JavaRDD<T> rdd) {
        // This can be a parameter
        long NO_OF_SAMPLE_ROWS = 1000l;
        long totalRows = rdd.count();
        System.out.println("OptimalPartitioning - Total Rows:" + totalRows);
        long  totalSize = 0l;
        double percentage = (NO_OF_SAMPLE_ROWS + 0.0) / (totalRows + 0.0);
        if (totalRows > NO_OF_SAMPLE_ROWS) {
            JavaRDD<T> sampleRDD = rdd.sample(true, percentage, 1234);
            long sampleRDDSize = GetRDDSize(sampleRDD);
            totalSize = sampleRDDSize *(totalRows) / (NO_OF_SAMPLE_ROWS);
        } else {
        // As the RDD is smaller than sample rows count, we can just calculate the total RDD size
            totalSize = GetRDDSize(rdd);
        }
        System.out.println("OptimalPartitioning - Total Sample Size:" + totalSize);
        return(totalSize);
    }
    
    public static <T> long GetTotalSize2(Dataset<T> rdd) {
        return (GetTotalSize2(rdd.javaRDD()) );
    }
    
    public static <T> long GetTotalSize2(JavaRDD<T> rdd) {
        // This can be a parameter
        long NO_OF_SAMPLE_ROWS = 1000l;
        long totalRows = rdd.count();
        System.out.println("OptimalPartitioning - Number of Rows:" + totalRows);
        long  totalSize = 0l;
        if (totalRows > NO_OF_SAMPLE_ROWS) {
            List<T> sampleRDD = rdd.take((int) NO_OF_SAMPLE_ROWS);
            //List<T> sampleRDD = rdd.takeSample(true, (int) NO_OF_SAMPLE_ROWS, 1234);
            long sampleRDDSize = GetRDDSize(sampleRDD);
            totalSize = sampleRDDSize *(totalRows) / (NO_OF_SAMPLE_ROWS);
        } else {
        // As the RDD is smaller than sample rows count, we can just calculate the total RDD size
            totalSize = GetRDDSize(rdd);
        }
        return(totalSize);
    }
    
    public static <T,U> long GetTotalSize2(JavaPairRDD<T, Iterable<U>> rdd) {
        // This can be a parameter
        long NO_OF_SAMPLE_ROWS = 500l;
        long totalRows = rdd.count();
        System.out.println("OptimalPartitioning - Number of Keys:" + totalRows);
        long  totalSize = 0l;
        if (totalRows > NO_OF_SAMPLE_ROWS) {
            List<Tuple2<T, Iterable<U>>> sampleRDD = rdd.take((int) NO_OF_SAMPLE_ROWS);
            //List<Tuple2<T, Iterable<U>>> sampleRDD = rdd.takeSample(true, (int) NO_OF_SAMPLE_ROWS, 1234);
            long sampleRDDSize = GetRDDSize(sampleRDD);
            totalSize = sampleRDDSize *(totalRows) / (NO_OF_SAMPLE_ROWS);
        } else {
        // As the RDD is smaller than sample rows count, we can just calculate the total RDD size
            totalSize = GetRDDSize(rdd);
        }
        return(totalSize);
    }
    
    public static <T> long GetRDDSize(List<T> rows){
        long rddSize = 0l;  
        for (int i= 0; i < rows.size(); i++) {
            rddSize += SizeEstimator.estimate(rows.get(i));
            //rddSize += SizeEstimator.estimate(rows.apply(i).toSeq.map { value => value.asInstanceOf[AnyRef] })
        }
        return(rddSize);
    }

    public static <T> long GetRDDSize(JavaRDD<T> rdd){
        long rddSize = 0l;
        List<T> rows = rdd.collect();      
        for (int i= 0; i < rows.size(); i++) {
            rddSize += SizeEstimator.estimate(rows.get(i));
            //rddSize += SizeEstimator.estimate(rows.apply(i).toSeq.map { value => value.asInstanceOf[AnyRef] })
        }
        return(rddSize);
    }
    
    public static <T, U> long GetRDDSize(JavaPairRDD<T, Iterable<U>> rdd){
        long rddSize = 0l;
        List<Tuple2<T, Iterable<U>>> rows = rdd.collect();      
        for (int i= 0; i < rows.size(); i++) {
            rddSize += SizeEstimator.estimate(rows.get(i));
            //rddSize += SizeEstimator.estimate(rows.apply(i).toSeq.map { value => value.asInstanceOf[AnyRef] })
        }
        return(rddSize);
    }
    
}

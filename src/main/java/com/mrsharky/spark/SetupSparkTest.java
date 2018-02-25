/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.spark;

import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
//import static org.junit.Assert.assertTrue;

/**
 *
 * @author jpierret
 */
public class SetupSparkTest {
    private Configuration conf;
    private FileSystem fs;
    //private MiniDFSCluster cluster;
    private JavaSparkContext sparkContext;
  
    public void copyFromLocalDir(FileSystem fs, File localDir, Path toPath) throws Exception {
        File[] localDirFiles = localDir.listFiles();
        Path[] localDirPaths = Arrays.stream(localDirFiles).map(f -> new Path(f.getAbsolutePath())).toArray(size -> new Path[size]);
        fs.mkdirs(toPath);
        fs.copyFromLocalFile(false, false, localDirPaths, toPath);
    }
  
    public void writeHDFSContent(FileSystem fs, Path path, String fileName, String[] content) throws IOException {
        Path filePath = new Path(path, fileName);
        FSDataOutputStream out = fs.create(filePath);
        for (String c : content) {
            out.writeBytes(c);
        }
        out.close();
    }
   
    public String[] readHDFSContent(FileSystem fs, Path path, String fileName) throws IOException {
        Path filePath = new Path(path, fileName);
        //assertTrue("File [" + filePath.toString() + "] has to exist", fs.exists(filePath));
        FileStatus fileStatus = fs.getFileStatus(filePath);
        //assertTrue("File [" + filePath.toString() + "] cannot be a directory", !fileStatus.isDirectory());
        BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(filePath)));
        List<String> lines = Lists.newArrayList();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lines.add(line);
        }
        return lines.toArray(new String[0]);
    }
    
    public void setup(int cores) throws Exception{
        /*System.out.println("Working Directory = " + System.getProperty("user.dir"));
        
        //File testDataPath = new File(getClass().getResource("/minicluster").getFile());
        File testDataPath = new File(PathUtils.getTestDir(getClass()), "miniclusters");
        
        System.clearProperty(MiniDFSCluster.PROP_TEST_BUILD_DATA);
        conf = new HdfsConfiguration();
        File testDataCluster1 = new File(testDataPath, "cluster1");
        String c1PathStr = testDataCluster1.getAbsolutePath();
        conf.set(MiniDFSCluster.HDFS_MINIDFS_BASEDIR, c1PathStr);
        cluster = new MiniDFSCluster.Builder(conf).build();
        
        fs = FileSystem.get(conf);
        
        String link = cluster.getURI().toString().replace("hdfs", "spark");*/

        SparkConf conf = new SparkConf().setAppName("Simple Application")
                //.setMaster("spark://localhost:49874")
                //.setMaster(link)
                .setMaster("local[" + cores + "]")
                //.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                ;
        
        /*Class[] classes = new Class[]{
            Customer1_DataSummary.class, Customer1_dataLoader.class, ParseDeviceInsight.class,
            Wurfl.class, LocalUtils.class, JSONUtils.class, DataLoaderAbstract.class
        };
        conf.registerKryoClasses(classes);*/
        
        sparkContext = new JavaSparkContext(conf);
        
        //File localFile = new File("ClientTestFiles/Data/");
        //Path toPath = new Path("/data/");
        //copyFromLocalDir(fs, localFile, toPath);
        
                
    }
    
    
    public void tearDown() {
        /*if (cluster != null) {
            cluster.shutdown();
            cluster = null;
        }*/
        if (sparkContext != null) {
            sparkContext.stop();
            sparkContext = null;
        }
    }
    
}

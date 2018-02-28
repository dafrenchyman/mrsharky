/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations.netcdf;

import static com.mrsharky.helpers.Utilities.recursiveDelete;
import static com.mrsharky.helpers.Utilities.recursiveSysOut;
import com.mrsharky.spark.SetupSparkTest;
import java.io.File;
import java.util.Map;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.SparkSession;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mrsharky
 */
public class NetCdfTest {
    
    private SetupSparkTest sparkSetup;
  
    @BeforeClass
    public static void setupTests() throws Exception {
    }

    @Before
    public void setup() throws Exception {
        sparkSetup = new SetupSparkTest();
        sparkSetup.setup(2);
    }

    @After
    public void tearDown() {
        sparkSetup.tearDown();
    }

    @Test
    public void testMain() throws Exception {
        
        String[] inputs = new String[]{"air.sfc.mon.mean.nc", "air.2m.mon.mean.nc"};
        String variable = "air";
        String time = "time";
       
        double[] minDistances = new double[]{0.0};
        int[]    minMonthYears = new int[]{ 30 };
        for (double minDistance : minDistances) {
            for (int minMonthYear : minMonthYears) {
                for (String input : inputs) {
                    String destination = "Results/Points/source=All." + input + "/km=" + minDistance + "/yrs=" + minMonthYear + "";
                    String stationsPath = destination + "/finalStations_Results.serialized";
                    File stationFile = new File(stationsPath);

                    if (!stationFile.exists()) {
                        String lowerBaseline = "1960-12-31";
                        String upperBaseline = "1990-12-31";

                        // Delete folder if exists
                        File destinationFile = new File(destination); 
                        recursiveDelete(destinationFile);

                        String inputData = "Data/" + input;

                        String args = 
                                "--input \""+ inputData + "\" " +
                                "--output \""+ destination + "\" " +
                                "--variable \""+ variable + "\" " +
                                "--time \"" + time + "\" " +
                                "--lowerBaseline \"" + lowerBaseline + "\" " + 
                                "--upperBaseline \"" + upperBaseline + "\" " + 
                                "--minDistance \"" + minDistance + "\"";
                        String[] arguments = args.split(" ");

                        //NetCdf.main(arguments);
                    }
                }
            }
        }
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations.ghcn;

import static com.mrsharky.helpers.Utilities.recursiveDelete;
import com.mrsharky.spark.SetupSparkTest;
import java.io.File;
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
public class GhcnV3Test {
    
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

        String sourceDir = "/media/dropbox/PhD/Reboot/Projects/ghcn_v3_new/ghcnm.tavg.latest.qca/ghcnm.v3.3.0.20171203/";
        String input = "ghcnm.tavg.v3.3.0.20171203.qca";
        String monthlyData = sourceDir + input + ".dat";
        String inventoryData = sourceDir + input + ".inv";
        
        double[] minDistances = new double[]{0.0};
        int[]    minMonthYears = new int[]{20, 30};
        String lowerBaseline = "1960-12-31";
        String upperBaseline = "1990-12-31";
        for (double minDistance : minDistances) {
            for (int minMonthYear : minMonthYears) {
                
                String pointFilename = "dataset=" + input + 
                                    "_lowerBaseline=" + lowerBaseline + 
                                    "_upperBaseline=" + upperBaseline +
                                    "_minDistance=" + minDistance + 
                                    "_minMonthYears=" + minMonthYear;
                
                String pointDataset = "Results/NewPoints/" +
                                    pointFilename +
                                    "/finalStations_Results.serialized";
                File stationFile = new File(pointDataset);
               
                if (!stationFile.exists()) {
                    String qcType = "QCA";
                    

                    // Delete folder if exists
                    File destinationFile = new File(pointDataset); 
                    recursiveDelete(destinationFile);

                    String args = 
                            "--monthlyData \""+ monthlyData + "\" " +
                            "--inventoryData \"" + inventoryData + "\" " +
                            "--qcType \"" + qcType + "\" " +
                            "--minDistance \"" + minDistance + "\" " +
                            "--minMonthYears \"" + minMonthYear + "\" " +
                            "--lowerBaseline \"" + lowerBaseline + "\" " + 
                            "--upperBaseline \"" + upperBaseline + "\" " + 
                            "--destination \"" + "Results/NewPoints/" + pointFilename + "\"";
                    String[] arguments = args.split(" ");

                    //GhcnV3.main(arguments);
                }
            }
        }
        

       
    }
    
}

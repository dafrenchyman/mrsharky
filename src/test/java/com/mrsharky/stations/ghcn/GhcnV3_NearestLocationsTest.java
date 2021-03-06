/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations.ghcn;

import static com.mrsharky.helpers.Utilities.recursiveDelete;
import com.mrsharky.spark.SetupSparkTest;
import com.mrsharky.stations.StationResults;
import java.io.File;
import java.util.List;
import org.apache.spark.sql.Row;
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
public class GhcnV3_NearestLocationsTest {
    
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
        String monthlyData = sourceDir + "ghcnm.tavg.v3.3.0.20171203.qca.dat";
        String inventoryData = sourceDir + "ghcnm.tavg.v3.3.0.20171203.qca.inv";
        double minDistance = 0.0;
        String qcType = "QCA";
        String lowerBaseline = "1960-12-31";
        String upperBaseline = "1990-12-31";
        int[]    minMonthYears = new int[]{20, 30};
        
        for (int minMonthYear : minMonthYears) {

            {
                String destination = "Results/Points/source=GhcnV3NearestLocation/km=" + minDistance + "/yrs=" + minMonthYear + "";

                String stationsPath = destination + "/finalStations_Results.serialized";
                File stationFile = new File(stationsPath);

                if (!stationFile.exists()) {

                    // Delete folder if exists
                    File destinationFile = new File(destination); 
                    recursiveDelete(destinationFile);

                    String args = 
                            "--monthlyData \""+ monthlyData + "\" " +
                            "--inventoryData \"" + inventoryData + "\" " +
                            "--qcType \"" + qcType + "\" " +
                            "--minDistance \"" + minDistance + "\" " +
                            "--minMonthYears \"" + minMonthYear + "\" " +
                            "--lowerBaseline \"" + lowerBaseline + "\" " + 
                            "--upperBaseline \"" + upperBaseline + "\" " + 
                            "--destination \""+ destination + "\"";
                    String[] arguments = args.split(" ");

                    //GhcnV3_NearestLocations.main(arguments);        
                }
            }
            
            {
                String destination = "Results/Points/source=GhcnV3_NearestLocationNoDups/km=" + minDistance + "/yrs=" + minMonthYear + "";

                String stationsPath = destination + "/finalStations_Results.serialized";
                File stationFile = new File(stationsPath);

                if (!stationFile.exists()) {

                    // Delete folder if exists
                    File destinationFile = new File(destination); 
                    recursiveDelete(destinationFile);

                    String args = 
                            "--monthlyData \""+ monthlyData + "\" " +
                            "--inventoryData \"" + inventoryData + "\" " +
                            "--qcType \"" + qcType + "\" " +
                            "--minDistance \"" + minDistance + "\" " +
                            "--minMonthYears \"" + minMonthYear + "\" " +
                            "--lowerBaseline \"" + lowerBaseline + "\" " + 
                            "--upperBaseline \"" + upperBaseline + "\" " + 
                            "--destination \""+ destination + "\"";
                    String[] arguments = args.split(" ");

                    //GhcnV3_NearestLocationsNoDups.main(arguments);        
                }
            }
        }
    }
    
}

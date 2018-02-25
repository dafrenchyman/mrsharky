/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_Global;
import com.mrsharky.spark.SetupSparkTest;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Julien Pierret
 */
public class Climate_PcaStationsTest {

    public Climate_PcaStationsTest() {
    }

    private SetupSparkTest sparkSetup;
  
    @BeforeClass
    public static void setupTests() throws Exception {
    }

    @Before
    public void setup() throws Exception {
        sparkSetup = new SetupSparkTest();
        sparkSetup.setup(4);
    }

    @After
    public void tearDown() {
        sparkSetup.tearDown();
    }

    @Test
    public void testMain() throws Exception {
        String eofData = "Results/NewPCA/" + 
                "dataset=air.sfc.mon.mean.nc_q=20_lowerbaseline=1850-12-31_upperBaseline=2014-12-31_startDate=1850-12-31_endDate=2014-12-31_startDate=1850-12-31_normalized=true.serialized";
        String stationData = "Results/NewPoints/" +
                "dataset=air.sfc.mon.mean.nc_lowerBaseline=1850-12-31_upperBaseline=2014-12-31_latCount=10_lonCount=20_minDistance=0.0_minMonthYears=30/" +
                "finalStations_Results.serialized";
   
        double varExplained = 0.9;
        
        // Spark
        if (true) {
            String finalOutput = "Results/sparkTest_1.csv";

            String args = 
                    "--eof \""+ eofData + "\" " +
                    "--output \"" + finalOutput + "\" " +
                    "--varExplained \"" + varExplained + "\" " +
                    "--normalized " + 
                    "--station \"" + stationData + "\"";

            String[] arguments = args.split(" ");

            Climate_PcaStations.main(arguments);
        }
        
        // regular
        if (true) {
            String finalOutput = "Results/sparkTest_2.csv";

            String args = 
                    "--eof \""+ eofData + "\" " +
                    "--output \"" + finalOutput + "\" " +
                    "--varExplained \"" + varExplained + "\" " +
                    "--normalized " + 
                    "--station \"" + stationData + "\"";

            String[] arguments = args.split(" ");
            ClimateFromStations_Global.main(arguments);
        }
    }

}
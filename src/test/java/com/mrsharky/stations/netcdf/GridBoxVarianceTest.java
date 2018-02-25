/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations.netcdf;

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
public class GridBoxVarianceTest {
    
    public GridBoxVarianceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of main method, of class GridBoxVariance.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");

        String dataset = "air.sfc.mon.mean.nc";
        
        String input = "Data/" + dataset;
        int varianceNumYears = 30;
        String output = "Results/Variance/Dataset=" + dataset + "/Years=" + varianceNumYears + "/variance.serialized";
        String variable = "air";
        String time = "time";

        String args =
                "--input \""+ input + "\" " +
                "--output \""+ output + "\" " +
                "--variable \""+ variable + "\" " +
                "--varianceNumYears \"" + varianceNumYears + "\" " +
                "--time \"" + time + "\"";
        String[] arguments = args.split(" ");
        GridBoxVariance.main(arguments);
    }
    
}

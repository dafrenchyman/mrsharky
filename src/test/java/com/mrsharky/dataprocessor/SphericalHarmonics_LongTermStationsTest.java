/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.util.List;
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
public class SphericalHarmonics_LongTermStationsTest {
    
    public SphericalHarmonics_LongTermStationsTest() {
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
     * Test of main method, of class NetCdfLoader.
     */
    @Test
    public void testMain() throws Exception {
        System.out.println("main");

    
        String input = "Data/air.sfc.mon.mean.nc";
        String output = "Results/air.sfc.mon.mean.nc_PCA";
        String variable = "air";
        String time = "time";
        int q = 20;

        String lowerBaseline = "1960-12-31";
        String upperBaseline = "1990-12-31";
        
        String args =
                "--input \""+ input + "\" " +
                "--output \""+ output + "\" " +
                "--variable \""+ variable + "\" " +
                "--q \"" + q + "\" " +
                "--lowerbaseline \"" + lowerBaseline + "\" " +
                "--upperbaseline \"" + upperBaseline + "\" " +
                "--time \"" + time + "\"";
        String[] arguments = args.split(" ");
        //SphericalHarmonics_LongTermStations.main(arguments);
    }
    
}

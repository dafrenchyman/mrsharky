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
public class SphericalHarmonics_OptimalQTest {
    
    public SphericalHarmonics_OptimalQTest() {
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
        String variable = "air";
        String time = "time";
        int q = 10;

        String lowerBaseline = "1960-12-31";
        String upperBaseline = "1990-12-31";
        
        String args =
                "--input \""+ input + "\" " +
                "--variable \""+ variable + "\" " +
                "--ql \"" + 92 + "\" " +
                "--lowerbaseline \"" + lowerBaseline + "\" " +
                "--upperbaseline \"" + upperBaseline + "\" " +
                "--qu \"" + 104 + "\" " +
                "--time \"" + time + "\"";
        String[] arguments = args.split(" ");
        //SphericalHarmonics_OptimalQ.main(arguments);
        
        SphericalHarmonics_OptimalQ_MultiThreaded_Compare.main(arguments);
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

/**
 *
 * @author mrsharky
 */
public class Sphericalharmonics_OptimalQ_tester {
    public static void main(String args[]) throws Exception {   
        System.out.println("main");

    
        String input = "Data/air.sfc.mon.mean.nc";
        String variable = "air";
        String time = "time";
        String lowerBaseline = "1960-12-31";
        //String upperBaseline = "1990-12-31";
        String upperBaseline = "1962-12-31";
        int qUpper = 4;
        int qLower = 3;

        String args2 =
                "--input \""+ input + "\" " +
                "--variable \""+ variable + "\" " +
                "--qlower \"" + qLower + "\" " +
                "--qupper \"" + qUpper + "\" " +
                "--lowerbaseline \"" + lowerBaseline + "\" " +
                "--upperbaseline \"" + upperBaseline + "\" " +
                "--time \"" + time + "\"";
        String[] arguments = args2.split(" ");
        SphericalHarmonics_OptimalQ_MultiThreaded_Compare.main(arguments);
    }
}

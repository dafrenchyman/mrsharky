/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

import java.io.File;

/**
 *
 * @author Julien Pierret
 */
public class NoiseAmountTester {
    public static void main(String args[]) throws Exception {
        String ncepDataset = "air.sfc.mon.mean.nc";
        String ncepVariable = "air";
        String ncepTime = "time";
        
        double varExplained = 0.8;        
        String lowerBaseline = "1850-12-31";
        String upperBaseline = "2014-12-31";
        
        String startDate = lowerBaseline;
        String endDate   = upperBaseline;
        
        int latCount = 0;
        int lonCount = 0;
        
        int q = 102;
        boolean normalized = true;
        
        String pointFilename = "dataset=" + ncepDataset + 
                "_lowerBaseline=" + lowerBaseline + 
                "_upperBaseline=" + upperBaseline +
                "_latCount=" + latCount +
                "_lonCount=" + lonCount +
                "_minDistance=0.0" +
                "_minMonthYears=30";
        String pointDataset = "Results/NewPoints/" +
                pointFilename +
                "/finalStations_Results.serialized";
        
        String pcaFilename = "dataset=" + ncepDataset + 
                "_q=" + q + 
                "_normalized=" + normalized + 
                "_lowerbaseline=" + lowerBaseline +
                "_upperBaseline=" + upperBaseline +
                "_startDate=" + startDate +
                "_endDate=" + endDate;           

        String pcaDataset = "Results/NewPCA/" +
                pcaFilename + 
                ".serialized";
        
        double[] noiseAmounts = new double[]{
            //0.0000000000001, // fails, singular matrix
            //0.000000000001,  // fails, singular matrix
            //0.00000000001,   // fails, singular matrix
            0.0000000001, 
            0.000000001, 
            0.00000001, 
            0.0000001, 
            0.000001, 
            0.00001, 
            0.0001, 
            0.001, 
            0.01, 
            0.1, 
            1, 
            10, 
            100};
        
        for (double noise : noiseAmounts) {
            System.setProperty("WeightNoiseAmount", String.valueOf(noise));
            
            // See influence on final value
            {
                String finalOutput = "Results/NoiseAmount/Value_" + noise + "_results.csv";
                File outputFile = new File(finalOutput);
                if (!outputFile.exists()) {
                    String args2 = 
                            "--eof \""+ pcaDataset + "\" " +
                            "--output \"" + finalOutput + "\" " +
                            "--varExplained \"" + varExplained + "\" " +
                            (normalized ? " --normalized " : "") + 
                            "--station \"" + pointDataset + "\"";
                    String[] arguments = args2.split(" ");
                    ClimateFromStations_Global.main(arguments);
                }
            }
            
            // See influence on weights
            {
                String finalOutput = "Results/NoiseAmount/Weight_" + noise + "_results.tsv";
                File outputFile = new File(finalOutput);
                if (!outputFile.exists()) {
                    String args2 = 
                            "--eof \""+ pcaDataset + "\" " +
                            "--output \"" + finalOutput + "\" " +
                            "--varExplained \"" + varExplained + "\" " +
                            (normalized ? " --normalized " : "") + 
                            "--station \"" + pointDataset + "\"";
                    String[] arguments = args2.split(" ");
                    ClimateFromStations_WeightTest.main(arguments);
                }
            }
        }
    }
}

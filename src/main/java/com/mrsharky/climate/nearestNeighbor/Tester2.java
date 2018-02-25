/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.nearestNeighbor;

import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations1;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_Global;
import com.mrsharky.stations.netcdf.NetCdf_NearestLocations;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class Tester2 {
    public static void main(String args[]) throws Exception {
        
        String[] ncepDatasets = new String[]{"air.sfc.mon.mean.nc"}; //, "air.2m.mon.mean.nc"};
        String ncepVariable = "air";
        String ncepTime = "time";
        
        double[] varExplained = new double[]{0.8}; //, 0.9};
        
        List<Pair<String, String>> baselines = new ArrayList<Pair<String, String>>();        
        baselines.add(Pair.with("1850-12-31", "2014-12-31"));
        
        //baselines.add(Pair.with("1870-12-31", "1900-12-31"));
        //baselines.add(Pair.with("1900-12-31", "1930-12-31"));
        //baselines.add(Pair.with("1930-12-31", "1960-12-31"));
        //baselines.add(Pair.with("1960-12-31", "1990-12-31"));
        
        List<Pair<Integer, Integer>> gridBoxes = new ArrayList<Pair<Integer, Integer>>();        
        //gridBoxes.add(Pair.with(0, 0));
        //gridBoxes.add(Pair.with(5, 10));
        gridBoxes.add(Pair.with(10, 20));
        gridBoxes.add(Pair.with(15, 30));
        gridBoxes.add(Pair.with(20, 40));
        
        int q = 10;
        boolean[] normalized = new boolean[]{true};
        
        if (true) {
            for (Pair<String, String> currBaseline : baselines) {
                String lowerBaseline = currBaseline.getValue0();
                String upperBaseline = currBaseline.getValue1();
                
                for (String input : ncepDatasets) {
                    for (Pair<Integer, Integer> gridBox : gridBoxes) {
                        int latCount = gridBox.getValue0();
                        int lonCount = gridBox.getValue1();
                    
                        String pointFilename = "dataset=" + input + 
                                "_lowerBaseline=" + lowerBaseline + 
                                "_upperBaseline=" + upperBaseline +
                                "_latCount=" + latCount +
                                "_lonCount=" + lonCount +
                                "_minDistance=0.0" +
                                "_minMonthYears=30";
                        String pointDataset = "Results/NewPoints/" +
                                pointFilename +
                                "/finalStations_Results.serialized";
                        File stationFile = new File(pointDataset);                      

                        if (!stationFile.exists()) {

                            String inputData = "Data/" + input;

                            String inputArgs = 
                                    "--input \""+ inputData + "\" " +
                                    "--output \""+ "Results/NewPoints/" + pointFilename + "\" " +
                                    "--variable \""+ ncepVariable + "\" " +
                                    "--time \"" + ncepTime + "\" " +
                                    "--latCount \"" + latCount + "\" " +
                                    "--lonCount \"" + lonCount + "\" " +
                                    "--createSpark " +
                                    "--lowerBaseline \"" + lowerBaseline + "\" " + 
                                    "--upperBaseline \"" + upperBaseline + "\"";
                            String[] arguments = inputArgs.split(" ");

                            NetCdf_NearestLocations.main(arguments);
                        }
                        
                        List<Pair<String, String>> pcaDates = new ArrayList<Pair<String, String>>();
                        pcaDates.add(Pair.with("1850-12-31", "2014-12-31"));
                        //pcaDates.add(Pair.with(lowerBaseline, upperBaseline));
                        
                        for (Pair<String, String> pcaDate : pcaDates) {
                            String startDate = pcaDate.getValue0();
                            String endDate   = pcaDate.getValue1();
                            
                            for (boolean normal : normalized) {         
                                
                                String pcaFilename = "dataset=" + input + 
                                            "_q=" + q + 
                                            "_normalized=" + normal + 
                                            "_lowerbaseline=" + lowerBaseline +
                                            "_upperBaseline=" + upperBaseline +
                                            "_startDate=" + startDate +
                                            "_endDate=" + endDate;   
                                
                                String pcaDataset = "Results/NewPCA/" +
                                        pcaFilename + 
                                        ".serialized";
                                
                                // Now use the point Dataset and PCA dataset together
                                for (double currVarExplained : varExplained) {
                                
                                    String finalOutput = "Results/NewFinal_global_q-1/" + 
                                            pcaFilename + "/" +
                                            pointFilename + "/VarExplained=" + currVarExplained + 
                                            "_results.csv";

                                    File outputFile = new File(finalOutput);

                                    //System.setProperty("verbose", "true");
                                    
                                    if (!outputFile.exists()) {
                                         String args2 = 
                                                 "--eof \""+ pcaDataset + "\" " +
                                                 "--q " + -1 + " " +
                                                 "--output \"" + finalOutput + "\" " +
                                                 "--varExplained \"" + currVarExplained + "\" " +
                                                 (normal ? " --normalized " : "") + 
                                                 "--station \"" + pointDataset + "\"";
                                         String[] arguments = args2.split(" ");
                                         ClimateFromStations1.main(arguments);
                                    }
                                }
                            }
                        }  
                    }
                }
            }
        }
    }
}

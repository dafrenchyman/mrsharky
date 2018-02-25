/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.nearestNeighbor;

import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_Global;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_Global_NoMeanDiff;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations_NoDiff;
import static com.mrsharky.helpers.Utilities.recursiveDelete;
import com.mrsharky.stations.netcdf.NetCdf_NearestLocations;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class Tester_NoMeanDiff {
    public static void main(String args[]) throws Exception {
        
        String[] ncepDatasets = new String[]{"air.sfc.mon.mean.nc"}; //, "air.2m.mon.mean.nc"};
        String ncepVariable = "air";
        String ncepTime = "time";
        
        double[] varExplained = new double[]{0.8};//, 0.9};
        
        List<Pair<String, String>> baselines = new ArrayList<Pair<String, String>>();        
        //baselines.add(Pair.with("1850-12-31", "2014-12-31"));
        //baselines.add(Pair.with("1850-12-31", "1880-12-31"));
        //baselines.add(Pair.with("1984-12-31", "2014-12-31"));
        //baselines.add(Pair.with("1870-12-31", "1900-12-31"));
        //baselines.add(Pair.with("1900-12-31", "1930-12-31"));
        //baselines.add(Pair.with("1930-12-31", "1960-12-31"));
        baselines.add(Pair.with("1960-12-31", "1990-12-31"));
        
        List<Pair<Integer, Integer>> gridBoxes = new ArrayList<Pair<Integer, Integer>>();        
        gridBoxes.add(Pair.with(0, 0));
        gridBoxes.add(Pair.with(5, 10));
        gridBoxes.add(Pair.with(10, 20));
        //gridBoxes.add(Pair.with(15, 30));
        //gridBoxes.add(Pair.with(20, 40));
        //gridBoxes.add(Pair.with(30, 60));
        
        List<Integer> qs = new ArrayList<Integer>();
        //qs.add(0);
        qs.add(10);
        //qs.add(20);
        //qs.add(30);
        //qs.add(40);
        //qs.add(50);
        //qs.add(60);
        //qs.add(70);
        //qs.add(102);
        boolean[] normalized = new boolean[]{true, false};
        
        // Generate Baselines
        if (true) {
            for (Pair<String, String> currBaseline : baselines) {
                String lowerBaseline = currBaseline.getValue0();
                String upperBaseline = currBaseline.getValue1();
                        
                for (String input : ncepDatasets) {
                    String baselineDataset = "Results/NewBaseline_NoDiff/dataset=" + input + 
                            "_lowerBaseline=" + lowerBaseline + 
                            "_upperBaseline=" + upperBaseline +
                            ".csv";
                    File baselineFile = new File(baselineDataset);
                    
                    if (!baselineFile.exists()) {
                        String inputData = "Data/" + input;

                        String inputArgs =
                                "--input \""+ inputData + "\" " +
                                "--output \""+ baselineDataset + "\" " +
                                "--variable \""+ ncepVariable + "\" " +
                                "--lowerbaseline \"" + lowerBaseline + "\" " +
                                "--upperbaseline \"" + upperBaseline + "\" " +
                                "--time \"" + ncepTime + "\"";
                        String[] arguments = inputArgs.split(" ");
                        NetCdfGlobal.main(arguments);
                    }
                }
            }
        }

        if (true) {
            for (int q : qs) {
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
                            //pcaDates.add(Pair.with("1850-12-31", "2014-12-31"));
                            pcaDates.add(Pair.with(lowerBaseline, upperBaseline));

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
                                            "_endDate=" + endDate;                                            ;

                                    String pcaDataset = "Results/NewPCA_NoDiff/" +
                                            pcaFilename + 
                                            ".serialized";
                                    File pcaFile = new File(pcaDataset);

                                    if (!pcaFile.exists()) {

                                        String inputData = "Data/" + input;

                                        String inputArgs =
                                            "--input \""+ inputData + "\" " +
                                            "--output \""+ pcaDataset + "\" " +
                                            "--variable \""+ ncepVariable + "\" " +
                                            "--q \"" + q + "\" " +
                                            "--lowerbaseline \"" + lowerBaseline + "\" " +
                                            "--upperbaseline \"" + upperBaseline + "\" " +
                                            "--startDate \"" + startDate + "\" " +
                                            "--endDate \"" + endDate + "\" " +
                                            (normal ? " --normalize " : "") +
                                            "--time \"" + ncepTime + "\"";

                                        String[] arguments = inputArgs.split(" ");
                                        SphericalHarmonics_LongTermStations_NoDiff.main(arguments);
                                    }

                                    // Now use the point Dataset and PCA dataset together
                                    for (double currVarExplained : varExplained) {

                                        String finalOutput = "Results/NewFinal_GlobalNoDiff/" + 
                                                pcaFilename + "/" +
                                                pointFilename + "/VarExplained=" + currVarExplained + 
                                                "_results.csv";

                                        File outputFile = new File(finalOutput);

                                        if (!outputFile.exists()) {
                                             String args2 = 
                                                     "--eof \""+ pcaDataset + "\" " +
                                                     "--output \"" + finalOutput + "\" " +
                                                     "--varExplained \"" + currVarExplained + "\" " +
                                                     "--q \"" + -1 + "\" " +
                                                     (normal ? " --normalized " : "") + 
                                                     "--station \"" + pointDataset + "\"";
                                             String[] arguments = args2.split(" ");
                                             ClimateFromStations_Global_NoMeanDiff.main(arguments);
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
}

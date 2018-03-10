/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate;

import com.mrsharky.climate.nearestNeighbor.NetCdfGlobalAverageSpectral;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations1;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations1_FullSpectra;
import com.mrsharky.climate.sphericalHarmonic.spark.Climate_PcaStations_IndivDates;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations_FullSpectral_multi;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations_FullSpectral_multi1;
import com.mrsharky.helpers.SparkMiniCluster;
import static com.mrsharky.helpers.Utilities.recursiveDelete;
import com.mrsharky.stations.ghcn.GhcnV3;
import com.mrsharky.stations.netcdf.NetCdf_NearestLocations;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class Test_fullQ {
    public static void main(String args[]) throws Exception {
        
        String[] ncepDatasets = new String[]{"air.sfc.mon.mean.nc"}; //, "air.2m.mon.mean.nc"};
        String ncepVariable = "air";
        String ncepTime = "time";
        
        double[] varExplained = new double[]{ 0.8};
        
        List<Pair<String, String>> baselines = new ArrayList<Pair<String, String>>();        
        //baselines.add(Pair.with("1850-12-31", "2014-12-31"));
        //baselines.add(Pair.with("1870-12-31", "1900-12-31"));
        //baselines.add(Pair.with("1900-12-31", "1930-12-31"));
        //baselines.add(Pair.with("1930-12-31", "1960-12-31"));
        baselines.add(Pair.with("1950-12-31", "1980-12-31"));
        //baselines.add(Pair.with("1960-12-31", "1990-12-31"));
        
        List<Pair<Integer, Integer>> gridBoxes = new ArrayList<Pair<Integer, Integer>>();        
        gridBoxes.add(Pair.with(0, 0));
        //gridBoxes.add(Pair.with(5, 10));
        gridBoxes.add(Pair.with(10, 20));
        //gridBoxes.add(Pair.with(15, 30));
        //gridBoxes.add(Pair.with(20, 40));
        //gridBoxes.add(Pair.with(40, 80));
        //gridBoxes.add(Pair.with(60, 120));
        
        boolean halfPca = false;
        
        int pointsQ = 102;
        
        List<Integer> qs = new ArrayList<Integer>();
        qs.add(0);
        //qs.add(10);
        //qs.add(20);
        //qs.add(30);
        //qs.add(40);
        //qs.add(50);
        //qs.add(60);
        //qs.add(102);
        boolean[] normalized = new boolean[]{ false };
        
        List<String> ghcnStationList = new ArrayList<String>();
        
        SparkMiniCluster smc = new SparkMiniCluster();
        smc.setUp();
        
        // Generate Baselines
        if (false) {
            for (Pair<String, String> currBaseline : baselines) {
                String lowerBaseline = currBaseline.getValue0();
                String upperBaseline = currBaseline.getValue1();
                        
                for (String input : ncepDatasets) {
                    String baselineDataset = "Results/NewBaseline_Spectral/dataset=" + input + 
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
                        NetCdfGlobalAverageSpectral.main(arguments);
                    }
                }
            }
        }
        
        // GHCN
        if (true) {
            String sourceDir = "/media/dropbox/PhD/Reboot/Projects/ghcn_v3_new/ghcnm.tavg.latest.qca/ghcnm.v3.3.0.20171203/";
            String input = "ghcnm.tavg.v3.3.0.20171203.qca";
            String monthlyData = sourceDir + input + ".dat";
            String inventoryData = sourceDir + input + ".inv";

            double[] minDistances = new double[]{500.0};
            int[]    minMonthYears = new int[]{30};
            String lowerBaseline = "1950-12-31";
            String upperBaseline = "1980-12-31";
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

                    ghcnStationList.add(pointDataset);
                    if (!stationFile.exists()) {
                        String qcType = "QCA";

                        // Delete folder if exists
                        File destinationFile = new File(pointDataset); 
                        recursiveDelete(destinationFile);

                        String args2 = 
                                "--monthlyData \""+ monthlyData + "\" " +
                                "--inventoryData \"" + inventoryData + "\" " +
                                "--qcType \"" + qcType + "\" " +
                                "--createSpark " +
                                "--minDistance \"" + minDistance + "\" " +
                                "--minMonthYears \"" + minMonthYear + "\" " +
                                "--lowerBaseline \"" + lowerBaseline + "\" " + 
                                "--upperBaseline \"" + upperBaseline + "\" " + 
                                "--destination \"" + "Results/NewPoints/" + pointFilename + "\"";
                        String[] arguments = args2.split(" ");

                        GhcnV3.main(arguments);
                    }
                }
            }
        }
        
        // Other stuff
        if (true) {
            for (Pair<String, String> currBaseline : baselines) {
                String lowerBaseline = currBaseline.getValue0();
                String upperBaseline = currBaseline.getValue1();
                
                for (String input : ncepDatasets) {
                    for (Pair<Integer, Integer> gridBox : gridBoxes) {
                        int latCount = gridBox.getValue0();
                        int lonCount = gridBox.getValue1();
                    
                        String pointFilename = "dataset=" + input + 
                                "_q=" + pointsQ +
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
                                    "--q \"" + pointsQ + "\" " +
                                    "--time \"" + ncepTime + "\" " +
                                    "--latCount \"" + latCount + "\" " +
                                    "--lonCount \"" + lonCount + "\" " +
                                    "--createSpark " +
                                    "--lowerBaseline \"" + lowerBaseline + "\" " + 
                                    "--upperBaseline \"" + upperBaseline + "\"";
                            String[] arguments = inputArgs.split(" ");

                            NetCdf_NearestLocations.main(arguments);
                        }
                        ghcnStationList.add(pointDataset);
                        
                        for (String currPointDataset : ghcnStationList) {

                            List<Pair<String, String>> pcaDates = new ArrayList<Pair<String, String>>();
                            pcaDates.add(Pair.with("1850-12-31", "2014-12-31"));

                            for (Pair<String, String> pcaDate : pcaDates) {
                                String startDate = pcaDate.getValue0();
                                String endDate   = pcaDate.getValue1();

                                for (boolean normal : normalized) {         
                                    for (int q : qs){

                                        String pcaFilename = "dataset=" + input + 
                                                        "_q=" + q + 
                                                        "_normalized=" + normal + 
                                                        "_lowerbaseline=" + lowerBaseline +
                                                        "_upperBaseline=" + upperBaseline +
                                                        "_startDate=" + startDate +
                                                        "_endDate=" + endDate;   
                                        String pcaDataset;

                                        if (halfPca) {
                                            pcaDataset = "Results/NewPCA/" +
                                                    pcaFilename + 
                                                    ".serialized";
                                        } else {
                                            pcaDataset = "Results/NewPCA_Full/" +
                                                    pcaFilename + 
                                                    ".serialized";
                                        }

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
                                            if (halfPca) {
                                                SphericalHarmonics_LongTermStations.main(arguments); 
                                            } else {
                                                SphericalHarmonics_LongTermStations_FullSpectral_multi1.main(arguments); 
                                            }
                                        }

                                        // Now use the point Dataset and PCA dataset together
                                        for (double currVarExplained : varExplained) {

                                            // first harmonic only
                                            if (true) {
                                                String finalOutput = "Results/NewFinal/" + 
                                                        pcaFilename + "/" +
                                                        currPointDataset + "/VarExplained=" + currVarExplained + 
                                                        "_results.csv";

                                                File outputFile = new File(finalOutput);

                                                //System.setProperty("verbose", "true");

                                                if (!outputFile.exists()) {
                                                     String args2 = 
                                                             "--eof \""+ pcaDataset + "\" " +
                                                             "--q " + 0 + " " +
                                                             "--output \"" + finalOutput + "\" " +
                                                             "--varExplained \"" + currVarExplained + "\" " +
                                                             (normal ? " --normalized " : "") + 
                                                             "--station \"" + currPointDataset + "\"";
                                                     String[] arguments = args2.split(" ");
                                                     ClimateFromStations1_FullSpectra.main(arguments);
                                                }
                                            }

                                            // Full Harmonic
                                            if (true) {
                                                String finalOutput;
                                                if (halfPca) {
                                                    finalOutput = "Results/NewFinal_global/" + 
                                                        pcaFilename + "/" +
                                                        currPointDataset + "/VarExplained=" + currVarExplained + 
                                                        "_results.csv";
                                                } else {
                                                    /*finalOutput = "Results/NewFinal_globalFullPca/" + 
                                                        pcaFilename + "/" +
                                                        currPointDataset + "/VarExplained=" + currVarExplained + 
                                                        "_results.csv";*/
                                                    finalOutput = "hdfs:///Results/";
                                                }

                                                File outputFile = new File(finalOutput);

                                                //System.setProperty("verbose", "true");

                                                if (!outputFile.exists()) {
                                                     String args2 = 
                                                             "--eof \""+ pcaDataset + "\" " +
                                                             "--q " + -1 + " " +
                                                             "--createSpark " + 
                                                             "--output \"" + finalOutput + "\" " +
                                                             "--varExplained \"" + currVarExplained + "\" " +
                                                             (normal ? " --normalized " : "") + 
                                                             "--station \"" + currPointDataset + "\"";
                                                     String[] arguments = args2.split(" ");

                                                     if (halfPca) {
                                                        ClimateFromStations1.main(arguments);
                                                     } else {
                                                        Climate_PcaStations_IndivDates.main(arguments); 
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
        }
        smc.tearDown();
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

import com.mrsharky.climate.nearestNeighbor.NearestNeighbor;
import com.mrsharky.climate.nearestNeighbor.NetCdfGlobalAverage;
import com.mrsharky.dataprocessor.SphericalHarmonics_LongTermStations;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Julien Pierret
 */
public class Tester {
    public static void main(String args[]) throws Exception {
        
        // Spherical Harmonic Transform
        if (true) {
            // Climate
            List<String> eofDataSets = Arrays.asList("air.sfc.mon.mean.nc");//, "air.2m.mon.mean.nc");
            //List<Integer> qs = Arrays.asList(93, 102);
            List<Integer> qs = Arrays.asList(20);
            //List<String> dates = Arrays.asList("All", "Baseline");
            List<String> dates = Arrays.asList("All");//, "Baseline");
            List<String> eofType = Arrays.asList("Normalized", "Regular");
            //List<String> eofType = Arrays.asList("Regular");//, "");

            // Stations
            List<String> pointsDataSets = Arrays.asList("BigJulSub.air.sfc.mon.mean.nc"); //, "Sub.air.2m.mon.mean.nc", "GhcnV3", "GhcnV3NearestLocation");
            //List<String> pointsDataSets = Arrays.asList("JulSub.air.sfc.mon.mean.nc", "JulSub.air.2m.mon.mean.nc");
            //List<String> pointsDataSets = Arrays.asList("air.sfc.mon.mean.nc", "air.2m.mon.mean.nc");
            //List<Double> kms = Arrays.asList(0.0, 300.0, 500.0);
            List<Double> kms = Arrays.asList(0.0);
            List<Integer> years = Arrays.asList(30);
            List<Double> varExplained = Arrays.asList(0.60, 0.80);

            for (String currEofData : eofDataSets) {
             for (Integer currQ : qs) {
              for (String currDate : dates) {
               for (String currType : eofType) {
                for (String currPointData : pointsDataSets) {
                 for (Double currKm : kms) {
                  for (Integer currYear : years) {

                   String eofDataPath = "Results/_PCA/Q=" + currQ + "/Dataset=" + currEofData + "/Date=" + currDate + "/Type=" + currType + "/Data.serialized";
                   String stationsPath = "Results/Points/source=" + currPointData + "/km=" + currKm + "/yrs=" + currYear + "/finalStations_Results.serialized";

                   File eofFile = new File(eofDataPath);
                   File stationFile = new File(stationsPath);

                   if (!eofFile.exists()) {
                        String input = "Data/"+ currEofData;
                        String variable = "air";
                        String time = "time";

                        //String lowerBaseline = "1960-12-31";
                        //String upperBaseline = "1990-12-31";
                        String lowerBaseline = "1850-12-31";
                        String upperBaseline = "2014-12-31";

                        String startDate = "";
                        String endDate = "";
                        
                        String normalized = currType.toUpperCase().equals("NORMALIZED") ? " --normalize " : "";

                        if (currDate.equals("All")) {
                            startDate     = "1850-12-31";
                            endDate       = "2014-12-31";
                        } else if (currDate.equals("Baseline")) {
                            startDate = lowerBaseline;
                            endDate = upperBaseline;
                        }

                        System.out.println("Generating PCA from all dates");
                        String args2 =
                                "--input \""+ input + "\" " +
                                "--output \""+ eofDataPath + "\" " +
                                "--variable \""+ variable + "\" " +
                                "--q \"" + currQ + "\" " +
                                "--lowerbaseline \"" + lowerBaseline + "\" " +
                                "--upperbaseline \"" + upperBaseline + "\" " +
                                "--startDate \"" + startDate + "\" " +
                                normalized + 
                                "--endDate \"" + endDate + "\" " +
                                "--time \"" + time + "\"";
                        String[] arguments = args2.split(" ");
                        SphericalHarmonics_LongTermStations.main(arguments);
                   }

                   if (eofFile.exists() && stationFile.exists()) {
                       for (Double currVarExplained : varExplained) {
                           
                           // Regular
                           {
                                String outputFilepath = "Results/Final/Scheme-SphericalHarmonics_Q-" + currQ + "_PcaData-" + currEofData + 
                                        "_Date-" + currDate + "_PcaType-" + currType + "_Stn-" + currPointData + 
                                        "_km-" + currKm + "_yrs-" + currYear + "_varExplained-" + currVarExplained + ".csv";

                                File outputFile = new File(outputFilepath);

                                String normalized = currType.toUpperCase().equals("NORMALIZED") ? " --normalized " : "";

                                if (!outputFile.exists()) {
                                     String args2 = 
                                             "--eof \""+ eofDataPath + "\" " +
                                             "--output \"" + outputFilepath + "\" " +
                                             "--varExplained \"" + currVarExplained + "\" " +
                                             normalized + 
                                             "--station \"" + stationsPath + "\"";
                                     String[] arguments = args2.split(" ");
                                     ClimateFromStations_Global.main(arguments);
                                }
                            }
                           
                           // New Variance
                           {
                                String outputFilepath = "Results/Final/Scheme-SphericalHarmonics_Q-" + currQ + "_PcaData-" + currEofData + 
                                        "_Date-" + currDate + "_PcaType-" + currType + "_Stn-" + currPointData + 
                                        "_km-" + currKm + "_yrs-" + currYear + "_varExplained-" + currVarExplained + "_NewVar.csv";

                                File outputFile = new File(outputFilepath);

                                String normalized = currType.toUpperCase().equals("NORMALIZED") ? " --normalized " : "";

                                if (!outputFile.exists()) {
                                     String args2 = 
                                             "--eof \""+ eofDataPath + "\" " +
                                             "--output \"" + outputFilepath + "\" " +
                                             "--varExplained \"" + currVarExplained + "\" " +
                                             normalized + 
                                             "--station \"" + stationsPath + "\"";
                                     String[] arguments = args2.split(" ");
                                     ClimateFromStations_Global_WithVariance.main(arguments);
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

        // Nearest Gridbox (NCEP Baseline)
        if (false) {
            List<String> eofDataSets = Arrays.asList("air.sfc.mon.mean.nc", "air.2m.mon.mean.nc");
            for (String currEofData : eofDataSets) {
                String outputFilepath = "Results/Final/Scheme-BaselineProof_PcaData-" + currEofData +  ".csv";
            
                String input = "Data/" + currEofData;
                String variable = "air";
                String time = "time";

                String lowerBaseline = "1960-12-31";
                String upperBaseline = "1990-12-31";

                String args2 =
                        "--input \""+ input + "\" " +
                        "--output \""+ outputFilepath + "\" " +
                        "--variable \""+ variable + "\" " +
                        "--lowerbaseline \"" + lowerBaseline + "\" " +
                        "--upperbaseline \"" + upperBaseline + "\" " +
                        "--time \"" + time + "\"";
                String[] arguments = args2.split(" ");
            
                NetCdfGlobalAverage.main(arguments);
            }
        }
        
        if (false) {
            List<String> eofDataSets = Arrays.asList("air.sfc.mon.mean.nc", "air.2m.mon.mean.nc");
            List<String> dates = Arrays.asList("All");
            List<Integer> qs = Arrays.asList(93, 102);
            List<String> pointsDataSets = Arrays.asList("All.air.sfc.mon.mean.nc", "All.air.2m.mon.mean.nc");
            List<String> eofType = Arrays.asList("Normalized");
            List<Double> kms = Arrays.asList(0.0);
            List<Integer> years = Arrays.asList(30);
            List<Double> varExplained = Arrays.asList(0.90);
            for (String currEofData : eofDataSets) {
             for (String currDate : dates) {
              for (String currPointData : pointsDataSets) {
               for (int currQ : qs) {
                for (double currKm : kms) {
                 for (String currType : eofType) {
                  for (Integer currYear : years) {
                    String eofDataPath = "Results/PCA/Q=" + currQ + "/Dataset=" + currEofData + "/Date=" + currDate + "/Type=" + currType + "/Data.serialized";
                    String stationsPath = "Results/Points/source=" + currPointData + "/km=" + currKm + "/yrs=" + currYear + "/finalStations_Results.serialized";
                    
                    File eofFile = new File(eofDataPath);
                    File stationFile = new File(stationsPath);
                    
                    if (eofFile.exists() && stationFile.exists()) {
                       for (Double currVarExplained : varExplained) {
                           String outputFilepath = "Results/Final/Scheme-NearestNeighbor_Q-" + currQ + "_PcaData-" + currEofData + 
                                   "_Date-" + currDate + "_PcaType-" + currType + "_Stn-" + currPointData + 
                                   "_km-" + currKm + "_yrs-" + currYear + "_varExplained-" + currVarExplained + ".csv";

                           File outputFile = new File(outputFilepath);

                           String normalized = currType.toUpperCase().equals("NORMALIZED") ? " --normalized " : "";

                           if (!outputFile.exists()) {
                                String args2 = 
                                        "--eof \""+ eofDataPath + "\" " +
                                        "--output \"" + outputFilepath + "\" " +
                                        "--station \"" + stationsPath + "\"";
                                String[] arguments = args2.split(" ");
                                NearestNeighbor.main(arguments);
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
}

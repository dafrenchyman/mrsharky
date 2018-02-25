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
public class Tester {
    public static void main(String args[]) throws Exception {
        
        //String input = "Data/air.sfc.mon.mean.nc";
        String level = "sfc";
        String input = "Data/air." + level + ".mon.mean.nc";
        String variable = "air";
        String time = "time";
        //int q = 93;
        int q = 0;
        String lowerBaseline = "1960-12-31";
        String upperBaseline = "1990-12-31";
        
        if (true) {
            String startDate     = "1850-12-31";
            String endDate       = "2014-12-31";
            
            if (false) {
                System.out.println("Generating PCA from all dates");
                String output = "Results/air." + level + ".mon.mean.nc_AllDates_PCA_" + q + "_half.serialized";
                String args2 =
                        "--input \""+ input + "\" " +
                        "--output \""+ output + "\" " +
                        "--variable \""+ variable + "\" " +
                        "--q \"" + q + "\" " +
                        "--lowerbaseline \"" + lowerBaseline + "\" " +
                        "--upperbaseline \"" + upperBaseline + "\" " +
                        "--startDate \"" + startDate + "\" " +
                        "--endDate \"" + endDate + "\" " +
                        "--time \"" + time + "\"";
                String[] arguments = args2.split(" ");
                SphericalHarmonics_LongTermStations.main(arguments);
            }
            
            if (true) {
                System.out.println("Generating Normalized PCA from all dates");
                String output = "Results/air." + level + ".mon.mean.nc_AllDates_norm_PCA_" + q + "_half.serialized";
                String args2 =
                        "--input \""+ input + "\" " +
                        "--output \""+ output + "\" " +
                        "--variable \""+ variable + "\" " +
                        "--q \"" + q + "\" " +
                        "--lowerbaseline \"" + lowerBaseline + "\" " +
                        "--upperbaseline \"" + upperBaseline + "\" " +
                        "--normalize " +
                        "--startDate \"" + startDate + "\" " +
                        "--endDate \"" + endDate + "\" " +
                        "--time \"" + time + "\"";
                String[] arguments = args2.split(" ");
                SphericalHarmonics_LongTermStations.main(arguments);
            }
        }
        
        if (true) {
            String startDate = lowerBaseline;
            String endDate = upperBaseline;
            if (true) {
                System.out.println("Generating Normalized PCA from baseline only");
                String output = "Results/air." + level + ".mon.mean.nc_BaselineDates_norm_PCA_" + q + "_half.serialized";
                
                String args2 =
                    "--input \""+ input + "\" " +
                    "--output \""+ output + "\" " +
                    "--variable \""+ variable + "\" " +
                    "--q \"" + q + "\" " +
                    "--normalize " +
                    "--lowerbaseline \"" + lowerBaseline + "\" " +
                    "--upperbaseline \"" + upperBaseline + "\" " +
                    "--startDate \"" + startDate + "\" " +
                    "--endDate \"" + endDate + "\" " +
                    "--time \"" + time + "\"";
                String[] arguments = args2.split(" ");
                SphericalHarmonics_LongTermStations.main(arguments);
            }
            
            if (false) {
                System.out.println("Generating PCA from baseline only");
                String output = "Results/air." + level + ".mon.mean.nc_BaselineDates_PCA_" + q + "_half.serialized";
                
                String args2 =
                    "--input \""+ input + "\" " +
                    "--output \""+ output + "\" " +
                    "--variable \""+ variable + "\" " +
                    "--q \"" + q + "\" " +
                    "--lowerbaseline \"" + lowerBaseline + "\" " +
                    "--upperbaseline \"" + upperBaseline + "\" " +
                    "--startDate \"" + startDate + "\" " +
                    "--endDate \"" + endDate + "\" " +
                    "--time \"" + time + "\"";
                String[] arguments = args2.split(" ");
                SphericalHarmonics_LongTermStations.main(arguments);
            }
            

            
            

            
        }
    }
}

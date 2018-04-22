/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import java.io.File;
import java.io.IOException;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author mrsharky
 */
public class Climate_ConvertSparkDatesToCsv {
 
    
    public Climate_ConvertSparkDatesToCsv(String folderLocation) throws IOException, Exception {
        
        File folder = new File(folderLocation);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile() && listOfFiles[i].getAbsolutePath().endsWith(".serialized")) {
                String currFile = listOfFiles[i].getCanonicalPath();
                
                String csvFile = currFile.replace(".serialized", ".csv");
                File file = new File(csvFile);
                if (!file.exists()) {                
                    SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(60);
                    finalStationHarmonic.SetHalfCompressedSpectra( (Complex[]) Utilities.LoadSerializedObject(currFile) );
                    InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                    double[][] stationHarmonics_spatial =invDst.ProcessGaussianDoubleArray(94, 192); 
                    String results = DoubleArray.toCsvString(stationHarmonics_spatial);
                    Utilities.SaveStringToFile(results, csvFile); 
                }
            }
        }
        
        
    }
    
    public static void main(String[] args) throws Exception {
        
        String fileLocation = "/media/dropbox/PhD/Projects/mrsharky/Results/"
                + "NewFinal_globalFullPca/"
                + "dataset=air.sfc.mon.mean.nc_q=102_normalized=false_lowerbaseline=1950-12-31_upperBaseline=1980-12-31_startDate=1850-12-31_endDate=2014-12-31"
                + "/Results/NewPoints/dataset=ghcnm.tavg.v3.3.0.20171203.qca_lowerBaseline=1950-12-31_upperBaseline=1980-12-31_minDistance=500.0_minMonthYears=30/"
                + "finalStations_Results.serialized/VarExplained=0.8_results.csv/Harmonics";
        
        fileLocation = "/media/dropbox/PhD/Projects/mrsharky/Results/"
                + "NewFinal_globalFullPca/"
                + "dataset=air.sfc.mon.mean.nc_q=30_normalized=false_lowerbaseline=1950-12-31_upperBaseline=1980-12-31_startDate=1850-12-31_endDate=2014-12-31/"
                + "Results/NewPoints/dataset=ghcnm.tavg.v3.3.0.20171203.qca_lowerBaseline=1950-12-31_upperBaseline=1980-12-31_minDistance=500.0_minMonthYears=30/"
                + "finalStations_Results.serialized/VarExplained=0.8_results.csv/Harmonics";
        
        fileLocation = "/media/dropbox/PhD/Projects/mrsharky/Results/"
                + "NewFinal_globalFullPca/"
                + "dataset=air.sfc.mon.mean.nc_q=30_normalized=false_lowerbaseline=1950-12-31_upperBaseline=1980-12-31_startDate=1850-12-31_endDate=2014-12-31/"
                + "Results/NewPoints/dataset=air.sfc.mon.mean.nc_q=30_lowerBaseline=1950-12-31_upperBaseline=1980-12-31_latCount=10_lonCount=20_minDistance=0.0_minMonthYears=30/"
                + "finalStations_Results.serialized/VarExplained=0.8_results.csv/Harmonics";
        
        fileLocation = "/media/dropbox/PhD/Projects/mrsharky/Results/"
                + "NewFinal_globalFullPca/"
                + "dataset=air.sfc.mon.mean.nc_q=60_normalized=false_lowerbaseline=1950-12-31_upperBaseline=1980-12-31_startDate=1850-12-31_endDate=2014-12-31/"
                + "Results/NewPoints/dataset=air.sfc.mon.mean.nc_q=60_lowerBaseline=1950-12-31_upperBaseline=1980-12-31_latCount=20_lonCount=40_minDistance=0.0_minMonthYears=30/"
                + "finalStations_Results.serialized/VarExplained=0.8_results.csv/Harmonics";
        
        Climate_ConvertSparkDatesToCsv csvSave = new Climate_ConvertSparkDatesToCsv(fileLocation);
        
    }
    
    
    
}

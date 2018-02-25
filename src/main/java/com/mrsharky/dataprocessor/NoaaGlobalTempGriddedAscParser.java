/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author dafre
 */
public class NoaaGlobalTempGriddedAscParser {
    public NoaaGlobalTempGriddedAscParser(String inputFile, String outputFile)
    {
        try 
        {
            OutputStream outputStream  = new FileOutputStream(outputFile);
            Writer outputStreamWriter = new OutputStreamWriter(outputStream);
            File myFile = new File(inputFile); 
            FileInputStream fis = new FileInputStream(myFile); // Finds the workbook instance for XLSX file 
            
            // Write out the header on the outputFile
            outputStreamWriter.append("Lat,Lon,Date,Valuen\n");

            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            String line = null;
            double lat = -87.5;
            double latStep = 5;
            double lon = 2.5;
            double lonStep = 5;
            String date = "";
            
            while ((line = br.readLine()) != null) {
                line = line.trim().replaceAll(" +", " ");
                String[] splitLine = line.split(" ");
                if (splitLine.length == 2) { // We're at a month / year line, reset everything {
                    lat = -87.5;
                    lon = 2.5;
                    date = StringUtils.leftPad(splitLine[1], 4, '0') + "-" + StringUtils.leftPad(splitLine[0], 2, '0') + "-01";                    
                } else {
                    StringBuilder sb = new StringBuilder();
                    lon = 2.5;
                    for (int counter = 0; counter < splitLine.length; counter++)
                    {
                        String value = splitLine[counter];
                        sb.append(lat + "," + (lon > 180 ? lon -360 : lon) + "," + date + "," + value + "\n");
                        lon = lon + lonStep;
                    }
                    outputStreamWriter.append(sb);
                    lat = lat + latStep;
                }  
            }
            br.close();
            outputStreamWriter.close();
            outputStream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NoaaGlobalTempGriddedAscParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (IOException ex) {
            Logger.getLogger(NoaaGlobalTempGriddedAscParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    public static void main(String args[]) {
        String inputFile = "";
        String outputFile = "";
        for (int argsCounter = 0; argsCounter < args.length; argsCounter++)
        {
            String currArg = args[argsCounter];
            switch (currArg.toUpperCase()) {
                case "-INPUT":
                    inputFile = args[++argsCounter];
                    break;
                case "-OUTPUT":
                    outputFile = args[++argsCounter];
                    break;
                default:
                    throw new IllegalArgumentException("Invalid input argument: " + currArg);
            }
        }
     
        //String inputFile = "Z://PhD/Reboot/Projects/Noaa/NOAAGlobalTemp.gridded.v4.0.1.201605.asc";
        //String outputFile = "Z://PhD/Reboot/Projects/Noaa/NOAAGlobalTemp.gridded.v4.0.1.201605.csv";
                
        NoaaGlobalTempGriddedAscParser noaaParser = new NoaaGlobalTempGriddedAscParser(inputFile, outputFile);
        System.exit(0);
    }
}

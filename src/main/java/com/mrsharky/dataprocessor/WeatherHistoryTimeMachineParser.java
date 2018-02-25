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
public class WeatherHistoryTimeMachineParser {
    public WeatherHistoryTimeMachineParser(String inputFile, String outputFile) {
        try  {
            OutputStream outputStream  = new FileOutputStream(outputFile);
            Writer outputStreamWriter = new OutputStreamWriter(outputStream);
            File myFile = new File(inputFile); 
            FileInputStream fis = new FileInputStream(myFile); // Finds the workbook instance for XLSX file 
            
            // Write out the header on the outputFile
            outputStreamWriter.append("Lat,Lon,Date,Valuen\n");

            //Construct BufferedReader from InputStreamReader
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));

            // Read Header line to get the dates
            int startDateLocation = 2;
            int endDateLocation = -1;
            String [] dates;
            {
                String line = br.readLine();
                dates = line.split(",");
                endDateLocation = dates.length;
            }
            
            String line = null;
            String date = "";
            
            while ((line = br.readLine()) != null) {
                String[] splitLine = line.split(",");
                double lat = Double.valueOf(splitLine[0]);
                double lon = Double.valueOf(splitLine[1]);
                
                // get all the dates
                StringBuilder sb = new StringBuilder();
                for (int currDateCounter = startDateLocation; currDateCounter < endDateLocation; currDateCounter++) {
                    String currDate = dates[currDateCounter] + "-01-01";
                    String currValue = splitLine[currDateCounter];
                    sb.append(lat + "," + (lon > 180 ? lon -360 : lon) + "," + currDate + "," + currValue + "\n");
                }
                outputStreamWriter.append(sb);
            }
            br.close();
            outputStreamWriter.close();
            outputStream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(WeatherHistoryTimeMachineParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (IOException ex) {
            Logger.getLogger(WeatherHistoryTimeMachineParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    public static void main(String args[]) {
        //String inputFile = "";
        //String outputFile = "";
        String inputFile = "Z://PhD//Reboot//Projects//WeatherHistoryTimeMachine//sogpa5degV1.0_corrected.csv";
        String outputFile = "Z://PhD//Reboot//Projects//WeatherHistoryTimeMachine//sogpa5degV1.0_final.csv";
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
                
        WeatherHistoryTimeMachineParser whtmParser = new WeatherHistoryTimeMachineParser(inputFile, outputFile);
        System.exit(0);
    }
}

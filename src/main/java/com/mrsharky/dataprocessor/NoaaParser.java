/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Julien Pierret
 */
public class NoaaParser {
    
    public NoaaParser(String inputFile, String outputFile, double nullValue)
    {
        try 
        {
            OutputStream outputStream  = new FileOutputStream(outputFile);
            Writer outputStreamWriter = new OutputStreamWriter(outputStream);
            File myFile = new File(inputFile); 
            FileInputStream fis = new FileInputStream(myFile); // Finds the workbook instance for XLSX file 
            XSSFWorkbook myWorkBook = new XSSFWorkbook (fis); // Return first sheet from the XLSX workbook 
            XSSFSheet mySheet = myWorkBook.getSheetAt(0); // Get iterator to all the rows in current sheet 
            Iterator<Row> rowIterator = mySheet.iterator(); // Traversing over each row of XLSX file 

            int latLocation = 0;
            int lonLocation = 0;
            Map<Integer, String> colToDateMap = new HashMap<Integer, String>();
            // First take care of the header row
            {
                Row row = rowIterator.next(); // For each row, iterate through each columns 
                Iterator<Cell> cellIterator = row.cellIterator(); 
                int colCounter = 0;
                while (cellIterator.hasNext()) 
                { 
                    Cell cell = cellIterator.next();
                    String currValue = "";
                    switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                            currValue = cell.getStringCellValue();
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            currValue = Double.toString(cell.getNumericCellValue());
                            break;
                        case Cell.CELL_TYPE_BOOLEAN:
                            currValue = cell.getBooleanCellValue() == true ? "1" : "0";
                            break;
                        default:
                    }
                    if (currValue.toUpperCase().equals("LAT")) {
                        latLocation = colCounter;
                    } else if (currValue.toUpperCase().equals("LON")) {
                        lonLocation = colCounter;
                    } else {
                        String[] dateValues = currValue.split(" ");
                        String date = dateValues[1] + "-" + StringUtils.leftPad(dateValues[0], 2, "0") + "-01";
                        colToDateMap.put(colCounter, date);
                    }
                    colCounter++;
                }
            }
            
            // build up the header
            outputStreamWriter.append("Lat,Lon,Date,Value\n");

            // Now, loop through the other rows
            while (rowIterator.hasNext()) { 
                Row row = rowIterator.next(); // For each row, iterate through each columns 
                Iterator<Cell> cellIterator = row.cellIterator(); 
                int colCounter = 0;
                double currLat = 0;
                double currLon = 0;
                StringBuilder sb = new StringBuilder();
                while (cellIterator.hasNext()) 
                { 
                    Cell cell = cellIterator.next();
                    double currValue = cell.getNumericCellValue();
                    if (colCounter == latLocation) {
                        currLat = currValue;
                    } else if (colCounter == lonLocation) {
                        if (currValue > 180) {
                            currValue = currValue - 360;
                        }
                        currLon = currValue;
                    } else {
                        String valueAsString = Double.toString(currValue);
                        /*if (currValue == nullValue) {
                            valueAsString = "null";
                        }*/
                        String currDate = colToDateMap.get(colCounter);
                        sb.append(currLat + "," + currLon + "," + currDate + "," + valueAsString + "\n");
                    }
                    colCounter++;
                }
                System.out.println("Finished Processing: " + currLat + ", " + currLon);
                outputStreamWriter.append(sb);
            }
            outputStreamWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static void main(String args[]) {
     
        String inputFile = "E://Dropbox/Dropbox/Julien/SamDataVisualization/NOAAGlobal5DegTemp188001-201601.xlsx";
        String outputFile = "E://Dropbox/Dropbox/Julien/SamDataVisualization/NOAAGlobal5DegTemp188001-201601.csv";
                
        NoaaParser noaaParser = new NoaaParser(inputFile, outputFile, -999.9);
        
    }
}

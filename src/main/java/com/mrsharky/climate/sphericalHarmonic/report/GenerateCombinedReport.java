/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.report;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.IteratorUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Julien Pierret
 */
public class GenerateCombinedReport {
    public GenerateCombinedReport(String inputfolder) {
        
        Set<String> PcaData = new HashSet<String>();
        Set<String> StnData = new HashSet<String>();
        Set<String> Scheme  = new HashSet<String>();
        Set<Integer> PcaQ = new HashSet<Integer>();
        Set<String> PcaDate = new HashSet<String>();
        Set<String> Normalized = new HashSet<String>();
        Set<Double> GhcnMinDistance = new HashSet<Double>();
        Set<Integer> GhcnMinYears = new HashSet<Integer>();
        Set<Double> PcaVarianceExplained = new HashSet<Double>();
        
        
        File folder = new File(inputfolder);
        File[] listOfFiles = folder.listFiles();
        

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String[] args = listOfFiles[i].getName().split("_");
                
                String currScheme = args[0].split("-")[1];
                int currQ = Integer.valueOf(args[1].split("-")[1]);
                String currPcaData = args[2].split("-")[1];
                String currDate = args[3].split("-")[1];
                String currNormalized = args[4].split("-")[1];
                String currStation = args[5].split("-")[1];
                double currStationMinDist = Double.valueOf(args[6].split("-")[1]);
                int currMinYears = Integer.valueOf(args[7].split("-")[1]);
                double currVarExplained = Double.valueOf(args[8].split("-")[1].replace(".csv", ""));
                
                
                
                
                
                
                System.out.println("File " + listOfFiles[i].getName());
            } 
        }
        
        
        // Climate
        List<String> eofDataSets = Arrays.asList("air.sfc.mon.mean.nc", "air.2m.mon.mean.nc");
        List<Integer> qs = Arrays.asList(93, 102);
        List<String> dates = Arrays.asList("All", "Baseline");
        List<String> eofType = Arrays.asList("Normalized");
        
        // Stations
        List<String> pointsDataSets = Arrays.asList("GhcnV3", "GhcnV3_NearestLocation");
        List<Double> kms = Arrays.asList(0.0, 300.0, 500.0);
        List<Integer> years = Arrays.asList(30);
        //List<Double> varExplained = Arrays.asList(0.80, 0.90, 0.95);
        List<Double> varExplained = Arrays.asList(0.90);
        
        /*
        for (String currEofData : eofDataSets) {
         for (Integer currQ : qs) {
          for (String currDate : dates) {
           for (String currType : eofType) {
            for (String currPointData : pointsDataSets) {
             for (Double currKm : kms) {
              for (Integer currYear : years) {
        
        
        // Climate
        List<String> eofDataSets = Arrays.asList("air.sfc.mon.mean.nc", "air.2m.mon.mean.nc");
        List<Integer> qs = Arrays.asList(93, 102);
        List<String> dates = Arrays.asList("All", "Baseline");
        List<String> eofType = Arrays.asList("Normalized");
        
        // Stations
        List<String> pointsDataSets = Arrays.asList("GhcnV3", "GhcnV3_NearestLocation");
        List<Double> kms = Arrays.asList(0.0, 300.0, 500.0);
        List<Integer> years = Arrays.asList(30);
        //List<Double> varExplained = Arrays.asList(0.80, 0.90, 0.95);
        List<Double> varExplained = Arrays.asList(0.90);
        
        for (String currEofData : eofDataSets) {
         for (Integer currQ : qs) {
          for (String currDate : dates) {
           for (String currType : eofType) {
            for (String currPointData : pointsDataSets) {
             for (Double currKm : kms) {
              for (Integer currYear : years) {
        
        
        
        
        */
    }
    
    public static void main(String args[]) throws Exception {
        new GenerateCombinedReport("Results/Final/");
    }
    
}

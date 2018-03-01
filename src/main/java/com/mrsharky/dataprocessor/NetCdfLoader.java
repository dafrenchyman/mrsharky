/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;


import org.javatuples.Pair;
import org.apache.commons.lang3.ArrayUtils;

/**
 *
 * @author Julien Pierret
 */
public class NetCdfLoader {
    
    private Map<Date, Pair<Integer, Integer>>__dates;
    private Map<Double, Pair<Integer, Integer>> __lats;
    private Map<Double, Pair<Integer, Integer>> __lons;
    private Map<Double, Pair<Integer, Integer>> __levels;
    private String _inputFile;
    private String _variableOfInterest;
    private String _timeVariable;
    private String[] _dimNames;
    private long _totalRows;
    
    public Date[] GetDates() {
        Date[] allDates = (Date[]) __dates.keySet().stream().sorted().collect(Collectors.toList()).toArray();
        return allDates;
    }
    
    public double[] GetLons() {
        Double [] output = new Double[this.__lons.keySet().size()];
        output = this.__lons.keySet().stream().sorted().collect(Collectors.toList()).toArray(output);
        return ArrayUtils.toPrimitive(output);
    }
    
    public double[] GetLats() {
        Double [] output = new Double[this.__lats.keySet().size()];
        output = this.__lats.keySet().stream().sorted().collect(Collectors.toList()).toArray(output);
        return ArrayUtils.toPrimitive(output);
    }
    
    private Map<Double, Pair<Integer, Integer>> GetVariableLookup(
            List<Double> origDataOrder, Map<Double, Integer> lookup,
            Comparator<Double> comparator) {
        List<Double> dataReOrdered = origDataOrder.stream().sorted(comparator).collect(Collectors.toList());
        Map<Double, Pair<Integer, Integer>> finalLookup = new HashMap<Double, Pair<Integer, Integer>>();
        for (int counter = 0; counter < dataReOrdered.size(); counter++) {
            double currData = dataReOrdered.get(counter);
            Pair<Integer, Integer> currPair = Pair.with(lookup.get(currData), counter);
            finalLookup.put(currData, currPair);
        }
        return finalLookup;
    }
    
    private Map<Date, Pair<Integer, Integer>> GetDateLookup(
            List<Date> origDataOrder, Map<Date, Integer> lookup,
            Comparator<Date> comparator) {
        List<Date> dataReOrdered = origDataOrder.stream().sorted(comparator).collect(Collectors.toList());
        Map<Date, Pair<Integer, Integer>> finalLookup = new HashMap<Date, Pair<Integer, Integer>>();
        for (int counter = 0; counter < dataReOrdered.size(); counter++) {
            Date currData = dataReOrdered.get(counter);
            Pair<Integer, Integer> currPair = Pair.with(lookup.get(currData), counter);
            finalLookup.put(currData, currPair);
        }
        return finalLookup;
    }
    
    private Map<Double, Pair<Integer, Integer>> GetLevelsFromVariable(Variable dimVar) throws Exception {
        String levelUnits = dimVar.getUnitsString();
        ArrayFloat.D1 dataArray = (ArrayFloat.D1) dimVar.read();
        
        System.out.println("Found Levels: " + dataArray.getShape()[0]);
        List<Double> origDataOrder = new ArrayList<Double>();
        Map<Double, Integer> lookup = new HashMap<Double, Integer>();
        for (int counter = 0; counter < dataArray.getShape()[0]; counter++) {
            double currData = dataArray.get(counter);
            origDataOrder.add(currData);
            lookup.put(currData, counter);
        }
        
        Comparator<Double> comparator = Comparator.comparing(entry -> entry);     
        return GetVariableLookup(origDataOrder, lookup,comparator);
    }
    
    private Map<Double, Pair<Integer, Integer>> GetLonsFromVariable(Variable dimVar, Comparator<Double> comparator) throws Exception {
        String currUnits = dimVar.getUnitsString();
        Map<Double, Pair<Integer, Integer>> finalLookup = new HashMap<Double, Pair<Integer, Integer>>();
        if (currUnits.equals("degrees_east")) {
            ArrayFloat.D1 dataArray = (ArrayFloat.D1) dimVar.read();
            System.out.println("Found Longitudes: " + dataArray.getShape()[0]);
            List<Double> origDataOrder = new ArrayList<Double>();
            Map<Double, Integer> lookup = new HashMap<Double, Integer>();
            for (int counter = 0; counter < dataArray.getShape()[0]; counter++) {
                double currData = dataArray.get(counter);
                currData = currData > 180 ? currData-360 : currData;
                origDataOrder.add(currData);
                lookup.put(currData, counter);
            }
            finalLookup = GetVariableLookup(origDataOrder, lookup,comparator);
            
        } else {
            throw new Exception("LonUnits: " + currUnits + " is unknown");
        }
        return finalLookup;
    }
    
    private Map<Double, Pair<Integer, Integer>> GetLatsFromVariable(Variable dimVar, Comparator<Double> comparator) throws Exception {
        String currUnits = dimVar.getUnitsString();
        Map<Double, Pair<Integer, Integer>> finalLookup = new HashMap<Double, Pair<Integer, Integer>>();
        if (currUnits.equals("degrees_north")) {
            ArrayFloat.D1 dataArray = (ArrayFloat.D1) dimVar.read();
            System.out.println("Found Latidutes: " + dataArray.getShape()[0]);
            List<Double> origDataOrder = new ArrayList<Double>();
            Map<Double, Integer> lookup = new HashMap<Double, Integer>();
            for (int counter = 0; counter < dataArray.getShape()[0]; counter++) {
                double currData = dataArray.get(counter);
                origDataOrder.add(currData);
                lookup.put(currData, counter);
            }
            finalLookup = GetVariableLookup(origDataOrder, lookup,comparator);
            
        } else {
            throw new Exception("LatUnits: " + currUnits + " is unknown");
        }
        return finalLookup;
    }
    
    private Date GetStartingDate(String timeUnits) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
        String[] splitUnits = timeUnits.split(" ")[2].split("-");
        String year = splitUnits[0];
        String month = StringUtils.leftPad(splitUnits[1], 2, "0");
        String day = StringUtils.leftPad(splitUnits[2], 2, "0");

        String inputStr = year + "-" + month + "-" + day;
        return dateFormat.parse(inputStr);
    }
    
    private Map<Date, Pair<Integer, Integer>> GetDatesFromVariable(Variable dimVar, Comparator<Date> comparator) throws Exception {
        String timeUnits = dimVar.getUnitsString();
        ArrayDouble.D1 timeArray = (ArrayDouble.D1) dimVar.read();
        System.out.println("Found Dates: " + timeArray.getShape()[0]);
        Date originDate = GetStartingDate(timeUnits);
        
        // Take care of different units we may see
        List<Date> origDataOrder = new ArrayList<Date>();
        Map<Date, Integer> lookup = new HashMap<Date, Integer>();
        if (timeUnits.equals("hours since 1800-1-1 00:00:0.0")) {
            for (int counter = 0; counter < timeArray.getShape()[0]; counter++) {
                int additionalHours = (int) timeArray.get(counter);
                Date currDate = DateUtils.addHours(originDate, additionalHours);
                lookup.put(currDate, counter);
                origDataOrder.add(currDate);
            }
        } else if (timeUnits.equals("days since 1800-1-1 00:00:0.0")) { 
            for (int counter = 0; counter < timeArray.getShape()[0]; counter++) {
                int additionalDays = (int) timeArray.get(counter);
                Date currDate = DateUtils.addDays(originDate, additionalDays);
                lookup.put(currDate, counter);
                origDataOrder.add(currDate);
            }
        } else {
            throw new Exception("TimeUnits: " + timeUnits + " is unknown");
        }
        Map<Date, Pair<Integer, Integer>> dates = GetDateLookup(origDataOrder, lookup, comparator);
        return dates;
    }
    
    public NetCdfLoader(String inputFile, String variableOfInterest, String timeVariable) {
        // Get database info
        _inputFile = inputFile;
        _variableOfInterest = variableOfInterest;
        _timeVariable = timeVariable;
        
        String variableOfInterestUnits = "";
        String timeUnits = "";
        
        // Open the file and check to make sure it's valid.
        NetcdfFile dataFile = null;

        try {
            System.out.println("------------------------------------------------------------------------");
            System.out.println("Loading NetCDF file: " + _inputFile);
            System.out.println("------------------------------------------------------------------------");
            dataFile = NetcdfFile.open(_inputFile, null);

            List<Variable> allVariables = dataFile.getVariables();
            System.out.println("Listing all available variables:");
            for (Variable currVar : allVariables) {
                System.out.print(currVar.getFullName() + "; ");
            }
            
            // Get the location of all the variables
            Variable varOfInterest = dataFile.findVariable(_variableOfInterest);
            _totalRows = 1;
            for (int currShape : varOfInterest.getShape() ) {
                _totalRows = _totalRows * currShape;
            }
            _dimNames = new String[varOfInterest.getShape().length];
            for (int counter = 0; counter < varOfInterest.getShape().length; counter++) {
                _dimNames[counter] = varOfInterest.getDimension(counter).getFullName();
            }
  
            System.out.println("\nFinding all parameters");
            for (Variable findVar : allVariables) {
                if (findVar.getFullName().toUpperCase().equals(variableOfInterest.toUpperCase())) {
                    Variable currVar = dataFile.findVariable(variableOfInterest);
                    variableOfInterestUnits = currVar.getUnitsString();
                    currVar.getDimensions().size();
                    
                    List<Dimension> dims = currVar.getDimensions();
                    
                    for (int dimCounter = 0; dimCounter < dims.size(); dimCounter++) {
                        Dimension dim = dims.get(dimCounter);
                        String currName = dim.getFullNameEscaped();
                        Variable dimVar = dataFile.findVariable(currName);
                        
                        // Dealing with date values
                        if (currName.toUpperCase().equals(timeVariable.toUpperCase())) {
                            Comparator<Date> comparator = Comparator.comparing(entry -> entry);
                            __dates = GetDatesFromVariable(dimVar, comparator);
                        // Dealing with lat values
                        } else if (currName.toUpperCase().equals("LAT")) {     
                            Comparator<Double> comparator = Comparator.comparing(entry -> -entry);
                            __lats = GetLatsFromVariable(dimVar, comparator);
                        // Dealing with lon values
                        } else if (currName.toUpperCase().equals("LON")) {
                            Comparator<Double> comparator = Comparator.comparing(entry -> entry);
                            __lons = GetLonsFromVariable(dimVar, comparator);
                        // Dealing with different levels
                        } else if (currName.toUpperCase().equals("LEVEL")) {   
                            __levels = GetLevelsFromVariable(dimVar);
                        } else {
                            throw new Exception ("Invalid/Unrecognized dimension name: " + currName);
                        }
                    }
                }
            }
        } catch (java.io.IOException e) {
            System.out.println(" fail = " + e);
            e.printStackTrace();
            System.exit(-1);
        } catch (ParseException ex) {
                Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
        } catch (Exception ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }
    
    public Map<Date, double[][]> LoadData() throws Exception {
        Map<Date, double[][]> output = new HashMap<Date, double[][]> ();

        NetcdfFile dataFile = null;
        try {
            dataFile = NetcdfFile.open(_inputFile, null);
            Variable varOfInterest = dataFile.findVariable(_variableOfInterest);

            double counter = 0;
            if (__levels == null) {
                ArrayFloat.D3 variableArray = (ArrayFloat.D3) varOfInterest.read();
     
                for (Date currDate : this.__dates.keySet().stream().sorted().collect(Collectors.toList())) {
                    int dateSource = this.__dates.get(currDate).getValue0();
                    double[][] dateValues = new double[this.__lats.size()][this.__lons.size()];
                    
                    int locations[] = new int[this._dimNames.length];
                    for (int i = 0; i < locations.length; i++) {
                        locations[i] = -1;
                    }
                    
                    // Date
                    for (int i = 0; i < locations.length; i++) {
                        if (this._dimNames[i].equals(_timeVariable)) {
                            locations[i] = dateSource;
                        }
                    }
                    
                    for (Double currLat : this.__lats.keySet()) {
                        int latSource = this.__lats.get(currLat).getValue0();
                        int latDest   = this.__lats.get(currLat).getValue1();
                        
                        // Lat
                        for (int i = 0; i < locations.length; i++) {
                            if (this._dimNames[i].toUpperCase().equals("LAT")) {
                                locations[i] = latSource;
                            }
                        }
                        
                        for (Double currLon : this.__lons.keySet()) {
                            int lonSource = this.__lons.get(currLon).getValue0();
                            int lonDest   = this.__lons.get(currLon).getValue1();
                                               
                            counter++;

                            // Lon
                            for (int i = 0; i < locations.length; i++) {
                                if (this._dimNames[i].toUpperCase().equals("LON")) {
                                    locations[i] = lonSource;
                                }
                            }

                            // Variable of interest
                            double value = variableArray.get(locations[0], locations[1], locations[2]);
                            dateValues[latDest][lonDest] = value;
                            if (counter % 10000 == 0) {
                                System.out.print("Complete: " + Math.round((counter/(_totalRows+0.0))*100.0*100.0)/100.0 + "%\r");
                            }
                        }
                    }
                    currDate.setHours(0);
                    output.put(currDate, dateValues);
                }
            } else {
                throw new Exception("ERROR: There is a level component.");
            }
        } catch (IOException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.exit(-1);
                }
            }
        }
        return output;
    }
    
    public Map<Date, double[][]> LoadData(double currLevel) throws Exception {
        Map<Date, double[][]> output = new HashMap<Date, double[][]> ();

        NetcdfFile dataFile = null;
        try {
            dataFile = NetcdfFile.open(_inputFile, null);
            Variable varOfInterest = dataFile.findVariable(_variableOfInterest);

            double counter = 0;
            if (__levels.size() > 0) {
                   
                int dim1length = varOfInterest.getDimension(0).getLength();
                int dim2length = varOfInterest.getDimension(1).getLength();
                int dim3length = varOfInterest.getDimension(2).getLength();
                int dim4length = varOfInterest.getDimension(3).getLength();
                double totalRows = dim1length * dim2length * dim3length * dim4length;
                String dim1name = varOfInterest.getDimension(0).getFullName();
                String dim2name = varOfInterest.getDimension(1).getFullName();
                String dim3name = varOfInterest.getDimension(2).getFullName();
                String dim4name = varOfInterest.getDimension(3).getFullName();
                    
                int location1 = -1;
                int location2 = -1;
                int location3 = -1;
                int location4 = -1;
                int locations[] = new int[this._dimNames.length];
                for (int i = 0; i < locations.length; i++) {
                    locations[i] = -1;
                }
                
                int levelSource = this.__levels.get(currLevel).getValue0();
                int[] origin;
                int[] size;

                // Level
                if (dim1name.toUpperCase().equals("LEVEL")){
                    location1 = levelSource;
                    origin = new int[]{location1,0,0,0};
                    size = new int[]{
                        1, 
                        varOfInterest.getDimension(1).getLength(), 
                        varOfInterest.getDimension(2).getLength(), 
                        varOfInterest.getDimension(3).getLength()}; 
                } else if (dim2name.toUpperCase().equals("LEVEL")) {
                    location2 = levelSource;
                    origin = new int[]{0,location2,0,0};
                    size = new int[]{
                        varOfInterest.getDimension(0).getLength(), 
                        1,
                        varOfInterest.getDimension(2).getLength(), 
                        varOfInterest.getDimension(3).getLength()}; 
                } else if (dim3name.toUpperCase().equals("LEVEL")) {
                    location3 = levelSource;
                    origin = new int[]{0,0,location3,0};
                    size = new int[]{
                        varOfInterest.getDimension(0).getLength(),
                        varOfInterest.getDimension(1).getLength(),
                        1, 
                        varOfInterest.getDimension(3).getLength()}; 
                } else if (dim4name.toUpperCase().equals("LEVEL")) {
                    location4 = levelSource;
                    origin = new int[]{0,0,0,location4};
                    size = new int[]{
                        varOfInterest.getDimension(0).getLength(),
                        varOfInterest.getDimension(1).getLength(),
                        varOfInterest.getDimension(2).getLength(),
                        1}; 
                } else {
                    throw new Exception("LEVEL Parameter not found");
                }
                ArrayFloat.D4 variableArray = (ArrayFloat.D4) varOfInterest.read(origin, size);

                for (Date currDate : this.__dates.keySet().stream().sorted().collect(Collectors.toList())) {
                    int dateSource = this.__dates.get(currDate).getValue0();
                    double[][] dateValues = new double[this.__lats.size()][this.__lons.size()];

                    // Date
                    for (int i = 0; i < locations.length; i++) {
                        if (this._dimNames[i].toUpperCase().equals(_timeVariable)) {
                            locations[i] = dateSource;
                        }
                    }

                    for (Double currLat : this.__lats.keySet()) {
                        int latSource = this.__lats.get(currLat).getValue0();
                        int latDest   = this.__lats.get(currLat).getValue1();

                        // Lat
                        for (int i = 0; i < locations.length; i++) {
                            if (this._dimNames[i].toUpperCase().equals("LAT")) {
                                locations[i] = latSource;
                            }
                        }

                        for (Double currLon : this.__lons.keySet()) {
                            int lonSource = this.__lons.get(currLon).getValue0();
                            int lonDest   = this.__lons.get(currLon).getValue1();

                            counter++;

                            // Lon
                            for (int i = 0; i < locations.length; i++) {
                                if (this._dimNames[i].equals("LON")) {
                                    locations[i] = lonSource;
                                }
                            }

                            // Variable of interest
                            double value = variableArray.get(locations[0], locations[1], locations[2], locations[3]);
                            dateValues[latDest][lonDest] = value;
                            if (counter % 10000 == 0) {
                                System.out.print("Complete: " + Math.round((counter/totalRows)*100.0*100.0)/100.0 + "%\r");
                            }
                        }
                    }
                    output.put(currDate, dateValues);
                }
            } 
        } catch (IOException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }  finally {
            if (dataFile != null) {
                try {
                    dataFile.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.exit(-1);
                }
            }
        }
        return output;
    }
}

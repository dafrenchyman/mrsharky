/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.climateDatabase.DbDate;
import com.mrsharky.climateDatabase.GridBox;
import com.mrsharky.climateDatabase.DbLevel;
import com.mrsharky.climateDatabase.ClimateDatabase;
import com.mrsharky.climateDatabase.SqlStatements;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayDouble;
import ucar.ma2.InvalidRangeException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

/**
 *
 * @author Julien Pierret
 */
public class NetCdfParser {
    
    private DBI _dbi;
    private Map<String, Integer> _dateMap;
    private Map<String, Integer> _levelMap;
    private Map<Double, Map<Double, Integer>> _gridBoxMap;
    private Map<Double, String> _levelToNameMap;
    private Map<String, Double> _nameToLevelMap;
    private String [] _dates;
    private Double[] _lats;
    private Double[] _lons;
    private String[] _levels;
    private String _inputFile;
    private String _variableOfInterest;
    private String _timeVariable;
    private ClimateDatabase _climateDatabase;
    
    private Map<Double, Map<Double, Integer>> GetGridBoxMapFromDb() {
        Map<Double, Map<Double, Integer>> gridBoxMap = new HashMap<Double, Map<Double, Integer>>();
        {
            Handle h = this._dbi.open();    
            SqlStatements g = h.attach(SqlStatements.class);
            List<GridBox> gridBoxList = g.GetGridBoxList();
            for (GridBox currGrid : gridBoxList)
            {
                if (!gridBoxMap.containsKey(currGrid.Lat)) {
                    gridBoxMap.put(currGrid.Lat, new HashMap<Double, Integer>());
                }
                if (!gridBoxMap.get(currGrid.Lat).containsKey(currGrid.Lon)) {
                    gridBoxMap.get(currGrid.Lat).put(currGrid.Lon, currGrid.GridBox_ID);
                }
            }
            h.close();
        }
        return gridBoxMap;
    }
    
    private Map<String, Integer> GetLevelMapFromDb() {
        Map<String, Integer> levelMap = new HashMap<String, Integer>();
        {
            Handle h = this._dbi.open();    
            SqlStatements g = h.attach(SqlStatements.class);
            List<DbLevel> dbLevelList = g.GetDbLevel();
            for (DbLevel currLevel : dbLevelList) {
                if (!levelMap.containsKey(currLevel.Level)) {
                    levelMap.put(currLevel.Level, currLevel.Level_ID);
                }
            }
            h.close();
        }
        return levelMap;
    }
    
    
    private String [] GetLevelsFromVariable(Variable dimVar) throws Exception {
        this._levelToNameMap = new HashMap<Double, String>();
        this._nameToLevelMap = new HashMap<String, Double>();
        String levelUnits = dimVar.getUnitsString();
        ArrayFloat.D1 levelArray = (ArrayFloat.D1) dimVar.read();
        String [] levels = new String [levelArray.getShape()[0]];
        System.out.println("Found Levels: " + levels.length);
        for (int counter = 0; counter < levels.length; counter++)
        {
            double currLevel = levelArray.get(counter);
            levels[counter] = currLevel + " " + levelUnits;
            _levelToNameMap.put(currLevel, levels[counter]);
            _nameToLevelMap.put(levels[counter], currLevel);
        }
        return levels;
    }
    
    private Double [] GetLonsFromVariable(Variable dimVar) throws Exception {
        String lonUnits = dimVar.getUnitsString();
        Double[] lons = new Double[0];
        if (lonUnits.equals("degrees_east")) {
            ArrayFloat.D1 lonArray = (ArrayFloat.D1) dimVar.read();
            lons = new Double [lonArray.getShape()[0]];
            System.out.println("Found Longitudes: " + lons.length);
            for (int counter = 0; counter < lons.length; counter++) {
                double currLon = lonArray.get(counter);
                lons[counter] = currLon > 180 ? currLon-360 : currLon;
            }
        } else {
            throw new Exception("LonUnits: " + lonUnits + " is unknown");
        }
        return lons;
    }
    
    private Double [] GetLatsFromVariable(Variable dimVar) throws Exception {
        String latUnits = dimVar.getUnitsString();
        Double[] lats = new Double[0];
        if (latUnits.equals("degrees_north"))
        {
            ArrayFloat.D1 latArray = (ArrayFloat.D1) dimVar.read();
            lats = new Double [latArray.getShape()[0]];
            System.out.println("Found Latidutes: " + lats.length);
            for (int counter = 0; counter < lats.length; counter++)
            {
                double currValue = latArray.get(counter);
                lats[counter] = currValue;
            }
        } else {
            throw new Exception("LonUnits: " + latUnits + " is unknown");
        }
        return lats;
    }
    
    private String [] GetDatesFromVariable(Variable dimVar) throws Exception {
        String [] dates = new String[0];
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
        String timeUnits = dimVar.getUnitsString();
        // Take care of different units we may see
        if (timeUnits.equals("hours since 1800-1-1 00:00:0.0")) {
            String[] splitUnits = timeUnits.split(" ")[2].split("-");
            String year = splitUnits[0];
            String month = StringUtils.leftPad(splitUnits[1], 2, "0");
            String day = StringUtils.leftPad(splitUnits[2], 2, "0");

            String inputStr = year + "-" + month + "-" + day;
            Date originDate = dateFormat.parse(inputStr);

            ArrayDouble.D1 timeArray = (ArrayDouble.D1) dimVar.read();
            dates = new String [timeArray.getShape()[0]];

            System.out.println("Found Dates: " + dates.length);
            for (int counter = 0; counter < dates.length; counter++)
            {
                int additionalHours = (int) timeArray.get(counter);
                Date currDate = DateUtils.addHours(originDate, additionalHours);
                dates[counter] = dateFormat.format(currDate);
            }
        } else if (timeUnits.equals("days since 1800-1-1 00:00:0.0")) {
            String[] splitUnits = timeUnits.split(" ")[2].split("-");
            String year = splitUnits[0];
            String month = StringUtils.leftPad(splitUnits[1], 2, "0");
            String day = StringUtils.leftPad(splitUnits[2], 2, "0");

            String inputStr = year + "-" + month + "-" + day;
            Date originDate = dateFormat.parse(inputStr);

            ArrayDouble.D1 timeArray = (ArrayDouble.D1) dimVar.read();
            dates = new String [timeArray.getShape()[0]];

            System.out.println("Found Dates: " + dates.length);
            for (int counter = 0; counter < dates.length; counter++)
            {
                int additionalDays = (int) timeArray.get(counter);
                Date currDate = DateUtils.addDays(originDate, additionalDays);
                dates[counter] = dateFormat.format(currDate);
            }
        } else {
            throw new Exception("TimeUnits: " + timeUnits + " is unknown");
        }
        return dates;
    }
    
    public NetCdfParser(
            String databaseLink, String databaseUsername, String databasePassword,
            String inputFile, String variableOfInterest, String timeVariable)
    {
        // Get database info
        _inputFile = inputFile;
        _variableOfInterest = variableOfInterest;
        _timeVariable = timeVariable;
        
        DBI dbi = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            dbi = new DBI(databaseLink, databaseUsername, databasePassword);
            this._dbi = dbi;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(NetCdfParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        
        _climateDatabase = new ClimateDatabase(dbi);
        
        String variableOfInterestUnits = "";
        String timeUnits = "";

        Map<Integer, String> dimValues = new HashMap<Integer, String>();
        _dates = new String[0];
        List<String> dateList = new ArrayList<String>();
        _lats = new Double[0];
        List<Double> latList = new ArrayList<Double>();
        _lons = new Double[0];
        List<Double> lonList = new ArrayList<Double>();
        _levels = new String[0];
        List<String> levelList = new ArrayList<String>();
        
        int latDimLocation;
        int lonDimLocation;
        int dateDimLocation;
        int levelDimLocation;
        
        // Open the file and check to make sure it's valid.
        NetcdfFile dataFile = null;

        try {
            System.out.println("Loading NetCDF file: " + _inputFile);
            dataFile = NetcdfFile.open(_inputFile, null);

            List<Variable> allVariables = dataFile.getVariables();
            System.out.println("Listing all available variables:");
            for (Variable currVar : allVariables) {
                System.out.print(currVar.getFullName() + "; ");
            }
            
            System.out.println("\nFinding all parameters");
            for (Variable findVar : allVariables) {
                if (findVar.getFullName().toUpperCase().equals(variableOfInterest.toUpperCase())) {
                    Variable currVar = dataFile.findVariable(variableOfInterest);
                    variableOfInterestUnits = currVar.getUnitsString();
                    currVar.getDimensions().size();
                    
                    List<Dimension> dims = currVar.getDimensions();
                    
                    for (int dimCounter = 0; dimCounter < dims.size(); dimCounter++) {
                        Handle h = dbi.open();
                        Dimension dim = dims.get(dimCounter);
                        String currName = dim.getFullNameEscaped();
                        Variable dimVar = dataFile.findVariable(currName);
                        
                        // Dealing with date values
                        if (currName.toUpperCase().equals(timeVariable.toUpperCase())) {
                            dateDimLocation = dimCounter;
                            _dates = GetDatesFromVariable(dimVar);
                            SqlStatements g = h.attach(SqlStatements.class);
                            dateList = new ArrayList<String>(Arrays.asList(_dates));
                            g.InsertDate(dateList);
                        // Dealing with lat values
                        } else if (currName.toUpperCase().equals("LAT")) {        
                            latDimLocation = dimCounter;
                            _lats = GetLatsFromVariable(dimVar);
                            SqlStatements g = h.attach(SqlStatements.class);
                            latList = new ArrayList<Double>(Arrays.asList(_lats));
                            g.InsertLat(latList);
                        // Dealing with lon values
                        } else if (currName.toUpperCase().equals("LON")) {   
                            lonDimLocation = dimCounter;
                            _lons = GetLonsFromVariable(dimVar);
                            SqlStatements g = h.attach(SqlStatements.class);
                            lonList = new ArrayList<Double>(Arrays.asList(_lons));
                            g.InsertLon(lonList);
                        // Dealing with different levels
                        } else if (currName.toUpperCase().equals("LEVEL")) {   
                            levelDimLocation = dimCounter;
                            _levels = GetLevelsFromVariable(dimVar);
                            SqlStatements g = h.attach(SqlStatements.class);
                            levelList = new ArrayList<String>(Arrays.asList(_levels));
                            g.InsertLevel(levelList);
                        }
                        h.close();
                    }
                    
                    // Add in all the lat/lon pairs
                    if (_lons.length > 0 && _lats.length > 0 ) {
                        for (Double currLat : latList) {
                            Handle h = dbi.open();
                            SqlStatements g = h.attach(SqlStatements.class);
                            g.InsertLatLon(currLat, lonList);
                            h.close();
                        }
                    }
                    
                    // Get the Gridbox Info
                    _gridBoxMap = GetGridBoxMapFromDb();
                    
                    // Get the Date Info
                    _dateMap = _climateDatabase.GetDateMapFromDb(this._dbi);
                    
                    // Get the Level Info
                    _levelMap = GetLevelMapFromDb();
                }
            }
        } catch (java.io.IOException e) {
            System.out.println(" fail = " + e);
            e.printStackTrace();
            System.exit(-1);
        } catch (ParseException ex) {
                Logger.getLogger(NetCdfParser.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            } catch (Exception ex) {
                Logger.getLogger(NetCdfParser.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            } finally {
        if (dataFile != null)
            try {
                dataFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(-1);
            }
        }
    }
    
    
    public void ProcessToCsvFile(String outputFile) {
        boolean sendToDb = false;
        NetcdfFile dataFile = null;
        try {
            dataFile = NetcdfFile.open(_inputFile, null);
            Variable varOfInterest = dataFile.findVariable(_variableOfInterest);

            OutputStream outputStream  = new FileOutputStream(outputFile);
            Writer outputStreamWriter = new OutputStreamWriter(outputStream);
            
            double counter = 0;
            if (_levels.length == 0) {
                ArrayFloat.D3 variableArray = (ArrayFloat.D3) varOfInterest.read();

                int variableDim[] = variableArray.getShape();
                double totalRows = variableDim[0] * variableDim[1] * variableDim[2];
                String dim1name = varOfInterest.getDimension(0).getFullName();
                String dim2name = varOfInterest.getDimension(1).getFullName();
                String dim3name = varOfInterest.getDimension(2).getFullName();

                for (int i = 0; i < variableDim[0]; i++) {
                    for (int j = 0; j < variableDim[1]; j++) {
                        List<Integer> datesToDb = new ArrayList<Integer>();
                        List<Float> valuesToDb = new ArrayList<Float>();
                        List<Integer> gridIdToDb = new ArrayList<Integer>();
                        List<Integer> datasetIdToDb = new ArrayList<Integer>();
                        for (int k = 0; k < variableDim[2]; k++)
                        {
                            counter++;
                            StringBuilder sb = new StringBuilder();
                            // Date
                            String date = "";
                            if (dim1name.equals(_timeVariable)){
                                date = _dates[i];
                            } else if (dim2name.equals(_timeVariable)) {
                                date = _dates[j];
                            } else if (dim3name.equals(_timeVariable)) {
                                date = _dates[k];
                            }
                            int currDate = _dateMap.get(date);
                            datesToDb.add(currDate);

                            // Lat
                            double lat = 0 ;
                            if (dim1name.toUpperCase().equals("LAT")){
                                lat = _lats[i];
                            } else if (dim2name.toUpperCase().equals("LAT")){
                                lat = _lats[j];
                            } else if (dim3name.toUpperCase().equals("LAT")){
                                lat = _lats[k];
                            }

                            // Lon
                            double lon = 0;
                            if (dim1name.toUpperCase().equals("LON")){
                                lon = _lons[i];
                            } else if (dim2name.toUpperCase().equals("LON")){
                                lon = _lons[j];
                            } else if (dim3name.toUpperCase().equals("LON")){
                                lon = _lons[k];
                            }

                            // GridBox
                            int gridBoxID = _gridBoxMap.get(lat).get(lon);
                            gridIdToDb.add(gridBoxID);

                            int currLevel = 1;

                            datasetIdToDb.add(currLevel);

                            // Variable of interest
                            float value = variableArray.get(i, j, k);
                            valuesToDb.add(value);

                            // processes the sb
                            sb.append(currLevel + "," + gridBoxID + "," + currDate + "," + value + "\n");
                            outputStreamWriter.append(sb);
                        }
                        if (sendToDb) {
                            Handle h = this._dbi.open();
                            SqlStatements g = h.attach(SqlStatements.class);
                            g.InsertGridData(datasetIdToDb, gridIdToDb, datesToDb, valuesToDb);
                            h.close();
                        }
                    }
                    System.out.print("Complete: " + Math.round((counter/totalRows)*100.0*100.0)/100.0 + "%\r");
                }
            } else if (_levels.length > 0) {
                int dim1length = varOfInterest.getDimension(0).getLength();
                int dim2length = varOfInterest.getDimension(1).getLength();
                int dim3length = varOfInterest.getDimension(2).getLength();
                int dim4length = varOfInterest.getDimension(3).getLength();
                double totalRows = dim1length * dim2length * dim3length * dim4length;
                String dim1name = varOfInterest.getDimension(0).getFullName();
                String dim2name = varOfInterest.getDimension(1).getFullName();
                String dim3name = varOfInterest.getDimension(2).getFullName();
                String dim4name = varOfInterest.getDimension(3).getFullName();

                for (int i = 0; i < dim1length; i++) {
                    int[] origin = new int[]{i,0,0,0};
                    int[] size = new int[]{1, 
                        varOfInterest.getDimension(1).getLength(), 
                        varOfInterest.getDimension(2).getLength(), 
                        varOfInterest.getDimension(3).getLength()};

                    ArrayFloat.D4 variableArray = (ArrayFloat.D4) varOfInterest.read(origin, size);
                    List<Integer> datesToDb = new ArrayList<Integer>();
                    List<Float> valuesToDb = new ArrayList<Float>();
                    List<Integer> gridIdToDb = new ArrayList<Integer>();
                    List<Integer> datasetIdToDb = new ArrayList<Integer>();
                    String level = "";
                    for (int j = 0; j < dim2length; j++) {
                        for (int k = 0; k < dim3length; k++) {
                            for (int l = 0; l < dim4length; l++) {
                                counter++;
                                StringBuilder sb = new StringBuilder();
                                // Date
                                String date = "";
                                if (dim1name.equals(_timeVariable)){
                                    date = _dates[i];
                                } else if (dim2name.equals(_timeVariable)) {
                                    date = _dates[j];
                                } else if (dim3name.equals(_timeVariable)) {
                                    date = _dates[k];
                                } else if (dim4name.equals(_timeVariable)) {
                                    date = _dates[l];
                                }
                                int currDate = _dateMap.get(date);
                                datesToDb.add(currDate);

                                // Lat
                                double lat = 0 ;
                                if (dim1name.toUpperCase().equals("LAT")){
                                    lat = _lats[i];
                                } else if (dim2name.toUpperCase().equals("LAT")){
                                    lat = _lats[j];
                                } else if (dim3name.toUpperCase().equals("LAT")){
                                    lat = _lats[k];
                                } else if (dim4name.toUpperCase().equals("LAT")){
                                    lat = _lats[l];
                                }

                                // Lon
                                double lon = 0;
                                if (dim1name.toUpperCase().equals("LON")){
                                    lon = _lons[i];
                                } else if (dim2name.toUpperCase().equals("LON")){
                                    lon = _lons[j];
                                } else if (dim3name.toUpperCase().equals("LON")){
                                    lon = _lons[k];
                                } else if (dim4name.toUpperCase().equals("LON")){
                                    lon = _lons[l];
                                }

                                // GridBox
                                int gridBoxID = _gridBoxMap.get(lat).get(lon);
                                gridIdToDb.add(gridBoxID);

                                // Level
                                if (dim1name.toUpperCase().equals("LEVEL")){
                                    level = _levels[i];
                                } else if (dim2name.toUpperCase().equals("LEVEL")){
                                    level = _levels[j];
                                } else if (dim3name.toUpperCase().equals("LEVEL")){
                                    level = _levels[k];
                                } else if (dim4name.toUpperCase().equals("LEVEL")){
                                    level = _levels[l];
                                }
                                int currLevel = _levelMap.get(level);

                                datasetIdToDb.add(currLevel);

                                // Variable of interest
                                float value = variableArray.get(0, j, k, l);
                                valuesToDb.add(value);
                                
                                // processes the sb
                                sb.append(currLevel + "," + gridBoxID + "," + currDate + "," + value + "\n");
                                outputStreamWriter.append(sb);
                            }
                        }
                    }
                    System.out.print("Complete: " + Math.round((counter/totalRows)*100.0*100.0)/100.0 + "%\r");
                    if (sendToDb)  {
                        Handle h = this._dbi.open();
                        SqlStatements g = h.attach(SqlStatements.class);
                        g.InsertGridData(datasetIdToDb, gridIdToDb, datesToDb, valuesToDb);
                        h.close();
                    }
                }
            }
            outputStreamWriter.close();        
        } catch (IOException ex) {
            Logger.getLogger(NetCdfParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NetCdfParser.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
    }
    
    public static void main(String args[]) throws Exception {
        
        String databaseUrl, databaseUsername, databasePassword;
        String inputFile, outputFile;
        String variableOfInterest, timeVariable;
        
        databaseUrl = databaseUsername = databasePassword = 
                inputFile = outputFile = variableOfInterest = timeVariable = "";

        for (int argsCounter = 0; argsCounter < args.length; argsCounter++) {
            String currArg = args[argsCounter];
            switch (currArg.toUpperCase()) {
                case "-INPUT":
                    inputFile = args[++argsCounter];
                    break;
                case "-OUTPUT":
                    outputFile = args[++argsCounter];
                    break;
                case "-DATABASEURL":
                    databaseUrl = args[++argsCounter];
                    break;
                case "-DATABASEUSERNAME":
                    databaseUsername = args[++argsCounter];
                    break;
                case "-DATABASEPASSWORD":
                    databasePassword = args[++argsCounter];
                    break;
                case "-VARIABLEOFINTEREST":
                    variableOfInterest = args[++argsCounter];
                    break;
                case "-TIMEVARIABLE":
                    timeVariable = args[++argsCounter];
                    break;
                default:
                    throw new IllegalArgumentException("Invalid input argument: " + currArg);
            }
        }
        
        if (timeVariable.length() > 0 && variableOfInterest.length() > 0 && 
                databaseUsername.length() > 0 && databaseUrl.length() > 0 && outputFile.length() > 0 &&
                inputFile.length() > 0)
        {
            NetCdfParser parser = new NetCdfParser(
                        databaseUrl, databaseUsername, databasePassword,
                        inputFile, variableOfInterest, timeVariable);
            parser.ProcessToCsvFile(outputFile);
        } else {
             throw new IllegalArgumentException("Problem with the input argumments");
        }
        
        if (false) {
            NetCdfParser parser = new NetCdfParser(
                    "jdbc:mysql://192.168.10.136/Ncep20CenReAV2c_Monthlies_pressure_air_mon_mean", "root", "plzf1xme",
                    "Z:\\PhD\\Reboot\\Projects\\GriddedDatabase\\Data\\Monthlies\\pressure\\air.mon.mean.nc", 
                    "air", "time");

            parser.ProcessToCsvFile("Z:\\PhD\\Reboot\\Projects\\GriddedDatabase\\Data\\Monthlies\\pressure\\air.mon.mean.csv");
        }
        
        if (false) {
            NetCdfParser parser = new NetCdfParser(
                    "jdbc:mysql://192.168.10.136/GriddedClimateData", "root", "plzf1xme",
                    "E:\\Dropbox\\Dropbox\\PhD\\Reboot\\Projects\\Ncep20thCenturyReanalysisV2c\\Data\\Monthlies\\gaussian\\monolevel\\air.2m.mon.mean.nc", 
                    "air", "time");

            parser.ProcessToCsvFile("E:\\Dropbox\\Dropbox\\PhD\\Reboot\\Projects\\Ncep20thCenturyReanalysisV2c\\Data\\Monthlies\\gaussian\\monolevel\\air.2m.mon.mean.csv");
        }
        System.exit(0);
    }
}

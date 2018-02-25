/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor.old;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransformLogDiv_old;
import com.mrsharky.climateDatabase.GridData;
import com.mrsharky.dataAnalysis.LoadData_svdTest;
import com.mrsharky.dataAnalysis.PcaCovJBlas;
import com.mrsharky.dataprocessor.NetCdfDateEntry;
import com.mrsharky.dataprocessor.NetCdfEntry;
import com.mrsharky.dataprocessor.SphericalHarmonics_InputParser;
import com.mrsharky.helpers.DoubleArray;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;


import static com.mrsharky.helpers.DoubleArray.Print;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import static com.mrsharky.helpers.DoubleArray.Print;
import com.mrsharky.helpers.JblasMatrixHelpers;
import static com.mrsharky.helpers.JblasMatrixHelpers.ApacheMath3ToJblas;
import static com.mrsharky.helpers.JblasMatrixHelpers.JblasToOjalgo;
import static com.mrsharky.helpers.JblasMatrixHelpers.Print;
import org.apache.commons.math3.complex.Complex;
import org.jblas.ComplexDoubleMatrix;
import org.ojalgo.matrix.store.ComplexDenseStore;

/**
 *
 * @author Julien Pierret
 */
public class NetCdfLoader {
    
    private String[] _dates;
    private Double[] _lats;
    private Double[] _lons;
    private String[] _levels;
    private String _inputFile;
    private String _variableOfInterest;
    private String _timeVariable;
    
    private String [] GetLevelsFromVariable(Variable dimVar) throws Exception {
        String levelUnits = dimVar.getUnitsString();
        ArrayFloat.D1 levelArray = (ArrayFloat.D1) dimVar.read();
        String [] levels = new String [levelArray.getShape()[0]];
        System.out.println("Found Levels: " + levels.length);
        for (int counter = 0; counter < levels.length; counter++)
        {
            double currLevel = levelArray.get(counter);
            levels[counter] = currLevel + " " + levelUnits;
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
            for (int counter = 0; counter < dates.length; counter++) {
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
    
    public NetCdfLoader(String inputFile, String variableOfInterest, String timeVariable) {
        // Get database info
        _inputFile = inputFile;
        _variableOfInterest = variableOfInterest;
        _timeVariable = timeVariable;
        
        String variableOfInterestUnits = "";
        String timeUnits = "";

        _dates = new String[0];
        _lats = new Double[0];
        _lons = new Double[0];
        _levels = new String[0];
        
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
                        Dimension dim = dims.get(dimCounter);
                        String currName = dim.getFullNameEscaped();
                        Variable dimVar = dataFile.findVariable(currName);
                        
                        // Dealing with date values
                        if (currName.toUpperCase().equals(timeVariable.toUpperCase())) {
                            _dates = GetDatesFromVariable(dimVar);
                        // Dealing with lat values
                        } else if (currName.toUpperCase().equals("LAT")) {        
                            _lats = GetLatsFromVariable(dimVar);
                        // Dealing with lon values
                        } else if (currName.toUpperCase().equals("LON")) {   
                            _lons = GetLonsFromVariable(dimVar);
                        // Dealing with different levels
                        } else if (currName.toUpperCase().equals("LEVEL")) {   
                            _levels = GetLevelsFromVariable(dimVar);
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
        if (dataFile != null)
            try {
                dataFile.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(-1);
            }
        }
    }
    
    public List<NetCdfEntry> LoadData() {
        List<NetCdfEntry> output = new ArrayList<NetCdfEntry>();

        NetcdfFile dataFile = null;
        try {
            dataFile = NetcdfFile.open(_inputFile, null);
            Variable varOfInterest = dataFile.findVariable(_variableOfInterest);

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

                            // Variable of interest
                            float value = variableArray.get(i, j, k);
                            
                            NetCdfEntry currEntry = new NetCdfEntry(date, lat, lon, value);
                            output.add(currEntry);
                        }
                    }
                    if (counter % 100 == 0) {
                        System.out.print("Complete: " + Math.round((counter/totalRows)*100.0*100.0)/100.0 + "%\r");
                    }
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

                                // Variable of interest
                                float value = variableArray.get(0, j, k, l);
                                
                                // processes the sb
                            }
                        }
                    }
                    System.out.print("Complete: " + Math.round((counter/totalRows)*100.0*100.0)/100.0 + "%\r");
                }
            } 
        } catch (IOException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        } catch (InvalidRangeException ex) {
            Logger.getLogger(NetCdfLoader.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }     
        return output;
    }
    
    
    public static void main(String args[]) throws Exception {   
        SphericalHarmonics_InputParser inputParser = new SphericalHarmonics_InputParser(args, NetCdfLoader.class.getName());
        if (inputParser.InputsCorrect()) {
            NetCdfLoader loader = new NetCdfLoader(inputParser.input, inputParser.variable, inputParser.time);
            List<NetCdfEntry> values = loader.LoadData();
            
            Comparator<NetCdfEntry> comparator = Comparator.comparing(entry -> entry.date);
            comparator = comparator.thenComparing(Comparator.comparing(entry -> -1*entry.lat));
            comparator = comparator.thenComparing(Comparator.comparing(entry -> entry.lon));
            
            values = values.stream().sorted(comparator).collect(Collectors.toList());
            
            NetCdfDateEntry allData = new NetCdfDateEntry(values);
            double[][] currDateData = allData.GetDateData(0);
            //Print(currDateData);
            
            DateFormat format = new SimpleDateFormat("yyyy-mm-dd");
            currDateData = allData.GetDateData(format.parse("1851-01-01"));
            //Print(currDateData);
            
            Date baselineLower = format.parse("1960-12-31");
            Date baselineUpper = format.parse("1990-12-31");
            
            boolean normalize = true;
            for (int month = 1; month <= 12; month++) {
                System.out.println("Processing Anomalies for month: " + month);
                
                // Getting baseline anomaly data
                double[][] gridBoxAnomalyMean3 = new double[allData.lat.length][allData.lon.length];
                double[][] gridBoxAnomalyVariance3 = new double[allData.lat.length][allData.lon.length];
                int currMonth = month -1;
                List<Date> baselinedates = allData.valuesMap.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth).sorted()
                        .collect(Collectors.toList());
                System.out.println("Number of Baseline Dates: " + baselinedates.size());
                for (int latCounter = 0; latCounter < allData.lat.length; latCounter++) {
                    for (int lonCounter = 0; lonCounter < allData.lon.length; lonCounter++) {
                        double[] baselineValues = new double[baselinedates.size()];
                        
                        for (int dateCounter = 0; dateCounter < baselinedates.size(); dateCounter++) {
                            Date currDate = baselinedates.get(dateCounter);
                            baselineValues[dateCounter] = allData.valuesMap.get(currDate)[latCounter][lonCounter];
                        }

                        DescriptiveStatistics ds = new DescriptiveStatistics(baselineValues);
                        gridBoxAnomalyMean3[latCounter][lonCounter] = ds.getMean();
                        gridBoxAnomalyVariance3[latCounter][lonCounter] = ds.getVariance();
                    }
                }

                // Modify the gridbox values by subtracting out the anomaly and normalizing
                List<Date> dataDates = allData.valuesMap.keySet().stream()
                        .filter(data -> data.getMonth() == currMonth)
                        .collect(Collectors.toList());
                for (int dateCounter = 0; dateCounter < dataDates.size(); dateCounter++) {
                    Date currDate = dataDates.get(dateCounter);
                    double[][] newValues = new double[allData.lat.length][allData.lon.length];
                    double[][] oldValues = allData.valuesMap.get(currDate);
                    for (int latCounter = 0; latCounter < allData.lat.length; latCounter++) {
                        for (int lonCounter = 0; lonCounter < allData.lon.length; lonCounter++) {
                            double anomaly = gridBoxAnomalyMean3[latCounter][lonCounter];
                            double variance = gridBoxAnomalyVariance3[latCounter][lonCounter];
                            double oldValue = oldValues[latCounter][lonCounter];
                            if (normalize) {
                                newValues[latCounter][lonCounter] = (oldValue - anomaly)/Math.sqrt(variance);
                            } else {
                                newValues[latCounter][lonCounter] = oldValue - anomaly;
                            }
                        }
                    }
                    allData.valuesMap.put(currDate, newValues);
                }
                
                currDateData = allData.valuesMap.get(dataDates.get(0));
                //Print(currDateData);   
            }
            
            
            for (int month = 1; month <= 12; month++) {
                System.out.println("Converting from Spatial to Spectral for month: " + month);
                int currMonth = month -1;
                List<Date> dates = allData.valuesMap.keySet().stream()
                        .filter(date -> date.after(baselineLower) && date.before(baselineUpper) && date.getMonth() == currMonth).sorted()
                        .collect(Collectors.toList());
                
                // Convert data from spatial to spectral
                int Q = 100;
                int M = 0;
                int N = 0;
                int numOfQpoints = (int) Math.pow(Q+1, 2.0);
                Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
                for (int dateCounter = 0; dateCounter < dates.size(); dateCounter++) {
                    Date currDate = dates.get(dateCounter);

                    System.out.println("Processing date: " + currDate);

                    double[][] currData2 = allData.valuesMap.get(currDate);

                    try {
                        DiscreteSphericalTransformLogDiv_old dst = new DiscreteSphericalTransformLogDiv_old(currData2, Q, true);
                        Complex[] spectral = dst.GetSpectraCompressed();
                        M = dst.GetM();
                        N = dst.GetN();
                        for (int rowCounter = 0; rowCounter < numOfQpoints; rowCounter++) {
                            qRealizations[rowCounter][dateCounter] = spectral[rowCounter];
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(LoadData_svdTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                ComplexDoubleMatrix qRealizationsMatrix = ApacheMath3ToJblas(qRealizations);


                System.out.println("Processing Complex Conjugate of Qrealizations");
                ComplexDoubleMatrix Qrealizations_trans = qRealizationsMatrix.transpose().conj();
                System.out.println("qRealizationsMatrix:" + qRealizationsMatrix.rows + "," + qRealizationsMatrix.columns);
                System.out.println("Qrealizations_trans:" + Qrealizations_trans.rows + "," + Qrealizations_trans.columns);

                System.out.println("Processing R_hat_s");
                //MatrixStore<ComplexNumber> R_hat_s = (qRealizationsMatrix.multiply(Qrealizations_trans)).multiply(1/(dates.size() + 0.0));
                ComplexDoubleMatrix R_hat_s = (qRealizationsMatrix.mmul(Qrealizations_trans)).mul(1/(dates.size() + 0.0));

                System.out.println("R_hat_s:" + R_hat_s.rows + "," + R_hat_s.columns);

                System.out.println("Processing PCA");
                PcaCovJBlas pca = new PcaCovJBlas(R_hat_s);
                
                Print(pca.GetEigenValues());
                Print(pca.GetEigenVectors());
                DoubleArray.Print(pca.GetVarianceExplained());
                
                
                ComplexDenseStore eigenVectors = JblasToOjalgo(pca.GetEigenVectors());
                
                JblasMatrixHelpers.Print(R_hat_s);
                
                double[] varExplained = pca.GetVarianceExplained();
            }
            
        }
    }
}

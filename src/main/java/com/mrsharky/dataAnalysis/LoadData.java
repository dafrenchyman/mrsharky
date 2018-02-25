/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataAnalysis;

import com.mrsharky.discreteSphericalTransform.old.DiscreteSphericalTransformLogDiv_old;
import com.mrsharky.discreteSphericalTransform.old.InvDiscreteSphericalTransform_old;
import com.mrsharky.discreteSphericalTransform.MeshGrid;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.climateDatabase.GridBox;
import com.mrsharky.climateDatabase.GridData;
import com.mrsharky.climateDatabase.ClimateDatabase;
import com.mrsharky.dataprocessor.NetCdfParser;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.PcaCov_test;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.skife.jdbi.v2.DBI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import static java.lang.Class.forName;
import java.util.Collections;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.ojalgo.matrix.store.ComplexDenseStore;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.scalar.ComplexNumber;

/**
 *
 * @author mrsharky
 */
public class LoadData {
    private DBI _dbi;
    private ClimateDatabase _climateDatabase;
    
    public LoadData() {
        DBI dbi = null;
        try {
            String databaseLink =  "jdbc:mysql://192.168.10.191/Ncp20CRA2c_Mon_gaus_mono_air_sfc_mon_mean";
            String databaseUsername = "root";
            String databasePassword = "plzf1xme";
            
            Class.forName("com.mysql.jdbc.Driver");
            dbi = new DBI(databaseLink, databaseUsername, databasePassword);
            this._dbi = dbi;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LoadData.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        _climateDatabase = new ClimateDatabase(dbi);
        
        int month = 1;
        DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY-MM-dd");
        String baselineLower = "1961-01-01";
        String baselineUpper = "1990-12-31";
        boolean normalize = true;
        
        DateTime dateLower   = formatter.parseDateTime(baselineLower);
        DateTime dateUpper   = formatter.parseDateTime(baselineUpper);
        
        // Get all the available dates
        List<String> dates = _climateDatabase.GetDateListFromDb(dbi);
        
        // If we're restricting our data to a certain month, remove out the other months
        if (month > 0) {           
            Iterator<String> dateIterator = dates.iterator();
            while (dateIterator.hasNext()) {
                String currDate = dateIterator.next();
                DateTime dt = formatter.parseDateTime(currDate);
                if (dt.getMonthOfYear() != month) {
                    dateIterator.remove();
                } else  if (dt.getMillis() < dateLower.getMillis() || dt.getMillis() > dateUpper.getMillis()) {
                    dateIterator.remove();
                }
            }
        }
        
        // Get all the available grid boxes
        List<GridBox> gridBoxes = _climateDatabase.GetGridBoxListFromDb(dbi);
         
        // Create an object to store all the gridbox values (GridBoxId, Date, GridData)
        Map<Integer, Map<String, GridData>> gridBoxValues = new HashMap<Integer, Map<String, GridData>>();
        Map<Integer, Map<String, GridData>> anomalyGridBoxValues = new HashMap<Integer, Map<String, GridData>>();
        
        //  Lat,        Lon,        Date    GridData
        Map<Double, Map<Double, Map<String, GridData>>> gridBoxValues2 = new HashMap<Double, Map<Double, Map<String, GridData>>>();
        Map<Double, Map<Double, Map<String, GridData>>> anomalyGridBoxValues2 = new HashMap<Double, Map<Double, Map<String, GridData>>>();
        
        // Greate objects to store the lat/lon values
        List<Double> lats = new ArrayList<Double>();
        List<Double> lons = new ArrayList<Double>();
        for (GridBox currGridBox : gridBoxes) {
            gridBoxValues.put(currGridBox.GridBox_ID, new HashMap<String, GridData>());
            anomalyGridBoxValues.put(currGridBox.GridBox_ID, new HashMap<String, GridData>());
            
            // Get all the Lat & Lon values
            double currLat = currGridBox.Lat;
            double currLon = currGridBox.Lon;
            if (!lats.contains(currLat)) {
                lats.add(currLat);
            }
            
            if (!lons.contains(currLon)) {
                lons.add(currLon);
            }
            
            if (!gridBoxValues2.containsKey(currLat)) {
                gridBoxValues2.put(currLat, new HashMap<Double, Map<String, GridData>>());
                anomalyGridBoxValues2.put(currLat, new HashMap<Double, Map<String, GridData>>());
            }
            if (!gridBoxValues2.get(currLat).containsKey(currLon)) {
                gridBoxValues2.get(currLat).put(currLon, new HashMap<String, GridData>());
                anomalyGridBoxValues2.get(currLat).put(currLon, new HashMap<String, GridData>());
            }
        }
        Collections.sort(lons);
        Collections.reverse(lats);
        
        // Create a lookup, double to lon location in list
        Map<Double, Integer> lonsKey = new HashMap<Double, Integer>();
        for (int location = 0; location < lons.size(); location++) {
            double value = lons.get(location);
            lonsKey.put(value, location);
        }
        Map<Double, Integer> latsKey = new HashMap<Double, Integer>();
        for (int location = 0; location < lats.size(); location++) {
            double value = lats.get(location);
            latsKey.put(value, location);
        }
        
        
        // Create mesh grids
        MeshGrid latLonMesh = new MeshGrid(lons, lats);
        double[][] meshX = latLonMesh.GetX();
        double[][] meshY = latLonMesh.GetY();
        
        // Get all the gridbox values from the database
        Map<String, GridData[][]> gridBoxValues3 = new HashMap<String, GridData[][]>();
        Map<String, GridData[][]> anomalyGridBoxValues3 = new HashMap<String, GridData[][]>();
        
        for (String currDate : dates) {
            
            gridBoxValues3.put(currDate, new GridData[lats.size()][lons.size()]);
            anomalyGridBoxValues3.put(currDate, new GridData[lats.size()][lons.size()]);
            
            System.out.println("Processing date: " + currDate);
            List<GridData> currData = _climateDatabase.GetGridDataListFromDb(dbi, currDate, 1);
            
             
            for (GridData currGridData : currData) {
                int id = currGridData.GridBox_ID;
                double currLat = currGridData.Lat;
                double currLon = currGridData.Lon;
                gridBoxValues.get(id).put(currDate, currGridData);
                gridBoxValues2.get(currLat).get(currLon).put(currDate, currGridData);
                gridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)] = currGridData;
            }
            
            DateTime dt = formatter.parseDateTime(currDate);
            if (dt.getMillis() >= dateLower.getMillis() && dt.getMillis() <= dateUpper.getMillis()) {
                System.out.println("Processing anomaly: " + currDate);
                for (GridData currGridData : currData) {
                    int id = currGridData.GridBox_ID;
                    double currLat = currGridData.Lat;
                    double currLon = currGridData.Lon;
                    anomalyGridBoxValues.get(id).put(currDate, currGridData);
                    anomalyGridBoxValues2.get(currLat).get(currLon).put(currDate, currGridData);
                    anomalyGridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)] = currGridData;
                }
            }
        }
        
        // Calculate the mean and variance for a baseline period
        Map<Integer, Double> gridBoxAnomalyMean = new HashMap<Integer, Double>();
        Map<Integer, Double> gridBoxAnomalyVariance = new HashMap<Integer, Double>();
        Map<Double, Map<Double, Double>> gridBoxAnomalyMean2 = new HashMap<Double, Map<Double, Double>>();
        Map<Double, Map<Double, Double>> gridBoxAnomalyVariance2 = new HashMap<Double, Map<Double, Double>>();
        
        GridData[][] gridBoxAnomalyMean3 = new GridData[lats.size()][lons.size()];
        GridData[][] gridBoxAnomalyVariance3 = new GridData[lats.size()][lons.size()];
        
        for (double currLat : lats) {
            for (double currLon : lons) {     
                if (!gridBoxAnomalyMean2.containsKey(currLat)) {
                    gridBoxAnomalyMean2.put(currLat, new HashMap<Double, Double>());
                    gridBoxAnomalyVariance2.put(currLat, new HashMap<Double, Double>());
                } 
                Map<String, GridData> currGridBox = anomalyGridBoxValues2.get(currLat).get(currLon);
                double[] values = new double[currGridBox.size()];
                int counter = 0;
                for (String currDate : currGridBox.keySet()) {
                    values[counter++] = currGridBox.get(currDate).Value;
                }
                DescriptiveStatistics ds = new DescriptiveStatistics(values);
                gridBoxAnomalyMean2.get(currLat).put(currLon, ds.getMean());
                gridBoxAnomalyVariance2.get(currLat).put(currLon, ds.getVariance());
                
                GridData firstGrid = currGridBox.entrySet().iterator().next().getValue();
                GridData meanGridData = new GridData(firstGrid.GridBox_ID, firstGrid.Lat, firstGrid.Lon, ds.getMean());
                GridData varGridData = new GridData(firstGrid.GridBox_ID, firstGrid.Lat, firstGrid.Lon, ds.getMean());
                gridBoxAnomalyMean3[latsKey.get(currLat)][lonsKey.get(currLon)] = meanGridData;
                gridBoxAnomalyVariance3[latsKey.get(currLat)][lonsKey.get(currLon)] = varGridData;
            }
        }
        
        for (int currGridBoxId : anomalyGridBoxValues.keySet()) {
            Map<String, GridData> currGridBox = anomalyGridBoxValues.get(currGridBoxId);
            double[] values = new double[currGridBox.size()];
            int counter = 0;
            for (String currDate : currGridBox.keySet()) {
                values[counter++] = currGridBox.get(currDate).Value;
            }
            DescriptiveStatistics ds = new DescriptiveStatistics(values);
            gridBoxAnomalyMean.put(currGridBoxId, ds.getMean());
            gridBoxAnomalyVariance.put(currGridBoxId, ds.getVariance());
        }
         
        // Modify the gridbox values by subtracting out the anomaly and normalizing
        for (double currLat : lats) {
            for (double currLon : lons) {  
                double anomaly = gridBoxAnomalyMean3[latsKey.get(currLat)][lonsKey.get(currLon)].Value;
                double variance = gridBoxAnomalyVariance3[latsKey.get(currLat)][lonsKey.get(currLon)].Value;
                for (String currDate: gridBoxValues3.keySet()) {
                    GridData currGridBox = gridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)];
                    double oldValue = currGridBox.Value;
                    if (normalize) {
                        currGridBox.Value = (oldValue - anomaly)/Math.sqrt(variance);
                    } else {
                        currGridBox.Value = oldValue - anomaly;
                    }
                }
            }
        }

        for (int currGridBoxId : gridBoxValues.keySet()) {
            Map<String, GridData> currData = gridBoxValues.get(currGridBoxId);
            double anomaly = gridBoxAnomalyMean.get(currGridBoxId);
            double variance = gridBoxAnomalyVariance.get(currGridBoxId);
            for (String currDate: currData.keySet()) {
                GridData currGridBox = currData.get(currDate);
                double oldValue = currGridBox.Value;
                if (normalize) {
                    currGridBox.Value = (oldValue - anomaly)/Math.sqrt(variance);
                } else {
                    currGridBox.Value = oldValue - anomaly;
                }
            }
        }
        
        // Convert data from spatial to spectral
        int Q = 50;
        int M = 0;
        int N = 0;
        int numOfQpoints = (int) Math.pow(Q+1, 2.0);
        Map<String, Double[][]> spatialData = new HashMap<String, Double[][]>();
        Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
        int dateCounter = 0;
        for (String currDate : dates) {
            System.out.println("Processing date: " + currDate);
            double[][] currData = new double[lats.size()][lons.size()];
            for (int currGridBoxId : gridBoxValues.keySet()) {
                GridData currGrid = gridBoxValues.get(currGridBoxId).get(currDate);
                currData[latsKey.get(currGrid.Lat)][lonsKey.get(currGrid.Lon)] = currGrid.Value;
            }
            
            double[][] currData2 = new double[lats.size()][lons.size()];
            for (double currLat : lats) {
                for (double currLon : lons) {  
                    GridData currGrid = gridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)];
                    currData2[latsKey.get(currGrid.Lat)][lonsKey.get(currGrid.Lon)] = currGrid.Value;
                }
            }

            try {
                DiscreteSphericalTransformLogDiv_old dst = new DiscreteSphericalTransformLogDiv_old(currData2, Q, true);
                Complex[] spectral = dst.GetSpectraCompressed();
                M = dst.GetM();
                N = dst.GetN();
                for (int rowCounter = 0; rowCounter < numOfQpoints; rowCounter++) {
                    qRealizations[rowCounter][dateCounter] = spectral[rowCounter];
                }
                        
            } catch (Exception ex) {
                Logger.getLogger(LoadData.class.getName()).log(Level.SEVERE, null, ex);
            }
            dateCounter++;
        }
        
        // Now that we've built up the "Qrealizations" built R_hat_s (Equation (18)
        
        ComplexDenseStore qRealizationsMatrix = ComplexArray.ComplexToComplexDenseStore(qRealizations);
        
        System.out.println("Processing Complex Conjugate of Qrealizations");
        MatrixStore<ComplexNumber> Qrealizations_trans = qRealizationsMatrix.conjugate();
        
        System.out.println("Processing R_hat_s");
        MatrixStore<ComplexNumber> R_hat_s = (qRealizationsMatrix.multiply(Qrealizations_trans)).multiply(1/(dates.size() + 0.0));
        
        System.out.println("Processing PCA");
        PcaCov_test pca = new PcaCov_test(R_hat_s);
        ComplexDenseStore eigenVectors = pca.GetEigenVectors();
        double[] varExplained = pca.GetVarianceExplained();
        
        // Iterate over each PCA (eigenvector)
        
        DBI dbi_destination = null;
        try {
            String databaseLink =  "jdbc:mysql://192.168.10.191/jp_Mon_gaus_mono_air_sfc_mon_mean";
            String databaseUsername = "root";
            String databasePassword = "plzf1xme";
            
            Class.forName("com.mysql.jdbc.Driver");
            dbi_destination = new DBI(databaseLink, databaseUsername, databasePassword);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LoadData.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        ClimateDatabase databaseDestination = new ClimateDatabase(dbi_destination);

        ComplexNumber[][][] PSI_k_spectral = new ComplexNumber[varExplained.length][Q+1][(Q+1)*2-1];
        
        String startDate = "0001-01-01";
        DateTime dtStartDate = formatter.parseDateTime(startDate);
   
        for (int eigenCounter = 0; eigenCounter < varExplained.length; eigenCounter++) {
            int counter = 0;
            for (int k = 0; k < Q+1; k++) {
                for (int l = -k; l <= k; l++) {
                    PSI_k_spectral[eigenCounter][k][l+Q] = eigenVectors.get(counter,eigenCounter);
                    counter = counter +1;
                }
            }
            
            DateTime currDate = dtStartDate.plusMonths(eigenCounter);
            String dtStr = formatter.print(currDate);
            List<String> dateStrings = new ArrayList<String>();
            dateStrings.add(dtStr);
            databaseDestination.PutDate(dbi, dateStrings);
            
            ComplexNumber[][] curr_spectral = ComplexArray.GetData(PSI_k_spectral, eigenCounter, 
                    Utilities.minToMaxByOne(0, Q), 
                    Utilities.minToMaxByOne(0,(Q+1)*2-2) );
            // Convert Spectral to spatial
            Complex[][] curr_spectral_comp = ComplexArray.ComplexNumberToComplex(curr_spectral);
            
            try {
                InvDiscreteSphericalTransform_old invDst = new InvDiscreteSphericalTransform_old(curr_spectral_comp, M, N, true);
                Complex[][] pca_spatial = invDst.GetSpatial();
                System.out.println("Eigenvalue: " + (eigenCounter+1) );
                ComplexArray.Print(pca_spatial);
                
                List<Integer> gridBox_IDs = new ArrayList<Integer>();
                List<Float> values = new ArrayList<Float>();
                List<Integer> date_IDs = new ArrayList<Integer>();
                for (double currLat : lats) {
                    for (double currLon : lons) {
                        
                        double currValue = pca_spatial[latsKey.get(currLat)][lonsKey.get(currLon)].getReal();
                        int grid_id = gridBoxAnomalyMean3[latsKey.get(currLat)][lonsKey.get(currLon)].GridBox_ID;
                        
                        gridBox_IDs.add(grid_id);
                        date_IDs.add(eigenCounter+1);
                        values.add((float) currValue);
                        
                    }
                }
                
                // Insert into the database
                //databaseDestination.PutGridData(dbi_destination, 1, gridBox_IDs, date_IDs, values);
             
            } catch (Exception ex) {
                Logger.getLogger(LoadData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
    
    public static void main(String args[]) throws Exception {
        LoadData ld = new LoadData();
    }
}

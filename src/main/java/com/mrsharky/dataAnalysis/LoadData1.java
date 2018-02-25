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
public class LoadData1 {
    private DBI _dbi;
    private ClimateDatabase _climateDatabase;
    
    public LoadData1() {
        DBI dbi = null;
        try {
            String databaseLink =  "jdbc:mysql://192.168.10.191/Ncp20CRA2c_Mon_gaus_mono_air_sfc_mon_mean";
            String databaseUsername = "root";
            String databasePassword = "plzf1xme";
            
            Class.forName("com.mysql.jdbc.Driver");
            dbi = new DBI(databaseLink, databaseUsername, databasePassword);
            this._dbi = dbi;
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(LoadData1.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        _climateDatabase = new ClimateDatabase(dbi);
        
        for (int month = 1; month <= 12; month++) {
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
                    }
                }
            }
            
            // Only use the baseline dates
            {
                Iterator<String> dateIterator = dates.iterator();
                while (dateIterator.hasNext()) {
                    String currDate = dateIterator.next();
                    DateTime dt = formatter.parseDateTime(currDate);
                    if (dt.getMillis() < dateLower.getMillis() || dt.getMillis() > dateUpper.getMillis()) {
                        dateIterator.remove();
                    }
                }
            }

            // Get all the available grid boxes
            List<GridBox> gridBoxes = _climateDatabase.GetGridBoxListFromDb(dbi);

            // Greate objects to store the lat/lon values
            List<Double> lats = new ArrayList<Double>();
            List<Double> lons = new ArrayList<Double>();
            for (GridBox currGridBox : gridBoxes) {       
                // Get all the Lat & Lon values
                double currLat = currGridBox.Lat;
                double currLon = currGridBox.Lon;
                if (!lats.contains(currLat)) {
                    lats.add(currLat);
                }

                if (!lons.contains(currLon)) {
                    lons.add(currLon);
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
                    gridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)] = currGridData;
                }

                DateTime dt = formatter.parseDateTime(currDate);
                if (dt.getMillis() >= dateLower.getMillis() && dt.getMillis() <= dateUpper.getMillis()) {
                    System.out.println("Processing anomaly: " + currDate);
                    for (GridData currGridData : currData) {
                        int id = currGridData.GridBox_ID;
                        double currLat = currGridData.Lat;
                        double currLon = currGridData.Lon;
                        anomalyGridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)] = currGridData;
                    }
                }
            }

            // Calculate the mean and variance for a baseline period
            GridData[][] gridBoxAnomalyMean3 = new GridData[lats.size()][lons.size()];
            GridData[][] gridBoxAnomalyVariance3 = new GridData[lats.size()][lons.size()];

            for (double currLat : lats) {
                for (double currLon : lons) {     


                    double[] values = new double[dates.size()];
                    int counter = 0;
                    for (String currDate : dates) {
                        GridData currGridBox = anomalyGridBoxValues3.get(currDate)[latsKey.get(currLat)][lonsKey.get(currLon)];
                        values[counter++] = currGridBox.Value;
                    }
                    DescriptiveStatistics ds = new DescriptiveStatistics(values);

                    GridData firstGrid = anomalyGridBoxValues3.get(dates.get(0))[latsKey.get(currLat)][lonsKey.get(currLon)];

                    GridData meanGridData = new GridData(firstGrid.GridBox_ID, firstGrid.Lat, firstGrid.Lon, ds.getMean());
                    GridData varGridData = new GridData(firstGrid.GridBox_ID, firstGrid.Lat, firstGrid.Lon, ds.getMean());
                    gridBoxAnomalyMean3[latsKey.get(currLat)][lonsKey.get(currLon)] = meanGridData;
                    gridBoxAnomalyVariance3[latsKey.get(currLat)][lonsKey.get(currLon)] = varGridData;
                }
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

            // Convert data from spatial to spectral
            int Q = 100;
            int M = 0;
            int N = 0;
            int numOfQpoints = (int) Math.pow(Q+1, 2.0);
            Complex[][] qRealizations = new Complex[numOfQpoints][dates.size()];
            int dateCounter = 0;
            for (String currDate : dates) {
                System.out.println("Processing date: " + currDate);

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
                    Logger.getLogger(LoadData1.class.getName()).log(Level.SEVERE, null, ex);
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
            double cummulativeVar = 0;

            // Iterate over each PCA (eigenvector)

            DBI dbi_destination = null;
            try {
                String databaseLink =  "jdbc:mysql://192.168.10.191/mrsharky_jp_Mon_gaus_mono_air_sfc_mon_mean";
                String databaseUsername = "root";
                String databasePassword = "plzf1xme";

                Class.forName("com.mysql.jdbc.Driver");
                dbi_destination = new DBI(databaseLink, databaseUsername, databasePassword);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(LoadData1.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1);
            }
            ClimateDatabase databaseDestination = new ClimateDatabase(dbi_destination);

            ComplexNumber[][][] PSI_k_spectral = new ComplexNumber[varExplained.length][Q+1][(Q+1)*2-1];

            String startDate = "0001-01-01";
            DateTime dtStartDate = formatter.parseDateTime(startDate);
            
            DateTime currDate;
            if (month == 0) {
                currDate = dtStartDate.plusYears(1);
            } else {
                currDate = dtStartDate.plusMonths(month-1);
            }

            for (int eigenCounter = 0; cummulativeVar <= 0.95; eigenCounter++) {
                cummulativeVar = cummulativeVar + varExplained[eigenCounter];
                int counter = 0;
                for (int k = 0; k < Q+1; k++) {
                    for (int l = -k; l <= k; l++) {
                        PSI_k_spectral[eigenCounter][k][l+Q] = eigenVectors.get(counter,eigenCounter);
                        counter = counter +1;
                    }
                }

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
                    int levelId = databaseDestination.PutLevelData(dbi, "Eigenvalue: " + (eigenCounter+1), "");
                    //Utilities.PrintComplexDoubleArray(pca_spatial);

                    List<Integer> gridBox_IDs = new ArrayList<Integer>();
                    List<Float> values = new ArrayList<Float>();
                    List<Integer> date_IDs = new ArrayList<Integer>();
                    List<Integer> level_IDs = new ArrayList<Integer>();
                    for (double currLat : lats) {
                        for (double currLon : lons) {

                            double currValue = pca_spatial[latsKey.get(currLat)][lonsKey.get(currLon)].getReal();
                            int grid_id = gridBoxAnomalyMean3[latsKey.get(currLat)][lonsKey.get(currLon)].GridBox_ID;

                            gridBox_IDs.add(grid_id);
                            if (month == 0) {
                                date_IDs.add(13);
                            } else {
                                date_IDs.add(month);
                            }
                            values.add((float) currValue);
                            level_IDs.add(levelId);

                        }
                    }

                    // Insert into the database
                    
                    if (false) {
                        databaseDestination.PutGridData(dbi_destination, level_IDs, gridBox_IDs, date_IDs, values);
                    }

                } catch (Exception ex) {
                    Logger.getLogger(LoadData1.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    
    public static void main(String args[]) throws Exception {
        LoadData1 ld = new LoadData1();
    }
}

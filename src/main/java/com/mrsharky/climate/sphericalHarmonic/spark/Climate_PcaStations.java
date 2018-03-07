/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec;
import com.mrsharky.dataprocessor.SphericalHarmonics_Results;
import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.SparkUtils.CreateDefaultSparkSession;
import static com.mrsharky.helpers.SparkUtils.PrintSparkSetting;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import static com.mrsharky.helpers.Utilities.SerializeObject;
import com.mrsharky.spark.SetupSparkTest;
import com.mrsharky.stations.StationSelectionResults;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Quintet;

/**
 * 
 * @author jpierret
 */
public class Climate_PcaStations {
    

    private SparkSession _spark;
    public Climate_PcaStations(String pcaDataLocation, String stationDataLocation, double varExpCutoff, boolean normalize, String ResultsDestination) throws Exception {   
        
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_Results pcaData = (SphericalHarmonics_Results) LoadSerializedObject(pcaDataLocation);
        
        // Setup Spark
        _spark = CreateDefaultSparkSession(this.getClass().getName());
        JavaSparkContext jsc = new JavaSparkContext(_spark.sparkContext());
        PrintSparkSetting(_spark);
        
        int q = 0; //pcaData.GetQ();
        
        // Trim the eigenValues & vectorns to "varExpCutoff"
        Pca_EigenValVec Eigens = new Pca_EigenValVec(pcaData, varExpCutoff);
        
        List<Quartet<Integer, Date, Integer, Integer>> rows = new ArrayList<Quartet<Integer, Date, Integer, Integer>>();
        System.out.println("Starting - " + this.getClass().getName());
         
        Map<Integer, Map<Integer, SphericalHarmonic>> eigenSpherical = new HashMap<Integer, Map<Integer, SphericalHarmonic>>();
        Map<Integer, Map<Integer, InvDiscreteSphericalTransform>> eigenInvDst = new HashMap<Integer, Map<Integer, InvDiscreteSphericalTransform>>();
        
        for (int month : stationData.GetMonths()) {
            System.out.println("Processing month: " + month);
            final int currMonth = (month == 0 ? 0 : month-1); 
            final Complex[][] eigenVectors_f = Eigens.GetEigenVectors(month);
            final Complex[] eigenValues_f = Eigens.GetEigenValues(month);
                 
            int numEigen = eigenValues_f.length;
            List<Date> monthDates = stationData.GetDates(month).stream()
                    .filter(d -> d.getMonth() == currMonth)
                    .sorted()
                    .collect(Collectors.toList());
            
            // Pre process some eigenvalue and eigenvector stuff
            eigenSpherical.put(month, new HashMap<Integer, SphericalHarmonic>());
            eigenInvDst.put(month, new HashMap<Integer, InvDiscreteSphericalTransform>());
            
            for (int e = 0; e < numEigen; e++) {
                Complex[] currEigenVector = eigenVectors_f[e];
                SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, true);
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
                eigenSpherical.get(month).put(e, currEigenSpherical);
                eigenInvDst.get(month).put(e, invDst);
            }      
                        
            for (Date date : monthDates) {
                for (int k = 0; k <= q; k++) {
                    for (int l = 0; l <= k; l++) {
                        rows.add(Quartet.with(month, date, k, l));  
                    }
                }
            }
        }
        JavaRDD<Quartet<Integer, Date, Integer, Integer>> rddData = jsc.parallelize(rows, 24);
        
        // Run the actual Spark job
        GenerateHarmonics generateWeights = new GenerateHarmonics(eigenSpherical, eigenInvDst, stationData, Eigens, q, normalize);
        JavaRDD<Quintet<Integer, Date, Integer, Integer, Complex>> javaRddData = rddData.mapPartitions(s -> generateWeights.call(s));
        List<Quintet<Integer, Date, Integer, Integer, Complex>> allHarmonics = javaRddData.collect();
        
        // Prepare all the data
        Map<Pair<Integer, Date>, SphericalHarmonic> finalStationHarmonic = new HashMap<Pair<Integer, Date>, SphericalHarmonic>();
        Map<Integer, List<Date>> availableDates = new HashMap<Integer, List<Date>>();
        for (Quintet<Integer, Date, Integer, Integer, Complex> row : allHarmonics) {
            int month = row.getValue0();
            Date date = row.getValue1();
            int k = row.getValue2();
            int l = row.getValue3();
            Complex S_lm = row.getValue4();
            
            // Set the date
            if (!availableDates.containsKey(month)) {
                availableDates.put(month, new ArrayList<Date>());
            }
            availableDates.get(month).add(date);
            
            // Set the harmonic
            Pair<Integer, Date> key = Pair.with(month, date);
            if (!finalStationHarmonic.containsKey(key)) {
                finalStationHarmonic.put(key, new SphericalHarmonic(q));
            }
            finalStationHarmonic.get(key).SetHarmonic(k, l, S_lm);
        }
        
        // Run the calculations
        TimeseriesResults finalResults = new TimeseriesResults();
        double[][] gridBox = pcaData.getGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        final double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
         
        for (int month : stationData.GetMonths()) {
            double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.getGridBoxAnomalyVariance(month), 0.5);
            
            if (availableDates.containsKey(month)) {
                List<Date> dates = availableDates.get(month).stream().sorted().collect(Collectors.toList());;

                for (Date date : dates) {
                    Pair<Integer, Date> key = Pair.with(month, date);

                    // Now that we have the final harmonic. Convert to spatial
                    InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic.get(key));

                    double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                    double value = DoubleArray.SumArray(
                            DoubleArray.Multiply(
                                    DoubleArray.Multiply(stationHarmonics_spatial, areaFraction/*areasForGrid.GetAreas()*/),
                                    gridBoxAnomSd));

                    int currYear = (date.getYear()+1900);
                    finalResults.Set(currYear, month, value, stationData.GetDate(month, date).size(), finalStationHarmonic.get(key));
                }
            }
        }
        
        // Generate the "Map" for each date
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(ResultsDestination);
        //SerializeObject(finalResults, ResultsDestination);
    }
     
    public static void main(String[] args) throws Exception {
        ClimateFromStations_InputParser parser = new ClimateFromStations_InputParser(args, Climate_PcaStations.class.getName());
        if (parser.InputsCorrect()) {
            Climate_PcaStations netCdf = new Climate_PcaStations(parser.dataEof, parser.dataStations, parser.varExplained, parser.normalized, parser.output);
        }
    }
}

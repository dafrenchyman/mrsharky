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
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec;
import com.mrsharky.dataprocessor.SphericalHarmonics_Results;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.SparkUtils.CreateDefaultSparkSession;
import static com.mrsharky.helpers.SparkUtils.PrintSparkSetting;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import com.mrsharky.spark.SetupSparkTest;
import com.mrsharky.stations.StationSelectionResults;
import java.io.File;
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
import org.javatuples.Quartet;
import org.javatuples.Quintet;
import static com.mrsharky.helpers.Utilities.SerializeObjectLocal;

/**
 * 
 * @author jpierret
 */
public class Climate_PcaStations_IndivDates {
    

    private SetupSparkTest sparkSetup;
    
    private SparkSession _spark;
    public Climate_PcaStations_IndivDates(String pcaDataLocation, String stationDataLocation, int q,
            double varExpCutoff, boolean normalize, String ResultsDestination, boolean createSpark, int sparkPartitions) throws Exception {   
        
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_Results pcaData = (SphericalHarmonics_Results) LoadSerializedObject(pcaDataLocation);
        q = q < 0 ? pcaData.getQ() : q;
        if ( q > pcaData.getQ() ) {
            throw new Exception("Submitted q is greater than Q from the PCA data");
        }
        
        // Setup Spark
        if (createSpark) {
            int threads = Runtime.getRuntime().availableProcessors();
            //threads = 1;
            //threads = (int) Math.max(1, threads-1);
            sparkSetup = new SetupSparkTest();
            sparkSetup.setup(threads);
        }
        _spark = CreateDefaultSparkSession(this.getClass().getName());
        JavaSparkContext jsc = new JavaSparkContext(_spark.sparkContext());
        PrintSparkSetting(_spark);
          
        // Trim the eigenValues & vectorns to "varExpCutoff"
        Pca_EigenValVec Eigens = new Pca_EigenValVec(pcaData, varExpCutoff);
        Map<Integer, Map<Integer, SphericalHarmonic>> eigenSpherical = new HashMap<Integer, Map<Integer, SphericalHarmonic>>();
        Map<Integer, Map<Integer, InvDiscreteSphericalTransform>> eigenInvDst = new HashMap<Integer, Map<Integer, InvDiscreteSphericalTransform>>();
        
        // Calculate the area
        double[][] gridBox = pcaData.getGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        final double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
           
        TimeseriesResults finalResults = new TimeseriesResults();
        
        for (int month : stationData.GetMonths()) {
            System.out.println("Processing month: " + month);
            final int currMonth = (month == 0 ? 0 : month-1); 
            final Complex[][] eigenVectors_f = Eigens.GetEigenVectors(month);
            final Complex[] eigenValues_f = Eigens.GetEigenValues(month);
                 
            int numEigen = eigenValues_f.length;
            List<Date> monthDates = stationData.GetDates(month).stream()
                    //.filter(d -> d.getMonth() == currMonth && d.getYear()+1900 >= 1851)
                    .filter(d -> d.getMonth() == currMonth && d.getYear()+1900 == 1980)
                    .sorted()
                    .collect(Collectors.toList());
            
            // Pre process some eigenvalue and eigenvector stuff
            eigenSpherical.put(month, new HashMap<Integer, SphericalHarmonic>());
            eigenInvDst.put(month, new HashMap<Integer, InvDiscreteSphericalTransform>());    
            for (int e = 0; e < numEigen; e++) {
                Complex[] currEigenVector = eigenVectors_f[e];
                SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, false);
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
                eigenSpherical.get(month).put(e, currEigenSpherical);
                eigenInvDst.get(month).put(e, invDst);
            }      
            
            // Get the standard deviation for the gridboxes
            double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.getGridBoxAnomalyVariance(month), 0.5);
                        
            for (Date date : monthDates) {
                int year = (date.getYear()+1900);
                SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(q);             
                String currResultsSave = ResultsDestination + "/Harmonics/month=" + month +"_year=" + year + ".serialized";
                if (!Utilities.checkIfFileExists(currResultsSave)) {
                    List<Quartet<Integer, Date, Integer, Integer>> rows = new ArrayList<Quartet<Integer, Date, Integer, Integer>>();
                    for (int k = 0; k <= q; k++) {
                        for (int l = 0; l <= k; l++) {
                            rows.add(Quartet.with(month, date, k, l));  
                        }
                    }

                    JavaRDD<Quartet<Integer, Date, Integer, Integer>> rddData = jsc.parallelize(rows, sparkPartitions);

                    // Run the actual Spark job
                    GenerateHarmonics generateWeights = new GenerateHarmonics(eigenSpherical, eigenInvDst, stationData, Eigens, q, normalize);
                    JavaRDD<Quintet<Integer, Date, Integer, Integer, Complex>> javaRddData = rddData.mapPartitions(s -> generateWeights.call(s));
                    List<Quintet<Integer, Date, Integer, Integer, Complex>> allHarmonics = javaRddData.collect();

                    // Generate the harmonic
                    for (Quintet<Integer, Date, Integer, Integer, Complex> row : allHarmonics) {
                        int k = row.getValue2();
                        int l = row.getValue3();
                        Complex S_lm = row.getValue4();
                        finalStationHarmonic.SetHarmonic(k, l, S_lm);
                    }     

                    // Save the harmonic to disk
                    Utilities.SerializeObject(finalStationHarmonic.GetHalfCompressedSpectra(), currResultsSave);
                } else {
                    finalStationHarmonic.SetHalfCompressedSpectra( (Complex[]) Utilities.LoadSerializedObject(currResultsSave) );
                }
                
                // Now that we have the final harmonic. Convert to spatial
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length); 
                if (normalize) {
                    stationHarmonics_spatial = DoubleArray.Multiply(stationHarmonics_spatial, gridBoxAnomSd);
                }
                
                double value = DoubleArray.SumArray( DoubleArray.Multiply( stationHarmonics_spatial, areaFraction) );
                finalResults.Set(year, month, value, stationData.GetDate(month, date).size(), finalStationHarmonic);
            }
        }

        // Generate the "Map" for each date
        finalResults.Print();
        finalResults.SaveOverallResultsToCsv(ResultsDestination + "/FinalResults.csv");
        //SerializeObject(finalResults, ResultsDestination);
    }
     
    public static void main(String[] args) throws Exception {
        Climate_PcaStations_InputParser parser = new Climate_PcaStations_InputParser(args, Climate_PcaStations_IndivDates.class.getName());
        if (parser.InputsCorrect()) {
            Climate_PcaStations_IndivDates netCdf = new Climate_PcaStations_IndivDates(parser.dataEof, 
                    parser.dataStations, parser.q, parser.varExplained, parser.normalized, parser.output, parser.createSpark, parser.partitions);
        }
    }
}

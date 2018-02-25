/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.old;

import com.mrsharky.climate.sphericalHarmonic.AreasForGrid;
import com.mrsharky.climate.sphericalHarmonic.ClimateFromStations_InputParser;
import com.mrsharky.climate.sphericalHarmonic.common.TimeseriesResults;
import com.mrsharky.climate.sphericalHarmonic.common.*;
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonicY;
import static com.mrsharky.discreteSphericalTransform.SphericalHelpers.UnCompressSpectral;
import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import static com.mrsharky.helpers.Utilities.LoadSerializedObject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Triplet;


/**
 *
 * @author Julien Pierret
 */
public class ClimateFromStations {
    
    public ClimateFromStations(String pcaDataLocation, String stationDataLocation) throws Exception {
        // Load station data
        StationSelectionResults stationData = (StationSelectionResults) LoadSerializedObject(stationDataLocation);
        
        // Load the pcaData
        SphericalHarmonics_PcaResults pcaData = (SphericalHarmonics_PcaResults) LoadSerializedObject(pcaDataLocation);
        
        // Create Array that will hold all the results
        TimeseriesResults finalResults = new TimeseriesResults();

        int q = 0;
        
        // Calculate the gridbox area
        double[][] gridBox = pcaData.GetGridBoxAnomalyVariance(0);
        AreasForGrid areasForGrid = new AreasForGrid(gridBox.length,gridBox[0].length,1.0);
        double[][] areaFraction = DoubleArray.Multiply(areasForGrid.GetAreas(), 1.0/(Math.PI*4.0));
        
        // Trim the eigenValues & vectorns to "varExpCutoff"
        Pca_EigenValVec Eigens = new Pca_EigenValVec(pcaData, 0.90);
        
        for (int month = 0; month <= 12; month++) {
            System.out.println("Processing month: " + month);
            final int currMonth = (month == 0 ? 0 : month-1); 
            final Complex[][] eigenVectors_f = Eigens.GetEigenVectors(month);
            final Complex[] eigenValues_f = Eigens.GetEigenValues(month);
                 
            int numEigen = eigenValues_f.length;
            List<Date> monthDates = stationData.GetDates(month).stream()
                    .filter(d -> d.getMonth() == currMonth)
                    .filter(d -> d.getYear()+1900 > 1880)
                    .sorted()
                    .collect(Collectors.toList());
            
            // Pre process some eigenvalue and eigenvector stuff
            Map<Integer, SphericalHarmonic> eigenSpherical = new HashMap<Integer, SphericalHarmonic>();
            Map<Integer, InvDiscreteSphericalTransform> eigenInvDst = new HashMap<Integer, InvDiscreteSphericalTransform>();
            for (int e = 0; e < numEigen; e++) {
                Complex[] currEigenVector = eigenVectors_f[e];
                SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, true);
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
                
                eigenSpherical.put(e, currEigenSpherical);
                eigenInvDst.put(e, invDst);
            }      
            
            // Key to store all the weights
            Map<Triplet<Integer, Integer, List<Long>>, double[]> weightsMap = new ConcurrentHashMap<Triplet<Integer, Integer, List<Long>>, double[]>();
            
            for (Date currDate : monthDates) {
                // Get the current System time
                long startTime = System.currentTimeMillis();
                
                List<StationResults> stations = stationData.GetDate(month, currDate);
                stations = stations.stream().sorted((a,b) -> Long.compare(b.StationId, a.StationId)).collect(Collectors.toList());
                
                double[] lats = new double[stations.size()];
                double[] lons = new double[stations.size()];
                double[] stationMean = new double[stations.size()];
                double[] stationSd = new double [stations.size()];
                double[] stationValue = new double [stations.size()];
                long[] stationId = new long[stations.size()];
                
                List<Long> stationList = new ArrayList<Long>();
                for (int i = 0; i < stations.size(); i++) {
                    StationResults currStation = stations.get(i);
                    stationId[i] = currStation.StationId;                     
                    lats[i] = Utilities.LatitudeToRadians(currStation.Lat);
                    lons[i] = Utilities.LongitudeToRadians(currStation.Lon);
                    stationMean[i] = currStation.BaselineMean;
                    stationValue[i] = currStation.Value;
                    stationSd[i] =  Math.sqrt(currStation.BaselineVariance);
                    stationList.add(currStation.StationId);
                }
                
                //double[][] gridBoxAnomMean = pcaData.GetGridBoxAnomalyMean(month);
                double[][] gridBoxAnomSd =  DoubleArray.Power(pcaData.GetGridBoxAnomalyVariance(month), 0.5);               
                
                SphericalHarmonicY stationHarmonics = new SphericalHarmonicY(lats, lons, q); 
                
                int threads = Runtime.getRuntime().availableProcessors();
                ExecutorService service = Executors.newFixedThreadPool(threads);
                List<Future<Quartet<Integer, Integer, Complex, double[]>>> futures = new ArrayList<Future<Quartet<Integer, Integer, Complex, double[]>>>();
                
                for (int k = 0; k <= q; k++) {
                    for (int l = 0; l <= k; l++) {

                        final int k_f = k;
                        final int l_f = l;
                        
                        // Process the spherical harmonics multi-threaded
                        Callable<Quartet<Integer, Integer, Complex, double[]>> callable = new Callable<Quartet<Integer, Integer, Complex, double[]>>() {
                            public Quartet<Integer, Integer, Complex, double[]> call() throws Exception {
                                
                                boolean uniformWeight = false;
                                
                                Complex S_lm = null;
                                
                                // Calculate the weights
                                double[] weights = new double[stationId.length];
                                if (uniformWeight) {
                                    for (int i = 0; i < weights.length; i++) {
                                        weights[i] = 1.0 / (weights.length + 0.0);
                                    }
                                } else {
                                    
                                    Triplet<Integer, Integer, List<Long>> weightKey = Triplet.with(k_f, l_f, stationList);
                                    //if (false) {
                                    if (weightsMap.containsKey(weightKey)) {
                                        weights = weightsMap.get(weightKey);   
                                    } else {
                                        Complex[] stationHarmonics_k_l = stationHarmonics.GetHarmonic(k_f, l_f);
                                        Complex[] stationHarmonics_k_l_conj = stationHarmonics.Conjugate().GetHarmonic(k_f, l_f);

                                        Complex[][] B_ys = ComplexArray.MeshMultiply(stationHarmonics_k_l_conj, stationHarmonics_k_l);

                                        double[][] B_sum = new double[stationId.length][stationId.length];
                                        double[]   C_sum = new double[stationId.length];
                                        for (int e = 0; e < numEigen; e++) {
                                            // B NxN
                                            Complex[] currEigenVector = eigenVectors_f[e];

                                            //SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, false);
                                            //SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, true);
                                            //InvDiscreteSphericalTransform_ass invDst = new InvDiscreteSphericalTransform_ass(currEigenSpherical);

                                            SphericalHarmonic currEigenSpherical = eigenSpherical.get(e);
                                            InvDiscreteSphericalTransform invDst = eigenInvDst.get(e);

                                            double[] stationHarmonics_spatial = invDst.ProcessPoints(lats, lons);
                                            double[][] B_phi = DoubleArray.MeshMultiply(stationHarmonics_spatial, stationHarmonics_spatial);
                                            double[][] B_NxN = DoubleArray.Multiply(ComplexArray.Real(ComplexArray.Multiply(B_phi, B_ys)), eigenValues_f[e].getReal());                            
                                            B_sum = DoubleArray.Add(B_sum, B_NxN);

                                            // C Nx1
                                            Complex Psi_lm = currEigenSpherical.Conjugate().GetHarmonic(k_f, l_f);
                                            double oscil = Math.pow(-1, l_f);
                                            Complex Psi_lm_conj = Psi_lm.conjugate().multiply(oscil);

                                            double[] Psi_n = stationHarmonics_spatial;
                                            Complex[] Y_lm_conj = stationHarmonics_k_l_conj;
                                            double[] C_real = ComplexArray.Real(ComplexArray.Multiply(ComplexArray.Multiply(Psi_lm_conj, Psi_n), Y_lm_conj));
                                            double[] C_Nx1 = DoubleArray.Multiply(C_real , eigenValues_f[e].getReal());
                                            C_sum = DoubleArray.Add(C_sum, C_Nx1);
                                        }

                                        // Modify the B_sum matrix (add some noise along the diagonal to make sure we get a full rank matrix)
                                        for (int i = 0; i < B_sum.length; i++) {
                                            B_sum[i][i] = B_sum[i][i] + 0.0000001;
                                        }

                                        // Create the A_matrix
                                        double[][] A_dblMatrix = DoubleArray.Resize(B_sum, B_sum.length+1, B_sum[0].length+1);
                                        // Last column: -1...-1, 0 
                                        int lastColumn = A_dblMatrix[0].length-1;
                                        for (int i = 0; i < A_dblMatrix.length -1; i++) {
                                            A_dblMatrix[i][lastColumn] = -1;
                                        }
                                        // Last row: 1,... 1, 0
                                        int lastRow = A_dblMatrix.length-1;
                                        for (int i = 0; i < A_dblMatrix[0].length-1; i++) {
                                            A_dblMatrix[lastRow][i] = 1;
                                        }

                                        // Create the b matrix
                                        double[] b_matrix = DoubleArray.Resize(C_sum, C_sum.length+1);
                                        b_matrix[b_matrix.length-1] = 4.0*Math.PI;

                                        RealMatrix A_matrix =  new Array2DRowRealMatrix(A_dblMatrix);
                                        RealVector b_vector =  new ArrayRealVector(b_matrix);

                                        DecompositionSolver solver;
                                        if (false) {
                                            solver = new LUDecomposition(A_matrix).getSolver();
                                        } else {
                                            solver = new SingularValueDecomposition(A_matrix).getSolver();
                                        }
                                        
                                        
                                        if (solver.isNonSingular()) {
                                            RealVector solution = solver.solve(b_vector);
                                            weights = solution.toArray();
                                            weightsMap.put(weightKey, weights);
                                        } else {
                                            System.out.println("Date: " + currDate + ", stationCount: " + stationId.length + ", k: " + k_f  + ", l:" + l_f);
                                        }
                                    }
                                }

                                // Now that we have the weights for given k,l, calculate the harmonic
                                double[] S_n = DoubleArray.Add(stationValue, DoubleArray.Multiply(stationMean, -1.0));

                                boolean normalize = false;
                                if (normalize) {
                                    S_n = DoubleArray.Multiply(S_n, DoubleArray.Power(stationSd, -1.0));
                                }
                                Complex[] S_n_Y_conj = ComplexArray.Multiply(S_n, stationHarmonics.Conjugate().GetHarmonic(k_f, l_f));
                                Complex[] S_n_Y_conj_w = ComplexArray.Multiply(S_n_Y_conj, weights);
                                
                                S_lm = ComplexArray.Sum(S_n_Y_conj_w);

                                Quartet<Integer, Integer, Complex, double[]> results = Quartet.with(k_f, l_f, S_lm, weights);
                                return results;
                            }};
                        futures.add(service.submit(callable));
                    }
                }
                
                service.shutdown();
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                SphericalHarmonic finalStationHarmonic = new SphericalHarmonic(q);
                for (Future<Quartet<Integer, Integer, Complex, double[]>> future : futures) {
                    int k = future.get().getValue0();
                    int l = future.get().getValue1();
                    Complex S_lm = future.get().getValue2();
                    double[] weights = future.get().getValue3();
                    finalStationHarmonic.SetHarmonic(k, l, S_lm);
                }
                
                // Now that we have the final harmonic. Convert to spatial
                InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(finalStationHarmonic);
                double[][] stationHarmonics_spatial = invDst.ProcessGaussianDoubleArray(gridBoxAnomSd.length, gridBoxAnomSd[0].length);
                double value = DoubleArray.SumArray(
                        DoubleArray.Multiply(
                                DoubleArray.Multiply(stationHarmonics_spatial, areaFraction/*areasForGrid.GetAreas()*/),
                                gridBoxAnomSd));          

                int currYear = (currDate.getYear()+1900);
                finalResults.Set(currYear, month, value, stations.size(), finalStationHarmonic);

            }
        }
        finalResults.Print();
        
    }
    
    public static void main(String args[]) throws Exception {   
        ClimateFromStations_InputParser in = new ClimateFromStations_InputParser(args, ClimateFromStations.class.getName());
        if (in.InputsCorrect()) {
            ClimateFromStations c = new ClimateFromStations(
                    in.dataEof, in.dataStations);
        }
    }
}

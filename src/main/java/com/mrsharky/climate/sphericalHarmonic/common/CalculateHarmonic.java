/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.climate.sphericalHarmonic.common;

import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonicY;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.Utilities;
import com.mrsharky.stations.StationResults;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 * 
 * @author Julien Pierret
 */
public class CalculateHarmonic {

    
    private final int _q_f;
    private Map<Triplet<Integer, Integer, List<String>>, double[]> _weightsMap;
    private final boolean _normalized;
    private final Complex[][] _eigenVectors_f;
    private final Complex[] _eigenValues_f;
    private final Map<Integer, SphericalHarmonic> _eigenSpherical;
    private final Map<Integer, InvDiscreteSphericalTransform> _eigenInvDst;
 
    public CalculateHarmonic(int q_f, boolean normalized,
            Complex[][] eigenVectors_f,
            Complex[] eigenValues_f) throws Exception {
        _q_f = q_f;
        _normalized = normalized;
        _eigenVectors_f = eigenVectors_f;
        _eigenValues_f = eigenValues_f;
        
        // Pre process some eigenvalue and eigenvector stuff
        int numEigen = eigenValues_f.length;
        Map<Integer, SphericalHarmonic> eigenSpherical = new HashMap<Integer, SphericalHarmonic>();
        Map<Integer, InvDiscreteSphericalTransform> eigenInvDst = new HashMap<Integer, InvDiscreteSphericalTransform>();
        for (int e = 0; e < numEigen; e++) {
            Complex[] currEigenVector = eigenVectors_f[e];
            SphericalHarmonic currEigenSpherical = new SphericalHarmonic(currEigenVector, false);
            InvDiscreteSphericalTransform invDst = new InvDiscreteSphericalTransform(currEigenSpherical);
            eigenSpherical.put(e, currEigenSpherical);
            eigenInvDst.put(e, invDst);
        }
        _eigenSpherical = eigenSpherical;
        _eigenInvDst = eigenInvDst;
        _weightsMap = new ConcurrentHashMap<Triplet<Integer, Integer, List<String>>, double[]>();
    }
    
    public Pair<Complex, double[]> Process(int k, int l, double[] lats, double[] lons, double[] stationValue) throws Exception {
        if (lats.length != lons.length && lats.length != stationValue.length) {
            throw new Exception("All arrays must have the same length");
        }
        
        double[] stationSd = new double[lats.length];
        double[] stationMean = new double[lats.length];
        List<String> stationList = new ArrayList<String>();
        for (int i = 0; i < lats.length; i++) {
            stationSd[i] = 1.0;
            String stationId = lats[i] + "," + lons[i];
            stationList.add(stationId);
        }
        stationList = stationList.stream().sorted((a,b) -> b.compareTo(a)).collect(Collectors.toList());
        return Process(k, l, lats, lons, stationValue, stationMean, stationSd, stationList);
    }
    
    public Pair<Complex, double[]> Process(int k, int l, List<StationResults> stations) throws Exception {
        stations = stations.stream().sorted((a,b) -> Long.compare(b.StationId, a.StationId)).collect(Collectors.toList());
        double[] lats = new double[stations.size()];
        double[] lons = new double[stations.size()];
        double[] stationMean = new double[stations.size()];
        double[] stationSd = new double [stations.size()];
        double[] stationValue = new double [stations.size()];

        List<String> stationList = new ArrayList<String>();
        for (int i = 0; i < stations.size(); i++) {
            StationResults currStation = stations.get(i);
            lats[i] = Utilities.LatitudeToRadians(currStation.Lat);
            lons[i] = Utilities.LongitudeToRadians(currStation.Lon);
            stationMean[i] = currStation.BaselineMean;
            stationValue[i] = currStation.Value;
            stationSd[i] =  Math.sqrt(currStation.BaselineVariance);
            stationList.add(String.valueOf(currStation.StationId));
        }
        return Process(k, l, lats, lons, stationValue, stationMean, stationSd, stationList);
    }
        
    private Pair<Complex, double[]> Process(int k, int l, double[] lats, double[] lons,
            double[] stationValue, double[] stationMean, double[] stationSd, List<String> stationList) throws Exception {
        
    
        SphericalHarmonicY stationHarmonics = new SphericalHarmonicY(lats, lons, _q_f); 

        boolean uniformWeight = false;

        Complex S_lm = null;

        // Calculate the weights
        double[] weights = new double[lats.length];
        if (uniformWeight) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = (1.0/(weights.length + 0.0)) * (4.0*Math.PI);
            }
        } else {
            Triplet<Integer, Integer, List<String>> weightKey = Triplet.with(k, l, stationList);
            if (_weightsMap.containsKey(weightKey)) {
                weights = _weightsMap.get(weightKey);
            } else {
                weights = GenerateWeights(k, l, lats, lons, stationHarmonics, _eigenVectors_f, _eigenValues_f, _eigenSpherical, _eigenInvDst);
                _weightsMap.put(weightKey, weights);
            }
        }

        // Now that we have the weights for given k,l, calculate the harmonic
        double[] S_n = DoubleArray.Add(stationValue, DoubleArray.Multiply(stationMean, -1.0));

        if (_normalized) {
            S_n = DoubleArray.Multiply(S_n, DoubleArray.Power(stationSd, -1.0));
        }
        Complex[] S_n_Y_conj = ComplexArray.Multiply(S_n, stationHarmonics.Conjugate().GetHarmonic(k, l));
        Complex[] S_n_Y_conj_w = ComplexArray.Multiply(S_n_Y_conj, weights);

        S_lm = ComplexArray.Sum(S_n_Y_conj_w);
        return Pair.with(S_lm, weights);
    }
    
    
    private double[] GenerateWeights(int k_f, int l_f, double[] lats, double[] lons, SphericalHarmonicY stationHarmonics
            , Complex[][] eigenVectors_f, Complex[] eigenValues_f
            , Map<Integer, SphericalHarmonic> eigenSpherical
            , Map<Integer, InvDiscreteSphericalTransform> eigenInvDst) throws Exception {
        
        double startingNoiseAmount = 1e-10;
        String property = System.getProperty("WeightNoiseAmount");
        if (property != null) {
            startingNoiseAmount = Double.parseDouble(property);;
        }
        
        Complex[] stationHarmonics_k_l = stationHarmonics.GetHarmonic(k_f, l_f);
        Complex[] stationHarmonics_k_l_conj = stationHarmonics.Conjugate().GetHarmonic(k_f, l_f);

        Complex[][] B_ys = ComplexArray.MeshMultiply(stationHarmonics_k_l_conj, stationHarmonics_k_l);

        double[][] B_sum = new double[lats.length][lats.length];
        double[]   C_sum = new double[lats.length];
        for (int e = 0; e < eigenValues_f.length; e++) {
            boolean sphericalEigenValue = true;
            
            // B NxN    
            double currEigenValue = eigenValues_f[e].getReal();
            
            // testing code (should remove eventually)
            SphericalHarmonic eigenValueHarm = new SphericalHarmonic(k_f);
            eigenValueHarm.SetHarmonic(k_f, l_f, new Complex(currEigenValue, 0.0));
            InvDiscreteSphericalTransform invEigenValDst = new InvDiscreteSphericalTransform(eigenValueHarm);
            double[] currEigenValueSpatial = invEigenValDst.ProcessPoints(lats, lons);
            double[][] currEigenValueSpatialMesh = DoubleArray.MeshMultiply(currEigenValueSpatial, currEigenValueSpatial);  

            SphericalHarmonic currEigenVectorSpherical = eigenSpherical.get(e);
            InvDiscreteSphericalTransform invDst = eigenInvDst.get(e);

            double[] stationHarmonics_spatial = invDst.ProcessPoints(lats, lons);
            double[][] B_phi = DoubleArray.MeshMultiply(stationHarmonics_spatial, stationHarmonics_spatial);
            double[][] B_NxN = ComplexArray.Real(ComplexArray.Multiply(B_phi, B_ys));
            
            
            if (sphericalEigenValue) {
                B_NxN = DoubleArray.Multiply(B_NxN, currEigenValue);
            } else {
                B_NxN = DoubleArray.Multiply(B_NxN, currEigenValueSpatialMesh);
            }
            B_sum = DoubleArray.Add(B_sum, B_NxN);

            // C Nx1
            Complex Psi_lm_conj = currEigenVectorSpherical.Conjugate().GetHarmonic(k_f, l_f);

            double[] Psi_n = stationHarmonics_spatial;
            Complex[] Y_lm_conj = stationHarmonics_k_l_conj;
            double[] C_real = ComplexArray.Real(ComplexArray.Multiply(ComplexArray.Multiply(Psi_lm_conj, Psi_n), Y_lm_conj));
            
            double[] C_Nx1 = null;
            if (sphericalEigenValue) {
                C_Nx1 = DoubleArray.Multiply(C_real , currEigenValue);
            } else {
                C_Nx1 = DoubleArray.Multiply(C_real , currEigenValueSpatial);
            }
            
            C_sum = DoubleArray.Add(C_sum, C_Nx1);
        }
           
        double[] weights = new double[lats.length];
        boolean solutionFound = false;
        double noiseAmount = startingNoiseAmount;
        while (!solutionFound) {
            // Modify the B_sum matrix (add some noise along the diagonal to make sure we get a full rank matrix)
            for (int i = 0; i < B_sum.length; i++) {
                B_sum[i][i] = B_sum[i][i] + noiseAmount;
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
            if (true) {
                solver = new LUDecomposition(A_matrix).getSolver();
            } else {
                solver = new SingularValueDecomposition(A_matrix).getSolver();
            }

            if (solver.isNonSingular()) {
                RealVector solution = solver.solve(b_vector);
                weights = solution.toArray();
                solutionFound = true;
            } else {
                //throw new Exception("Signular Matrix!!");
                noiseAmount = noiseAmount*10;
            }
            if (noiseAmount >= 0.00001) {
                throw new Exception("Signular Matrix!!");
            }
            
        }
        weights = DoubleArray.Resize(weights, weights.length-1);
        return weights;
    }
    
    
    private double[] GenerateWeights_old(int k_f, int l_f, double[] lats, double[] lons, SphericalHarmonicY stationHarmonics
            , Complex[][] eigenVectors_f, double[] eigenValues_f
            , Map<Integer, SphericalHarmonic> eigenSpherical
            , Map<Integer, InvDiscreteSphericalTransform> eigenInvDst) throws Exception {
        
        double startingNoiseAmount = 1e-10;
        String property = System.getProperty("WeightNoiseAmount");
        if (property != null) {
            startingNoiseAmount = Double.parseDouble(property);;
        }
        
        Complex[] stationHarmonics_k_l = stationHarmonics.GetHarmonic(k_f, l_f);
        Complex[] stationHarmonics_k_l_conj = stationHarmonics.Conjugate().GetHarmonic(k_f, l_f);

        Complex[][] B_ys = ComplexArray.MeshMultiply(stationHarmonics_k_l_conj, stationHarmonics_k_l);

        double[][] B_sum = new double[lats.length][lats.length];
        double[]   C_sum = new double[lats.length];
        for (int e = 0; e < eigenValues_f.length; e++) {
            // B NxN
            Complex[] currEigenVector = eigenVectors_f[e];
            double currEigenValue = eigenValues_f[e];

            SphericalHarmonic currEigenSpherical = eigenSpherical.get(e);
            InvDiscreteSphericalTransform invDst = eigenInvDst.get(e);

            double[] stationHarmonics_spatial = invDst.ProcessPoints(lats, lons);
            double[][] B_phi = DoubleArray.MeshMultiply(stationHarmonics_spatial, stationHarmonics_spatial);
            double[][] B_NxN = DoubleArray.Multiply(ComplexArray.Real(ComplexArray.Multiply(B_phi, B_ys)), currEigenValue);   
            B_sum = DoubleArray.Add(B_sum, B_NxN);

            // C Nx1
            Complex Psi_lm = currEigenSpherical.Conjugate().GetHarmonic(k_f, l_f);
            double oscil = Math.pow(-1, l_f);
            Complex Psi_lm_conj = Psi_lm.conjugate().multiply(oscil);

            double[] Psi_n = stationHarmonics_spatial;
            Complex[] Y_lm_conj = stationHarmonics_k_l_conj;
            double[] C_real = ComplexArray.Real(ComplexArray.Multiply(ComplexArray.Multiply(Psi_lm_conj, Psi_n), Y_lm_conj));
            double[] C_Nx1 = DoubleArray.Multiply(C_real , currEigenValue);
            C_sum = DoubleArray.Add(C_sum, C_Nx1);
        }
           
        double[] weights = new double[lats.length];
        boolean solutionFound = false;
        double noiseAmount = startingNoiseAmount;
        while (!solutionFound) {
            // Modify the B_sum matrix (add some noise along the diagonal to make sure we get a full rank matrix)
            for (int i = 0; i < B_sum.length; i++) {
                B_sum[i][i] = B_sum[i][i] + noiseAmount;
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
            if (true) {
                solver = new LUDecomposition(A_matrix).getSolver();
            } else {
                solver = new SingularValueDecomposition(A_matrix).getSolver();
            }

            if (solver.isNonSingular()) {
                RealVector solution = solver.solve(b_vector);
                weights = solution.toArray();
                solutionFound = true;
            } else {
                //throw new Exception("Signular Matrix!!");
                noiseAmount = noiseAmount*10;
            }
            if (noiseAmount >= 0.00001) {
                throw new Exception("Signular Matrix!!");
            }
            
        }
        weights = DoubleArray.Resize(weights, weights.length-1);
        return weights;
    }
    
    
}

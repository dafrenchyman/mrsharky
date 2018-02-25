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
import com.mrsharky.stations.StationSelectionResults;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
import org.javatuples.Triplet;

/**
 * 
 * @author Julien Pierret
 */
public class Helpers implements Serializable {
    
    public static double[] GenerateWeights(int k_f, int l_f, double[] lats, double[] lons, SphericalHarmonicY stationHarmonics
            , Complex[][] eigenVectors_f, Complex[] eigenValues_f
            , Map<Integer, SphericalHarmonic> eigenSpherical
            , Map<Integer, InvDiscreteSphericalTransform> eigenInvDst) throws Exception {
                
        Complex[] stationHarmonics_k_l = stationHarmonics.GetHarmonic(k_f, l_f);
        Complex[] stationHarmonics_k_l_conj = stationHarmonics.Conjugate().GetHarmonic(k_f, l_f);

        Complex[][] B_ys = ComplexArray.MeshMultiply(stationHarmonics_k_l_conj, stationHarmonics_k_l);

        double[][] B_sum = new double[lats.length][lats.length];
        double[]   C_sum = new double[lats.length];
        for (int e = 0; e < eigenValues_f.length; e++) {
            // B NxN
            Complex[] currEigenVector = eigenVectors_f[e];

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
        double noiseAmount = 1e-10;
        String property = System.getProperty("WeightNoiseAmount");
        if (property != null) {
            noiseAmount = Double.parseDouble(property);;
        }
        
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

        double[] weights = new double[lats.length];
        if (solver.isNonSingular()) {
            RealVector solution = solver.solve(b_vector);
            weights = solution.toArray();
        } else {
            throw new Exception("Signular Matrix!!");
        }
        weights = DoubleArray.Resize(weights, weights.length-1);
        return weights;
    }
}

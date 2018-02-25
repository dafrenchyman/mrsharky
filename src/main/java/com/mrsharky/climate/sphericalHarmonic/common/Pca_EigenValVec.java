/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.climate.sphericalHarmonic.common;

import com.mrsharky.dataprocessor.old.SphericalHarmonics_PcaResults;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.stations.StationSelectionResults;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Triplet;

/**
 * 
 * @author Julien Pierret
 */
public class Pca_EigenValVec implements Serializable {

    private Map<Integer, Complex[][]> _eigenVectors;
    private Map<Integer, Complex[]> _eigenValues;
    private Map<Integer, double[]> _varExplained;
    
    public Complex[][] GetEigenVectors(int month) {
        return _eigenVectors.get(month);
    }
    
    public Complex[] GetEigenValues(int month) {
        return _eigenValues.get(month);
    }
    
    public double[] GetVarExplained(int month) {
        return _varExplained.get(month);
    }
    
    public static Triplet<Complex[][], Complex[], double[]> CutoffVarExplained(double varExpCutoff, Complex[][] eigenVectors, Complex[] eigenValues, double[] varExplained) throws Exception {
        Complex[][] newEigenVectors = null;
        Complex[] newEigenValues = null;
        double[] newVarExplained = null;
        if (varExpCutoff <= 1.0) {
            // find where the cutoff is
            int cutOffLocation = 0;
            for (int i = 0; i < varExplained.length; i++) {
                cutOffLocation = i;
                if (varExplained[i] >= varExpCutoff) {
                    //System.out.println("    cuttoff: " + cutOffLocation + " of " + (varExplained.length-1));
                    break;
                }
            }
            newEigenVectors = ComplexArray.Subset(eigenVectors, 0, cutOffLocation, 0, eigenVectors[0].length-1);
            newEigenValues = ComplexArray.Subset(eigenValues, 0, cutOffLocation);
            newVarExplained = DoubleArray.Subset(varExplained, 0, cutOffLocation);
        }
        Triplet<Complex[][], Complex[], double[]> value = Triplet.with(newEigenVectors, newEigenValues, newVarExplained);
        return value;
    }
    
    
    public Pca_EigenValVec(SphericalHarmonics_PcaResults pcaData, double varExpCutoff) throws Exception {
        _eigenVectors = new HashMap<Integer, Complex[][]>();
        _eigenValues = new HashMap<Integer, Complex[]>();
        _varExplained = new HashMap<Integer, double[]>();
   
        int months[] = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12};
        
        for (int month : months) {
            System.out.println("Processing month: " + month);
            Complex[][] eigenVectors = pcaData.GetEigenVectors(month);
            Complex[] eigenValues = pcaData.GetEigenvalues(month);
            double[] varExplained = pcaData.GetVarianceExplained(month);
            
            if (varExpCutoff <= 1.0) {
                // find where the cutoff is
                int cutOffLocation = 0;
                for (int i = 0; i < varExplained.length; i++) {
                    cutOffLocation = i;
                    if (varExplained[i] >= varExpCutoff) {
                        System.out.println("    cuttoff: " + cutOffLocation + " of " + (varExplained.length-1));
                        break;
                    }
                }
                eigenVectors = ComplexArray.Subset(eigenVectors, 0, cutOffLocation, 0, eigenVectors[0].length-1);
                eigenValues = ComplexArray.Subset(eigenValues, 0, cutOffLocation);
                varExplained = DoubleArray.Subset(varExplained, 0, cutOffLocation);
            }
            
            _eigenVectors.put(month, eigenVectors);
            _eigenValues.put(month, eigenValues);
            _varExplained.put(month, varExplained);
        }
    }
}

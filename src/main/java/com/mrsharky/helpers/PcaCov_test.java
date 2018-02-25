/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import java.util.List;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.store.ComplexDenseStore;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.task.InverterTask;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.scalar.ComplexNumber;

/**
 *
 * @author mrsharky
 */
public class PcaCov_test {
    
    private ComplexDenseStore _eigenValues;
    private ComplexDenseStore _eigenVectors;
    private double[] _varianceExplained;
    
    public ComplexDenseStore GetEigenValues() {
        return this._eigenValues;
    }
    
    public ComplexDenseStore GetEigenVectors() {
        return this._eigenVectors;
    }
    
    public double[] GetVarianceExplained() {
        return this._varianceExplained;
    }
    
    public PcaCov_test(MatrixStore<ComplexNumber> covMatrix) {    
        Eigenvalue<ComplexNumber> eigenDecomposition;
        eigenDecomposition = Eigenvalue.COMPLEX.make(true);

        if(eigenDecomposition.decompose(covMatrix)) {
            MatrixStore<ComplexNumber> eigenValues_unsorted = eigenDecomposition.getD();
            MatrixStore<ComplexNumber> eigenVectors_unsorted = eigenDecomposition.getV();

            PhysicalStore.Factory<ComplexNumber, ComplexDenseStore> storeFactory = ComplexDenseStore.FACTORY;

            int numberOfEigenvalues = (int) eigenValues_unsorted.countRows();
            ComplexDenseStore eigenValues_sorted = storeFactory.makeZero(numberOfEigenvalues, numberOfEigenvalues);
            ComplexDenseStore eigenVectors_sorted = storeFactory.makeZero(numberOfEigenvalues, numberOfEigenvalues);

            Double[] eigenValues_index = new Double[numberOfEigenvalues];

            // Get the index order of decreasing eigenvalues (this library doesn't organize the eigenvalues)
            for (int col = 0; col < numberOfEigenvalues; col++) {
                eigenValues_index[col] = eigenValues_unsorted.get(col,col).getReal();
            }

            IndexSorterDesc<Double> is = new IndexSorterDesc<Double>(eigenValues_index);
            is.sort();
            Integer[] eigenValues_i = is.getIndexes();

            for (int col = 0; col < numberOfEigenvalues; col++) {
                int col_original = eigenValues_i[col];
                double eigenSign = (int) Math.signum(eigenValues_index[col_original]);
                if (eigenSign < 0.0) {
                    eigenValues_sorted.set(col,col, eigenValues_unsorted.get(col_original,col_original).multiply(-1.0));
                } else {
                    eigenValues_sorted.set(col,col, eigenValues_unsorted.get(col_original,col_original));
                }

                for (int row = 0; row < eigenVectors_sorted.countRows(); row++) {
                    if (eigenSign < 0.0) {
                        eigenVectors_sorted.set(row,col, eigenVectors_unsorted.get(row,col_original).multiply(-1.0));
                    } else {
                        eigenVectors_sorted.set(row,col, eigenVectors_unsorted.get(row,col_original));
                    }
                }
            }

            this._eigenValues = eigenValues_sorted;
            this._eigenVectors = eigenVectors_sorted;
            
            // Calculate variance Explained
            double eigenValueTotal = 0.0;
            double[] varExplained = new double[numberOfEigenvalues];
            for (int col = 0; col < numberOfEigenvalues; col++) {
                double currValue = eigenValues_sorted.get(col,col).getReal();
                varExplained[col] = currValue;
                eigenValueTotal += currValue;
            }
            
            for (int col = 0; col < numberOfEigenvalues; col++) {
                varExplained[col] = varExplained[col] / eigenValueTotal;
            }
            this._varianceExplained = varExplained;
        }
   
    }
    
}

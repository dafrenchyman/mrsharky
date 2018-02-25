/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataAnalysis;

import com.mrsharky.helpers.IndexSorterDesc;
import com.mrsharky.helpers.PcaCov_test;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ojalgo.array.Array1D;
import org.ojalgo.matrix.BasicMatrix;
import org.ojalgo.matrix.ComplexMatrix;
import org.ojalgo.matrix.decomposition.Eigenvalue;
import org.ojalgo.matrix.decomposition.LU;
import org.ojalgo.matrix.decomposition.MatrixDecomposition;
import org.ojalgo.matrix.decomposition.QR;
import org.ojalgo.matrix.store.ComplexDenseStore;
import org.ojalgo.matrix.store.ElementsSupplier;
import org.ojalgo.matrix.store.MatrixStore;
import org.ojalgo.matrix.store.PhysicalStore;
import org.ojalgo.matrix.task.InverterTask;
import org.ojalgo.matrix.task.SolverTask;
import org.ojalgo.matrix.task.TaskException;
import org.ojalgo.netio.BasicLogger;
import org.ojalgo.random.Weibull;
import org.ojalgo.scalar.ComplexNumber;




/**
 *
 * @author mrsharky
 */
public class PcaCovOjalgo {
    
    
    static ComplexNumber c(double a, double b){
        return ComplexNumber.of(a, b);
    }
    
    public PcaCovOjalgo() {
        
        
        // ComplexNumber Test (sanity check, looks good)
        if (true) {
            ComplexNumber testComplex1 = ComplexNumber.of(1, -1);
            
            BasicLogger.debug("Number To Square: " + testComplex1.getReal() + ", " + testComplex1.getImaginary() + "i");
            ComplexNumber square = testComplex1.multiply(testComplex1);
            BasicLogger.debug("Squared Value: " + square.getReal() + ", " + square.getImaginary() + "i");
            
        }
        
        if (true) {
            int m = 10;
            int numOfDates = 20;
            int numOfGridBox = (int) Math.round(Math.pow(m,2.0));

            PhysicalStore.Factory<ComplexNumber, ComplexDenseStore> storeFactory = ComplexDenseStore.FACTORY;
            ComplexDenseStore Qrealizations = storeFactory.makeZero(numOfGridBox, numOfDates);
            for (int row = 0; row < numOfGridBox; row++) {
                for (int col = 0; col < numOfDates; col++) {
                    double real = (Math.random()-0.5) * 2.0;
                    double imag = (Math.random()-0.5) * 2.0;
                    Qrealizations.set(row, col, ComplexNumber.of(real, imag));
                    //Qrealizations.set(col, row, ComplexNumber.of(real, -1*imag));
                }
            }
            
            BasicLogger.debug("Dataset: ", Qrealizations);
            
            // TODO: Mean zero Qrealizations
            MatrixStore<ComplexNumber> Qrealizations_trans = Qrealizations.conjugate();
            BasicLogger.debug("Dataset Transpose: ", Qrealizations_trans);
            MatrixStore<ComplexNumber> R_hat_s = (Qrealizations.multiply(Qrealizations_trans)).multiply(1/(numOfDates + 0.0));
            
            
            BasicLogger.debug("Covariance Matrix", R_hat_s);
              
            Eigenvalue<ComplexNumber> eigenDecomposition;
            
            if (false) {
                //eigenDecomposition = Eigenvalue.makeComplex();
            } else {
                eigenDecomposition = Eigenvalue.COMPLEX.make(true);
                
                if (false) { // both these fail, when I try: eigenDecomposition.decompose(R_hat_s)
                    eigenDecomposition = Eigenvalue.COMPLEX.make();
                    eigenDecomposition = Eigenvalue.COMPLEX.make(R_hat_s);
                }
            }
            boolean worked2 = eigenDecomposition.decompose(R_hat_s);
            List<ComplexNumber> eigenValues = eigenDecomposition.getEigenvalues();
            MatrixStore<ComplexNumber> eigen = eigenDecomposition.getD();
            MatrixStore<ComplexNumber> eigenVector = eigenDecomposition.getV();
 
            // Unordered test
            if (false) {                
                BasicLogger.debug("EigenValues", eigen);    
                BasicLogger.debug("EigenVector", eigenVector);

                try {
                    InverterTask<ComplexNumber> invTask = InverterTask.COMPLEX.make(eigenVector);
                    MatrixStore<ComplexNumber> eigenVectorInv = invTask.invert(eigenVector);
                    BasicLogger.debug("EigenVector Inverse", eigenVectorInv);
                    MatrixStore<ComplexNumber> rebuilt = eigenVector.multiply(eigen).multiply(eigenVectorInv);
                    BasicLogger.debug("Orig", R_hat_s);
                    BasicLogger.debug("Rebuilt", rebuilt);
                    MatrixStore error = rebuilt.add(R_hat_s.multiply(-1));
                    BasicLogger.debug("ERROR", error);

                } catch (TaskException ex) {
                    Logger.getLogger(PcaCovOjalgo.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
            // ordered and positive test
            if (false) {

                int numberOfEigenvalues = (int) eigen.countRows();
                ComplexDenseStore eigenToSort = storeFactory.makeZero(numberOfEigenvalues, numberOfEigenvalues);
                ComplexDenseStore eigenVectorToSort = storeFactory.makeZero(numberOfEigenvalues, numberOfEigenvalues);

                int[]      eigenValues_neg = new int[numberOfEigenvalues];
                Double[] eigenValues_index = new Double[numberOfEigenvalues];

                // Get the index order of decreasing eigenvalues
                for (int col = 0; col < numberOfEigenvalues; col++) {
                    eigenValues_index[col] = eigen.get(col,col).getReal();
                }

                IndexSorterDesc<Double> is = new IndexSorterDesc<Double>(eigenValues_index);
                is.sort();
                Integer[] eigenValues_i = is.getIndexes();
                System.out.print("Unsorted: ");
                for ( Double i : eigenValues_index ){
                    System.out.print(i);
                    System.out.print("\t");
                }

                System.out.println();
                System.out.print("Sorted");
                for ( Integer i : is.getIndexes() ){
                    System.out.print(eigenValues_index[i]);
                    System.out.print("\t");
                }

                //for (Integer col : is.getIndexes()) {
                for (int col = 0; col < numberOfEigenvalues; col++) {
                    int col_source = eigenValues_i[col];
                    eigenValues_neg[col_source] = (int) Math.signum(eigenValues_index[col_source]);
                    double eigenSign = eigenValues_neg[col_source];
                    if (eigenSign < 0.0) {
                        eigenToSort.set(col,col, eigen.get(col_source,col_source).multiply(-1.0));
                    } else {
                        eigenToSort.set(col,col, eigen.get(col_source,col_source));
                    }

                    for (int row = 0; row < eigenVectorToSort.countRows(); row++) {
                        if (eigenSign < 0.0) {
                            eigenVectorToSort.set(row,col, eigenVector.get(row,col_source).multiply(-1.0));
                        } else {
                            eigenVectorToSort.set(row,col, eigenVector.get(row,col_source));
                        }
                    }
                }


                try {
                    InverterTask<ComplexNumber> invTask = InverterTask.COMPLEX.make(eigenVectorToSort);
                    MatrixStore<ComplexNumber> eigenVectorInv = invTask.invert(eigenVectorToSort);

                    BasicLogger.debug("EigenVector Inverse", eigenVectorInv);
                    BasicLogger.debug("EigenValues", eigenToSort);

                    MatrixStore<ComplexNumber> rebuilt = eigenVectorToSort.multiply(eigenToSort).multiply(eigenVectorInv);

                    BasicLogger.debug("Orig", R_hat_s);
                    BasicLogger.debug("Rebuilt", rebuilt);
                    MatrixStore error = rebuilt.add(R_hat_s.multiply(-1));
                    BasicLogger.debug("ERROR", error);

                } catch (TaskException ex) {
                    Logger.getLogger(PcaCovOjalgo.class.getName()).log(Level.SEVERE, null, ex);
                }  


            }
            
            // Try the new function
            if (true) {
                PcaCov_test pcaTest = new PcaCov_test(R_hat_s);
                
                
                try {
                    InverterTask<ComplexNumber> invTask = InverterTask.COMPLEX.make(pcaTest.GetEigenVectors());
                    MatrixStore<ComplexNumber> eigenVectorInv = invTask.invert(pcaTest.GetEigenVectors());

                    BasicLogger.debug("EigenVector Inverse", eigenVectorInv);
                    BasicLogger.debug("EigenValues", pcaTest.GetEigenValues());

                    MatrixStore<ComplexNumber> rebuilt = pcaTest.GetEigenVectors().multiply(pcaTest.GetEigenValues()).multiply(eigenVectorInv);

                    BasicLogger.debug("Orig", R_hat_s);
                    BasicLogger.debug("Rebuilt", rebuilt);
                    MatrixStore error = rebuilt.add(R_hat_s.multiply(-1));
                    BasicLogger.debug("ERROR", error);
                    System.out.println("Variance Explained: ");
                    for (double vars : pcaTest.GetVarianceExplained()) {
                        System.out.println(vars);
                    }

                } catch (TaskException ex) {
                    Logger.getLogger(PcaCovOjalgo.class.getName()).log(Level.SEVERE, null, ex);
                }  
                
            }

            
            
        }
        
        
        
        
        
        
        
        
        
        
        

    }
    
    public static void main(String args[]) throws Exception {

        PcaCovOjalgo test = new PcaCovOjalgo();
        
    }
}

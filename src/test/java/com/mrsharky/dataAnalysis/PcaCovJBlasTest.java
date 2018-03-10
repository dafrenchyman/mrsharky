/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataAnalysis;

import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.JblasMatrixHelpers.Print;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.math3.complex.Complex;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mrsharky
 */
public class PcaCovJBlasTest {
    
    public PcaCovJBlasTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testMultithreading() throws Exception {
        int threads = Runtime.getRuntime().availableProcessors();
        int numRow = 50;
        int numCol = 50;
        ExecutorService service = Executors.newFixedThreadPool(threads);
        List<Future<PcaCovJBlas>> futures = new ArrayList<Future<PcaCovJBlas>>();

        System.out.println("Processing PCA - Multi-Threaded");
        for (int i = 0; i <= 10; i++) {
            final int count = i;
            Callable<PcaCovJBlas> callable = new Callable<PcaCovJBlas>() {
                public PcaCovJBlas call() throws Exception {
                    
                    
                    double[][] dbl_real = DoubleArray.RandomDoubleArray(numRow, numCol);
                    double[][] dbl_imag = DoubleArray.RandomDoubleArray(numRow, numCol);

                    ComplexDoubleMatrix X = new ComplexDoubleMatrix(
                            new DoubleMatrix(dbl_real),
                            new DoubleMatrix(dbl_imag));

                    // Get Column Means
                    ComplexDoubleMatrix colMeans = X.columnMeans();
                    X.subRowVector(colMeans);
                    //X = RowSubtractExpansion(X, colMeans);

                    // Create the Covarience Matrix (Should divide by number of dates)
                    ComplexDoubleMatrix X_conjTrans = X.conj().transpose();
                    ComplexDoubleMatrix X_cov = (X_conjTrans.mmul(X)).mul(1/(X.columns + 0.0));

                    System.out.println("Starting - PCA: " + count);
                    PcaCovJBlas pca = new PcaCovJBlas(X_cov);
                    System.out.println("Finished - PCA: " + count);
                    
                    
                    
                    return pca;
                }};
            futures.add(service.submit(callable));
        }
            
        // Wait for the threads to finish and then save all the dates
        service.shutdown();
        service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        for (Future<PcaCovJBlas> future : futures) {
            PcaCovJBlas pca = future.get();
            
            // Save results
            //Print(pca.GetEigenValues());
            //ComplexArray.Print(pca.GetEigenValuesMath3());
            //Print(pca.GetEigenVectors());
            //ComplexArray.Print(pca.GetEigenVectorsMath3());
            //DoubleArray.Print(pca.GetVarianceExplained());
        }

    }
    
}

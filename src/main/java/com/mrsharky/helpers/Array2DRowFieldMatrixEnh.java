/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import java.io.Serializable;
import org.apache.commons.math3.Field;
import org.apache.commons.math3.FieldElement;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NoDataException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author Julien Pierret
 */
public class Array2DRowFieldMatrixEnh<T extends FieldElement<T>>
    extends Array2DRowFieldMatrix<T>
    implements Serializable {
    
    public Array2DRowFieldMatrixEnh(Field<T> field) {
        super(field);
    }


    public Array2DRowFieldMatrixEnh(Field<T> field, int rowDimension, int columnDimension) throws NotStrictlyPositiveException {
        super(field, rowDimension, columnDimension);
    }

    public Array2DRowFieldMatrixEnh(T[][] d) throws DimensionMismatchException, NullArgumentException, NoDataException {
        super(d);
    }

    public Array2DRowFieldMatrixEnh(Field<T> field, T[][] d) throws DimensionMismatchException, NullArgumentException, NoDataException {
        super(field, d);
    }

    public Array2DRowFieldMatrixEnh(T[][] d, boolean copyArray) throws DimensionMismatchException, NoDataException, NullArgumentException {
        super(d, copyArray);
    }

    public Array2DRowFieldMatrixEnh(Field<T> field, T[][] d, boolean copyArray) throws DimensionMismatchException, NoDataException, NullArgumentException {
        super(field, d, copyArray);
    }

    public Array2DRowFieldMatrixEnh(T[] v) throws NoDataException {
        super(v);
    }

    public Array2DRowFieldMatrixEnh(Field<T> field, T[] v) {
        super(field, v);
    }
    
    public RealMatrix ComplexToRealMatrix() {
        double[][] data = new double[this.getRowDimension()][this.getColumnDimension()];
        for (int row = 0; row < this.getRowDimension(); row++) {
            for (int col = 0; col < this.getColumnDimension(); col++) {
                Complex currValue = (Complex) this.getEntry(row, col);
                data[row][col] = currValue.getReal();
            }
        }
        return MatrixUtils.createRealMatrix(data);
    }
}

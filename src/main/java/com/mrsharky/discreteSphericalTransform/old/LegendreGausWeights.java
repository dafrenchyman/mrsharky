/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform.old;

import com.mrsharky.helpers.MatrixUtilities;
import static com.mrsharky.helpers.MatrixUtilities.MatrixAbs;
import static com.mrsharky.helpers.MatrixUtilities.MatrixMax;
import static com.mrsharky.helpers.Utilities.ArrayOnes;
import static com.mrsharky.helpers.Utilities.arange;
import static com.mrsharky.helpers.Utilities.linspace;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import static com.mrsharky.helpers.MatrixUtilities.ElementwiseMultiplication;
import static com.mrsharky.helpers.MatrixUtilities.PrintMatrix;
import static com.mrsharky.helpers.MatrixUtilities.ElementwiseMatrixPower;

/**
 *
 * @author dafre
 */
public class LegendreGausWeights {
    private int _N;
    private int _N1;
    private int _N2;
    private double _a;
    private double _b;
    private RealMatrix _x;
    private RealMatrix _w;
    
    public RealMatrix GetWeights() {
        return _w;
    }
    
    public RealMatrix GetValues() {
        return _x; 
   }
    
    public LegendreGausWeights(int n, double a, double b) throws Exception {
        if (n < 0) {
            throw new Exception ("Input argument n must be greater than 1: " + n);
        }
        this._N = n-1;
        this._N1 = _N + 1;
        this._N2 = _N + 2;
        this._a = a;
        this._b = b;
        
        RealMatrix xu = new Array2DRowRealMatrix(linspace(a,b,_N1));
        RealMatrix zeroToN1 = new Array2DRowRealMatrix(arange(0,_N1,1));
        
        // Calculate inital guess
        RealMatrix left = (zeroToN1.scalarMultiply(2.0).scalarAdd(1.0)).scalarMultiply((Math.PI)/(2.0*_N+2.0));
        left = MatrixUtilities.Cos(left);
        
        RealMatrix right = xu.scalarMultiply(Math.PI*_N/_N2);
        right = MatrixUtilities.Sin(right).scalarMultiply(0.27/_N1);
        
        RealMatrix y = left.add(right);
        /*for (int i = 0; i < y.getRowDimension(); i++) {
            System.out.println(y.getEntry(i, 0));
        }*/

        // Legendre-Gauss CosineOfMatrixValuesandermonde Matrix
        RealMatrix L =  MatrixUtils.createRealMatrix(_N1, _N2);
        RealMatrix Lp =  MatrixUtils.createRealMatrix(1, _N2);
        
        double epsilonCompare = MatrixMax(MatrixAbs(y.scalarAdd(2)));
        while (epsilonCompare > Math.ulp(1.0)) {
            L.setColumn(0, ArrayOnes(L.getRowDimension()));
            L.setColumnMatrix(1, y);
            
            double[] l_loop  = arange(2,_N1+1.0,1);
            
            for (double k : l_loop) {
                int k_ = (int) Math.round(k);
                RealMatrix loopLeft = ElementwiseMultiplication(y,L.getColumnMatrix(k_-1)).
                        scalarMultiply(2.0*k-1);
                RealMatrix loopRight = L.getColumnMatrix(((int)Math.round(k))-2).scalarMultiply(k-1);
                L.setColumnMatrix(k_, (loopLeft.add(loopRight.scalarMultiply(-1.0))).scalarMultiply(1.0/k));
            }
            
            // Derivative of LGVM
            RealMatrix lgvm_numerator = L.getColumnMatrix(_N1-1).add(ElementwiseMultiplication(y,L.getColumnMatrix(_N2-1)).scalarMultiply(-1.0))
                    .scalarMultiply(_N2);
            RealMatrix lgvm_denominator = ElementwiseMatrixPower(
                            ElementwiseMatrixPower(y,2.0).scalarMultiply(-1.0).scalarAdd(1.0), -1.0);
            Lp = ElementwiseMultiplication (lgvm_numerator,lgvm_denominator);
            
            RealMatrix y0 = y.copy();
            
            y = y0.add(ElementwiseMultiplication(L.getColumnMatrix(_N2-1).scalarMultiply(-1.0),
                    ElementwiseMatrixPower(Lp,-1.0)));
            epsilonCompare = MatrixMax(MatrixAbs(y.add(y0.scalarMultiply(-1.0))));
        }
        
        // Linear map from [-1,1] to [a,b]
        RealMatrix x_left = (y.scalarMultiply(-1.0).scalarAdd(1.0)).scalarMultiply(a);
        RealMatrix x_right = (y.scalarAdd(1.0)).scalarMultiply(b);
        _x = (x_left.add(x_right)).scalarMultiply(1.0/2.0);
        
        // Compute the weights
        RealMatrix w_left = ElementwiseMatrixPower(y,2.0).scalarMultiply(-1.0).scalarAdd(1.0);
        RealMatrix w_right = ElementwiseMatrixPower(Lp, 2.0);
        double w_scalar = (b-a)*Math.pow(((double)_N2)/((double)_N1),2.0);
        _w = ElementwiseMatrixPower(ElementwiseMultiplication(w_left,w_right),-1.0)
                .scalarMultiply(w_scalar);
    }
    
    
    public static void main(String args[]) {
        try {
            LegendreGausWeights lgwt = new LegendreGausWeights(101, -1.0, 1.0);
            PrintMatrix(lgwt.GetValues());
            
            PrintMatrix(lgwt.GetWeights());
           
        } catch (Exception ex) {
            Logger.getLogger(LegendreGausWeights.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

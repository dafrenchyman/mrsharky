/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import static com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform.GenerateL2M1;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import java.io.Serializable;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonicY implements Serializable {
    private SphericalHarmonic[] _data;
    private int _q;
    
    public SphericalHarmonic[] GetData() {
        return _data;
    }
    
    public int Size(){
        return _data.length;
    }
    
    public int GetQ(){
        return _q;
    }
    
    public SphericalHarmonicY(SphericalHarmonic[] s) {
        _data = s;
        _q = s[0].GetQ();
    }
    
    
    public SphericalHarmonicY(SphericalHarmonicY s) throws Exception {
        SphericalHarmonic[] data = s.GetData();
        _data = new SphericalHarmonic[data.length];
        _q = s.GetQ();
        for (int i = 0; i < _data.length; i++) {            
            _data[i] = new SphericalHarmonic(_q);
            _data[i].SetHalfCompressedSpectra(data[i].GetHalfCompressedSpectra());
        }        
    }
    
    public SphericalHarmonicY Clone() throws Exception {
        SphericalHarmonicY newS = new SphericalHarmonicY(this);
        return newS;
    }
    
    private Complex[][] ExponentialHelper(int L, double[] theta) {
        Complex[][] output = new Complex[L+1][theta.length];
        for (int l = 0; l <= L; l++) {
            for (int t = 0; t < theta.length; t++) {
                Complex calc = new Complex(0,1);
                calc = calc.multiply(l).multiply(theta[t]).exp();
                output[l][t] = calc;
            }
        }
        return output;
    }
    
    public SphericalHarmonicY (double[] latPoints, double[] lonPoints, int q) throws Exception {
        if (latPoints.length != lonPoints.length) {
            throw new Exception("number of latPoints and number of lonPoints must be the same");
        }
        
        double[] latPoints_c = new double[latPoints.length];
        double[] lonPoints_c = new double[lonPoints.length];
        
        // Check lat/lon points are valid
        for (int i = 0; i < latPoints.length; i++) {
            double currLat = latPoints[i];
            double currLon = lonPoints[i];
            if (currLat < -Math.PI/2.0 -(10e-10)  || currLat > Math.PI/2.0 +(10e-10)) {
                throw new Exception("Lat points need to be between -pi/2 & pi/2");
            }
            
            if (currLon < -Math.PI -(10e-10) || currLon > Math.PI + (10e-10) ) {
                throw new Exception("Lon points need to be between -pi & pi");
            }
            
            // Adjust lat so its in ranges 0 < lat < pi 
            latPoints_c[i] = currLat + Math.PI/2.0;
            
            // Adjust lon so its in ranges 0 < lon < 2pi
            lonPoints_c[i] = currLon + Math.PI;
        }
        
        _q = q;
        _data = new SphericalHarmonic[latPoints_c.length];
        
        for (int i = 0; i < _data.length; i++) {
            _data[i] = new SphericalHarmonic(q);
        }

        double[] phi = DoubleArray.Cos(latPoints_c);
        SphericalAssociatedLegendrePolynomials P_k_l = new SphericalAssociatedLegendrePolynomials(q,phi);
        double[] theta = lonPoints_c;
        Complex[][] expHelp = this.ExponentialHelper(q, theta);
            
        for (int k = 0; k <= q; k++) {
            for (int l = 0; l <= k; l++) {
                double[] pkl = P_k_l.GetAsDouble(k, l);
                Complex[] thetaExp  = ComplexArray.GetRow(expHelp, l);
                Complex[] currY     = ComplexArray.Multiply(pkl, thetaExp);
                double osci = Math.pow(-1.0, l);
                currY = ComplexArray.Multiply(currY, osci);
                
                for (int i = 0; i < _data.length; i++) {
                    _data[i].SetHarmonic(k, l, currY[i]);
                }   
            }
        }
    }
        
    public SphericalHarmonicY Conjugate() throws Exception {
        SphericalHarmonicY newSpherical = this.Clone();
        SphericalHarmonic[] data = newSpherical.GetData();
        for (int i = 0; i < data.length; i++) {
            newSpherical.SetHalfCompressedSpectra(i, data[i].Conjugate().GetHalfCompressedSpectra());
        }
        return newSpherical;
    }
    
    public void SetHalfCompressedSpectra(int i, Complex[] halfCompressedSpectra) throws Exception {
        if (halfCompressedSpectra.length != this._data[i].GetHalfCompressedSpectra().length) {
            throw new Exception("Length of Spectra submitted doesn't match the length expected");
        }
        _data[i].SetHalfCompressedSpectra(halfCompressedSpectra);
    }
    
    public Complex[] GetFullCompressedSpectra(int i) throws Exception {
        return _data[i].GetFullCompressedSpectra();
    }
    
    public Complex[][] GetFullSpectral(int i) throws Exception {
        return _data[i].GetFullSpectral();
    }
    
    public Complex[] GetHalfCompressedSpectra(int i) {
        return _data[i].GetHalfCompressedSpectra();
    }
    
    public Complex GetHarmonic(int i, int k, int l) throws Exception {
        Complex currValue = this._data[i].GetHarmonic(k, l);
        return currValue;
    }
    
    public Complex[] GetHarmonic(int k, int l) throws Exception {
        Complex[] values = new Complex[_data.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = _data[i].GetHarmonic(k, l);
        }
        return values;
    }
    
    public void SetHarmonic(int i, int k, int l, Complex value) throws Exception {
        this._data[i].SetHarmonic(k, l, value);
    }
    
    public static void main(String[] args) throws Exception {

        Complex[] test = new Complex[21]; 
        for (int i = 0; i < test.length; i++) {
            test[i] = new Complex(i,0);
        }
        
        

        int lmax = 3;		// maximum degree of spherical harmonics
        int nlat = 32;		// number of points in the latitude direction  (constraint: nlat >= lmax+1)
        int nphi = 10;   
        double[][] DATA2 = GenerateL2M1(lmax, nlat, nphi);
        int M = nlat;
        int N = nphi;
        int Q = lmax;
        
        //int Q = 5;
        DiscreteSphericalTransform dst = new DiscreteSphericalTransform(DATA2, lmax, true);
        
        double[] lats = dst.GetLatitudeCoordinates();
        double[] lons = dst.GetLongitudeCoordinates();
          
        double[] newLats = new double[lats.length*lons.length];
        double[] newLons = new double[lats.length*lons.length];
        int counter = 0;
        for (int i = 0; i < lats.length; i++) {
            for (int j = 0; j < lons.length; j++) {
                newLats[counter] = lats[i];
                newLons[counter] = lons[j];
                counter++;
            }
        }

        
        SphericalHarmonicY sh = new SphericalHarmonicY(newLats, newLons, Q);
        
        for (int i = 0; i < sh.Size(); i++) {
            System.out.println("Location: " + i);
            ComplexArray.Print(sh.GetFullSpectral(i));
        }
        
        
    }
}

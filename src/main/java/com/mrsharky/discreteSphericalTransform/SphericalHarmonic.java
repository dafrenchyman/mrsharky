/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import com.mrsharky.helpers.ComplexArray;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.complex.Complex;
import org.javatuples.Triplet;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonic implements Serializable {
    private Complex[] _data;
    private int _q;
    private int[] _firstKLookup;
    
    
    public SphericalHarmonic Multiply(SphericalHarmonic sh2) throws Exception {
        SphericalHarmonic sh1 = this;
        SphericalHarmonic sh3 = new SphericalHarmonic(sh1.GetQ() + sh2.GetQ());
        
        // Generate the new harmonic
        List<Triplet<Integer, Integer, Integer>> loopsToSkip = new ArrayList<Triplet<Integer, Integer, Integer>>();
        long counter = 0;
        for (int k = 0; k <= sh3.GetQ(); k++) {
            double constant_k = (4*Math.PI*(2.0*k + 1));
            
            // Loop on first harmonic
            for (int a_k = 0; a_k <= sh1.GetQ(); a_k++) {
                double constant_a_k = (2*a_k + 1);
                
                // Math.min so we always pass the triangle inequality
                for (int b_k = 0; b_k <= Math.min(k + a_k, sh2.GetQ()); b_k++) {

                    if ((a_k + b_k + k) % 2 == 0) {

                        double constant_b_k = (2*b_k + 1);

                        double coef1 = SphericalHelpers.ClebschGordanCoefficients2(a_k, 0, b_k, 0, k, 0);
                        if (coef1 == 0.0) {
                            continue;
                        }

                        double frac = Math.sqrt(constant_a_k * constant_b_k / constant_k);

                        // We can only have 1 valid b_l because of Wigner 3j rules used by ClebschGordanCoeff
                        for (int l = 0; l <= k; l++) {
                            for (int a_l = -a_k; a_l <= a_k; a_l++) {
                                
                                int b_l = l - a_l;
                                if (Math.abs(b_l) > b_k){
                                    continue; 
                                }

                                double coef2 = SphericalHelpers.ClebschGordanCoefficients2(a_k, a_l, b_k, b_l, k, l);
                                
                                if (coef2 == 0.0) {
                                    continue; 
                                }
                                
                                Complex oldHarm1 = sh1.GetHarmonic(a_k, a_l);
                                Complex oldHarm2 = sh2.GetHarmonic(b_k, b_l);
                                
                                double bigConstant = 0;
                                Complex newHarm = new Complex(0.0, 0.0);
                                //if (a_l == 0) {
                                    bigConstant = frac*(coef1*coef2);
                                    newHarm = oldHarm1.multiply(oldHarm2).multiply(bigConstant);
                                /*} else {
                                    double coef2_sym = Math.pow(-1.0, a_k + b_k - k) * coef2;
                                    double coef1_sym = Math.pow(-1.0, a_k + b_k - k) * coef1;
                                    Complex oldHarm1_sym = sh1.GetHarmonic(a_k, -a_l);
                                    oldHarm1_sym = oldHarm1_sym.multiply(coef1_sym*coef2_sym);
                                    oldHarm1 = oldHarm1.multiply(coef1*coef2);
                                    newHarm = (oldHarm1.add(oldHarm1_sym)).multiply(oldHarm2).multiply(frac);
                                }*/
                                Complex addHarm = sh3.GetHarmonic(k, l).add(newHarm);
                                sh3.SetHarmonic(k, l, addHarm);
                                counter++;
                            }
                        }
                    }
                }
            }
        }
        //System.out.println("counter: " + counter);
        return sh3;
    }
    
    public SphericalHarmonic Multiply2(SphericalHarmonic sh2) throws Exception {
        SphericalHarmonic sh1 = this;
        SphericalHarmonic sh3 = new SphericalHarmonic(sh1.GetQ() + sh2.GetQ());
        
        long counter = 0;
        for (int k = 0; k <= sh3.GetQ(); k++) {
            double constant_k = (4*Math.PI*(2.0*k + 1));
            for (int l = 0; l <= k; l++) {
        
                // Loop on first harmonic
                for (int a_k = 0; a_k <= sh1.GetQ(); a_k++) {
                    double constant_a_k = (2*a_k + 1);
                    for (int a_l = -a_k; a_l <= a_k; a_l++) {
                        Complex oldHarm1 = sh1.GetHarmonic(a_k, a_l);

                        // Loop on the second harmonic
                        
                        boolean oddValue = a_k + k % 2 == 1 ? true : false;
                        
                        // Math.min so we always pass the triangle inequality
                        for (int b_k = 0; b_k <= Math.min(k + a_k, sh2.GetQ()); b_k++) {
                            
                            if ((a_k + b_k + k) % 2 == 0) {
                            
                                double constant_b_k = (2*b_k + 1);

                                double coef1 = SphericalHelpers.ClebschGordanCoefficients2(a_k, 0, b_k, 0, k, 0);
                                if (coef1 == 0.0) {
                                    continue;
                                }

                                double frac = Math.sqrt(constant_a_k * constant_b_k / constant_k);

                                // We can only have 1 valid b_l because of Wigner 3j rules used by ClebschGordanCoeff
                                int b_l = l - a_l;
                                if (Math.abs(b_l) > b_k){
                                    continue; 
                                }


                                double coef2 = SphericalHelpers.ClebschGordanCoefficients2(a_k, a_l, b_k, b_l, k, l);
                                if (coef2 == 0.0) {
                                    continue; 
                                }

                                double bigConstant = frac*coef1*coef2;
                                Complex oldHarm2 = sh2.GetHarmonic(b_k, b_l);

                                Complex newHarm = oldHarm1.multiply(oldHarm2).multiply(bigConstant);
                                Complex addHarm = sh3.GetHarmonic(k, l).add(newHarm);
                                sh3.SetHarmonic(k, l, addHarm);
                                counter++;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("counter: " + counter);
        return sh3;
    }
    
    public void PrintHarmonic() throws Exception {
        System.out.print("k\\l");
        
        // Print header
        for (int k = 0; k <= _q; k++) {
            System.out.print("\t" + k);
        }
        System.out.println();
        
        for (int k = 0; k <= _q; k++) {
            System.out.print(k);
            for (int l = 0; l <= k; l++) {
                Complex currValue = GetHarmonic(k, l);
                System.out.print("\t" + currValue);
            }
            System.out.println();
        }
    }
    
    
    public SphericalHarmonic(SphericalHarmonic sp) {
        _data = sp.GetHalfCompressedSpectra();
        _q = sp.GetQ();
        GenerateFirstLookup();
    }
    
    public SphericalHarmonic(Complex[] compressedSpectral, boolean half) throws Exception {
        if (half) {
            _data = new Complex[compressedSpectral.length];
            for (int i = 0; i < _data.length; i++) {
                Complex curr = compressedSpectral[i];
                _data[i] = new Complex(curr.getReal(), curr.getImaginary());
            }
            double sqrt = Math.sqrt(9 + 8*(compressedSpectral.length-1));
            if (sqrt != ((int) sqrt) + 0.0) {
                throw new Exception("Invalid length on input");
            }
            _q = (int) ((-3.0 + sqrt) / 2.0);
            GenerateFirstLookup();
        } else { // full compressed one (both +/-l
            _q = (int) Math.sqrt(compressedSpectral.length) - 1;
            _data = new Complex[CalculateNumPointsFromQ(_q)];
            for (int i = 0; i < _data.length; i++) {
                _data[i] = new Complex(0,0);
            }
            GenerateFirstLookup();
            int counter = 0;
            for (int k = 0; k <= _q; k++) {
                for (int l = -k; l <= k; l++) {
                    Complex currValue = compressedSpectral[counter++];
                    if (l >= 0) {
                        SetHarmonic(k, l, currValue);
                    }
                }
            }
        }
    }
    
    public static int CalculateNumPointsFromQ(int q) {
        return (int) (Math.pow(q+1,2.0)/ 2.0 + (q/2.0 +0.5));
    }
    
    public SphericalHarmonic(int q) {
        _q = q;
        int numPoints = CalculateNumPointsFromQ(q);
        _data = new Complex[numPoints];
        for (int i = 0; i < _data.length; i++) {
            _data[i] = new Complex(0,0);
        }
        GenerateFirstLookup();
    }
    
    public SphericalHarmonic Clone() {
        SphericalHarmonic newS = new SphericalHarmonic(this);
        return newS;
    }
    
    public SphericalHarmonic Multiply(double constant) throws Exception {
        SphericalHarmonic newSpherical = this.Clone();
        Complex[] newData = new Complex[this._data.length];
        for (int i = 0; i < _data.length; i++) {
            newData[i] = _data[i].multiply(constant);
        }
        newSpherical.SetHalfCompressedSpectra(newData);
        return newSpherical;
    }
    
    public SphericalHarmonic Conjugate() throws Exception {
        SphericalHarmonic newSpherical = this.Clone();
        Complex[] newData = new Complex[this._data.length];
        for (int k = 0; k <= this._q; k++) {
            for (int l = 0; l <= k; l++) {
                double oscil = Math.pow(-1, l);
                int currIndex = GetIndex(k, l);
                newData[currIndex] = GetHarmonic(k,l).conjugate().multiply(oscil);
            }
        }
        newSpherical.SetHalfCompressedSpectra(newData);
        return newSpherical;
    }
    
    public void SetHalfCompressedSpectra(Complex[] halfCompressedSpectra) throws Exception {
        if (halfCompressedSpectra.length != this._data.length) {
            throw new Exception("Length of Spectra submitted doesn't match the length expected");
        }
        for (int i = 0; i < _data.length; i++) {
            Complex curr = halfCompressedSpectra[i];
            _data[i] = new Complex(curr.getReal(), curr.getImaginary());
        }
    }
    
    public int GetQ() {
        return _q;
    }
    
    private void GenerateFirstLookup() {
        // Generate the "quick lookup" values
        int lastLookup = 0;
        _firstKLookup = new int[_q +1];
        for (int i = 0; i <= _q; i++) {
            _firstKLookup[i] = i + lastLookup;
            lastLookup = _firstKLookup[i];
        }
    }
    
    public Complex[] GetFullCompressedSpectra() throws Exception {
        int size = (int) Math.pow(_q+1.0, 2.0);
        Complex[] fullCompressedSpectra = new Complex[size];
        int counter = 0;
        for (int k = 0; k <= _q; k++) {
            for (int l = -k; l <= k; l++) {
                fullCompressedSpectra[counter++] = GetHarmonic(k, l);
            }
        }
        return fullCompressedSpectra;
    }
    
    public Complex[][] GetFullSpectral() throws Exception {
        Complex[][] uncompressedSpectral = ComplexArray.CreateComplex(_q+1,2*(_q+1)-1);
        for (int k = 0; k <= _q; k++) {
            for (int l = -k; l <= k; l++) {
                uncompressedSpectral[k][l+(_q)] = GetHarmonic(k, l);
            }
        }
        return uncompressedSpectral;
    }
    
    public Complex[][] GetHalfSpectral() throws Exception {
        Complex[][] uncompressedSpectral = ComplexArray.CreateComplex(_q+1,_q+1);
        for (int k = 0; k <= _q; k++) {
            for (int l = 0; l <= k; l++) {
                uncompressedSpectral[k][l] = GetHarmonic(k, l);
            }
        }
        return uncompressedSpectral;
    }
    
    public Complex[] GetHalfCompressedSpectra() {
        Complex[] newData = new Complex[_data.length];
        for (int i = 0; i < newData.length; i++) {
            Complex curr = _data[i];
            newData[i] = new Complex(curr.getReal(), curr.getImaginary());
        }
        return newData;
    }
    
    public Complex GetHarmonic(int k, int l) throws Exception {
        int index = GetIndex(k,l);
        Complex newValue = new Complex(0.0, 0.0);
        if (index + 1 <= _data.length ) {
            Complex currValue = this._data[GetIndex(k,l)];
            newValue = new Complex(currValue.getReal(), currValue.getImaginary());
            if (l < 0) {
                double oscil = Math.pow(-1, l);
                newValue = newValue.conjugate().multiply(oscil);
            }
        }
        return newValue;
    }
    
    public synchronized void SetHarmonic(int k, int l, Complex value) throws Exception {
        Complex newValue = new Complex(value.getReal(), value.getImaginary());
        if (l < 0) {
            double oscil = Math.pow(-1, l);
            newValue = newValue.conjugate().multiply(oscil);
        }
        int index = GetIndex(k,l);
        if (index + 1 > _data.length) {
            throw new Exception("Error: k=" + k + ",l=" + l +" harmonic doesn't exist for q=" + _q);
        }
        this._data[index] = newValue;
    }
    
    private int GetIndex(int k, int l) throws Exception {
        if (Math.abs(l) > k) {
            throw new Exception("abs(l) can't be greater than k!");
        }
        int index; // = _firstKLookup[k] + Math.abs(l);
        if (true) {
            index = (int) (Math.pow(k,2) + k + 2)/2 + Math.abs(l) -1;
        } 
        return index;
    }
    
    public static void main(String[] args) throws Exception {

        Complex[] test = new Complex[21]; 
        for (int i = 0; i < test.length; i++) {
            test[i] = new Complex(i,0);
        }
        
        SphericalHarmonic sh = new SphericalHarmonic(test, true);
        System.out.println(sh.GetHarmonic(5, 1));
        System.out.println(sh.GetHarmonic(5, -1));
        
        ComplexArray.Print(sh.GetHalfCompressedSpectra());
        ComplexArray.Print(sh.GetFullCompressedSpectra());
        ComplexArray.Print(sh.GetFullSpectral());
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.discreteSphericalTransform;

import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLatitudeCoordinates;
import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLongitudeCoordinates;
import com.mrsharky.helpers.ComplexArray;
import com.mrsharky.helpers.DoubleArray;
import com.mrsharky.helpers.LegendreGausWeights;
import static com.mrsharky.helpers.Utilities.AreaQuad;
import static com.mrsharky.helpers.Utilities.RadiansToLatitude;
import static com.mrsharky.helpers.Utilities.RadiansToLongitude;
import static com.mrsharky.helpers.Utilities.linspace;
import com.svengato.math.HalfInteger;
import com.svengato.math.Wigner3jSymbol;
import org.apache.commons.math3.complex.Complex;

/**
 *
 * @author mrsharky
 */
public class SphericalHelpers {
    
    public static double ClebschGordanCoefficients2(int j1, int m1, int j2, int m2, int J, int M)  {
        double osc1 = Math.pow(-1, -j1+j2-M);
        double sqrt = Math.sqrt(2*J + 1);
        
        String j1_s = String.valueOf(j1);
        String J_s = String.valueOf(J);
        String j2_s = String.valueOf(j2);
        String m1_s = String.valueOf(m1);
        String M_s = String.valueOf(-M);
        String m2_s = String.valueOf(m2);
        Wigner3jSymbol wigner3j;
        double value = 0.0;
        try {
            wigner3j = new Wigner3jSymbol(
                    HalfInteger.valueOf(j1_s),
                    HalfInteger.valueOf(j2_s),
                    HalfInteger.valueOf(J_s),
                    HalfInteger.valueOf(m1_s),
                    HalfInteger.valueOf(m2_s),
                    HalfInteger.valueOf(M_s));
            value = osc1*sqrt*wigner3j.doubleValue();
        } catch (Exception ex) {
            value = 0.0;
        }
        return value;
    }
    
    public static double ClebschGordanCoefficients(int j1, int m1, int j2, int m2, int J, int M)  {
        double osc1 = Math.pow(-1, 2*j2);
        double osc2 = Math.pow(-1, J-M);
        double sqrt = Math.sqrt(2*J + 1);
        
        String j1_s = String.valueOf(j1);
        String J_s = String.valueOf(J);
        String j2_s = String.valueOf(j2);
        String m1_s = String.valueOf(m1);
        String M_s = String.valueOf(-M);
        String m2_s = String.valueOf(m2);
        Wigner3jSymbol wigner3j;
        double value = 0.0;
        try {
            wigner3j = new Wigner3jSymbol(
                    HalfInteger.valueOf(j1_s),
                    HalfInteger.valueOf(J_s),
                    HalfInteger.valueOf(j2_s),
                    HalfInteger.valueOf(m1_s),
                    HalfInteger.valueOf(M_s),
                    HalfInteger.valueOf(m2_s));
            value = osc1*osc2*sqrt*wigner3j.doubleValue();
        } catch (Exception ex) {
            value = 0.0;
        }
        return value;
    }
    
    public static double[][] GenerateL2M1(int lmax, int nlat, int nphi) throws Exception {
        double[][] data = new double[1][1];

        LegendreGausWeights lgw = new LegendreGausWeights(nlat,-1,1);
        double[] legZerosM = lgw.GetValues();
        double[] gausWeights = lgw.GetWeights();
        double[] legZerosRad = DoubleArray.Add(DoubleArray.ArcSin(legZerosM), (Math.PI/2.0));   // TESTED

        double[] phi2 = DoubleArray.Cos(legZerosRad);                            // TESTED

        data = new double[nlat][nphi];

        // generate some gridded data:
        for (int ip=0; ip<nphi; ip++) {		// loop on longitude
            double phi = (2.*Math.PI/(nphi)) * ip;
            double cos_phi = Math.cos(phi);
            for (int it=0; it<nlat; it++) {		// loop on latitude
                //double cos_theta = shtns->ct[it];
                double cos_theta = phi2[it];
                double sin_theta = Math.sqrt(1.0 - cos_theta*cos_theta);
                data[it][ip] = cos_phi*cos_theta*sin_theta;
                //Sh[ip*nlat + it] = cos_phi*cos_theta*sin_theta;	// this is an m=1, l=2 harmonic [mres==1 is thus required]
            }
        }
        return data;
    }
    
    public static double[][] GenerateSinCosData (int xSize, int ySize) {
        double xLength = 4*Math.PI;
        double[] x = linspace(0,xLength,xSize+1);

        double yLength = 4*Math.PI;
        double[] y = linspace(0,yLength,ySize+1);

        MeshGrid mesh = new MeshGrid(x,y);
        double[][] X = mesh.GetX();
        double[][] Y = mesh.GetY();

        double[][] data = new double[y.length][x.length];

        for (int row = 0; row < y.length; row++) {
            for (int col = 0; col < x.length; col++) {
                data[row][col] = Math.sin(X[row][col]) + Math.cos(Y[row][col]) + 1.0;
            }
        }
        return data;
    }
    
    
    /**
     * Converts non-compressed Spectral data to compressed
     * @param spectral Non-compressed spectral data generated from {@link DiscreteSphericalTransform}
     * @return compressed Spectral data
     */
    public static Complex[] FullCompressSpectral(Complex[][] spectral) {
        int q = spectral.length;
        int size = (int) Math.pow(q+1.0, 2.0);
        Complex[] compressedSpectra = new Complex[size];
        int counter = 0;
        for (int k = 0; k <= q; k++) {
            for (int l = -k; l <= k; l++) {
                compressedSpectra[counter++] = spectral[k][l+(q)];
            }
        }
        return compressedSpectra;
    }
    
  
        
    public static Complex[][] UnCompressSpectral(Complex[] compressedSpectral) {
        int Q = (int) Math.sqrt(compressedSpectral.length) - 1;
        ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        Complex[][] uncompressedSpectral = ComplexArray.CreateComplex(Q+1,2*(Q+1)-1);
        int counter = 0;
        for (int k = 0; k <= Q; k++) {
            for (int l = -k; l <= k; l++) {
                uncompressedSpectral[k][l+(Q)] = compressedSpectral[counter++];
            }
        }
        return uncompressedSpectral;
    }
    
    public static void main(String args[]) throws Exception {
        
        
        double[] latitudeCoordinates = RadiansToLatitude(DoubleArray.Add(GetLatitudeCoordinates(20), -(Math.PI/2.0)));
        double[] longitudeCoordinates = RadiansToLongitude(DoubleArray.Add(GetLongitudeCoordinates(20), - Math.PI));
        
        // Setup the latitudes for AreaQuad
        double[] lats1 = new double[latitudeCoordinates.length];
        double[] lats2 = new double[latitudeCoordinates.length];
        for (int i = 0; i < latitudeCoordinates.length; i++) {
            // If we're on the first/last elements, special logic
            if (i == 0) {
                if (latitudeCoordinates[i] < 0) {
                    lats1[i] = -90.0;
                } else if (latitudeCoordinates[i] > 0) {
                    lats1[i] = 90.0;
                }
                lats2[i] = (latitudeCoordinates[i] + latitudeCoordinates[i+1])/2.0;
            } else if (i == latitudeCoordinates.length-1) {
                if (latitudeCoordinates[i] < 0) {
                    lats2[i] = -90.0;
                } else if (latitudeCoordinates[i] > 0) {
                    lats2[i] = 90.0;
                }
                lats1[i] = (latitudeCoordinates[i-1] + latitudeCoordinates[i])/2.0;
            } else {
                lats1[i] = (latitudeCoordinates[i-1] + latitudeCoordinates[i])/2.0;
                lats2[i] = (latitudeCoordinates[i] + latitudeCoordinates[i+1])/2.0;
            }
        }
        
        // Setup the longitudes for AreaQuad
        double[] lons1 = new double[longitudeCoordinates.length];
        double[] lons2 = new double[longitudeCoordinates.length];
        for (int i = 0; i < longitudeCoordinates.length; i++) {
            // If we're on the first/last elements, special logic
            if (i == 0) {
                lons2[i] = (longitudeCoordinates[i] + longitudeCoordinates[i+1])/2.0;
                
                if (longitudeCoordinates[i] < 0 && longitudeCoordinates[longitudeCoordinates.length-1] > 0) {
                    lons1[i] = longitudeCoordinates[i] - (longitudeCoordinates[i] + longitudeCoordinates[longitudeCoordinates.length-1])/2.0;
                } else {
                    throw new Exception("Data not as expected");
                }
                
            } else if (i == longitudeCoordinates.length-1) {
                lons1[i] = (longitudeCoordinates[i-1] + longitudeCoordinates[i])/2.0;
                lons2[i] = (longitudeCoordinates[0] + longitudeCoordinates[i])/2.0;
                lons2[i] = lons1[0];
            } else {
                lons1[i] = (longitudeCoordinates[i-1] + longitudeCoordinates[i])/2.0;
                lons2[i] = (longitudeCoordinates[i] + longitudeCoordinates[i+1])/2.0;
            }
        }
        
        double[][] latData = new double[latitudeCoordinates.length][3];
        latData = DoubleArray.SetColumn(latData, 0, latitudeCoordinates);
        latData = DoubleArray.SetColumn(latData, 1, lats1);
        latData = DoubleArray.SetColumn(latData, 2, lats2);
        
        double[][] lonData = new double[longitudeCoordinates.length][3];
        lonData = DoubleArray.SetColumn(lonData, 0, longitudeCoordinates);
        lonData = DoubleArray.SetColumn(lonData, 1, lons1);
        lonData = DoubleArray.SetColumn(lonData, 2, lons2);
        
        
        double quad_area = 0;
        double[][] area = new double[lons1.length][lats1.length];
        for (int i = 0; i < lats1.length; i++) {
            for (int j = 0; j < lons1.length; j++) {
                double lat1 = lats1[i];
                double lat2 = lats2[i];
                double lon1 = lons1[j];
                double lon2 = lons2[j];
                area[i][j] = AreaQuad(lat1, lon1, lat2, lon2, 1.0);
                quad_area += area[i][j];
            }
        }
        double sphere_area = Math.PI * Math.pow(1.0, 2.0)*4.0;

        System.out.println("Quad Area:   " + quad_area);
        System.out.println("Sphere Area: " + sphere_area);
        
        DoubleArray.Print(area);
        
        DoubleArray.Print(latData);
        DoubleArray.Print(lonData);
        DoubleArray.Print(longitudeCoordinates);
    }
}

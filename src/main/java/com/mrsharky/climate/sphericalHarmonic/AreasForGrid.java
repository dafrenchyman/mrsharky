/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLatitudeCoordinates;
import static com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform.GetLongitudeCoordinates;
import com.mrsharky.helpers.DoubleArray;
import static com.mrsharky.helpers.Utilities.AreaQuad;
import static com.mrsharky.helpers.Utilities.RadiansToLatitude;
import static com.mrsharky.helpers.Utilities.RadiansToLongitude;



/**
 *
 * @author Julien Pierret
 */
public class AreasForGrid {
    
    
    private final double[][] _area;
    private final double[] _latitude;
    private final double[] _longitude;
    
    public double[][] GetAreas() {
        return this._area;
    }
    
    public AreasForGrid(int latCount, int lonCount, double radius) throws Exception {
        _latitude  = RadiansToLatitude(DoubleArray.Add(GetLatitudeCoordinates(latCount), -(Math.PI/2.0)));
        _longitude = RadiansToLongitude(DoubleArray.Add(GetLongitudeCoordinates(lonCount), - Math.PI));
        _area = ProcessLatLon (_latitude, _longitude, radius);
    }
    
    public AreasForGrid(double[] latitudeCoordinates, double[] longitudeCoordinates, double radius) throws Exception {
        _latitude = latitudeCoordinates;
        _longitude = longitudeCoordinates;
        _area = ProcessLatLon (latitudeCoordinates, longitudeCoordinates, radius);
    }
    
    public double[] GetLatitude() {
        return _latitude;
    }
    
    public double[] GetLongitude() {
        return _longitude;
    }
    
    private double[][] ProcessLatLon (double[] latitudeCoordinates, double[] longitudeCoordinates, double radius) throws Exception {
        
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
                
                if (    (longitudeCoordinates[i] <= 0 && longitudeCoordinates[longitudeCoordinates.length-1] > 0 ) ||
                        (longitudeCoordinates[i] < 0 && longitudeCoordinates[longitudeCoordinates.length-1] >= 0 )) {
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
        
        // Calculate all the areas
        double[][] area = new double[lats1.length][lons1.length];
        for (int i = 0; i < lats1.length; i++) {
            for (int j = 0; j < lons1.length; j++) {
                double lat1 = lats1[i];
                double lat2 = lats2[i];
                double lon1 = lons1[j];
                double lon2 = lons2[j];
                area[i][j] = AreaQuad(lat1, lon1, lat2, lon2, radius);
            }
        }
        return area;
    }
    
    public static void main(String args[]) throws Exception {
        
        
        AreasForGrid a1 = new AreasForGrid(20,20,1.0);
         
        double[] latitudeCoordinates = RadiansToLatitude(DoubleArray.Add(GetLatitudeCoordinates(20), -(Math.PI/2.0)));
        double[] longitudeCoordinates = RadiansToLongitude(DoubleArray.Add(GetLongitudeCoordinates(20), - Math.PI));
        AreasForGrid a2 = new AreasForGrid(latitudeCoordinates,longitudeCoordinates,1.0);


        DoubleArray.Print(a1.GetAreas());
        DoubleArray.Print(a2.GetAreas());
        System.out.println("Total Area: " + DoubleArray.SumArray(a1.GetAreas())/(Math.PI*4.0));
    }
    
}

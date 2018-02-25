/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climateDatabase;

/**
 *
 * @author Julien Pierret
 */
public class GridBox {
    public int GridBox_ID;
    public int Lat_ID;
    public int Lon_ID;
    public double Lat;
    public double Lon;
    
    GridBox(
        int gridBox_ID,
        int lat_ID,
        int lon_ID,
        double lat,
        double lon)
    {
        this.GridBox_ID = gridBox_ID;
        this.Lat_ID = lat_ID;
        this.Lon_ID = lon_ID;
        this.Lat = lat;
        this.Lon = lon;
    }
}

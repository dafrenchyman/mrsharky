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
public class GridData {
    public int GridBox_ID;
    public double Lat;
    public double Lon;
    public double Value;
    
    public GridData(
        int gridBox_ID,
        double lat,
        double lon,
        double value)
    {
        this.GridBox_ID = gridBox_ID;
        this.Lat = lat;
        this.Lon = lon;
        this.Value = value;
    }
}

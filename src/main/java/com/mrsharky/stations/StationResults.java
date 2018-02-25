/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.stations;

import java.io.Serializable;

/**
 *
 * @author mrsharky
 */
public class StationResults implements Serializable {
    public long StationId;
    public double Lat;
    public double Lon;
    public long Cnt;
    public double OrigValue;
    public double Value;
    public double BaselineMean;
    public double BaselineVariance; 
}

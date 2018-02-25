/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.googlecode.cqengine.query.option.QueryOptions;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mrsharky
 */
public class NetCdfEntry {
    
    public final String date;
    public Date ddate;
    public final double lat;
    public final double lon;
    public final float value;
    public final String level;
    
    //private final DateFormat _format = new SimpleDateFormat("yyyy-mm-dd");
    
    public String GetDate() {
        return this.date;
    }
    
    public NetCdfEntry(String date, double lat, double lon, float value) {
        this.date = date;
        this.lat = lat;
        this.lon = lon;
        this.value = value;
        this.level = "None";
        
        /*try {
            this.ddate = _format.parse(date);
        } catch (ParseException ex) {
            Logger.getLogger(NetCdfEntry.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    public NetCdfEntry(String date, String level, double lat, double lon, float value) {
        this.date = date;
        this.lat = lat;
        this.lon = lon;
        this.value = value;
        this.level = level;
    }
}

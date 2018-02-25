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
public class DbDate {
    public int Date_ID;
    public String Date;
    
    DbDate(
        int date_ID,
        String date)
    {
        this.Date_ID = date_ID;
        this.Date = date;
    }
}

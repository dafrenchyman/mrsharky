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
public class DbLevel {
    public int Level_ID;
    public String Level;
    
    DbLevel(
        int level_ID,
        String level)
    {
        this.Level_ID = level_ID;
        this.Level = level;
    }
}

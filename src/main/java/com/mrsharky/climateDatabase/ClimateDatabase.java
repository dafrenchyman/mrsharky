/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climateDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

/**
 *
 * @author mrsharky
 */
public class ClimateDatabase {
    
    private DBI _dbi;
    public ClimateDatabase(DBI dbi) {
        _dbi = dbi;
        
        // TODO: need to add some checks to make sure this database is in the correct format
    }
    
    public Map<String, Integer> GetDateMapFromDb(DBI dbi) {
        Map<String, Integer> dateMap = new HashMap<String, Integer>();
        {
            Handle h = this._dbi.open();    
            SqlStatements g = h.attach(SqlStatements.class);
            List<DbDate> dbDateList = g.GetDbDate();
            for (DbDate currDate : dbDateList) {
                if (!dateMap.containsKey(currDate.Date)) {
                    dateMap.put(currDate.Date, currDate.Date_ID);
                }
            }
            h.close();
        }
        return dateMap;
    }
    
    public List<String> GetDateListFromDb(DBI dbi) {
        List<String> dateList = new ArrayList<String>();
        {
            Handle h = this._dbi.open();
            SqlStatements g = h.attach(SqlStatements.class);
            List<DbDate> dbDateList = g.GetDbDate();
            for (DbDate currDate : dbDateList) {
                dateList.add(currDate.Date);
            }
            h.close();
        }
        return dateList;
    }
    
    public List<GridData> GetGridDataListFromDb(DBI dbi, String date, int level_ID) {
        Handle h = this._dbi.open(); 
        SqlStatements g = h.attach(SqlStatements.class);
        List<GridData> dbGridDataList = g.GetGridDataList(level_ID, date);
        h.close();
        return dbGridDataList;
    }
    
    public List<GridBox> GetGridBoxListFromDb(DBI dbi) {
        Handle h = this._dbi.open();
        SqlStatements g = h.attach(SqlStatements.class);
        List<GridBox> gridBoxList = g.GetGridBoxList();
        h.close();
        return gridBoxList;
    }
    
    public Integer PutLevelData(DBI dbi, String name, String description) {
        Handle h = this._dbi.open();
        SqlStatements g = h.attach(SqlStatements.class);
        Integer gridBoxList = g.InsertGetDbLevel(name, description);
        h.close();
        return gridBoxList;
    }
    
    
    public void PutGridData(DBI dbi, List<Integer> level_ID, List<Integer> gridBox_IDs, List<Integer> date_IDs, List<Float> value) {
        Handle h = this._dbi.open();
        SqlStatements g = h.attach(SqlStatements.class);
        g.InsertGridDataStoredProc(level_ID, gridBox_IDs, date_IDs, value);
        h.close();
    }
    
    public void PutDate(DBI dbi, List<String> dates) {
        Handle h = this._dbi.open();
        SqlStatements g = h.attach(SqlStatements.class);
        g.InsertDate(dates);
        h.close();
    }
}

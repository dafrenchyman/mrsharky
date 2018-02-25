/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climateDatabase;

import java.util.Iterator;
import java.util.List;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

/**
 *
 * @author Julien Pierret
 */
public interface SqlStatements {
     
    @SqlQuery("CALL p_GetGridBox();")
    @Mapper(GridBoxMapper.class)
    List<GridBox> GetGridBoxList();
    
    @SqlQuery("CALL p_GetGridData(:in_level_ID, :in_date);")
    @Mapper(GridDataMapper.class)
    List<GridData> GetGridDataList(
            @Bind("in_level_ID") Integer level_ID,
            @Bind("in_date") String date
            );
    
    @SqlQuery("CALL p_GetDate();")
    @Mapper(DbDateMapper.class)
    List<DbDate> GetDbDate();
    
    @SqlQuery("CALL p_GetLevel();")
    @Mapper(DbLevelMapper.class)
    List<DbLevel> GetDbLevel();
    
    @SqlQuery("CALL p_InsertLevel(:in_Name, :in_Description);")
    @Mapper(DbLevelIdMapper.class)
    Integer InsertGetDbLevel(@Bind("in_Name") String name,
                        @Bind("in_Description") String description);
    
    
    @SqlBatch("INSERT IGNORE INTO `Level` (`Name`) VALUES (:in_name)")
    @BatchChunkSize(100)
    void InsertLevel(@Bind("in_name") List<String> levels);
    
    @SqlBatch("INSERT IGNORE INTO `Date` (`Date`) VALUES (:in_date)")
    @BatchChunkSize(100)
    void InsertDate(@Bind("in_date") List<String> dates);
    
    @SqlBatch("INSERT IGNORE INTO `Lat` (`Lat`) VALUES (:in_lat)")
    @BatchChunkSize(100)
    void InsertLat(@Bind("in_lat") List<Double> lats);
    
    @SqlBatch("INSERT IGNORE INTO `Lon` (`Lon`) VALUES (:in_lon)")
    @BatchChunkSize(100)
    void InsertLon(@Bind("in_lon") List<Double> lons);
    
    @SqlBatch("CALL p_InsertLatLon (:in_lat, :in_lon)")
    @BatchChunkSize(100)
    void InsertLatLon(@Bind("in_lat") Double lat,
                      @Bind("in_lon") List<Double> lons
                      );
    
    @SqlBatch("CALL p_InsertLatLon (:in_lat, :in_lon)")
    @BatchChunkSize(100)
    void InsertLatLon(@Bind("in_lat") List<Double> lat,
                      @Bind("in_lon") Double lons
                      );
    
    @SqlBatch("CALL p_InsertGridData (:in_level_ID, :in_gridBox_ID, :in_date_ID, :in_value)")
    @BatchChunkSize(1000)
    void InsertGridDataStoredProc(@Bind("in_level_ID") List<Integer> level_ID,
                        @Bind("in_gridBox_ID") List<Integer> gridBox_IDs,
                        @Bind("in_date_ID") List<Integer> date_IDs,
                        @Bind("in_value") List<Float> value
                        );
    
    @SqlBatch("INSERT INTO `GridData` (`Dataset_ID`, `GridBox_ID`, `Date_ID`, `Value`) VALUES " +
              " (:in_dataset_ID, :in_gridBox_ID, :in_date_ID, :in_value)" +
              " ON DUPLICATE KEY UPDATE `Value`=VALUES(`Value`);")
    @BatchChunkSize(5000)
    void InsertGridData(@Bind("in_dataset_ID") int dataset_ID,
                        @Bind("in_gridBox_ID") List<Integer> gridBox_IDs,
                        @Bind("in_date_ID") List<Integer> dates,
                        @Bind("in_value") List<Float> value
                        );
    
    @SqlBatch("INSERT INTO `GridData` (`GridBox_ID`, `Date_ID`, `:in_columnName1`) VALUES " +
              " (:in_gridBox_ID, :in_date_ID, :in_value)" +
              " ON DUPLICATE KEY UPDATE `:in_columnName2`=VALUES(`:in_columnName3`);")
    @BatchChunkSize(5000)
    void InsertGridDataTest(@Bind("in_columnName1") String columnName1,
                        @Bind("in_columnName2") String columnName2,
                        @Bind("in_columnName3") String columnName3,
                        @Bind("in_gridBox_ID") List<Integer> gridBox_IDs,
                        @Bind("in_date_ID") List<Integer> dates,
                        @Bind("in_value") List<Float> value
                        );
    
    
    
    @SqlBatch("INSERT INTO `GridData` (`Dataset_ID`, `GridBox_ID`, `Date_ID`, `Value`) VALUES " +
              " (:in_dataset_ID, :in_gridBox_ID, :in_date_ID, :in_value)" +
              " ON DUPLICATE KEY UPDATE `Value`=VALUES(`Value`);")
    @BatchChunkSize(10000)
    void InsertGridData(@Bind("in_dataset_ID") List<Integer> dataset_ID,
                        @Bind("in_gridBox_ID") List<Integer> gridBox_IDs,
                        @Bind("in_date_ID") List<Integer> dates,
                        @Bind("in_value") List<Float> value
                        );
}

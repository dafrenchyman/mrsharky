/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.netcdf;

import com.mrsharky.dataprocessor.NetCdfLoader;
import com.mrsharky.stations.ghcn.GhcnV3_Helpers.QcType;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
//import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 *
 * @author jpierret
 */
public class NetCdf_MonthlyDataLoader implements Serializable, FlatMapFunction<Iterator<java.util.Date>, Row> {

    private Map<java.util.Date, double[][]> _allData;
    private double[] _lats;
    private double[] _lons;
    private final List<Long> _stationIds;
    
    public NetCdf_MonthlyDataLoader (Map<java.util.Date, double[][]> allData, double[] lats, double[] lons) throws Exception {
        _allData = allData;
        _lats = lats;
        _lons = lons;
        _stationIds = new ArrayList<Long>();
    }
    
    public NetCdf_MonthlyDataLoader (Map<java.util.Date, double[][]> allData, double[] lats, double[] lons, List<Long> stations) throws Exception {
        _allData = allData;
        _lats = lats;
        _lons = lons;
        _stationIds = stations;
    }

    public StructType GetSchema() throws Exception{
        List<StructField> fields = new ArrayList<StructField>();
        fields.add(DataTypes.createStructField("ID", DataTypes.LongType, false));
        fields.add(DataTypes.createStructField("ELEMENT", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("DATE", DataTypes.DateType, false));
        fields.add(DataTypes.createStructField("VALUE", DataTypes.DoubleType, false));
        fields.add(DataTypes.createStructField("DMFLAG", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("QCAFLAG", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("DSFLAG", DataTypes.StringType, true));

        StructType schema = DataTypes.createStructType(fields);
        schema = DataTypes.createStructType(fields);
        return schema;
    }

    @Override
    public Iterator<Row> call(Iterator<java.util.Date> dates) throws Exception {
        
        List<Row> rows = new ArrayList<Row>();
        long rowsIn = 0;
        long rowsOut = 0;
        System.out.println("Starting - " + this.getClass().getName());
        while (dates.hasNext()) {
            rowsIn++;
            java.util.Date currDate = dates.next();
        
            double[][] currData = _allData.get(currDate); 
            long stationCounter = 0;
            System.out.println("Processing: " + currDate);
            
            for (int i = 0; i < _lats.length; i++) {
                double latitude = _lats[i];
                for (int j = 0; j < _lons.length; j++) {
                    double longitude = _lons[j];
                    Double currValue = currData[i][j];
                    stationCounter++;
                    
                    long stationId = stationCounter;
                    
                    if (_stationIds.isEmpty() || _stationIds.contains(stationId)) {
                        int year = currDate.getYear()+1900;
                        int month = currDate.getMonth() + 1;
                        int day = currDate.getDate();
                        String currDateS = StringUtils.leftPad(Integer.toString(year), 4, "0") + "-" + StringUtils.leftPad(Integer.toString(month), 2, "0") +"-" +
                                StringUtils.leftPad(Integer.toString(day), 2, "0");

                        Date date = java.sql.Date.valueOf(currDateS);

                        String currDmFlag = null;                     
                        String currQcFlag = null;
                        String currDsFlag = null;
                        String element = null;

                        // Save everything to the list
                        List<Object> values = new ArrayList<Object>();
                        values.add(stationId);
                        values.add(element);
                        values.add(date);
                        values.add(currValue);
                        values.add(currDmFlag);
                        values.add(currQcFlag);
                        values.add(currDsFlag);
                        rows.add(RowFactory.create(values.toArray()));
                        rowsOut++;
                    } 
                }
            }
        }
        System.out.println("Finished - " + this.getClass().getName() + " (rowsIn: " + rowsIn + ", rowsOut: " + rowsOut + ")");
        return rows.iterator();  
    }
}

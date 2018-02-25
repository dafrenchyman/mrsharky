/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.ghcn;

import com.mrsharky.stations.ghcn.GhcnV3_Helpers.QcType;
import java.io.Serializable;
import java.sql.Date;
import java.util.ArrayList;
//import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

/**
 *
 * @author jpierret
 */
public class GhcnV3_MonthlyDataLoader implements Serializable, FlatMapFunction<Iterator<String>, Row> {

    private final QcType _qcType;
    
    public GhcnV3_MonthlyDataLoader (GhcnV3_Helpers.QcType qcType) {
        _qcType = qcType;
    }
    
    public StructType GetSchema() throws Exception{
        List<StructField> fields = new ArrayList<StructField>();
        fields.add(DataTypes.createStructField("ID", DataTypes.LongType, false));
        fields.add(DataTypes.createStructField("ELEMENT", DataTypes.StringType, false));
        fields.add(DataTypes.createStructField("DATE", DataTypes.DateType, false));
        fields.add(DataTypes.createStructField("VALUE", DataTypes.DoubleType, false));
        fields.add(DataTypes.createStructField("DMFLAG", DataTypes.StringType, true));
        if (_qcType == QcType.QCA) {
            fields.add(DataTypes.createStructField("QCAFLAG", DataTypes.StringType, true));
        } else if (_qcType == QcType.QCU) {
            fields.add(DataTypes.createStructField("QCUFLAG", DataTypes.StringType, true));
        } else {
            throw new Exception ("Invalid QcType");
        }
        fields.add(DataTypes.createStructField("DSFLAG", DataTypes.StringType, true));

        StructType schema = DataTypes.createStructType(fields);
        schema = DataTypes.createStructType(fields);
        return schema;
    }
    
    @Override
    public Iterator<Row> call(Iterator<String> linesPerPartition) throws Exception {
        
        List<Row> rows = new ArrayList<Row>();
        long rowsIn = 0;
        long rowsOut = 0;
        System.out.println("Starting - " + this.getClass().getName());
        while (linesPerPartition.hasNext()) {
            String currLine = linesPerPartition.next();
            try {
                
                long stationId = Long.parseLong(currLine.substring(0,11).trim());
                int year = Integer.parseInt(currLine.substring(11,15).trim());
                String element = currLine.substring(15,19).trim();
                
                // Prep all the indecies and lengths that we will loop on
                int valueLoc = 19;
                int valueLen = 5;
                int dmFlagLoc = 24;
                int dmFlagLen = 1;
                int qcFlagLoc = 25;
                int qcFlagLen = 1;
                int dsFlagLoc = 26;
                int dsFlagLen = 1;
                int totalLength = valueLen + dmFlagLen + qcFlagLen + dsFlagLen;
                
                for (int month = 0; month < 12; month++) {

                    // Create the date into the correct format for Spark
                    String currDate = StringUtils.leftPad(Integer.toString(year), 4, "0") + "-" + StringUtils.leftPad(Integer.toString(month+1), 2, "0") + "-01";
                    Date date = java.sql.Date.valueOf(currDate);

                    int currValueLoc = valueLoc + month*totalLength;
                    Double currValue = Double.parseDouble(currLine.substring(currValueLoc,currValueLoc+valueLen).trim());
                    currValue = (currValue == -9999.0) ? null : currValue/100.0;
                    
                    // If we're not missing the value
                    if (currValue != null) {
                        
                        // DM FLAG
                        int currDmFlagLoc = dmFlagLoc + month*totalLength;
                        String currDmFlag = currLine.substring(currDmFlagLoc, currDmFlagLoc+dmFlagLen);
                        currDmFlag = currDmFlag.trim().length() == 0 ? null : currDmFlag;
                        
                        // QC FLAG
                        int currQcFlagLoc = qcFlagLoc + month*totalLength;
                        String currQcFlag = currLine.substring(currQcFlagLoc, currQcFlagLoc+qcFlagLen);
                        currQcFlag = currQcFlag.trim().length() == 0 ? null : currQcFlag;
                        
                        // DS FLAG
                        int currDsFlagLoc = dsFlagLoc + month*totalLength;
                        String currDsFlag = currLine.substring(currDsFlagLoc, currDsFlagLoc+dsFlagLen);
                        currDsFlag = currDsFlag.trim().length() == 0 ? null : currDsFlag;
                        
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
                if (rowsIn % 20000 == 0) {
                    System.out.println("Running - " + this.getClass().getName() + " (rowsIn: " + rowsIn + ", rowsOut: " + rowsOut + ")");
                }
            } catch (Exception ex ) {
                System.out.println("ERROR Got an error on line:\n" + currLine.toString());
                System.err.println("ERROR Got an error on line:\n" + currLine.toString());
                System.out.println(ex.getMessage());
                System.err.println(ex.getMessage());
            }
            rowsIn++;
        }
        System.out.println("Finished - " + this.getClass().getName() + " (rowsIn: " + rowsIn + ", rowsOut: " + rowsOut + ")");
        return rows.iterator();  
    }
}

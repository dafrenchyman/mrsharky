/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.ghcn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
public class GhcnV3_InventoryDataLoader implements Serializable, FlatMapFunction<Iterator<String>, Row> {
    
    public GhcnV3_InventoryDataLoader () {
    }
    
    public StructType GetSchema() throws Exception{
        List<StructField> fields = new ArrayList<StructField>();
        fields.add(DataTypes.createStructField("ID", DataTypes.LongType, false));
        fields.add(DataTypes.createStructField("LATITUDE", DataTypes.DoubleType, false));
        fields.add(DataTypes.createStructField("LONGITUDE", DataTypes.DoubleType, false));
        fields.add(DataTypes.createStructField("STNELEV", DataTypes.DoubleType, true));
        fields.add(DataTypes.createStructField("NAME", DataTypes.StringType, false));
        fields.add(DataTypes.createStructField("GRELEV", DataTypes.IntegerType, true));
        fields.add(DataTypes.createStructField("POPCLS_ID", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("POPSIZ", DataTypes.IntegerType, true));
        fields.add(DataTypes.createStructField("TOPO_ID", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("STVEG_ID", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("STLOC", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("OCNDIST", DataTypes.IntegerType, true));
        fields.add(DataTypes.createStructField("AIRSTN", DataTypes.BooleanType, true));
        fields.add(DataTypes.createStructField("TOWNDIS", DataTypes.IntegerType, true));
        fields.add(DataTypes.createStructField("GRVEG", DataTypes.StringType, true));
        fields.add(DataTypes.createStructField("POPCSS_ID", DataTypes.StringType, true));
        StructType schema = DataTypes.createStructType(fields);
        schema = DataTypes.createStructType(fields);
        return schema;
    }
    
    @Override
    public Iterator<Row> call(Iterator<String> linesPerPartition) throws Exception {
        
        List<Row> rows = new ArrayList<Row>();
        long counter = 0;
        System.out.println("Starting - " + this.getClass().getName());
        while (linesPerPartition.hasNext()) {
            String currLine = linesPerPartition.next();
            try {
                // Get all the values
                long stationId   = Long.parseLong(currLine.substring(0,11).trim());
                double latitude  = Double.parseDouble(currLine.substring(12,20).trim());
                double longitude = Double.parseDouble(currLine.substring(21,30).trim());
                
                String stnElevS  = currLine.substring(31,37).trim(); 
                Double stnElev   = stnElevS.equals("-999.0") ? null : Double.parseDouble(stnElevS);
                
                String name = currLine.substring(38,68).trim();
                
                // The following fields can be completely missing
                String grElevS = currLine.substring(69,73).trim();
                Integer grElev = grElevS.length() > 0 ? Integer.parseInt(grElevS) : null;
                
                String popCls = currLine.substring(73,74).trim();
                popCls = popCls.length() == 0 ? null : popCls;
                
                String popSizS = currLine.substring(74,79).trim();
                Integer popSiz = popSizS.length() == 0 || popSizS.equals("-9") ? null : Integer.parseInt(popSizS)*1000;
                
                String topoS = currLine.substring(79,81).trim();
                String topo = topoS.length() > 0 ? topoS : null;
                
                String stVegS = currLine.substring(81,83).trim();
                String stVeg = stVegS.length() == 0 || stVegS.equals("xx") ? null : stVegS;
                
                String stLocS = currLine.substring(83,85).trim();
                String stLoc = stLocS.length() == 0 || stLocS.equals("no") ? null : stLocS;
                
                String ocnDisS = currLine.substring(85,87).trim();
                Integer ocnDis = ocnDisS.length() == 0 || ocnDisS.equals("-9") ? null : Integer.parseInt(ocnDisS);
                
                String airStnS = currLine.substring(87,88).trim();
                Boolean airStn = airStnS.length() == 0 ? null :
                        airStnS.equals("A") ? true : false;
                
                String townDisS = currLine.substring(88,90).trim();
                Integer townDis = townDisS.length() == 0 || townDisS.equals("-9") ? null : Integer.parseInt(townDisS);
                
                String grVegS = currLine.substring(90,106).trim();
                String grVeg = grVegS.length() > 0 ? grVegS : null;
                
                String popCssS = currLine.substring(106,107).trim();
                String popCss = popCssS.length() > 0 ? popCssS : null;
                
                // Set all the values
                List<Object> values = new ArrayList<Object>();
                values.add(stationId);
                values.add(latitude);
                values.add(longitude);
                values.add(stnElev);
                values.add(name);
                values.add(grElev);
                values.add(popCls);
                values.add(popSiz);
                values.add(topo);
                values.add(stVeg);
                values.add(stLoc);
                values.add(ocnDis);
                values.add(airStn);
                values.add(townDis);
                values.add(grVeg);
                values.add(popCss);

                rows.add(RowFactory.create(values.toArray()));  
                        
                if (counter % 20000 == 0) {
                    System.out.println("Running - " + this.getClass().getName() + " (" + counter + ")");
                }
            } catch (Exception ex ) {
                System.out.println("ERROR Got an error on line:\n" + currLine.toString());
                System.err.println("ERROR Got an error on line:\n" + currLine.toString());
                System.out.println(ex.getMessage());
                System.err.println(ex.getMessage());
            }
            counter++;
        }
        System.out.println("Finished - " + this.getClass().getName() + " (" + counter + ")");
        return rows.iterator();  
    }
}

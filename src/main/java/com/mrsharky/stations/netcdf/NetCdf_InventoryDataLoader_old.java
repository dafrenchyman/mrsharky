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
import static com.mrsharky.helpers.Utilities.HaversineDistance;

import java.util.ArrayList;
import java.util.List;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.javatuples.Triplet;

/**
 *
 * @author jpierret
 */
public class NetCdf_InventoryDataLoader_old {
    
    private List<Row> _data;
    private List<Long> _stationList;
    
    public List<Long> GetStationList() {
        return _stationList;
    }
    
    public NetCdf_InventoryDataLoader_old (NetCdfLoader loader, double[] stnLats, double[] stnLons) throws Exception {
        
        if (stnLats.length != stnLons.length) {
            throw new Exception("stnLats & stnLons must have same length");
        }
        
        _stationList= new ArrayList<Long>();
        double[] lats = loader.GetLats();
        double[] lons = loader.GetLons();
        List<Triplet<Long, Double, Double>> allStations = new ArrayList<Triplet<Long, Double, Double>>();
        
        long stationCounter = 0;
        for (int i = 0; i < lats.length; i++) {
            double latitude = lats[i];
            for (int j = 0; j < lons.length; j++) {
                double longitude = lons[j];
                stationCounter++;
                allStations.add(Triplet.with(stationCounter, latitude, longitude));
            }
        }
        
        for (int i = 0; i < stnLats.length; i++) {
            // Find nearest stationId
            double lat1 = stnLats[i];
            double lon1 = stnLons[i]; 
            double bestDistance = Double.MAX_VALUE;
            long bestStationId = -1;
            for (int j = 0; j < allStations.size(); j++) {
                Triplet<Long, Double, Double> currStation = allStations.get(j);
                long stationId = currStation.getValue0();
                double lat2 = currStation.getValue1();
                double lon2 = currStation.getValue2();
                double currDistance = HaversineDistance(lat1, lat2, lon1, lon2, 0.0, 0.0)/1000.0;
                if (currDistance < bestDistance) {
                    bestDistance = currDistance;
                    bestStationId = stationId;
                }
            }
            _stationList.add(bestStationId);
        }
        ProcessStations(loader);
    }
    
    private void ProcessStations(NetCdfLoader loader) {
        _data = new ArrayList<Row>();
        
        double[] lats = loader.GetLats();
        double[] lons = loader.GetLons();
        
        long stationCounter = 0;
        for (int i = 0; i < lats.length; i++) {
            double latitude = lats[i];
            for (int j = 0; j < lons.length; j++) {
                double longitude = lons[j];
                
                // Get the station data
                // Get all the values
                long stationId   = stationCounter;
                
                if (_stationList.isEmpty() || _stationList.contains(stationId)) {
                    Double stnElev   = null;
                    String name = "Grid point " + stationId;

                    // The following fields can be completely missing
                    Integer grElev = null;
                    String popCls = null;
                    Integer popSiz = null;
                    String topo = null;
                    String stVeg = null;
                    String stLoc = null;
                    Integer ocnDis = null;
                    Boolean airStn = null;
                    Integer townDis = null;
                    String grVeg = null;
                    String popCss = null;

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
                    _data.add(RowFactory.create(values.toArray()));
                }
                stationCounter++;
            }
        }
    }
    
    public NetCdf_InventoryDataLoader_old (NetCdfLoader loader) {
        _stationList= new ArrayList<Long>();
        ProcessStations(loader);
    }
    
    public List<Row> GetData() {
        return _data;
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
    
}

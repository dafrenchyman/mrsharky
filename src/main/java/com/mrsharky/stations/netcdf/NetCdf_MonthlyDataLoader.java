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
import com.mrsharky.discreteSphericalTransform.DiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.helpers.DoubleArray;
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
    private final double[] _lats;
    private final double[] _lons;
    private int _q;
    private final List<Long> _stationIds;
    
    public NetCdf_MonthlyDataLoader (Map<java.util.Date, double[][]> allData, double[] lats, double[] lons, int q) throws Exception {
        _allData = allData;
        _lats = lats;
        _lons = lons;
        _q = q;
        _stationIds = new ArrayList<Long>();
    }
    
    public NetCdf_MonthlyDataLoader (Map<java.util.Date, double[][]> allData, double[] lats, double[] lons, List<Long> stations, int q) throws Exception {
        _allData = allData;
        _lats = lats;
        _lons = lons;
        _q = q;
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
            //if (currDate.getYear() +1900 != 1970) { continue; }
            System.out.println("Processing: " + rowsIn + ", "+ currDate + ", " + _lats.length);
        
            double[][] currData = _allData.get(currDate);
            
            DiscreteSphericalTransform dst = new DiscreteSphericalTransform(currData, _q, true);
            SphericalHarmonic sh = dst.GetSpectra();
            
            InvDiscreteSphericalTransform idst = new InvDiscreteSphericalTransform(sh);
            
            double[] values = idst.ProcessPoints(_lats, _lons);
            
            // try to rebuild
            if (false) {
                // Print the predicted values
                System.out.println("Predicted Lat");
                DoubleArray.Print(_lats);
                
                System.out.println("Predicted Lon");
                DoubleArray.Print(_lons);
                
                System.out.println("Values");
                DoubleArray.Print(values);
                
                InvDiscreteSphericalTransform idstr = new InvDiscreteSphericalTransform(sh);
                double[][] rebuilt = idstr.ProcessGaussianDoubleArray(dst.GetM(), dst.GetN());
                double[][] diff = DoubleArray.Add(currData, DoubleArray.Multiply(rebuilt, -1.0));

                System.out.println("Lat");
                DoubleArray.Print(dst.GetLatitudeCoordinates());
                
                System.out.println("Lon");
                DoubleArray.Print(dst.GetLongitudeCoordinates());
                
                System.out.println("Original");
                DoubleArray.Print(currData);
                
                System.out.println("Rebuilt");
                DoubleArray.Print(rebuilt);
                
                System.out.println("Diff");
                DoubleArray.Print(diff);
                
                
            }
            
            
            for (int i = 0; i < values.length; i++) {
                
                long stationId = i;

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
                List<Object> currValues = new ArrayList<Object>();
                currValues.add(stationId);
                currValues.add(element);
                currValues.add(date);
                currValues.add(values[i]);
                currValues.add(currDmFlag);
                currValues.add(currQcFlag);
                currValues.add(currDsFlag);
                rows.add(RowFactory.create(currValues.toArray()));
                rowsOut++;
            }
        }
        System.out.println("Finished - " + this.getClass().getName() + " (rowsIn: " + rowsIn + ", rowsOut: " + rowsOut + ")");
        return rows.iterator();  
    }
}

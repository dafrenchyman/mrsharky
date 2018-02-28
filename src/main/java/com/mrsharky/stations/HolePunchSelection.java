/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations;

import static com.mrsharky.helpers.SparkUtils.CreateDefaultSparkSession;
import static com.mrsharky.helpers.SparkUtils.PrintSparkSetting;
import static com.mrsharky.helpers.Utilities.HaversineDistance;
import static com.mrsharky.helpers.Utilities.SerializeObject;
import com.mrsharky.spark.SetupSparkTest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.javatuples.Pair;

/**
 * 
 * @author jpierret
 */
public abstract class HolePunchSelection {
    
    protected boolean _verbose = false;
    protected boolean _streamYearly = true;
    protected boolean _streamMonthly = true;
    protected SparkSession _spark;
    
    private final String _lowerBaseline;
    private final String _upperBaseline;
    private final double _minDistance;
    
    private SetupSparkTest sparkSetup;
    
    public HolePunchSelection(String lowerBaseline, String upperBaseline, double minDistance, boolean createSpark) throws Exception {
        if (createSpark) {
            int threads = Runtime.getRuntime().availableProcessors();
            //threads = 1;
            //threads = (int) Math.max(1, threads-1);
            sparkSetup = new SetupSparkTest();
            sparkSetup.setup(threads);
        }
        _spark = CreateDefaultSparkSession(this.getClass().getName());
        _lowerBaseline = lowerBaseline;
        _upperBaseline = upperBaseline;
        _minDistance = minDistance;
        PrintSparkSetting(_spark);
    }
    
    public void Close() {
        sparkSetup.tearDown();
    }
    
    protected void Process(Dataset<Row> InventoryDataset, Dataset<Row> MonthlyDataset, int minMonthYears, String destination, boolean saveCsv) throws Exception {
  
        // Output the results to disk
        if (saveCsv) {
            MonthlyDataset.explain(true);
            MonthlyDataset.coalesce(1).write().option("header", true).option("quoteAll", true).csv(destination + "/Csv/Monthly");
            MonthlyDataset.coalesce(1).write().parquet(destination + "/Parquet/Monthly");
            InventoryDataset.explain(true);
            InventoryDataset.coalesce(1).write().option("header", true).option("quoteAll", true).csv(destination + "/Csv/Inventory");
            InventoryDataset.coalesce(1).write().parquet(destination + "/Parquet/Inventory");
        }
        
        
        // Get the baseline temperature and variance for all the stations during analysis period 
        
            
        // MonthlyDataset:   ID, ELEMENT, DATE, VALUE, DMFLAG, QCAFLAG, DSFLAG        
        // InventoryDataset: ID, LATITUDE, LONGITUDE, STNELEV, NAME, GRELEV, 
        //                   POPCLS_ID, POPSIZ, TOPO_ID, STVEG_ID, STLOC, 
        //                   OCNDIST, AIRSTN, TOWNDIS, GRVEG,POPCSS_ID

        StationSelectionResults finalResults = new StationSelectionResults();
         
        // Process yearly data
        {
            System.out.println("Processing Yearly Data");
            Dataset<Row> BaselineData = _spark.sql("SELECT "
                    + "ID"
                    + ", COUNT(*) AS Cnt "
                    + ", AVG(VALUE) AS Value "
                    + ", VAR_POP(VALUE) VarValue "
                    + " FROM MonthlyDataset "
                    + " WHERE DATE >= '" + _lowerBaseline + "' AND DATE <= '" + _upperBaseline + "' " 
                    + " GROUP BY ID "
                    + " ORDER BY ID ASC");
            BaselineData.createOrReplaceTempView("BaselineData");
            
            Dataset<Row> StationHistory = _spark.sql("SELECT "
                    + " ID"
                    + ", COUNT(*) AS Cnt "
                    + " FROM MonthlyDataset "
                    + " GROUP BY ID "
                    + " ORDER BY ID ASC");
            StationHistory.createOrReplaceTempView("StationHistory");

            BaselineData = _spark.sql("SELECT "
                    + "b.ID "
                    + ", i.LATITUDE "
                    + ", i.LONGITUDE "
                    + ", h.Cnt AS HistoricalCount "
                    + ", b.Value AS BaselineValue "
                    + ", b.VarValue AS BaselineVar "
                    + ", b.Cnt AS BaselineCnt "
                    + " FROM BaselineData b "
                    + " JOIN InventoryDataset i ON b.ID = i.ID "
                    + " JOIN StationHistory h ON b.ID = h.ID "
                    + " WHERE b.Cnt >= (12*30) "
                    + " ORDER BY h.Cnt DESC, b.ID ASC");
            BaselineData.createOrReplaceTempView("BaselineData"); 
            
            // Gather all the station data once
            // Get the station data for current date
            Dataset<Row> AllStationData = _spark.sql("SELECT * FROM ( SELECT "
                    + " m.ID "
                    + " , YEAR(m.DATE) AS Year"
                    + ", AVG(m.value) AS Value "
                    + ", MIN(m.DATE) AS DATE "
                    + ", COUNT(*) as Cnt "
                    + " FROM MonthlyDataset m "
                    + " GROUP BY m.ID, YEAR(m.DATE) "
                    + ") WHERE Cnt >= 12 "
                    + " ORDER BY Year ASC, ID ASC");
            AllStationData.createOrReplaceTempView("AllStationData");
            
            Dataset<Row> AllYearData = _spark.sql("SELECT "
                    + " m.ID "
                    + ", m.DATE "
                    + ", b.LATITUDE "
                    + ", b.LONGITUDE "
                    + ", b.HistoricalCount "
                    + ", m.value AS Value"
                    + ", b.BaselineValue "
                    + ", b.BaselineVar "
                    + " FROM AllStationData m "
                    + " JOIN BaselineData b ON b.ID = m.ID"
                    + " ORDER BY m.DATE ASC, b.HistoricalCount DESC");
            AllYearData.createOrReplaceTempView("AllYearData");
            AllYearData.persist();
                
            Date minDate = null;
            Date maxDate = null;
            List<Row> AllData = null;
            if (_streamYearly) {
                AllData = AllYearData.collectAsList();
                minDate = AllData.stream().min((a, b) -> a.getDate(1).compareTo(b.getDate(1))).get().getDate(1);
                maxDate = AllData.stream().max((a, b) -> a.getDate(1).compareTo(b.getDate(1))).get().getDate(1);
            } else {
                Row minMaxDate = _spark.sql("SELECT "
                    + " MIN(DATE) AS MinDate "
                    + ", MAX(DATE) AS MaxDate "
                    + " FROM AllYearData").first();
                minDate = minMaxDate.getDate(0);
                maxDate = minMaxDate.getDate(1);
            }
            
            int startingYear = minDate.getYear() + 1900;
            int endingYear   = maxDate.getYear() + 1900;
                      
            Map<Integer, List<Row>> yearlyLookup = new HashMap<Integer, List<Row>>();
            for (int year = startingYear; year < endingYear; year++) {          
                final int currYear = year;

                // Get the current station data
                List<Row> StationData = null;
                if (_streamYearly) {
                    StationData = AllData.stream()
                            .filter(a -> a.getDate(1).getYear() +1900 == currYear)
                            .sorted((a,b) -> Long.compare(b.getLong(4), a.getLong(4)))
                            .collect(Collectors.toList());
                } else {
                    Dataset<Row> currYearData = _spark.sql("SELECT "
                        + " ID "
                        + ", DATE "
                        + ", LATITUDE "
                        + ", LONGITUDE "
                        + ", HistoricalCount "
                        + ", Value"
                        + ", BaselineValue "
                        + ", BaselineVar "
                        + " FROM AllYearData "
                        + " WHERE YEAR(DATE) = " + currYear
                        + " ORDER HistoricalCount DESC");
                    StationData = new ArrayList<Row>(currYearData.collectAsList()); // Need to make a copy of it, or it.remove fails
                }
                yearlyLookup.put(year, StationData);                
            }
            
            int threads = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(threads);
            List<Future<Pair<Date, List<StationResults>>>> futures = new ArrayList<Future<Pair<Date, List<StationResults>>>>();
            
            for (int year = startingYear; year < endingYear; year++) {
                
                final int year_f = year;
                Callable<Pair<Date, List<StationResults>>> callable = new Callable<Pair<Date, List<StationResults>>>() {
                    public Pair<Date, List<StationResults>> call() throws Exception {
                        Pair<Date, List<StationResults>> results = null;
                        List<Row> StationData = yearlyLookup.get(year_f);
                        int origStationCount = StationData.size();
                        List<StationResults> finalStations = GetStations(StationData);    
                        String currDate = StringUtils.leftPad(Integer.toString(year_f), 4, "0") + "-01-01";
                        try {
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = dateFormat.parse(currDate);
                            System.out.println("Date: " + date + " - Final Stations selected: " + finalStations.size() + " / " + origStationCount);
                            results = Pair.with(date, finalStations);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return results;
                    }
                };
                futures.add(service.submit(callable));
            }

            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for (Future<Pair<Date, List<StationResults>>> future : futures) {
                Date date = future.get().getValue0();
                List<StationResults> finalStations = future.get().getValue1();
                if (finalStations.size() > 0) {
                    finalResults.AddDate(0, date, finalStations);
                }
            }
            
            AllYearData.unpersist();
        }
        
        // Process monthly data
        for (int month = 1; month <= 12; month++) {
        
            final int currMonth = month;
            System.out.println("Processing Month: " + month);
            Dataset<Row> BaselineData = _spark.sql("SELECT "
                    + "ID"
                    + ", COUNT(*) AS Cnt "
                    + ", AVG(VALUE) AS Value "
                    + ", VAR_POP(VALUE) VarValue "
                    + " FROM MonthlyDataset "
                    + " WHERE MONTH(DATE) = " + month + " AND DATE >= '" + _lowerBaseline + "' AND DATE <= '" + _upperBaseline + "' " 
                    + " GROUP BY ID "
                    + " ORDER BY ID ASC");
            BaselineData.createOrReplaceTempView("BaselineData");
            
            Dataset<Row> StationHistory = _spark.sql("SELECT "
                    + " ID"
                    + ", COUNT(*) AS Cnt "
                    + " FROM MonthlyDataset "
                    + " WHERE MONTH(DATE) = " + month
                    + " GROUP BY ID "
                    + " ORDER BY ID ASC");
            StationHistory.createOrReplaceTempView("StationHistory");

            BaselineData = _spark.sql("SELECT "
                    + "b.ID "
                    + ", i.LATITUDE "
                    + ", i.LONGITUDE "
                    + ", h.Cnt AS HistoricalCount "
                    + ", b.Value AS BaselineValue "
                    + ", b.VarValue AS BaselineVar "
                    + " FROM BaselineData b "
                    + " JOIN InventoryDataset i ON b.ID = i.ID "
                    + " JOIN StationHistory h ON b.ID = h.ID "
                    + " WHERE b.Cnt >= " + minMonthYears
                    + " ORDER BY h.Cnt DESC, b.ID ASC");
            BaselineData.createOrReplaceTempView("BaselineData");
            BaselineData.persist();
            
            Dataset<Row> AllMonthlyData = _spark.sql("SELECT "
                    + " m.ID "
                    + ", m.DATE "
                    + ", b.LATITUDE "
                    + ", b.LONGITUDE "
                    + ", b.HistoricalCount "
                    + ", m.value AS Value"
                    + ", b.BaselineValue "
                    + ", b.BaselineVar "
                    + " FROM MonthlyDataset m "
                    + " JOIN BaselineData b ON b.ID = m.ID"
                    + " WHERE MONTH(m.DATE) = " + month 
                    + " ORDER BY m.DATE ASC, b.HistoricalCount DESC");
            AllMonthlyData.createOrReplaceTempView("AllMonthlyData");
            
            Date minDate = null;
            Date maxDate = null;
            List<Row> AllData = null;
            if (_streamMonthly) {
                AllData = AllMonthlyData.collectAsList();
                minDate = AllData.stream().min((a, b) -> a.getDate(1).compareTo(b.getDate(1))).get().getDate(1);
                maxDate = AllData.stream().max((a, b) -> a.getDate(1).compareTo(b.getDate(1))).get().getDate(1);
            } else {
                // Geth the max/min dates in the dataset
                Row minMaxDate = _spark.sql("SELECT "
                        + " MIN(DATE) AS MinDate "
                        + ", MAX(DATE) AS MaxDate "
                        + " FROM MonthlyDataset").first();
                minDate = minMaxDate.getDate(0);
                maxDate = minMaxDate.getDate(1);
            }
            
            int startingYear = minDate.getYear() + 1900;
            int endingYear   = maxDate.getYear() + 1900;
              
            Map<Integer, List<Row>> yearlyLookup = new HashMap<Integer, List<Row>>();
            for (int year = startingYear; year < endingYear; year++) {
                
                final int currYear = year;
                
                // Get the current station data
                List<Row> StationData = null;
                if (_streamYearly) {
                    StationData = AllData.stream()
                            .filter(a -> a.getDate(1).getYear() +1900 == currYear && a.getDate(1).getMonth()+1 == currMonth)
                            .sorted((a,b) -> Long.compare(b.getLong(4), a.getLong(4)))
                            .collect(Collectors.toList());
                } else {
                    Dataset<Row> currMonthData = _spark.sql("SELECT "
                        + " m.ID "
                        + ", m.DATE "
                        + ", b.LATITUDE "
                        + ", b.LONGITUDE "
                        + ", b.HistoricalCount "
                        + ", m.value AS Value"
                        + ", b.BaselineValue "
                        + ", b.BaselineVar "
                        + " FROM MonthlyDataset m "
                        + " JOIN BaselineData b ON b.ID = m.ID"
                        + " WHERE MONTH(m.DATE) = " + month + " AND YEAR(m.DATE) = " + year 
                        + " ORDER BY b.HistoricalCount DESC");
                    StationData = new ArrayList<Row>(currMonthData.collectAsList()); // Need to make a copy of it, or it.remove fails
                }
                yearlyLookup.put(year, StationData);
            }
            
            int threads = Runtime.getRuntime().availableProcessors();
            ExecutorService service = Executors.newFixedThreadPool(threads);
            List<Future<Pair<Date, List<StationResults>>>> futures = new ArrayList<Future<Pair<Date, List<StationResults>>>>();
            
            for (int year = startingYear; year < endingYear; year++) {
                
                final int year_f = year;
                Callable<Pair<Date, List<StationResults>>> callable = new Callable<Pair<Date, List<StationResults>>>() {
                    public Pair<Date, List<StationResults>> call() throws Exception {
                        List<Row> StationData = yearlyLookup.get(year_f);
                        int origStationCount = StationData.size();
                        List<StationResults> finalStations = GetStations(StationData);
                        String currDate = StringUtils.leftPad(Integer.toString(year_f), 4, "0") + "-" + StringUtils.leftPad(Integer.toString(currMonth), 2, "0") + "-01";
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date = dateFormat.parse(currDate);
                        System.out.println("Date: " + date + " - Final Stations selected: " + finalStations.size() + " / " + origStationCount);
                        Pair<Date, List<StationResults>> results = Pair.with(date, finalStations);
                        return results;
                    }
                };
                futures.add(service.submit(callable));
            }
            
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            for (Future<Pair<Date, List<StationResults>>> future : futures) {
                Date date = future.get().getValue0();
                List<StationResults> finalStations = future.get().getValue1();
                if (finalStations.size() > 0) {
                    finalResults.AddDate(month, date, finalStations);
                }

            }
            BaselineData.unpersist();
        }
        
        SerializeObject(finalResults, destination + "/finalStations_Results.serialized");
        if (saveCsv) {
            finalResults.SaveToCsv(destination + "/finalStation_Results.csv");
        }
    }
    
    
    protected List<StationResults> GetStations(List<Row> StationData) {
        List<Long> FinalStationIds = new ArrayList<Long>();
        List<StationResults> finalStations = new ArrayList<StationResults>();

        // While we still have station data
        while (StationData.size() > 0) {
            ListIterator<Row> it = StationData.listIterator();

            // Get the first station in the list
            Row currRow = it.next();

            long stationId = currRow.getLong(0);
            //Date currDate = currRow.getDate(1);
            double lat1 = currRow.getDouble(2);
            double lon1 = currRow.getDouble(3);
            long cnt = currRow.getLong(4);
            double value = currRow.getDouble(5);
            double mean = currRow.getDouble(6);
            double var = currRow.getDouble(7);

            StationResults currStation = new StationResults();
            currStation.Cnt = cnt;
            currStation.Lat = lat1;
            currStation.Lon = lon1;
            currStation.BaselineMean = mean;
            currStation.StationId = stationId;
            currStation.Value = value;
            currStation.BaselineVariance = var;

            finalStations.add(currStation);

            it.remove();
            if (_minDistance > 0.0) {
                while (it.hasNext()) {
                    Row newRow = it.next();
                    double lat2 = newRow.getDouble(2);
                    double lon2 = newRow.getDouble(3);
                    double distance = HaversineDistance(lat1, lat2, lon1, lon2, 0.0, 0.0)/1000.0;
                    if (distance < _minDistance) {
                        it.remove();
                    }
                }
            }
            FinalStationIds.add(stationId);
        }
        if (_verbose) {
            System.out.println("ID\tLat\tLon\tValue\tHistorical Count");
            for (int i = 0; i < finalStations.size(); i++) {
                StationResults currStat = finalStations.get(i);
                System.out.println(currStat.StationId + "\t" + currStat.Lat + "\t" + currStat.Lon + "\t" + currStat.Value + "\t" + currStat.Cnt);
            }
        }
        return (finalStations);
    }
}

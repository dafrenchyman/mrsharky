/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.ghcn;

import com.mrsharky.stations.ghcn.GhcnV3_Helpers;
import com.mrsharky.stations.ghcn.GhcnV3_Helpers.QcType;
import com.mrsharky.helpers.InputParser_Abstract;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 *
 * @author jpierret
 */
public class GhcnV3_InputParser extends InputParser_Abstract {
    
    public String monthlyData;
    public int minMonthYears;
    public String inventoryData;
    public String lowerBaseline;
    public String upperBaseline;
    public GhcnV3_Helpers.QcType qcType;
    public String destination; 
    public double minDistance;
    public boolean createSpark;
    
    public GhcnV3_InputParser(String[] args, String className ) throws Exception{
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) throws Exception {
        // Required Variables
        monthlyData = line.getOptionValue("monthlyData");
        inventoryData = line.getOptionValue("inventoryData");
        destination = line.getOptionValue("destination");
        minDistance = Double.parseDouble(line.getOptionValue("minDistance"));
        lowerBaseline = line.getOptionValue("lowerBaseline");
        upperBaseline = line.getOptionValue("upperBaseline");
        minMonthYears = line.hasOption("minMonthYears") ? Integer.parseInt(line.getOptionValue("minMonthYears")) : 30;
        createSpark = line.hasOption("createSpark");

        switch(line.getOptionValue("qcType").toUpperCase()) {
            case "QCA" :
                qcType = QcType.QCA;
                break;
            case "QCU" :
                qcType = QcType.QCU;
                break;
            default :
                throw new Exception("Invalid qcType");
        }
    }

    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(OptionBuilder
                .withLongOpt("lowerBaseline").hasArg().isRequired()
                .withArgName("date")
                .withDescription("Baseline lower date cutoff value (yyyy-MM-dd)")
                .create('l'));
        options.addOption(OptionBuilder
                .withLongOpt("upperBaseline").hasArg().isRequired()
                .withArgName("date")
                .withDescription("Baseline upper date cutoff value (yyyy-MM-dd)")
                .create('u'));
        options.addOption(OptionBuilder
                .withLongOpt("monthlyData").hasArg().isRequired()
                .withArgName("file(s)")
                .withDescription("Monthly Data File")
                .create('m'));
        options.addOption(OptionBuilder
                .withLongOpt("minMonthYears").hasArg()
                .withArgName("years")
                .withDescription("Minimum number of years for monthly data")
                .create('y'));
        options.addOption(OptionBuilder
                .withLongOpt("inventoryData").hasArg().isRequired()
                .withArgName("file(s)")
                .withDescription("Station Inventory File")
                .create('s'));
        options.addOption(OptionBuilder
                .withLongOpt("destination").hasArg().isRequired()
                .withArgName("folder")
                .withDescription("destination location")
                .create('d'));
        options.addOption(OptionBuilder
                .withLongOpt("minDistance").hasArg().isRequired()
                .withArgName("distance")
                .withDescription("Minimum distance between stations (km)")
                .create("min"));
        options.addOption(OptionBuilder
                .withLongOpt("qcType").hasArg().isRequired()
                .withArgName("qcType")
                .withDescription("Quality Control type: QCA or QCU")
                .create('q'));
        options.addOption(Option.builder("s").required(false)
                .longOpt("createSpark")
                .desc("Create a spark session (use this option if you don't have a spark cluster to run the code locally)")
                .build());
        return options;
    }
}

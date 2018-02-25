/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.stations.netcdf;

import com.mrsharky.helpers.InputParser_Abstract;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 * @author jpierret
 */
public class NetCdf_InputParser extends InputParser_Abstract {
    
    public String input;
    public String variable;
    public String time;

    public String output; 
    public double minDistance;
    public String upperBaseline;
    public String lowerBaseline;
    public int minMonthYears;
    public boolean createSpark;
    
    public NetCdf_InputParser(String[] args, String className ) throws Exception{
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) throws Exception {
        input = line.getOptionValue("input");
        output = line.getOptionValue("output");
        variable = line.getOptionValue("variable");
        time = line.getOptionValue("time");
        lowerBaseline = line.getOptionValue("lowerBaseline");
        upperBaseline = line.getOptionValue("upperBaseline");
        minDistance = Double.parseDouble(line.getOptionValue("minDistance"));
        minMonthYears = line.hasOption("minMonthYears") ? Integer.parseInt(line.getOptionValue("minMonthYears")) : 30;
        createSpark = line.hasOption("createSpark");
    }

    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(Option.builder("l")
                .longOpt("lowerBaseline").hasArg().required()
                .argName("date")
                .desc("Baseline lower date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("u")
                .longOpt("upperBaseline").hasArg().required()
                .argName("date")
                .desc("Baseline upper date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("i")
                .longOpt("input").hasArg().required()
                .argName("file")
                .desc("NetCdf file to load")
                .build());
        options.addOption(Option.builder("o")
                .longOpt("output").hasArg().required()
                .argName("file")
                .desc("Output to store 'station' data results")
                .build());
        options.addOption(Option.builder("v")
                .longOpt("variable").hasArg().required()
                .argName("field")
                .desc("Variable values to load from NetCdf")
                .build());
        options.addOption(Option.builder("t")
                .longOpt("time").hasArg().required()
                .argName("field")
                .desc("Name of time field from NetCdf file")
                .build());
        options.addOption(Option.builder("min")
                .longOpt("minDistance").hasArg().required()
                .argName("distance")
                .desc("Minimum distance between stations (km)")
                .build());
        options.addOption(Option.builder("y")
                .longOpt("minMonthYears").hasArg()
                .argName("years")
                .desc("Minimum number of years for monthly data")
                .build());
        options.addOption(Option.builder("s").required(false)
                .longOpt("createSpark")
                .desc("Create a spark session (use this option if you don't have a spark cluster to run the code locally)")
                .build());
        return options;
    }
}

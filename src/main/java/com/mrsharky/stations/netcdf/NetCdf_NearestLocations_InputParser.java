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
public class NetCdf_NearestLocations_InputParser extends InputParser_Abstract {
    
    public String input;
    public String variable;
    public String time;

    public String output; 
    public String upperBaseline;
    public String lowerBaseline;
    public boolean createSpark;
    
    public int latCount;
    public int lonCount;
    
    public NetCdf_NearestLocations_InputParser(String[] args, String className ) throws Exception{
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
        latCount = line.hasOption("latCount") ? Integer.parseInt((line.getOptionValue("latCount"))) : 0;
        lonCount = line.hasOption("lonCount") ? Integer.parseInt((line.getOptionValue("lonCount"))) : 0;
        createSpark = line.hasOption("createSpark");
    }

    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(Option.builder("lat")
                .longOpt("latCount").hasArg().required(false)
                .argName("int")
                .desc("Number of lat locations to use for generating locations (If none given will use Angel-Korshover Network locations)")
                .build());
        options.addOption(Option.builder("lon")
                .longOpt("lonCount").hasArg().required(false)
                .argName("int")
                .desc("Number of lon locations to use for generating locations (If none given will use Angel-Korshover Network locations)")
                .build());
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
        options.addOption(Option.builder("s").required(false)
                .longOpt("createSpark")
                .desc("Create a spark session (use this option if you don't have a spark cluster to run the code locally)")
                .build());
        return options;
    }
}

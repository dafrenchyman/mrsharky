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
public class GridBoxVariance_InputParser extends InputParser_Abstract {
    
    public String input;
    public String output;
    public String variable;
    public String time;

    public int varianceNumYears;
    
    public GridBoxVariance_InputParser(String[] args, String className ) throws Exception{
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) throws Exception {
        input = line.getOptionValue("input");
        output = line.getOptionValue("output");
        variable = line.getOptionValue("variable");
        time = line.getOptionValue("time");
        varianceNumYears = line.hasOption("varianceNumYears") ? Integer.parseInt(line.getOptionValue("varianceNumYears")) : 30;
    }

    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
                
        options.addOption(Option.builder("i")
                .longOpt("input").hasArg().required()
                .argName("file")
                .desc("NetCdf file to load")
                .build());
        options.addOption(Option.builder("o")
                .longOpt("output").hasArg().required()
                .argName("file")
                .desc("Output to store gridbox variance results")
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
        options.addOption(Option.builder("y")
                .longOpt("varianceNumYears").hasArg()
                .argName("years")
                .desc("Number of years to calculate variance (Optional, defaults to 30)")
                .build());
        return options;
    }
}

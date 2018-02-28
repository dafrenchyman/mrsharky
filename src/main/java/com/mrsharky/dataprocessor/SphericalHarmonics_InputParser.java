/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import com.mrsharky.helpers.InputParser_Abstract;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_InputParser extends InputParser_Abstract {
        
    public String input;
    public String variable;
    public String time;
    public String output;
    public Date lowerDateCutoff;
    public Date upperDateCutoff;
    public Date startDate;
    public Date endDate;
    public boolean normalize;
    public int q;
    
    public SphericalHarmonics_InputParser(String[] args, String className) {
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) throws java.text.ParseException {               
        
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        // Required Variables
        input = line.getOptionValue("input");
        output = line.getOptionValue("output");
        variable = line.getOptionValue("variable");
        time = line.getOptionValue("time");           
        q = Integer.valueOf(line.getOptionValue("q"));
        lowerDateCutoff = format.parse(line.getOptionValue("lowerbaseline"));
        upperDateCutoff = format.parse(line.getOptionValue("upperbaseline"));
        normalize = line.hasOption("normalize");

        startDate = format.parse(line.getOptionValue("startDate"));
        endDate = format.parse(line.getOptionValue("endDate"));
        _inputsCorrect = true;
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
                .desc("Output to store serialized PCA results")
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
        options.addOption(Option.builder("q")
                .longOpt("q").hasArg().required()
                .argName("cut-off")
                .desc("Spherical Harmonics cutoff value")
                .build());
        options.addOption(Option.builder("l")
                .longOpt("lowerbaseline").hasArg().required()
                .argName("date")
                .desc("Baseline lower date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("u")
                .longOpt("upperbaseline").hasArg().required()
                .argName("date")
                .desc("Baseline upper date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("s")
                .longOpt("startDate").hasArg().required()
                .argName("date")
                .desc("Processing lower date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("e")
                .longOpt("endDate").hasArg().required()
                .argName("date")
                .desc("Processing upper date cutoff value (yyyy-MM-dd)")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("normalize")
                .desc("Normalize (divide by standard dev) the grid points")
                .build());
        return options;
    }
}

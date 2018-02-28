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
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_OptimalQ_InputParser extends InputParser_Abstract {
        
    public String input;
    public String variable;
    public String time;
    public String output;
    public Date lowerDateCutoff;
    public Date upperDateCutoff;
    public int qLower;
    public int qUpper;
    public boolean normalize;
    
    
    public SphericalHarmonics_OptimalQ_InputParser(String[] args, String className) {
        super(args, className);
    }
        
    @Override
    protected void ProcessInputs(CommandLine line) throws java.text.ParseException {               
        
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        // Required Variables
        input = line.getOptionValue("input");
        variable = line.getOptionValue("variable");
        time = line.getOptionValue("time");           
        qLower = Integer.valueOf(line.getOptionValue("qlower"));
        qUpper = Integer.valueOf(line.getOptionValue("qupper"));
        lowerDateCutoff = format.parse(line.getOptionValue("lowerbaseline"));
        upperDateCutoff = format.parse(line.getOptionValue("upperbaseline"));
        normalize = line.hasOption("normalize");
        _inputsCorrect = true;

    }
    
    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(OptionBuilder
                .withLongOpt("input").hasArg().isRequired()
                .withArgName("file")
                .withDescription("NetCdf file to load")
                .create('i'));
        options.addOption(OptionBuilder
                .withLongOpt("variable").hasArg().isRequired()
                .withArgName("field")
                .withDescription("Variable values to load from NetCdf")
                .create('v'));
        options.addOption(OptionBuilder
                .withLongOpt("time").hasArg().isRequired()
                .withArgName("field")
                .withDescription("Name of time field from NetCdf file")
                .create('t'));
        options.addOption(OptionBuilder
                .withLongOpt("qlower").hasArg().isRequired()
                .withArgName("cut-off")
                .withDescription("Spherical Harmonics lower cutoff value")
                .create("ql"));
        options.addOption(OptionBuilder
                .withLongOpt("qupper").hasArg().isRequired()
                .withArgName("cut-off")
                .withDescription("Spherical Harmonics upper cutoff value")
                .create("qu"));
        options.addOption(OptionBuilder
                .withLongOpt("lowerbaseline").hasArg().isRequired()
                .withArgName("date")
                .withDescription("Lower date cutoff value (yyyy-MM-dd)")
                .create('l'));
        options.addOption(OptionBuilder
                .withLongOpt("upperbaseline").hasArg().isRequired()
                .withArgName("date")
                .withDescription("Upper date cutoff value (yyyy-MM-dd)")
                .create('u'));
        options.addOption(OptionBuilder
                .withLongOpt("normalize").isRequired(false)
                .withArgName("bool")
                .withDescription("normalize the data (defaults to false)")
                .create('n'));
        options.addOption(OptionBuilder
                .withLongOpt("help").isRequired(false)
                .withDescription("Print this message")
                .create('h'));
        return options;
    }
}

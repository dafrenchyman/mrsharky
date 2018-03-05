/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.helpers.InputParser_Abstract;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 * @author Julien Pierret
 */
public class Climate_PcaStations_InputParser extends InputParser_Abstract {
       
    public String dataEof;
    public String dataStations;
    public int q;
    public String output;
    public double varExplained;
    public boolean normalized;
    public boolean createSpark;
    
    public Climate_PcaStations_InputParser(String[] args, String className) {
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) {                
        // Required Variables
        dataEof = line.getOptionValue("eof");              
        dataStations = line.getOptionValue("station");
        output = line.getOptionValue("output");
        varExplained = Double.parseDouble(line.getOptionValue("varExplained"));
        normalized = line.hasOption("normalized") ? true : false;
        q = line.hasOption("q") ? Integer.parseInt(line.getOptionValue("q")) : -1;
        createSpark = line.hasOption("createSpark");
        
        _inputsCorrect = true;
    }
    
    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(Option.builder("e")
                .longOpt("eof").hasArg().required()
                .argName("file")
                .desc("EOF Java Serialized data")
                .build());
        options.addOption(Option.builder("s")
                .longOpt("station").hasArg().required()
                .argName("file")
                .desc("Station Java Serialized data")
                .build());
        options.addOption(Option.builder("o")
                .longOpt("output").hasArg().required()
                .argName("file")
                .desc("Output CSV file")
                .build());
        options.addOption(Option.builder("v")
                .longOpt("varExplained").hasArg().required()
                .argName("double")
                .desc("Variance explained cut-off (0.0 - 1.0)")
                .build());
        options.addOption(Option.builder("n")
                .longOpt("normalized")
                .desc("Normalize the data")
                .build());
        options.addOption(Option.builder("q").hasArg()
                .longOpt("q")
                .required(false)
                .desc("Q truncation to use. If none given, defaults to that to the one from the eof dataset")
                .build());
        options.addOption(Option.builder("s").required(false)
                .longOpt("createSpark")
                .desc("Create a spark session (use this option if you don't have a spark cluster to run the code locally)")
                .build());
        return options;
    }
}

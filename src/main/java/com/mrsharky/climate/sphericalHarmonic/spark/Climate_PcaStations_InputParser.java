/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.helpers.InputParser_Abstract;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
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
    public int partitions;
    
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
        partitions = line.hasOption("partitions") ? Integer.parseInt(line.getOptionValue("partitions")) : 24;
        _inputsCorrect = true;
    }
    
    @Override
    protected Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(OptionBuilder
                .withLongOpt("eof").hasArg(true).isRequired(true)
                .withArgName("file")
                .withDescription("EOF Java Serialized data")
                .create('e'));
        options.addOption(OptionBuilder
                .withLongOpt("station").hasArg(true).isRequired(true)
                .withArgName("file")
                .withDescription("Station Java Serialized data")
                .create('s'));
        options.addOption(OptionBuilder
                .withLongOpt("output").hasArg(true).isRequired(true)
                .withArgName("file")
                .withDescription("Output CSV file")
                .create('o'));
        options.addOption(OptionBuilder
                .withLongOpt("varExplained").hasArg(true).isRequired(true)
                .withArgName("double")
                .withDescription("Variance explained cut-off (0.0 - 1.0)")
                .create('v'));
        options.addOption(OptionBuilder
                .withLongOpt("normalized").hasArg(false).isRequired(false)
                .withDescription("Normalize the data")
                .create('n'));
        options.addOption(OptionBuilder
                .withLongOpt("q").hasArg(true).isRequired(false)
                .withArgName("int")
                .withDescription("Q truncation to use. If none given, defaults to that to the one from the eof dataset")
                .create('q'));
        options.addOption(OptionBuilder
                .withLongOpt("createSpark").hasArg(false).isRequired(false)
                .withDescription("Create a spark session (use this option if you don't have a spark cluster to run the code locally)")
                .create('s'));
        options.addOption(OptionBuilder
                .withLongOpt("partitions").hasArg(true).isRequired(false)
                .withArgName("int")
                .withDescription("Number of spark partitions to split data by")
                .create('p'));
        
        
        return options;
    }
}

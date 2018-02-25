/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.nearestNeighbor;

import com.mrsharky.helpers.InputParser_Abstract;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


/**
 *
 * @author Julien Pierret
 */
public class NearestNeighbor_InputParser extends InputParser_Abstract {
       
    public String dataEof;
    public String dataStations;
    public String output;
    
    public NearestNeighbor_InputParser(String[] args, String className) {
        super(args, className);
    }
    
    @Override
    protected void ProcessInputs(CommandLine line) {               
        // Required Variables
        dataEof = line.getOptionValue("eof");              
        dataStations = line.getOptionValue("station");
        output = line.getOptionValue("output");
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
        return options;
    }
}

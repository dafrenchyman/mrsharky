/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Julien Pierret
 */
public class ClimateFromStations_InputParserOld {
    
    private boolean _inputsCorrect;
    
    public String dataEof;
    public String dataStations;
    public int q;
    
    public boolean InputsCorrect() {
        return this._inputsCorrect;
    }
    
    public ClimateFromStations_InputParserOld(String[] args) {
        
        _inputsCorrect = false;
       
        // create the command line parser
        CommandLineParser parser = new GnuParser();

        Options options = GenerateOptions();
           
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            
            if (line.hasOption("help")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "Resolve Report 1 Help", options );
                _inputsCorrect = false;
            } else {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                
                // Required Variables
                dataEof = line.getOptionValue("eof");              
                dataStations = line.getOptionValue("station");
                _inputsCorrect = true;
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Resolve Report 1 Help", options );
            _inputsCorrect = false;
        }
    }
    
    private Options GenerateOptions() {
        // create the Options
        Options options = new Options();
        
        options.addOption(OptionBuilder
                .withLongOpt("eof").hasArg().isRequired()
                .withArgName("file")
                .withDescription("EOF Java Serialized data")
                .create('e'));
        options.addOption(OptionBuilder
                .withLongOpt("station").hasArg().isRequired()
                .withArgName("file")
                .withDescription("Station Java Serialized data")
                .create('s'));
        options.addOption(OptionBuilder
                .withLongOpt("help").isRequired(false)
                .withDescription("Print this message")
                .create('h'));
        return options;
    }
}

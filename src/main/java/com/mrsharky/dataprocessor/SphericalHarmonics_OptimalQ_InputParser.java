/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

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
 * @author mrsharky
 */
public class SphericalHarmonics_OptimalQ_InputParser {
    
    private boolean _inputsCorrect;
    
    public String input;
    public String variable;
    public String time;
    public String output;
    public Date lowerDateCutoff;
    public Date upperDateCutoff;
    public int qLower;
    public int qUpper;
    public boolean normalize;
    
    public boolean InputsCorrect() {
        return this._inputsCorrect;
    }
    
    public SphericalHarmonics_OptimalQ_InputParser(String[] args) {
        
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
                input = line.getOptionValue("input");
                variable = line.getOptionValue("variable");
                time = line.getOptionValue("time");           
                qLower = Integer.valueOf(line.getOptionValue("qlower"));
                qUpper = Integer.valueOf(line.getOptionValue("qupper"));
                lowerDateCutoff = format.parse(line.getOptionValue("lowerbaseline"));
                upperDateCutoff = format.parse(line.getOptionValue("upperbaseline"));
                normalize = line.hasOption("normalize") ? true : false;
                _inputsCorrect = true;
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "Resolve Report 1 Help", options );
            _inputsCorrect = false;
        } catch (java.text.ParseException ex) {
            Logger.getLogger(SphericalHarmonics_OptimalQ_InputParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Options GenerateOptions() {
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

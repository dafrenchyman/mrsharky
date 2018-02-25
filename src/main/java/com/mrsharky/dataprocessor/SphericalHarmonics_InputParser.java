/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.dataprocessor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author mrsharky
 */
public class SphericalHarmonics_InputParser {
    
    private boolean _inputsCorrect;
    
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
    
    public boolean InputsCorrect() {
        return this._inputsCorrect;
    }
    
    public SphericalHarmonics_InputParser(String[] args, String className) {
        
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
                formatter.printHelp( className + " Help", options );
                _inputsCorrect = false;
            } else {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                
                // Required Variables
                input = line.getOptionValue("input");
                output = line.getOptionValue("output");
                variable = line.getOptionValue("variable");
                time = line.getOptionValue("time");           
                q = Integer.valueOf(line.getOptionValue("q"));
                lowerDateCutoff = format.parse(line.getOptionValue("lowerbaseline"));
                upperDateCutoff = format.parse(line.getOptionValue("upperbaseline"));
                normalize = line.hasOption("normalize") ? true : false;
                
                startDate = format.parse(line.getOptionValue("startDate"));
                endDate = format.parse(line.getOptionValue("endDate"));
                _inputsCorrect = true;
                
                
                // Print out all the input arguments
                try {
                    System.out.println("------------------------------------------------------------------------");
                    System.out.println(className);
                    System.out.println("------------------------------------------------------------------------");
                    Field[] fields = this.getClass().getFields();
                    for (Field field : fields) {
                        if (Modifier.isPublic(field.getModifiers())) {
                            System.out.println(field.getName() + ": " + field.get(this).toString());
                        }
                    }
                    System.out.println();
                } catch (Exception ex) {
                    
                }
            }
        } catch ( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
            
            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( className + " Help", options );
            _inputsCorrect = false;
        } catch (java.text.ParseException ex) {
            Logger.getLogger(SphericalHarmonics_InputParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Options GenerateOptions() {
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

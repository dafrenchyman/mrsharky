/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
public abstract class InputParser_Abstract {
    
    protected boolean _inputsCorrect;
        
    public boolean InputsCorrect() {
        return this._inputsCorrect;
    }
    
    public InputParser_Abstract(String[] args, String className) {
        
        _inputsCorrect = false;
       
        // create the command line parser
        CommandLineParser parser = new GnuParser();

        Options options = GenerateOptions();
        
        if (!options.hasShortOption("h")) {
            // Add the help menu option
            options.addOption(Option.builder("h")
                .longOpt("help").required(false)
                .desc("Print this message")
                .build());  
        }
        

        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            
            if (line.hasOption("help")) {
                // automatically generate the help statement
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( className + " Help", options );
                _inputsCorrect = false;
            } else {
                
                ProcessInputs(line);
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
        } catch (Exception ex) {
            Logger.getLogger(InputParser_Abstract.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected abstract Options GenerateOptions();
    
    protected abstract void ProcessInputs(CommandLine line) throws Exception;
    
}

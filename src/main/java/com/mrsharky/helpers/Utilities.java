/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.Array2DRowFieldMatrix;
import org.apache.commons.math3.linear.ArrayFieldVector;
import org.apache.commons.math3.linear.FieldMatrix;
import org.apache.commons.math3.linear.FieldVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author dafre
 */
public class Utilities {
    
    public static double[] ArrayOnes(int size) {
        double[] ret = new double[size];
        for (int counter = 0; counter < ret.length; counter++) {
            ret[counter] = 1;
        } 
        return ret;
    }
    
    public static int[] minToMaxByOne(int min, int max) {
        int[] ret = new int[max-min+1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = min + i;
        }
        return ret;
    }
    
    public static double[] minToMaxByOne(double min, double max) {
        double[] ret = new double[(int) max - (int) min+1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = min + i;
        }
        return ret;
    }
    
    public static double[] linspace(double min, double max, int points) {  
        double[] d = new double[points]; 
        if (points == 1) {
            d[0] = min;
        } else {
            for (int i = 0; i < points; i++){  
                d[i] = min + i * (max - min) / (points - 1);  
            }
        }
        return d;  
    }
    
    public static int[] linspace(int min, int max, int points) {  
        int[] d = new int[points]; 
        if (points == 1) {
            d[0] = min;
        } else {
            for (int i = 0; i < points; i++){  
                d[i] = min + i * (max - min) / (points - 1);  
            }
        }
        return d;  
    }
    
    public static double[] arange(double start, double end, double step) {
        int points = (int) Math.floor((end-start)/step);
        double[] d = new double[points];
        for (int i = 0; i < points; i++) {
            d[i] = start + step*i;
        }
        return d;
    }
    
    public static double[] randomLatitudeRadians(int num) {
        double[] ret = new double[num];
        for (int i = 0; i < num; i++) {
            ret[i] = Math.random()*Math.PI - Math.PI/2.0;
        }
        return ret;
    }
    
    public static double[] randomLongitudeRadians(int num) {
        double[] ret = new double[num];
        for (int i = 0; i < num; i++) {
            ret[i] = Math.random()*2.0*Math.PI - Math.PI;
        }
        return ret;
    }
    
    public static double[] randomLatitudeRadians(int num, Random rand) {
        double[] ret = new double[num];
        for (int i = 0; i < num; i++) {
            ret[i] = rand.nextDouble()*Math.PI - Math.PI/2.0;
        }
        return ret;
    }
    
    public static double[] randomLongitudeRadians(int num, Random rand) {
        double[] ret = new double[num];
        for (int i = 0; i < num; i++) {
            ret[i] = rand.nextDouble()*2.0*Math.PI - Math.PI;
        }
        return ret;
    }
    
    public static RealVector CosineOfVectorValues (RealVector vec) {
        RealVector ret = vec.copy();
        for (int counter = 0; counter < ret.getDimension(); counter++) {
            ret.setEntry(counter, Math.cos(ret.getEntry(counter)));
        }
        return ret;
    }
    
    public static RealVector SineOfVectorValues (RealVector vec) {
        RealVector ret = vec.copy();
        for (int counter = 0; counter < ret.getDimension(); counter++) {
            ret.setEntry(counter, Math.sin(ret.getEntry(counter)));
        }
        return ret;
    }
    
    
    public static int ComplexExample() {
        // create a 2x2 complex matrix 
        Complex[][] matrixData = new Complex[][] { 
            { new Complex(1.0,  0.0), new Complex( 0.0, 1.0) }, 
            { new Complex(0.0, -1.0), new Complex(-1.0, 0.0) } 
        }; 
        FieldMatrix<Complex> m = new Array2DRowFieldMatrix<Complex>(matrixData); 


        // create a vector 
        Complex[] vectorData = new Complex[] { 
            new Complex(1.0, 2.0), 
            new Complex(3.0, 4.0), 
        }; 
        FieldVector<Complex> u = new ArrayFieldVector<Complex>(vectorData); 

        // perform matrix-vector multiplication 
        FieldVector<Complex> v = m.operate(u); 
        

        // print the initial vector 
        for (int i = 0; i < u.getDimension(); ++i) { 
            //System.out.println(ComplexFormat.formatComplex(u.getEntry(i))); 
        } 

        System.out.println(); 

        // print the result 
        for (int i = 0; i < v.getDimension(); ++i) { 
            //System.out.println(ComplexFormat.formatComplex(v.getEntry(i))); 
        } 

        return 1;
    }
    
    public static Object GetValueFromKey(Map map, String[] key) {
        Object returnObject = null;
        if (key.length == 1) {
            return map.get(key[0]);
        } else {
            if (map.containsKey(key[0])) {
                if (map.get(key[0]) != null) {
                    Object newMap = map.get(key[0]);
                    try {
                        returnObject = GetValueFromKey((Map) newMap, Arrays.copyOfRange(key, 1, key.length));
                    } catch (Exception ex) {
                        System.out.println("ERROR: " + ex.getMessage());
                    }
                }
            }
        }
        return returnObject;
    }
    
    public static boolean CheckKeyNotNullNotEmptyTrim(Map map, String key) {
        boolean returnBool = false;
        if (map.containsKey(key)) {
            if (map.get(key) != null) {
                if (map.get(key).toString().trim().length() > 0) {
                    returnBool = true;
                }
            }
        }
        return returnBool;
    }
    
    public static boolean CheckKeyNotNullNotEmptyTrim(Map map, String[] key) {
        boolean returnBool = false;
        if (key.length == 1) {
            return CheckKeyNotNullNotEmptyTrim(map, key[0]);
        } else {
            if (map.containsKey(key[0])) {
                if (map.get(key[0]) != null) {
                    Object newMap = map.get(key[0]);
                    try {
                        returnBool = CheckKeyNotNullNotEmptyTrim((Map) newMap, Arrays.copyOfRange(key, 1, key.length));
                    } catch (Exception ex) {
                        System.out.println("ERROR: " + ex.getMessage());
                        returnBool = false;
                    }
                } 
            }
        }
        return returnBool;
    }
    
    /**
     * 
     * @param inputFile
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public static BufferedReader GetReaderForFile(String inputFile) throws FileNotFoundException, IOException { 
        BufferedReader br = null;
        String ext = FilenameUtils.getExtension(inputFile);

        if (ext.equals("log") ||  ext.equals("tsv") || ext.equals("csv") || ext.equals("") || ext.equals("parquet")) {
            br = new BufferedReader(new FileReader(inputFile));
        } else if (ext.equals("gz")) {
            br = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(inputFile))));
        }     
        return br;
    }
    
    public static void recursiveDelete(File index) {
        if (index.exists()) {
            String[]entries = index.list();
            for(String s: entries){
                File currentFile = new File(index.getPath(),s);
                if (currentFile.isDirectory()) {
                    recursiveDelete(currentFile);
                }
                currentFile.delete();
            }
            index.delete();
        }
    }
    
    public static void recursiveSysOut(File index) {
        try {
            if (index.exists()) {
                String[]entries = index.list();
                for(String s: entries){
                    File currentFile = new File(index.getPath(),s);
                    if (currentFile.isDirectory()) {
                        System.out.println();
                        System.out.println("------------------------------------");
                        System.out.println(currentFile.toString());
                        System.out.println("------------------------------------");
                        recursiveSysOut(currentFile);
                    } else {
                        BufferedReader br = GetReaderForFile(index.getPath() + "/" + s);
                        if (br != null) {
                            while (br.ready()) {
                                String currLine = br.readLine();
                                System.out.println(currLine);
                            }
                            br.close();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            
        }
    }
    
    /**
    * Calculate distance between two points in latitude and longitude taking
    * into account height difference. If you are not interested in height
    * difference pass 0.0. Uses Haversine method as its base.
    * 
    * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
    * 
    * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
    * el2 End altitude in meters
    * @returns Distance in Meters
    */
    public static double HaversineDistance(double lat1, double lat2, double lon1,
            double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
    
    public static void SerializeObject(Object classToSave, String output) {
        try {
            System.out.println("Saving results to: " + output);
            
            File file = new File(output);
            File folder = file.getParentFile();
            if (!folder.exists()) {
                folder.mkdirs();
            }

            FileOutputStream fileOut = new FileOutputStream(output);
            OutputStream buffer = new BufferedOutputStream(fileOut);
            ObjectOutputStream out = new ObjectOutputStream(buffer);
            out.writeObject(classToSave);
            out.close();
            fileOut.close();
            System.out.println("Finished saving results");
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
    
    public static Object LoadSerializedObject(String input) {
        Object output = null;
        try {
            System.out.println("Loading object from: " + input);
            FileInputStream fileIn = new FileInputStream(input);
            InputStream buffer = new BufferedInputStream(fileIn);
            ObjectInputStream in = new ObjectInputStream(buffer);
            output = in.readObject();
            in.close();
            fileIn.close();
            System.out.println("Finished loading object");
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        return output;
    }
    
    public static void SaveStringToFile(String valueToSave, String fileLocation) {       
        System.out.println("Saving results to: " + fileLocation);
            
        File file = new File(fileLocation);
        File folder = file.getParentFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        try {
            FileUtils.writeStringToFile(new File(fileLocation), valueToSave);
        } catch (IOException ex) {
            Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static double[] LatitudeToRadians(double[] lat) {
        double[] latRadians = new double[lat.length];
        for (int i = 0; i < lat.length; i++) {
            latRadians[i]= LatitudeToRadians(lat[i]);
        }
        return latRadians;
    }
    
    public static double LatitudeToRadians(double lat) {
        double latRadians = lat * Math.PI / 180.0;
        return latRadians;
    }
    
    public static double[] RadiansToLatitude(double[] radians) {
        double[] lats = new double[radians.length];
        for (int i = 0; i < lats.length; i++) {
            lats[i] = RadiansToLatitude(radians[i]);
        }
        return lats;
    }
    
    public static double RadiansToLatitude(double radians) {
        double lat = radians / Math.PI * 180.0;
        return lat;
    }
    
    public static double[] RadiansToLongitude(double[] rads) {
        double[] lons = new double[rads.length];
        for (int i = 0; i < rads.length; i++) {
            lons[i] = RadiansToLongitude(rads[i]);
        }
        return lons;
    }
    
    public static double RadiansToLongitude(double rad) {
        double lon = RadiansToLatitude(rad);
        if (lon > 180) {
            lon = lon -360;
        }
        return lon;
    }
    
    public static double[] LongitudeToRadians(double[] lon) {
        double[] newLon = new double[lon.length];
        for (int i = 0; i < lon.length; i++) {
            newLon[i] = LongitudeToRadians(lon[i]);
        }
        return newLon;
    }
    
    public static double LongitudeToRadians(double lon) {
        double newLon = lon;
        if (newLon > 180) {
            newLon = newLon -360;
        }
        return LatitudeToRadians(lon);
    }
    
    /**
     * Logic comes from here: https://en.wikipedia.org/wiki/Spherical_cap
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @param radius 
     */
    public static double AreaQuad(double lat1, double lon1, double lat2, double lon2, double radius) {
        // 
        // First calculate each "Sphere cap"
        double theta1 = Utilities.LatitudeToRadians(90.0 - lat1);
        double theta2 = Utilities.LatitudeToRadians(90.0 - lat2);
        
        double sphereCap1 = 2*Math.PI*Math.pow(radius, 2.0) * (1-Math.cos(theta1));
        double sphereCap2 = 2*Math.PI*Math.pow(radius, 2.0) * (1-Math.cos(theta2));
        
        double pieSlice = ((lon2+180) - (lon1+180)) / 360;
        if (pieSlice < 0) {
            pieSlice = 1.0 + pieSlice;
        }
        
        double bigCap = Math.max(sphereCap1, sphereCap2);
        double smaCap = Math.min(sphereCap1, sphereCap2);
        
        double area = (bigCap-smaCap) * pieSlice;
        return area;
    }
    
    
    
    
    
    
    public static void main(String args[]) throws Exception {
        
        // Check that if you cut up and use AreaQuad for a whole cirle
        // you'll get about the area for calculating the whole circle directly
        {
            double[] lats = arange(-90.0, 100, 10);
            double[] lons = arange(-180.0, 190, 10);

            double quad_area = 0;
            for (int i = 0; i < lats.length-1; i++) {
                for (int j = 0; j < lons.length-1; j++) {
                    double lat1 = lats[i];
                    double lat2 = lats[i+1];
                    double lon1 = lons[j];
                    double lon2 = lons[j+1];
                    quad_area += AreaQuad(lat1, lon1, lat2, lon2, 1.0);
                }
            }
            double sphere_area = Math.PI * Math.pow(1.0, 2.0)*4.0;

            System.out.println("Quad Area: " + quad_area);
            System.out.println("Sphere Area: " + sphere_area);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mrsharky.stations.netcdf;

/**
 * 
 * @author Julien Pierret
 */
public class AngellKorshoverNetwork {
    
    private double[] _lats;
    private double[] _lons;
    
    public double[] GetLats() {
        return _lats;
    }
    
    public double[] GetLons() {
        return _lons;
    }
    
    public AngellKorshoverNetwork() {

        _lons = new double[]{-8.4,
            -62.2,
            -119.2,
            -156.5,
            162.2,
            133.2,
            80.1,
            26.4,
            -10.2,
            -58.3,
            -80.4,
            -111.2,
            -131.3,
            -170.1,
            141.4,
            108.1,
            73.1,
            55.1,
            30.3,
            11.4,
            -17.3,
            -66,
            -97.3,
            -117.1,
            -155,
            166.4,
            144.5,
            114.1,
            88.2,
            72.5,
            2.1,
            32.3,
            -3.6,
            -74.1,
            -139,
            171.2,
            160,
            104,
            77,
            37,
            -52.2,
            -43.1,
            -70.3,
            -109.3,
            -194.4,
            177.3,
            146.5,
            118.4,
            96.5,
            47.3,
            28.4,
            -9.5,
            -73.1,
            -176.3,
            138.3,
            77.3,
            37.5,
            -2.2,
            -26.4,
            166.4,
            110.4,
            62.5,
            0
        };

        _lats = new double[]{
            70.6,
            82.3,
            76.1,
            71.2,
            70.4,
            67.3,
            73.3,
            67.2,
            51.6,
            48.3,
            51.2,
            47.3,
            55,
            57.1,
            45.2,
            57.5,
            54.6,
            51.5,
            50.2,
            48.1,
            14.4,
            18.3,
            25.5,
            32.4,
            19.4,
            19.2,
            13.3,
            22.2,
            22.3,
            19.1,
            13.3,
            15.4,
            5.2,
            4.4,
            -9.5,
            7,
            -9.2,
            1.2,
            8.3,
            -1.2,
            4.6,
            -22.5,
            -23.3,
            -27.1,
            -17.3,
            -17.4,
            -19.2,
            -20.2,
            -12.1,
            -18.5,
            -20.1,
            -40.2,
            -41.3,
            -43.6,
            -34.6,
            -37.5,
            -46.5,
            -70.2,
            -75.3,
            -77.5,
            -66.2,
            -67.4,
            -90
        };
        
        for (int i = 0; i < _lats.length; i++) {
            if (_lats[i] < -90) {
                _lats[i] = _lats[i] + 180;
            }
            
            if (_lats[i] > 90) {
                _lats[i] = _lats[i] - 180;
            }
            
            if (_lons[i] < -180) {
                _lons[i] = _lons[i] + 360;
            }
            
            if (_lons[i] > 180) {
                _lons[i] = _lons[i] - 360;
            }
            
        }
        
        
    }
}

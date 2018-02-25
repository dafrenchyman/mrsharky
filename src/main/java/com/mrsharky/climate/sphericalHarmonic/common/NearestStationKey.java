/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climate.sphericalHarmonic.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.javatuples.Pair;

/**
 *
 * @author Julien Pierret
 */
public class NearestStationKey {
    
    Map<List<Long>, Map<Pair<Integer, Integer>, Integer>> _data;
    public NearestStationKey() {
        _data = new ConcurrentHashMap<List<Long>, Map<Pair<Integer,Integer>, Integer>>();
    }
    
    public synchronized void Set(int lat, int lon, List<Long> stationList, Integer i) {
        stationList = stationList.stream().sorted().collect(Collectors.toList());
        if (!_data.containsKey(stationList)) {
            _data.put(stationList, new ConcurrentHashMap<Pair<Integer, Integer>, Integer>());
        }
        _data.get(stationList).put(Pair.with(lat, lon), i);
    }
    
    public synchronized boolean ContainsKey(int lat, int lon, List<Long> stationList) {
        stationList = stationList.stream().sorted().collect(Collectors.toList());
        boolean hasKey = false;
        if (_data.containsKey(stationList)) {
            if (_data.get(stationList).containsKey(Pair.with(lat, lon))) {
                hasKey = true;
            }
        }
        return hasKey;
    }
    
    public synchronized Integer Get(int lat, int lon, List<Long> stationList) {
        stationList = stationList.stream().sorted().collect(Collectors.toList());
        return _data.get(stationList).get(Pair.with(lat, lon));
    }
}

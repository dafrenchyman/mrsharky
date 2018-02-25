/*
 * Created by:
 *  ▐▄▄▄▄• ▄▌▄▄▌  ▪  ▄▄▄ . ▐ ▄ 
 *   ·███▪██▌██•  ██ ▀▄.▀·•█▌▐█
 * ▪▄ ███▌▐█▌██▪  ▐█·▐▀▀▪▄▐█▐▐▌
 * ▐▌▐█▌▐█▄█▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌
 *  ▀▀▀• ▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ 
 */
package com.mrsharky.climate.sphericalHarmonic.spark;

import com.mrsharky.climate.sphericalHarmonic.common.CalculateHarmonic;
import com.mrsharky.climate.sphericalHarmonic.common.Pca_EigenValVec;
import com.mrsharky.discreteSphericalTransform.InvDiscreteSphericalTransform;
import com.mrsharky.discreteSphericalTransform.SphericalHarmonic;
import com.mrsharky.stations.StationResults;
import com.mrsharky.stations.StationSelectionResults;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.complex.Complex;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.javatuples.Pair;
import org.javatuples.Quartet;
import org.javatuples.Quintet;

/**
 *
 * @author jpierret
 */
public class GenerateHarmonics implements Serializable
        , FlatMapFunction<Iterator<Quartet<Integer, Date, Integer, Integer>>
        , Quintet<Integer, Date, Integer, Integer, Complex>> {

    private final StationSelectionResults _stationData;
    private final Pca_EigenValVec _eigens;
    private final int _q;
    private final boolean _normalize;
    private final Map<Integer, Map<Integer, SphericalHarmonic>> _eigenSpherical;
    private final Map<Integer, Map<Integer, InvDiscreteSphericalTransform>> _eigenInvDst;
    
    public GenerateHarmonics (Map<Integer, Map<Integer, SphericalHarmonic>> eigenSpherical, 
            Map<Integer, Map<Integer, InvDiscreteSphericalTransform>> eigenInvDst, 
            StationSelectionResults stationData, 
            Pca_EigenValVec eigens, int q, boolean normalize) throws Exception {
        _eigenSpherical = eigenSpherical;
        _eigenInvDst = eigenInvDst;
        _stationData = stationData;
        _eigens = eigens;
        _q = q;
        _normalize = normalize;
    }
    
    @Override
    public Iterator<Quintet<Integer, Date, Integer, Integer, Complex>> call(Iterator<Quartet<Integer, Date, Integer, Integer>> inputRows) throws Exception {
        
        List<Quintet<Integer, Date, Integer, Integer, Complex>> results = new ArrayList<Quintet<Integer, Date, Integer, Integer, Complex>>();
        long rowsIn = 0;
        long rowsOut = 0;
        System.out.println("Starting - " + this.getClass().getName());
        while (inputRows.hasNext()) {
            rowsIn++;
            
            // Gather all the input data
            Quartet<Integer, Date, Integer, Integer> currRow =  inputRows.next();            
            int month = currRow.getValue0();
            Date date = currRow.getValue1();
            int k = currRow.getValue2();
            int l = currRow.getValue3();
            
            final Complex[][] eigenVectors = _eigens.GetEigenVectors(month);
            final Complex[] eigenValues = _eigens.GetEigenValues(month);
            
            // Calculate the harmonic
            CalculateHarmonic CalcHarm = new CalculateHarmonic(_q, _normalize, eigenVectors, eigenValues);
            List<StationResults> stations = _stationData.GetDate(month, date);
            Pair<Complex, double[]> values = CalcHarm.Process(k, l, stations);
            Complex S_lm = values.getValue0();
            
            results.add(Quintet.with(month, date, k, l, S_lm));
            rowsOut++;
        }
        System.out.println("Finished - " + this.getClass().getName() + " (rowsIn: " + rowsIn + ", rowsOut: " + rowsOut + ")");
        return results.iterator();  
    }
}

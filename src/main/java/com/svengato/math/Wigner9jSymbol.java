// =============================================================================
// Wigner9jSymbol.java
// =============================================================================
package com.svengato.math;

import java.util.Vector;
// =============================================================================

/** Wigner 9j symbol calculator engine */
public class Wigner9jSymbol extends Number
{
  public Wigner9jSymbol(HalfInteger j1, HalfInteger j2, HalfInteger j3,
    HalfInteger j4, HalfInteger j5, HalfInteger j6,
    HalfInteger j7, HalfInteger j8, HalfInteger j9) throws Exception {
    // check rules:
    //    ji > 0 (true by definition of HalfInteger)
    //    triangle relation: abs(j1-j2) <= j3 <= j1+j2
    if (HalfInteger.Sum(j1, j2).numerator < j3.numerator ||
      HalfInteger.Sum(j2, j3).numerator < j1.numerator ||
      HalfInteger.Sum(j3, j1).numerator < j2.numerator) {
      throw new Exception("(j1, j2, j3) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j4, j5).numerator < j6.numerator ||
      HalfInteger.Sum(j5, j6).numerator < j4.numerator ||
      HalfInteger.Sum(j6, j4).numerator < j5.numerator) {
      throw new Exception("(j4, j5, j6) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j7, j8).numerator < j9.numerator ||
      HalfInteger.Sum(j8, j9).numerator < j7.numerator ||
      HalfInteger.Sum(j9, j7).numerator < j8.numerator) {
      throw new Exception("(j7, j8, j9) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j1, j4).numerator < j7.numerator ||
      HalfInteger.Sum(j4, j7).numerator < j1.numerator ||
      HalfInteger.Sum(j7, j1).numerator < j4.numerator) {
      throw new Exception("(j1, j4, j7) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j2, j5).numerator < j8.numerator ||
      HalfInteger.Sum(j5, j8).numerator < j2.numerator ||
      HalfInteger.Sum(j8, j2).numerator < j5.numerator) {
      throw new Exception("(j2, j5, j8) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j3, j6).numerator < j9.numerator ||
      HalfInteger.Sum(j6, j9).numerator < j3.numerator ||
      HalfInteger.Sum(j9, j3).numerator < j6.numerator) {
      throw new Exception("(j3, j6, j9) triangle relation is not satisfied");
    } else {
      // calculate the result
      int nmin = Math.abs(j4.numerator - j8.numerator);
      int q19 = Math.abs(j1.numerator - j9.numerator);
      if (q19 > nmin) nmin = q19;
      int q26 = Math.abs(j2.numerator - j6.numerator);
      if (q26 > nmin) nmin = q26;
      int nmax = j4.numerator + j8.numerator;
      q19 = j1.numerator + j9.numerator;
      if (q19 < nmax) nmax = q19;
      q26 = j2.numerator + j6.numerator;
      if (q26 < nmax) nmax = q26;

//      double vvalue = 0;
      Vector vPft = new Vector();
      for (int n = nmin; n <= nmax; n += 2) {
        // n is the numerator of k (k = n/2)
        HalfInteger k = new HalfInteger(n);
        Wigner6jSymbol sixj1 = new Wigner6jSymbol(j1, j4, j7, j8, j9, k);
        PrimeFactorTable pftk = sixj1.pft;
        Wigner6jSymbol sixj2 = new Wigner6jSymbol(j2, j5, j8, j4, k, j6);
        pftk.Multiply(sixj2.pft);
        Wigner6jSymbol sixj3 = new Wigner6jSymbol(j3, j6, j9, k, j1, j2);
        pftk.Multiply(sixj3.pft);
        int signk = (n % 2 == 0 ? 1 : -1);
        if (signk == -1) ++pftk.primeFactor[1];
        PrimeFactorTable pftn1 = new PrimeFactorTable(n + 1);
        pftn1.Square();
        pftk.Multiply(pftn1);
//        double valk = signk*(n + 1)*
//          sixj1.doubleValue()*sixj2.doubleValue()*sixj3.doubleValue();
//        vvalue += valk;
        vPft.addElement(pftk);
      }
      PrimeFactorTable pft = PrimeFactorTable.Sum(vPft);
      RationalSqrt rs = new RationalSqrt(pft);
      value = rs.doubleValue();
      nnddStr = rs.toString();
//System.out.println("  -> pft = " + value + ", vvalue = " + vvalue);
    }
  }
  public String toString() { return nnddStr; }
  public short shortValue() { return (short)doubleValue(); }
  public int intValue() { return (int)doubleValue(); }
  public long longValue() { return (long)doubleValue(); }
  public float floatValue() { return (float)doubleValue(); }
  public double doubleValue() { return value; }

  PrimeFactorTable pft;
  double value;
  String nnddStr;
}

// =============================================================================

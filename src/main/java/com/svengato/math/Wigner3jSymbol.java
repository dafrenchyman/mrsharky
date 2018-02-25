// =============================================================================
// Wigner3jSymbol.java
// =============================================================================
package com.svengato.math;
// =============================================================================

/** Wigner 3j symbol calculator engine */
public class Wigner3jSymbol extends Number
{
  public Wigner3jSymbol(HalfInteger j1, HalfInteger j2, HalfInteger j3,
    HalfInteger m1, HalfInteger m2, HalfInteger m3) throws Exception {
    // check rules:
    //    ji > 0 (true by definition of HalfInteger)
    //    abs(mi) <= ji
    //    ji+mi = integer
    //    triangle relation: abs(j1-j2) <= j3 <= j1+j2
    //    m1+m2 = m3 (or -m3 if using that convention?)
    //    j1+j2+j3 <= jmax
    if (Math.abs(m1.numerator) > j1.numerator) {
      throw new Exception("abs(m1) > j1");
    } else if (Math.abs(m2.numerator) > j2.numerator) {
      throw new Exception("abs(m2) > j2");
    } else if (Math.abs(m3.numerator) > j3.numerator) {
      throw new Exception("abs(m3) > j3");
    } else if (HalfInteger.Sum(j1, m1).isInteger() == false) {
      throw new Exception("j1 and m1 must both be integer or half-integer");
    } else if (HalfInteger.Sum(j2, m2).isInteger() == false) {
      throw new Exception("j2 and m2 must both be integer or half-integer");
    } else if (HalfInteger.Sum(j3, m3).isInteger() == false) {
      throw new Exception("j3 and m3 must both be integer or half-integer");
    } else if (HalfInteger.Sum(j1, j2).numerator < j3.numerator ||
      HalfInteger.Sum(j2, j3).numerator < j1.numerator ||
      HalfInteger.Sum(j3, j1).numerator < j2.numerator) {
      throw new Exception("(j1, j2, j3) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(m1, m2).numerator != -m3.numerator) {
       String chNotEqual = "!="; //'\u2260';
      throw new Exception("(m1 + m2 + m3) " + chNotEqual + " 0");
    } else {
      // calculate the result
      long sum = 0;

      int q1 = (j1.numerator - m1.numerator)/2;
      int q2 = (j2.numerator + m2.numerator)/2;
      int q3 = (j1.numerator + j2.numerator - j3.numerator)/2;
      int q4 = (j3.numerator + j1.numerator - j2.numerator)/2;
      int q5 = (j2.numerator + j3.numerator - j1.numerator)/2;
      int kmax = (q1 < q2 ? q1 : q2);
      kmax = (q3 < kmax ? q3 : kmax);

      int q6 = (j2.numerator - j3.numerator - m1.numerator)/2; // q1 - q4
      int q7 = (j1.numerator - j3.numerator + m2.numerator)/2; // q2 - q5
      int kmin = (q6 > q7 ? q6 : q7);
      kmin = (0 > kmin ? 0 : kmin);

      for (int k=kmin; k<=kmax; k++) {
        PrimeFactorTable pftk = PrimeFactorTable.BinomialCoefficient(q3, k);
        pftk.Multiply(PrimeFactorTable.BinomialCoefficient(q4, q1-k));
        pftk.Multiply(PrimeFactorTable.BinomialCoefficient(q5, q2-k));
        pftk.Square();

        int signk = (k % 2 == 0 ? 1 : -1);
        if (signk == -1) ++pftk.primeFactor[1];
        sum += pftk.longValue();
      }
      int sign_sum = (sum >= 0 ? 1 : -1);
      pft = new PrimeFactorTable(sum);
      pft.Square(); // put in rational sqrt form,
      if (sign_sum == -1) ++pft.primeFactor[1]; // but retain its sign

      int q8 = (j1.numerator + m1.numerator)/2;
      int q9 = (j2.numerator - m2.numerator)/2;
      int q10 = (j3.numerator + m3.numerator)/2;
      int q11 = (j3.numerator - m3.numerator)/2;
      pft.Multiply(PrimeFactorTable.Factorial(q8));
      pft.Multiply(PrimeFactorTable.Factorial(q1));
      pft.Multiply(PrimeFactorTable.Factorial(q2));
      pft.Multiply(PrimeFactorTable.Factorial(q9));
      pft.Multiply(PrimeFactorTable.Factorial(q10));
      pft.Multiply(PrimeFactorTable.Factorial(q11));

      int q12 = (j1.numerator + j2.numerator + j3.numerator)/2 + 1;
      pft.DivideBy(PrimeFactorTable.Factorial(q3));
      pft.DivideBy(PrimeFactorTable.Factorial(q4));
      pft.DivideBy(PrimeFactorTable.Factorial(q5));
      pft.DivideBy(PrimeFactorTable.Factorial(q12));
      int sign = ((q8-q9) % 2 == 0 ? 1 : -1);
      if (sign == -1) ++pft.primeFactor[1];

      RationalSqrt rs = new RationalSqrt(pft);
      value = rs.doubleValue();
      nnddStr = rs.toString();
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

// =============================================================================
// Wigner6jSymbol.java
// =============================================================================
package com.svengato.math;
// =============================================================================

/** Wigner 6j symbol calculator engine */
public class Wigner6jSymbol extends Number
{
  public Wigner6jSymbol(HalfInteger j1, HalfInteger j2, HalfInteger j3,
    HalfInteger j4, HalfInteger j5, HalfInteger j6) throws Exception {
    // check rules:
    //    ji > 0 (true by definition of HalfInteger)
    //    triangle relations:
    //      abs(j1-j2) <= j3 <= j1+j2 and j1+j2+j3 = integer
    //      abs(j4-j5) <= j3 <= j4+j5 and j4+j5+j3 = integer
    //      abs(j4-j6) <= j2 <= j4+j6 and j4+j2+j6 = integer
    //      abs(j5-j6) <= j1 <= j5+j6 and j1+j5+j6 = integer
    //    j1+j2+j3 <= jmax
    if (HalfInteger.Sum(j1, j2, j3).isInteger() == false) {
      throw new Exception("Sum of (j1, j2, j3) must be an integer");
    } else if (HalfInteger.Sum(j4, j5, j3).isInteger() == false) {
      throw new Exception("Sum of (j4, j5, j3) must be an integer");
    } else if (HalfInteger.Sum(j4, j2, j6).isInteger() == false) {
      throw new Exception("Sum of (j4, j2, j6) must be an integer");
    } else if (HalfInteger.Sum(j1, j5, j6).isInteger() == false) {
      throw new Exception("Sum of (j1, j5, j6) must be an integer");
    } else if (HalfInteger.Sum(j1, j2).numerator < j3.numerator ||
      HalfInteger.Sum(j2, j3).numerator < j1.numerator ||
      HalfInteger.Sum(j3, j1).numerator < j2.numerator) {
      throw new Exception("(j1, j2, j3) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j4, j5).numerator < j3.numerator ||
      HalfInteger.Sum(j5, j3).numerator < j4.numerator ||
      HalfInteger.Sum(j3, j4).numerator < j5.numerator) {
      throw new Exception("(j4, j5, j3) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j4, j2).numerator < j6.numerator ||
      HalfInteger.Sum(j2, j6).numerator < j4.numerator ||
      HalfInteger.Sum(j6, j4).numerator < j2.numerator) {
      throw new Exception("(j4, j2, j6) triangle relation is not satisfied");
    } else if (HalfInteger.Sum(j1, j5).numerator < j6.numerator ||
      HalfInteger.Sum(j5, j6).numerator < j1.numerator ||
      HalfInteger.Sum(j6, j1).numerator < j5.numerator) {
      throw new Exception("(j1, j5, j6) triangle relation is not satisfied");
    } else {
      // calculate the result
      long sum = 0;

      int q1 = (j1.numerator + j2.numerator - j3.numerator)/2;
      int q2 = (j4.numerator + j5.numerator - j3.numerator)/2;
      int q3 = (j4.numerator + j2.numerator - j6.numerator)/2;
      int q4 = (j1.numerator + j5.numerator - j6.numerator)/2;
      int kmax = (q1 < q2 ? q1 : q2);
      kmax = (q3 < kmax ? q3 : kmax);
      kmax = (q4 < kmax ? q4 : kmax);

      int q5 = (j1.numerator + j4.numerator - j3.numerator - j6.numerator)/2;
      int q6 = (j2.numerator + j5.numerator - j3.numerator - j6.numerator)/2;
      int kmin = (q5 > q6 ? q5 : q6);
      kmin = (0 > kmin ? 0 : kmin);

      int q7 = (j1.numerator + j2.numerator + j4.numerator + j5.numerator)/2;
      int q8 = (j1.numerator + j2.numerator + j3.numerator)/2 + 1;
      for (int k=kmin; k<=kmax; k++) {
        PrimeFactorTable pftk = PrimeFactorTable.BinomialCoefficient(q1, k);
        pftk.Multiply(PrimeFactorTable.BinomialCoefficient(q7 + 1 - k, q8));
        pftk.Multiply(PrimeFactorTable.BinomialCoefficient(q3-q5, q3-k));
        pftk.Multiply(PrimeFactorTable.BinomialCoefficient(q4-q6, q4-k));
        pftk.Square();

        int signk = (k % 2 == 0 ? 1 : -1);
        if (signk == -1) ++pftk.primeFactor[1];
        sum += pftk.longValue();
      }
      int sign_sum = (sum >= 0 ? 1 : -1);
      pft = new PrimeFactorTable(sum);
      pft.Square(); // put in rational sqrt form,
      if (sign_sum == -1) ++pft.primeFactor[1]; // but retain its sign

      pft.Multiply(PrimeFactorTable.Factorial(q8));
      pft.Multiply(PrimeFactorTable.Factorial(q2));
      pft.Multiply(PrimeFactorTable.Factorial(q3));
      pft.Multiply(PrimeFactorTable.Factorial(q4));
      int q9 = (j4.numerator - j5.numerator + j3.numerator)/2;
      int q10 = (j5.numerator - j4.numerator + j3.numerator)/2;
      pft.Multiply(PrimeFactorTable.Factorial(q9));
      pft.Multiply(PrimeFactorTable.Factorial(q10));
      int q11 = (j1.numerator - j5.numerator + j6.numerator)/2;
      int q12 = (-j1.numerator + j5.numerator + j6.numerator)/2;
      pft.Multiply(PrimeFactorTable.Factorial(q11));
      pft.Multiply(PrimeFactorTable.Factorial(q12));
      int q13 = (j4.numerator - j2.numerator + j6.numerator)/2;
      int q14 = (-j4.numerator + j2.numerator + j6.numerator)/2;
      pft.Multiply(PrimeFactorTable.Factorial(q13));
      pft.Multiply(PrimeFactorTable.Factorial(q14));

      pft.DivideBy(PrimeFactorTable.Factorial(q1));
      int q15 = (j3.numerator + j2.numerator - j1.numerator)/2;
      int q16 = (j1.numerator + j3.numerator - j2.numerator)/2;
      pft.DivideBy(PrimeFactorTable.Factorial(q15));
      pft.DivideBy(PrimeFactorTable.Factorial(q16));
      int q17 = (j4.numerator + j5.numerator + j3.numerator)/2 + 1;
      int q18 = (j4.numerator + j2.numerator + j6.numerator)/2 + 1;
      int q19 = (j1.numerator + j5.numerator + j6.numerator)/2 + 1;
      pft.DivideBy(PrimeFactorTable.Factorial(q17));
      pft.DivideBy(PrimeFactorTable.Factorial(q18));
      pft.DivideBy(PrimeFactorTable.Factorial(q19));

      int sign = (q7 % 2 == 0 ? 1 : -1);
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

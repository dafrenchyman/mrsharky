// =============================================================================
// PrimeFactorTable.java
// =============================================================================
package com.svengato.math;

import java.util.Vector;
// =============================================================================

/** Prime factor representation of the square root of a rational number */
public class PrimeFactorTable extends Number {
  // constants
  public static final int jMax = 65; // (jMax + 1)! / m! (jMax + 1 - m)! < 2^63 (limit of signed 64-bit integer)
  public static final long ithPrime[] = {
    0, -1, 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61
  };
  public static final int maxPrimes = ithPrime.length; // = 20, enough to include all primes up to jMax + 1

  PrimeFactorTable(long n, int power) {
    // value = Product[ithPrime[i]^(primeFactor[i]*power)] * remainder
    primeFactor = new int[maxPrimes];
    remainder = 1;

    if (n == 0) {
      primeFactor[0] = 1;
      for (int i = 1; i < maxPrimes; i++) primeFactor[i] = 0;

    } else {
      primeFactor[0] = 0;

      if (n < 0) {
        primeFactor[1] = power; // (-1)^power
        n = -n; // use the absolute value
      } else {
        primeFactor[1] = 0;
      }

      for (int i = 2; i < maxPrimes && n > 1; i++) {
        primeFactor[i] = 0;
        while (n % ithPrime[i] == 0) {
          primeFactor[i] += power;
          n /= ithPrime[i];
        }
      }

      if (n != 1) remainder = n; // remaining factors
    }
  }
  PrimeFactorTable(long n) {
    this(n, 1);
  }

  public void Multiply(PrimeFactorTable pft) {
    if (pft.equalsZero()) {
      primeFactor[0] = 1;
    } else {
      for (int i = 1; i < maxPrimes; i++) primeFactor[i] += pft.primeFactor[i];
      remainder *= pft.remainder;
    }
  }

  public void DivideBy(PrimeFactorTable pft) {
    if (pft.equalsZero()) {
      // divide by zero error...
    } else {
      for (int i = 1; i < maxPrimes; i++) primeFactor[i] -= pft.primeFactor[i];
      remainder /= pft.remainder;
    }
  }

  public static PrimeFactorTable Sum(Vector vpft) {
    PrimeFactorTable pft = new PrimeFactorTable(0L);
    RationalSqrt rs = new RationalSqrt(pft);
    rs.integerizeNumerator();

    // loop over all combinations in vpft, match like terms
    for (int i = 0; i < vpft.size(); i++) {
      PrimeFactorTable pfti = (PrimeFactorTable)vpft.elementAt(i);
      if (pfti.equalsZero()) continue;

      RationalSqrt rsi = new RationalSqrt(pfti);
      rsi.integerizeNumerator();

      // having converted the numerator to an integer, we can sum the terms
      if (pft.equalsZero()) {
        pft = pfti;
        rs = new RationalSqrt(pft);
        rs.integerizeNumerator();
      } else {
        // subsequent terms
        long n = rs.n1*rsi.d1 + rsi.n1*rs.d1;
        int signi = (n < 0 ? -1 : 1);
        pft = new PrimeFactorTable(n);
        pft.DivideBy(new PrimeFactorTable(rs.d1)); // treat denominators separately to avoid numeric overflow
        pft.DivideBy(new PrimeFactorTable(rsi.d1));
        pft.Square();
        if (signi == -1) ++pft.primeFactor[1];
        pft.DivideBy(new PrimeFactorTable(rsi.d2)); // knowing that rsi.d2 == rs.d2 after integerizing the numerators
        rs = new RationalSqrt(pft);
        rs.integerizeNumerator();
      }
    }

    return pft;
  }

  public void Square() {
    for (int i = 0; i < maxPrimes; i++) primeFactor[i] *= 2;
    remainder *= remainder;
  }

/*  public void Sqrt() {
    // need to check that primeFactor[i] is even...
    for (int i = 2; i < maxPrimes; i++) primeFactor[i] /= 2;
    remainder = (long)Math.sqrt(remainder);
  } */

  public static PrimeFactorTable Factorial(int n) {
    PrimeFactorTable pft = new PrimeFactorTable(1);
    for (int i = 2; i <= n; i++) {
      PrimeFactorTable pfti = new PrimeFactorTable(i);
      pft.Multiply(pfti);
    }

    return pft;
  }
  public static PrimeFactorTable BinomialCoefficient(int n, int k) {
    PrimeFactorTable pft;

    if (k == 0 || k == n) {
      pft = new PrimeFactorTable(1);
    } else if (k == 1 || k == n - 1) {
      pft = new PrimeFactorTable(n);
    } else {
      pft = Factorial(n);
      pft.DivideBy(Factorial(k));
      pft.DivideBy(Factorial(n - k));
    }

    return pft;
  }

  public boolean equalsZero() { return (primeFactor[0] != 0); }

  public short shortValue() { return (short)doubleValue(); }
  public int intValue() { return (int)doubleValue(); }
  public long longValue() { return (long)doubleValue(); }
  public float floatValue() { return (float)doubleValue(); }
  public double doubleValue() {
    RationalSqrt rs = new RationalSqrt(this);
    double dval = (double)rs.n1 * Math.sqrt((double)rs.n2) /
      ((double)rs.d1 * Math.sqrt((double)rs.d2));
    return dval;
  }

  public String toString() {
    StringBuffer strbuf = new StringBuffer();

    strbuf.append("{ ");
    for (int i = 0; i < maxPrimes; i++) {
      strbuf.append(primeFactor[i] + ", ");
    }
    strbuf.append(remainder + " }");

    return (strbuf.toString());
  }

  public int primeFactor[];
  public long remainder;
}

// =============================================================================

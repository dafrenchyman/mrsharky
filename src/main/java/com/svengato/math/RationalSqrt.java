// =============================================================================
// RationalSqrt.java
// =============================================================================
package com.svengato.math;
// =============================================================================

/** Square root of a rational number, in the form (n1*sqrt(n2))/(d1*sqrt(d2)) where n1, n2, d1, d2 are integers */
public class RationalSqrt extends Number {
  RationalSqrt(PrimeFactorTable pft) {
    int i, j;

    n2 = d1 = d2 = 1;
    if (pft.primeFactor[0] != 0) {
      n1 = 0;
    } else {
      long sign = (pft.primeFactor[1] % 2 == 0 ? 1 : -1);
      n1 = (long)Math.sqrt(pft.remainder) * sign;
      for (i = 2; i < pft.maxPrimes; i++) {
        if (pft.primeFactor[i] < 0) {
          int m = -pft.primeFactor[i]; // m = power of ithPrime[i]
          if (m % 2 != 0) {
            // odd power of ithPrime[i]
            d2 *= pft.ithPrime[i];
            for (j = 1; j <= ((m-1)/2); j++) d1 *= pft.ithPrime[i];
          } else {
            // even power of ithPrime[i]
            for (j = 1; j <= (m/2); j++) d1 *= pft.ithPrime[i];
          }
        } else if (pft.primeFactor[i] > 0) {
          int m = pft.primeFactor[i]; // m = power of ithPrime[i]
          if (m % 2 != 0) {
            // odd power of ithPrime[i]
            n2 *= pft.ithPrime[i];
            for (j = 1; j <= ((m-1)/2); j++) n1 *= pft.ithPrime[i];
          } else {
            // even power of ithPrime[i]
            for (j = 1; j <= (m/2); j++) n1 *= pft.ithPrime[i];
          }
        }
      }
    }
  }

  public void integerizeNumerator() {
    n1 *= n2;
    d2 *= n2;
    n2 = 1;
  }

  public String toString() {
    // return "(n1*sqrt(n2))/(d1*sqrt(d2))" format
/*    String sqrtOpen = "sqrt(";
    String sqrtClose = ")"; */
    String sqrtOpen = new String("\u221a"); // square root/radical sign (√)
    String sqrtClose = "";
    StringBuffer strbuf = new StringBuffer();

    if (n1 == 1) {
      if (n2 == 1) {
        strbuf.append(n1);
      } else {
        strbuf.append(sqrtOpen);
        strbuf.append(n2);
        strbuf.append(sqrtClose);
      }
    } else if (n1 == -1) {
      if (n2 == 1) {
        strbuf.append(n1);
      } else {
        strbuf.append("-");
        strbuf.append(sqrtOpen);
        strbuf.append(n2);
        strbuf.append(sqrtClose);
      }
    } else {
      strbuf.append(n1);
      if (n2 != 1) {
        strbuf.append(" " + sqrtOpen);
        strbuf.append(n2);
        strbuf.append(sqrtClose);
      }
    }

    if (d1 == 1) {
      if (d2 != 1) {
        strbuf.append(" / ");
        strbuf.append(sqrtOpen);
        strbuf.append(d2);
        strbuf.append(sqrtClose);
      }
    } else {
      strbuf.append(" / ");
      strbuf.append(d1);
      if (d2 != 1) {
        strbuf.append(" " + sqrtOpen);
        strbuf.append(d2);
        strbuf.append(sqrtClose);
      }
    }

    return (strbuf.toString());
  }

  public short shortValue() { return (short)doubleValue(); }
  public int intValue() { return (int)doubleValue(); }
  public long longValue() { return (long)doubleValue(); }
  public float floatValue() { return (float)doubleValue(); }
  public double doubleValue() {
    double dval = (double)n1 * Math.sqrt((double)n2) / ((double)d1 * Math.sqrt((double)d2));
    return dval;
  }

  long n1, n2, d1, d2;
}

// =============================================================================

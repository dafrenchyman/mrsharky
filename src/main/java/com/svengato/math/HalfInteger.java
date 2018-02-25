// =============================================================================
// HalfInteger.java
// =============================================================================
package com.svengato.math;
// =============================================================================

/** Converts an integer or (valid) string to a half-integer representation */
public class HalfInteger extends Number
{
  HalfInteger(int n) {
    numerator = n;
  }
  HalfInteger(String str) {
    if (str.endsWith("/2")) {
      numerator = Integer.valueOf(str.substring(0, str.length() - 2)).intValue();
    } else if (str.endsWith(".5")) {
      double value = Double.valueOf(str).doubleValue();
      numerator = 2*(int)value;
      numerator += (value < 0 ? -1 : 1);
    } else {
      numerator = 2 * Integer.valueOf(str).intValue();
//    } else {
//      throw new Exception("j and m values must be in the form N, N.5, or N/2");
    }
  }
  public static HalfInteger valueOf(String str) throws NumberFormatException {
    return new HalfInteger(str);
  }
  public String toString() {
    if (isInteger()) {
      return(Integer.toString(numerator/2));
    } else {
      return(Integer.toString(numerator) + "/2");
    }
  }

  public boolean isOddHalfInteger() { return (numerator % 2 != 0); }
  public boolean isInteger() { return (numerator % 2 == 0); }
  public static HalfInteger Sum(HalfInteger hint1, HalfInteger hint2) {
    return new HalfInteger(hint1.numerator + hint2.numerator);
  }
  public static HalfInteger Sum(HalfInteger hint1, HalfInteger hint2, HalfInteger hint3) {
    return new HalfInteger(hint1.numerator + hint2.numerator + hint3.numerator);
  }

  public short shortValue() { return (short)(numerator/2); }
  public int intValue() { return numerator/2; }
  public long longValue() { return numerator/2; }
  public float floatValue() { return (float)(0.5*numerator); }
  public double doubleValue() { return 0.5*numerator; }

  public int numerator;
}

// =============================================================================

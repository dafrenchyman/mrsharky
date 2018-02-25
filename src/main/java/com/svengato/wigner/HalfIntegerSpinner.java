// =============================================================================
// HalfIntegerSpinner.java
// =============================================================================
package com.svengato.wigner;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import java.util.ArrayList;
import com.svengato.math.HalfInteger;
// =============================================================================

/** Spinner widget for half-integer values (... -3/2, -1, -1/2, 0, 1/2, 1, 3/2 ...) */
public class HalfIntegerSpinner extends JSpinner
{
  HalfIntegerSpinner(String initialValue, int minValue, int maxValue) {
    ArrayList<String> values = new ArrayList<String>(2*(maxValue - minValue) + 1);
    for (int i = minValue; i <= maxValue; ++i) {
      String v = (i % 2 == 0 ? "" + (i/2) : i + "/2");
      values.add(v);
    }
    setModel(new SpinnerListModel(values));
    setValue(initialValue);

    // to be effective, this must come after setting the spinner model
    ((JSpinner.DefaultEditor)getEditor()).getTextField().setColumns(4);
  }
}

// =============================================================================

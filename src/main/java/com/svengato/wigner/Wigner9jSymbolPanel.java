// =============================================================================
// Wigner9jSymbolPanel.java
// =============================================================================
package com.svengato.wigner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import com.svengato.gui.SelectableLabel;
import com.svengato.math.HalfInteger;
import com.svengato.math.Wigner9jSymbol;
// =============================================================================

/** Wigner 9j symbol calculator panel */
public class Wigner9jSymbolPanel extends AbstractWignerNjSymbolPanel
{
  HalfIntegerSpinner j1Txt, j2Txt, j3Txt;
  HalfIntegerSpinner j4Txt, j5Txt, j6Txt;
  HalfIntegerSpinner j7Txt, j8Txt, j9Txt;

  public Wigner9jSymbolPanel() {
    pnlCalculator.setLayout(new GridLayout(3, 3, 5, 5));

    j1Txt = new HalfIntegerSpinner("2", 0, 12);
    j2Txt = new HalfIntegerSpinner("2", 0, 12);
    j3Txt = new HalfIntegerSpinner("1", 0, 12);
    j4Txt = new HalfIntegerSpinner("2", 0, 12);
    j5Txt = new HalfIntegerSpinner("1", 0, 12);
    j6Txt = new HalfIntegerSpinner("2", 0, 12);
    j7Txt = new HalfIntegerSpinner("1", 0, 12);
    j8Txt = new HalfIntegerSpinner("2", 0, 12);
    j9Txt = new HalfIntegerSpinner("2", 0, 12);
    j1Txt.setBackground(Color.white);
    pnlCalculator.add(j1Txt);
    j2Txt.setBackground(Color.white);
    pnlCalculator.add(j2Txt);
    j3Txt.setBackground(Color.white);
    pnlCalculator.add(j3Txt);
    j4Txt.setBackground(Color.white);
    pnlCalculator.add(j4Txt);
    j5Txt.setBackground(Color.white);
    pnlCalculator.add(j5Txt);
    j6Txt.setBackground(Color.white);
    pnlCalculator.add(j6Txt);
    j7Txt.setBackground(Color.white);
    pnlCalculator.add(j7Txt);
    j8Txt.setBackground(Color.white);
    pnlCalculator.add(j8Txt);
    j9Txt.setBackground(Color.white);
    pnlCalculator.add(j9Txt);

    doCalculate();
  }

  public void doCalculate() {
    try {
      Wigner9jSymbol wigner9j = new Wigner9jSymbol(
        HalfInteger.valueOf(j1Txt.getValue().toString()),
        HalfInteger.valueOf(j2Txt.getValue().toString()),
        HalfInteger.valueOf(j3Txt.getValue().toString()),
        HalfInteger.valueOf(j4Txt.getValue().toString()),
        HalfInteger.valueOf(j5Txt.getValue().toString()),
        HalfInteger.valueOf(j6Txt.getValue().toString()),
        HalfInteger.valueOf(j7Txt.getValue().toString()),
        HalfInteger.valueOf(j8Txt.getValue().toString()),
        HalfInteger.valueOf(j9Txt.getValue().toString())
      );
      lblAnalytic.setText("Analytic:  " + wigner9j.toString());
      lblDecimal.setText("Decimal:   " + Double.toString(wigner9j.doubleValue()));
    } catch (Exception err) {
      lblAnalytic.setText("Error: " + err.getMessage());
      lblDecimal.setText("");
    }
  }

// -----------------------------------------------------------------------------

  public static void main(String[] args) {
    JFrame frame = new JFrame("Wigner 9j Symbol Calculator");
    frame.setSize(new Dimension(256, 224));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.add(new Wigner9jSymbolPanel(), BorderLayout.CENTER);
    frame.setVisible(true);
  }

// -----------------------------------------------------------------------------

}

// =============================================================================

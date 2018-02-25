// =============================================================================
// Wigner6jSymbolPanel.java
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
import com.svengato.math.Wigner6jSymbol;
// =============================================================================

/** Wigner 6j symbol calculator panel */
public class Wigner6jSymbolPanel extends AbstractWignerNjSymbolPanel
{
  HalfIntegerSpinner j1Txt, j2Txt, j3Txt;
  HalfIntegerSpinner j4Txt, j5Txt, j6Txt;

  public Wigner6jSymbolPanel() {
    pnlCalculator.setLayout(new GridLayout(2, 3, 5, 5));

    j1Txt = new HalfIntegerSpinner("2", 0, 12);
    j2Txt = new HalfIntegerSpinner("2", 0, 12);
    j3Txt = new HalfIntegerSpinner("1", 0, 12);
    j4Txt = new HalfIntegerSpinner("2", 0, 12);
    j5Txt = new HalfIntegerSpinner("1", 0, 12);
    j6Txt = new HalfIntegerSpinner("2", 0, 12);
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

    doCalculate();
  }

  public void doCalculate() {
    try {
      Wigner6jSymbol wigner6j = new Wigner6jSymbol(
        HalfInteger.valueOf(j1Txt.getValue().toString()),
        HalfInteger.valueOf(j2Txt.getValue().toString()),
        HalfInteger.valueOf(j3Txt.getValue().toString()),
        HalfInteger.valueOf(j4Txt.getValue().toString()),
        HalfInteger.valueOf(j5Txt.getValue().toString()),
        HalfInteger.valueOf(j6Txt.getValue().toString())
      );
      lblAnalytic.setText("Analytic:  " + wigner6j.toString());
      lblDecimal.setText("Decimal:   " + Double.toString(wigner6j.doubleValue()));
    } catch (Exception err) {
      lblAnalytic.setText("Error: " + err.getMessage());
      lblDecimal.setText("");
    }
  }

// -----------------------------------------------------------------------------

  public static void main(String[] args) {
    JFrame frame = new JFrame("Wigner 6j Symbol Calculator");
    frame.setSize(new Dimension(256, 192));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.add(new Wigner6jSymbolPanel(), BorderLayout.CENTER);
    frame.setVisible(true);
  }

// -----------------------------------------------------------------------------

}

// =============================================================================

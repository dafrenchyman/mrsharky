// =============================================================================
// Wigner3jSymbolPanel.java
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
import com.svengato.math.Wigner3jSymbol;
// =============================================================================

/** Wigner 3j symbol calculator panel */
public class Wigner3jSymbolPanel extends AbstractWignerNjSymbolPanel
{
  HalfIntegerSpinner j1Txt, j2Txt, j3Txt;
  HalfIntegerSpinner m1Txt, m2Txt, m3Txt;

  public Wigner3jSymbolPanel() {
    pnlCalculator.setLayout(new GridLayout(2, 3, 5, 5));

    j1Txt = new HalfIntegerSpinner("3/2", 0, 12);
    j2Txt = new HalfIntegerSpinner("1", 0, 12);
    j3Txt = new HalfIntegerSpinner("1/2", 0, 12);
    m1Txt = new HalfIntegerSpinner("1/2", -12, 12);
    m2Txt = new HalfIntegerSpinner("-1", -12, 12);
    m3Txt = new HalfIntegerSpinner("1/2", -12, 12);
    j1Txt.setBackground(Color.white);
    pnlCalculator.add(j1Txt);
    j2Txt.setBackground(Color.white);
    pnlCalculator.add(j2Txt);
    j3Txt.setBackground(Color.white);
    pnlCalculator.add(j3Txt);
    m1Txt.setBackground(Color.white);
    pnlCalculator.add(m1Txt);
    m2Txt.setBackground(Color.white);
    pnlCalculator.add(m2Txt);
    m3Txt.setBackground(Color.white);
    pnlCalculator.add(m3Txt);

    doCalculate();
  }

  public void doCalculate() {
    try {
      Wigner3jSymbol wigner3j = new Wigner3jSymbol(
        HalfInteger.valueOf(j1Txt.getValue().toString()),
        HalfInteger.valueOf(j2Txt.getValue().toString()),
        HalfInteger.valueOf(j3Txt.getValue().toString()),
        HalfInteger.valueOf(m1Txt.getValue().toString()),
        HalfInteger.valueOf(m2Txt.getValue().toString()),
        HalfInteger.valueOf(m3Txt.getValue().toString())
      );
      lblAnalytic.setText("Analytic:  " + wigner3j.toString());
      lblDecimal.setText("Decimal:   " + Double.toString(wigner3j.doubleValue()));
    } catch (Exception err) {
      lblAnalytic.setText("Error: " + err.getMessage());
      lblDecimal.setText("");
    }
  }

// -----------------------------------------------------------------------------

  public static void main(String[] args) {
    JFrame frame = new JFrame("Wigner 3j Symbol Calculator");
    frame.setSize(new Dimension(256, 192));
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.add(new Wigner3jSymbolPanel(), BorderLayout.CENTER);
    frame.setVisible(true);
  }

// -----------------------------------------------------------------------------

}

// =============================================================================

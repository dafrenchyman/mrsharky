// =============================================================================
// AbstractWignerNjSymbolPanel.java
// =============================================================================
package com.svengato.wigner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import com.svengato.gui.SelectableLabel;
// =============================================================================

/** Abstract Wigner [N]j symbol panel, superclass of the 3j, 6j, 9j symbol panels */
abstract public class AbstractWignerNjSymbolPanel extends JPanel implements ActionListener
{
  JPanel pnlCalculator;
  JButton btnCalculate;
  SelectableLabel lblAnalytic, lblDecimal;
//  Font font;

  AbstractWignerNjSymbolPanel() {
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.gridwidth = gbc.gridheight = 1;

    gbc.weightx = gbc.weighty = 0.0;
    gbc.anchor = GridBagConstraints.NORTH;
    gbc.fill = GridBagConstraints.NONE;

    gbc.gridx = 0; gbc.gridy = 0;
    add(pnlCalculator = new JPanel(), gbc);

    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = 3;
    gbc.weightx = 1.0;
    gbc.gridx = 0; gbc.gridy = 1;
    add(lblAnalytic = new SelectableLabel(), gbc);
    gbc.gridx = 0; gbc.gridy = 2;
    add(lblDecimal = new SelectableLabel(), gbc);

    gbc.fill = GridBagConstraints.NONE;
    gbc.gridx = 0; gbc.gridy = 3;
    add(btnCalculate = new JButton("Calculate"), gbc);

    btnCalculate.addActionListener(this);

//    font = new Font("SansSerif", Font.PLAIN, 12);
//    setFont(this, font); // recursively sets all children
  }

  abstract public void doCalculate();

  // ActionListener methods
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == btnCalculate) {
      doCalculate();
    }
  }

/*  private void setFont(Container c, Font font) {
    Component[] cc = c.getComponents();
    for (int i = 0; i < cc.length; i++) {
      cc[i].setFont(font);
      if (cc[i] instanceof Container) setFont((Container)cc[i], font);
    }
  } */
}

// =============================================================================

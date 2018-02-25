// =============================================================================
// SelectableLabel.java
// =============================================================================
package com.svengato.gui;

import javax.swing.JTextPane;
// =============================================================================

/** Essentially a JLabel, but with selectable text that you can copy to the clipboard */
public class SelectableLabel extends JTextPane
{
  public SelectableLabel() {
    this("");
  }
  public SelectableLabel(String text) {
    setBackground(null);
    setBorder(null);
    setEditable(false);
    setText(text);
  }
}

// =============================================================================

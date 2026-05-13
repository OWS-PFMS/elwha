package com.owspfm.ui.components.card.playground;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.lang.reflect.Method;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Reference panel that lists every cursor available to {@link com.owspfm.ui.components.card.list
 * .FlatCardList} alongside a hover-zone cell that activates that cursor, so callers can compare
 * them side-by-side.
 *
 * <p>Includes the AWT predefined cursors (DEFAULT, HAND, MOVE, TEXT, WAIT, CROSSHAIR, the eight
 * resize variants) and the two custom cursors that the list ships internally (GRAB, GRABBING).
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class CursorReferencePanel extends JPanel {

  /** Builds the reference panel. */
  public CursorReferencePanel() {
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

    final JLabel heading = new JLabel("Cursor reference");
    heading.putClientProperty("FlatLaf.styleClass", "h3");

    final JLabel sub = new JLabel("Hover any row's preview to activate that cursor.");
    sub.putClientProperty("FlatLaf.styleClass", "small");

    final JPanel header = new JPanel(new BorderLayout());
    header.add(heading, BorderLayout.NORTH);
    header.add(sub, BorderLayout.CENTER);
    header.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
    add(header, BorderLayout.NORTH);

    final JPanel table = new JPanel(new GridBagLayout());
    table.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
    int row = 0;

    final JLabel awtHeader = new JLabel("AWT predefined cursors");
    awtHeader.putClientProperty("FlatLaf.styleClass", "h4");
    addSectionHeader(table, row++, awtHeader);
    addRow(
        table, row++, "DEFAULT", "Plain arrow", Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    addRow(
        table,
        row++,
        "HAND",
        "Pointing finger — clickable button or link",
        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addRow(
        table,
        row++,
        "MOVE",
        "Four-direction arrow — content can be moved",
        Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    addRow(
        table,
        row++,
        "TEXT",
        "I-beam — text editing",
        Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
    addRow(
        table,
        row++,
        "WAIT",
        "Hourglass / spinner — busy",
        Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    addRow(
        table,
        row++,
        "CROSSHAIR",
        "Plus — precise selection",
        Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    addRow(
        table,
        row++,
        "N_RESIZE",
        "Vertical resize (top edge)",
        Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "S_RESIZE",
        "Vertical resize (bottom edge)",
        Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "E_RESIZE",
        "Horizontal resize (right edge)",
        Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "W_RESIZE",
        "Horizontal resize (left edge)",
        Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "NE_RESIZE",
        "Diagonal resize",
        Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "NW_RESIZE",
        "Diagonal resize",
        Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "SE_RESIZE",
        "Diagonal resize",
        Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
    addRow(
        table,
        row++,
        "SW_RESIZE",
        "Diagonal resize",
        Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));

    final JLabel customHeader = new JLabel("Custom cursors (rendered by FlatCardList)");
    customHeader.putClientProperty("FlatLaf.styleClass", "h4");
    addSectionHeader(table, row++, customHeader);
    addRowWithIcon(
        table,
        row++,
        "GRAB",
        "Open hand — surface is draggable (Capitaine, LGPL-3.0)",
        loadCustomCursor("grab"),
        loadCustomImage("grab"));
    addRowWithIcon(
        table,
        row++,
        "GRABBING",
        "Closed fist — currently dragging (Capitaine, LGPL-3.0)",
        loadCustomCursor("grabbing"),
        loadCustomImage("grabbing"));

    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  /**
   * Reflectively asks the package-private {@code Cursors} helper for one of its custom cursors —
   * keeps the playground's no-app-dependency property without making {@code Cursors} public.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  private static Cursor loadCustomCursor(final String theName) {
    try {
      final Class<?> clazz = Class.forName("com.owspfm.ui.components.card.list.Cursors");
      final Method method = clazz.getDeclaredMethod(theName);
      method.setAccessible(true);
      return (Cursor) method.invoke(null);
    } catch (ReflectiveOperationException e) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
  }

  private static Image loadCustomImage(final String theBaseName) {
    try {
      final Class<?> clazz = Class.forName("com.owspfm.ui.components.card.list.Cursors");
      final Method method = clazz.getDeclaredMethod("previewImage", String.class, boolean.class);
      method.setAccessible(true);
      final boolean dark = isDarkTheme();
      return (Image) method.invoke(null, theBaseName, dark);
    } catch (ReflectiveOperationException e) {
      return null;
    }
  }

  private static boolean isDarkTheme() {
    final Color panel = UIManager.getColor("Panel.background");
    if (panel == null) {
      return false;
    }
    return (panel.getRed() + panel.getGreen() + panel.getBlue()) / 3 < 128;
  }

  private static void addSectionHeader(
      final JPanel theTable, final int theRow, final JLabel theLabel) {
    theLabel.setBorder(BorderFactory.createEmptyBorder(theRow == 0 ? 0 : 16, 0, 6, 0));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = theRow;
    gc.gridwidth = 3;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(0, 0, 0, 0);
    theTable.add(theLabel, gc);
  }

  private static void addRowWithIcon(
      final JPanel theTable,
      final int theRow,
      final String theName,
      final String theDescription,
      final Cursor theCursor,
      final Image theIcon) {
    addRow(theTable, theRow, theName, theDescription, theCursor);
    if (theIcon == null) {
      return;
    }
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 3;
    gc.gridy = theRow;
    gc.insets = new Insets(4, 8, 4, 0);
    final JLabel iconLbl = new JLabel(new ImageIcon(theIcon));
    iconLbl.setToolTipText("Cursor artwork preview");
    theTable.add(iconLbl, gc);
  }

  private static void addRow(
      final JPanel theTable,
      final int theRow,
      final String theName,
      final String theDescription,
      final Cursor theCursor) {
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridy = theRow;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(4, 0, 4, 12);

    final JLabel nameLbl = new JLabel(theName);
    nameLbl.putClientProperty("FlatLaf.styleClass", "monospaced");
    final JLabel descLbl = new JLabel(theDescription);
    descLbl.setForeground(UIManager.getColor("Label.disabledForeground"));

    final JPanel hover = new JPanel(new BorderLayout());
    final Color base = UIManager.getColor("Component.borderColor");
    hover.setBackground(blend(UIManager.getColor("Panel.background"), base, 0.15f));
    hover.setBorder(BorderFactory.createLineBorder(base != null ? base : Color.GRAY));
    hover.setCursor(theCursor);
    hover.setPreferredSize(new Dimension(180, 32));
    final JLabel hint = new JLabel("hover here", SwingConstants.CENTER);
    hint.putClientProperty("FlatLaf.styleClass", "small");
    hover.add(hint, BorderLayout.CENTER);

    gc.gridx = 0;
    gc.weightx = 0;
    theTable.add(nameLbl, gc);
    gc.gridx = 1;
    gc.weightx = 1;
    theTable.add(descLbl, gc);
    gc.gridx = 2;
    gc.weightx = 0;
    gc.fill = GridBagConstraints.NONE;
    theTable.add(hover, gc);
  }

  private static Color blend(final Color theA, final Color theB, final float theT) {
    if (theA == null) {
      return theB == null ? Color.LIGHT_GRAY : theB;
    }
    if (theB == null) {
      return theA;
    }
    final float t = Math.max(0f, Math.min(1f, theT));
    final int r = (int) (theA.getRed() * (1 - t) + theB.getRed() * t);
    final int g = (int) (theA.getGreen() * (1 - t) + theB.getGreen() * t);
    final int b = (int) (theA.getBlue() * (1 - t) + theB.getBlue() * t);
    return new Color(r, g, b);
  }
}

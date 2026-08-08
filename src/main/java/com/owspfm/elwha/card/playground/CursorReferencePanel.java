package com.owspfm.elwha.card.playground;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 * Reference panel listing every cursor a card-rendering {@link com.owspfm.elwha.list.ElwhaItemList}
 * can hand out: AWT predefined cursors plus the two custom grab / grabbing cursors.
 *
 * <p>The live {@link java.awt.Cursor} instances are pulled reflectively from the package-private
 * {@code com.owspfm.elwha.list.ReorderCursors} loader, so a row hands out exactly the cursor the
 * list would. The preview <em>rasters</em> beside them are read straight off the bundled assets — a
 * reference panel showing what shipped should read what shipped, and it keeps the library from
 * carrying a preview accessor no library code calls (#612).
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.2.0
 */
public final class CursorReferencePanel extends JPanel {

  /** Where the reorder cursor rasters live, relative to this class. */
  private static final String CURSOR_ASSETS = "/com/owspfm/elwha/list/cursors/";

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

    final JLabel customHeader = new JLabel("Custom cursors (rendered by ElwhaItemList)");
    customHeader.putClientProperty("FlatLaf.styleClass", "h4");
    addSectionHeader(table, row++, customHeader);
    addRowWithIcon(
        table,
        row++,
        "GRAB",
        "Open hand — surface is draggable",
        loadCustomCursor("grab"),
        loadCustomImage("grab"));
    addRowWithIcon(
        table,
        row++,
        "GRABBING",
        "Closed fist — currently dragging",
        loadCustomCursor("grabbing"),
        loadCustomImage("grabbing"));

    add(new JScrollPane(table), BorderLayout.CENTER);
  }

  private static Cursor loadCustomCursor(final String name) {
    try {
      final Class<?> clazz = Class.forName("com.owspfm.elwha.list.ReorderCursors");
      final Method method = clazz.getDeclaredMethod(name);
      method.setAccessible(true);
      return (Cursor) method.invoke(null);
    } catch (ReflectiveOperationException e) {
      return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
    }
  }

  private static Image loadCustomImage(final String baseName) {
    final String variant = isDarkTheme() ? "dark" : "light";
    final URL url =
        CursorReferencePanel.class.getResource(
            CURSOR_ASSETS + baseName + "-" + variant + "-32.png");
    if (url == null) {
      return null;
    }
    try (InputStream in = url.openStream()) {
      return ImageIO.read(in);
    } catch (final IOException missing) {
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

  private static void addSectionHeader(final JPanel table, final int row, final JLabel label) {
    label.setBorder(BorderFactory.createEmptyBorder(row == 0 ? 0 : 16, 0, 6, 0));
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 0;
    gc.gridy = row;
    gc.gridwidth = 3;
    gc.fill = GridBagConstraints.HORIZONTAL;
    table.add(label, gc);
  }

  private static void addRowWithIcon(
      final JPanel table,
      final int row,
      final String name,
      final String description,
      final Cursor cursor,
      final Image icon) {
    addRow(table, row, name, description, cursor);
    if (icon == null) {
      return;
    }
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridx = 3;
    gc.gridy = row;
    gc.insets = new Insets(4, 8, 4, 0);
    final JLabel iconLbl = new JLabel(new ImageIcon(icon));
    iconLbl.setToolTipText("Cursor artwork preview");
    table.add(iconLbl, gc);
  }

  private static void addRow(
      final JPanel table,
      final int row,
      final String name,
      final String description,
      final Cursor cursor) {
    final GridBagConstraints gc = new GridBagConstraints();
    gc.gridy = row;
    gc.fill = GridBagConstraints.HORIZONTAL;
    gc.insets = new Insets(4, 0, 4, 12);

    final JLabel nameLbl = new JLabel(name);
    nameLbl.putClientProperty("FlatLaf.styleClass", "monospaced");
    final JLabel descLbl = new JLabel(description);
    descLbl.setForeground(UIManager.getColor("Label.disabledForeground"));

    final JPanel hover = new JPanel(new BorderLayout());
    hover.setBackground(UIManager.getColor("Panel.background"));
    hover.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
    hover.setCursor(cursor);
    hover.setPreferredSize(new Dimension(180, 32));
    final JLabel hint = new JLabel("hover here", SwingConstants.CENTER);
    hint.putClientProperty("FlatLaf.styleClass", "small");
    hover.add(hint, BorderLayout.CENTER);

    gc.gridx = 0;
    gc.weightx = 0;
    table.add(nameLbl, gc);
    gc.gridx = 1;
    gc.weightx = 1;
    table.add(descLbl, gc);
    gc.gridx = 2;
    gc.weightx = 0;
    gc.fill = GridBagConstraints.NONE;
    table.add(hover, gc);
  }
}

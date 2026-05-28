package com.owspfm.elwha.icons.playground;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.Config;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Story #225 smoketest — {@link MaterialIcons.Symbol} fill-axis. Renders every bundled symbol in a
 * grid: name, {@code unselected()}, {@code selected()}, and a fill-axis marker (✓ if {@link
 * MaterialIcons.Symbol#hasSelectedVariant()}, em-dash otherwise — meaning the symbol gracefully
 * falls back to the unfilled glyph because Material Symbols itself doesn't publish a distinct
 * fill-1 for that linework glyph).
 *
 * <p>Mode toggle (light / dark / system) verifies that the shared theme color filter still tints
 * the fill-1 glyphs the same way it tints the unfilled ones — i.e., neither axis paints in a stale
 * color after a runtime LAF switch.
 *
 * <p>Run:
 *
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.owspfm.elwha.icons.playground.MaterialIconsSymbolPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.3.0
 * @since v0.3.0
 */
public final class MaterialIconsSymbolPlayground {

  /**
   * Every Material Symbol bundled under {@code resources/com/owspfm/icons/material/}, sorted. Some
   * have a fill-1 variant (resolved automatically); some don't and gracefully fall back.
   */
  private static final List<String> BUNDLED_SYMBOLS =
      List.of(
          "add",
          "anchor",
          "autorenew",
          "background_grid_small",
          "brightness_auto",
          "cached",
          "check",
          "colorize",
          "dark_mode",
          "delete",
          "deselect",
          "edit",
          "expand_less",
          "expand_more",
          "favorite",
          "grid_view",
          "help",
          "info",
          "keyboard_tab",
          "layers",
          "light_mode",
          "more_vert",
          "palette",
          "push_pin",
          "remove",
          "rotate_90_degrees_ccw",
          "rotate_90_degrees_cw",
          "rotate_left",
          "rotate_right",
          "select_all",
          "star",
          "start",
          "table",
          "visibility",
          "widgets");

  private static final int ICON_PX = 24;

  private final JFrame frame = new JFrame("MaterialIcons.Symbol — fill axis (#225)");

  private MaterialIconsSymbolPlayground() {}

  /**
   * Launches the playground.
   *
   * @param args unused
   * @version v0.3.0
   * @since v0.3.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());
    SwingUtilities.invokeLater(() -> new MaterialIconsSymbolPlayground().launch());
  }

  private void launch() {
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(buildModeBar(), BorderLayout.NORTH);
    frame.add(new JScrollPane(buildGrid()), BorderLayout.CENTER);
    frame.setSize(720, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private JPanel buildGrid() {
    final JPanel grid = new JPanel(new GridLayout(0, 4, 16, 8));
    grid.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    grid.add(header("Symbol"));
    grid.add(header("unselected()"));
    grid.add(header("selected()"));
    grid.add(header("fill axis"));

    for (final String name : BUNDLED_SYMBOLS) {
      final MaterialIcons.Symbol sym = MaterialIcons.symbol(name);
      grid.add(left(name));
      grid.add(iconCell(sym.unselected(ICON_PX)));
      grid.add(iconCell(sym.selected(ICON_PX)));
      grid.add(center(sym.hasSelectedVariant() ? "✓" : "—"));
    }
    return grid;
  }

  private static JLabel header(final String text) {
    final JLabel l = new JLabel(text, SwingConstants.CENTER);
    l.setFont(l.getFont().deriveFont(l.getFont().getSize2D() + 1f).deriveFont(java.awt.Font.BOLD));
    return l;
  }

  private static JLabel left(final String text) {
    return new JLabel(text, SwingConstants.LEFT);
  }

  private static JLabel center(final String text) {
    return new JLabel(text, SwingConstants.CENTER);
  }

  private static Component iconCell(final FlatSVGIcon icon) {
    final JLabel l = new JLabel(icon);
    l.setHorizontalAlignment(SwingConstants.CENTER);
    return l;
  }

  private JPanel buildModeBar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
    bar.add(new JLabel("Mode:"));
    final ButtonGroup group = new ButtonGroup();
    for (final Mode mode : Mode.values()) {
      final JToggleButton b = new JToggleButton(mode.name());
      b.setSelected(mode == Mode.SYSTEM);
      b.addActionListener(e -> applyMode(mode));
      group.add(b);
      bar.add(b);
    }
    return bar;
  }

  private void applyMode(final Mode mode) {
    final Config next = ElwhaTheme.current().withMode(mode);
    ElwhaTheme.install(next);
    SwingUtilities.updateComponentTreeUI(frame);
  }
}

package com.owspfm.elwha.appbar;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * Visual smoke for expanded-headline text wrapping (#478) — the M3 Expressive flexibility the V1
 * app bar deferred.
 *
 * <p>Three things to look at. The <strong>specimen column</strong> shows the same long headline at
 * one, two and three lines on both flexible variants: each extra line should add exactly one line
 * of that variant's own headline type role to the bar's height, with the icon strip and the
 * subtitle staying exactly where they were. Dragging the window <strong>narrower</strong> should
 * reflow a headline that fitted on one line onto two and grow the bar as it does; widening it again
 * should give the height back. The <strong>scroll pane</strong> at the bottom proves the collapse
 * range followed: a wrapped bar has further to travel, and should still land precisely on the
 * 64&nbsp;px strip rather than overshooting or stopping short.
 *
 * <p>The RTL toggle checks that both lines hang off the trailing margin together.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class ElwhaAppBarWrapDemo {

  private static final String HEADLINE = "Quarterly revenue and operating expenses";

  private ElwhaAppBarWrapDemo() {}

  /**
   * Launches the demo frame.
   *
   * @param args unused
   * @version v0.5.0
   * @since v0.5.0
   */
  public static void main(final String[] args) {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
    SwingUtilities.invokeLater(ElwhaAppBarWrapDemo::buildFrame);
  }

  private static void buildFrame() {
    final JFrame frame = new JFrame("ElwhaAppBar — expanded headline wrapping (#478)");
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    final List<ElwhaAppBar> specimens =
        List.of(
            bar(AppBarVariant.MEDIUM_FLEXIBLE, 1, false),
            bar(AppBarVariant.MEDIUM_FLEXIBLE, 2, false),
            bar(AppBarVariant.MEDIUM_FLEXIBLE, 2, true),
            bar(AppBarVariant.LARGE_FLEXIBLE, 1, false),
            bar(AppBarVariant.LARGE_FLEXIBLE, 2, false),
            bar(AppBarVariant.LARGE_FLEXIBLE, 3, false));
    final List<String> labels =
        List.of(
            "Medium flexible — 1 line (V1 behaviour: ellipsis)",
            "Medium flexible — 2 lines",
            "Medium flexible — 2 lines + subtitle",
            "Large flexible — 1 line (V1 behaviour: ellipsis)",
            "Large flexible — 2 lines (the M3 Expressive figure)",
            "Large flexible — 3 lines (past the spec, but honoured)");

    final JPanel column = new JPanel(new GridLayout(0, 1, 0, 14));
    column.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
    for (int i = 0; i < specimens.size(); i++) {
      column.add(titled(labels.get(i), specimens.get(i)));
    }

    final ElwhaAppBar scrolling = bar(AppBarVariant.LARGE_FLEXIBLE, 2, true);
    final JScrollPane page = page();
    scrolling.setScrollSource(page);
    final JPanel collapsing = new JPanel(new BorderLayout());
    collapsing.setBorder(BorderFactory.createEmptyBorder(0, 14, 14, 14));
    collapsing.add(
        titled("Scroll me — a wrapped bar has further to collapse", scrolling), BorderLayout.NORTH);
    collapsing.add(page, BorderLayout.CENTER);
    collapsing.setPreferredSize(new Dimension(0, 320));

    final ElwhaButton mode = ElwhaButton.outlinedButton("Toggle light / dark");
    mode.addActionListener(
        e -> {
          final Mode next =
              ElwhaTheme.current().mode().resolved() == Mode.DARK ? Mode.LIGHT : Mode.DARK;
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(next).build());
          SwingUtilities.updateComponentTreeUI(frame);
        });

    final ElwhaButton rtl = ElwhaButton.filledTonalButton("Toggle RTL");
    rtl.addActionListener(
        e -> {
          final ComponentOrientation next =
              frame.getComponentOrientation().isLeftToRight()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT;
          frame.applyComponentOrientation(next);
          frame.revalidate();
          frame.repaint();
        });

    final ElwhaButton shorten = ElwhaButton.textButton("Toggle short / long headline");
    shorten.addActionListener(
        e -> {
          for (final ElwhaAppBar bar : specimens) {
            bar.setTitle(HEADLINE.equals(bar.getTitle()) ? "Inbox" : HEADLINE);
          }
          frame.revalidate();
        });

    final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 12, 12));
    controls.add(mode);
    controls.add(rtl);
    controls.add(shorten);

    frame.setLayout(new BorderLayout());
    frame.add(column, BorderLayout.NORTH);
    frame.add(collapsing, BorderLayout.CENTER);
    frame.add(controls, BorderLayout.SOUTH);
    frame.setSize(720, 980);
    frame.setLocationByPlatform(true);
    frame.setVisible(true);
  }

  private static ElwhaAppBar bar(
      final AppBarVariant variant, final int maxLines, final boolean subtitle) {
    final ElwhaAppBar bar = new ElwhaAppBar(variant);
    bar.setTitle(HEADLINE);
    bar.setTitleMaxLines(maxLines);
    if (subtitle) {
      bar.setSubtitle("Fiscal year to date");
    }
    bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    bar.addAction(MaterialIcons.moreVert(), "More options", null);
    return bar;
  }

  private static JScrollPane page() {
    final JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    for (int i = 1; i <= 40; i++) {
      content.add(new JLabel("Page content row " + i));
      content.add(Box.createVerticalStrut(12));
    }
    final JScrollPane pane = new JScrollPane(content);
    pane.setBorder(BorderFactory.createEmptyBorder());
    pane.getVerticalScrollBar().setUnitIncrement(16);
    return pane;
  }

  private static JPanel titled(final String label, final ElwhaAppBar bar) {
    final JPanel panel = new JPanel(new BorderLayout(0, 4));
    panel.setOpaque(false);
    panel.add(new JLabel(label), BorderLayout.NORTH);
    panel.add(bar, BorderLayout.CENTER);
    return panel;
  }
}

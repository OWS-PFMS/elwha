package com.owspfm.elwha.card.playground;

import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Runnable, standalone playground for the V3 {@link com.owspfm.elwha.card.ElwhaCard}.
 *
 * <p>Layout:
 *
 * <ul>
 *   <li>Toolbar: light / dark / system mode switcher
 *   <li>Tab 1 ({@code ElwhaCard}): {@link GalleryPanel} (left) split with a vertical stack of
 *       {@link LiveConfigPanel} + {@link SnippetPanel} (right)
 *   <li>Tab 2 ({@code ElwhaCardList}): {@link ElwhaCardListShowcase}
 *   <li>Tab 3 ({@code Cursors}): {@link CursorReferencePanel}
 * </ul>
 *
 * <p>Launch:
 *
 * <pre>{@code
 * mvn compile exec:java -Dexec.mainClass="com.owspfm.elwha.card.playground.ElwhaCardPlayground"
 * }</pre>
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ElwhaCardPlayground {

  private ElwhaCardPlayground() {
    // entry point only
  }

  /**
   * Entry point.
   *
   * @param args command-line args (ignored)
   * @version v0.2.0
   * @since v0.2.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(ElwhaCardPlayground::launch);
  }

  private static void launch() {
    ElwhaTheme.install(
        ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());

    final JFrame frame = new JFrame("ElwhaCard V3 — playground");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final JTabbedPane tabs = new JTabbedPane();
    tabs.addTab("ElwhaCard", buildElwhaCardTab());
    tabs.addTab("ElwhaCardList", new ElwhaCardListShowcase());
    tabs.addTab("Cursors", new CursorReferencePanel());

    frame.add(buildToolbar(), BorderLayout.NORTH);
    frame.add(tabs, BorderLayout.CENTER);

    frame.setSize(1280, 860);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JPanel buildElwhaCardTab() {
    final GalleryPanel gallery = new GalleryPanel();
    final LiveConfigPanel live = new LiveConfigPanel();
    final SnippetPanel snippet = new SnippetPanel();
    snippet.update(live.snapshot());
    live.addConfigChangeListener(snippet::update);

    final JPanel right = new JPanel(new BorderLayout());
    right.add(live, BorderLayout.CENTER);
    right.add(snippet, BorderLayout.SOUTH);
    snippet.setPreferredSize(new Dimension(620, 240));

    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gallery, right);
    split.setResizeWeight(0.5);
    split.setDividerLocation(620);

    final JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(split, BorderLayout.CENTER);
    return wrap;
  }

  private static JToolBar buildToolbar() {
    final JToolBar bar = new JToolBar();
    bar.setFloatable(false);
    bar.setBorderPainted(false);
    bar.add(new JLabel("Mode: "));
    final JComboBox<Mode> modeBox = new JComboBox<>(Mode.values());
    modeBox.setSelectedItem(Mode.SYSTEM);
    modeBox.addActionListener(
        e -> {
          final Mode m = (Mode) modeBox.getSelectedItem();
          if (m != null) {
            ElwhaTheme.install(
                ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(m).build());
            for (final java.awt.Window w : java.awt.Window.getWindows()) {
              SwingUtilities.updateComponentTreeUI(w);
              w.repaint();
            }
          }
        });
    bar.add(modeBox);
    return bar;
  }
}

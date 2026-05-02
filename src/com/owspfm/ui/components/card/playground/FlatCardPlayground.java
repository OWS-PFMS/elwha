package com.owspfm.ui.components.card.playground;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

/**
 * Runnable, standalone playground for {@link com.owspfm.ui.components.card.FlatCard}.
 *
 * <p>Layout:
 *
 * <ul>
 *   <li>Toolbar: theme switcher
 *   <li>Left pane: scrollable {@link GalleryPanel} with static examples of every variant / mode /
 *       collapsible state / slot configuration
 *   <li>Right pane: {@link LiveConfigPanel} on top, {@link SnippetPanel} below — change a slider on
 *       the right and the focus card and the displayed snippet update in real time
 * </ul>
 *
 * <p>Launch it from the terminal:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.card.playground.FlatCardPlayground
 * </pre>
 *
 * @author Charles Bryan
 * @version v1.1.0-alpha.2
 * @since v1.1.0-alpha.2
 */
public final class FlatCardPlayground {

  private FlatCardPlayground() {
    // utility class — invoked via main()
  }

  /**
   * Entry point for the playground app.
   *
   * @param args command-line arguments (unused)
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(FlatCardPlayground::launch);
  }

  private static void launch() {
    FlatLightLaf.setup();

    JFrame frame = new JFrame("FlatCard playground");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    final GalleryPanel gallery = new GalleryPanel();
    final LiveConfigPanel live = new LiveConfigPanel();
    final SnippetPanel snippet = new SnippetPanel();
    snippet.update(live.snapshot());
    live.addConfigChangeListener(snippet::update);

    JPanel right = new JPanel(new BorderLayout());
    right.add(live, BorderLayout.CENTER);
    right.add(snippet, BorderLayout.SOUTH);
    snippet.setPreferredSize(new Dimension(600, 220));

    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gallery, right);
    split.setResizeWeight(0.5);
    split.setDividerLocation(560);
    frame.add(buildToolbar(frame), BorderLayout.NORTH);
    frame.add(split, BorderLayout.CENTER);

    frame.setSize(1280, 820);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static JToolBar buildToolbar(final JFrame frame) {
    JToolBar bar = new JToolBar();
    bar.setFloatable(false);
    bar.setBorderPainted(false);
    bar.add(new JLabel("Theme: "));

    Map<String, Runnable> themes = new LinkedHashMap<>();
    themes.put("FlatLight", FlatLightLaf::setup);
    themes.put("FlatDark", FlatDarkLaf::setup);
    themes.put("IntelliJ", FlatIntelliJLaf::setup);
    themes.put("Darcula", FlatDarculaLaf::setup);

    JComboBox<String> themeBox = new JComboBox<>(themes.keySet().toArray(new String[0]));
    themeBox.addActionListener(
        e -> {
          Runnable r = themes.get((String) themeBox.getSelectedItem());
          if (r != null) {
            r.run();
            try {
              UIManager.setLookAndFeel(UIManager.getLookAndFeel());
            } catch (UnsupportedLookAndFeelException ignored) {
              // setup() already installed it; nothing to do
            }
            for (java.awt.Window w : java.awt.Window.getWindows()) {
              SwingUtilities.updateComponentTreeUI(w);
              w.repaint();
            }
            frame.pack();
            frame.setSize(1280, 820);
          }
        });
    bar.add(themeBox);
    return bar;
  }
}

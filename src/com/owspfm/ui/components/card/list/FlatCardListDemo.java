package com.owspfm.ui.components.card.list;

import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.ui.components.card.CardVariant;
import com.owspfm.ui.components.card.FlatCard;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Minimal smoke-test entry point for {@link FlatCardList}.
 *
 * <p>Mirrors the role of {@code FlatCardDemo} in the parent package: a single-frame app with a
 * pre-populated model, default vertical layout, and single selection enabled. For an interactive
 * exploration, use the playground instead.
 *
 * <p>Run from the terminal:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.ui.components.card.list.FlatCardListDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class FlatCardListDemo {

  private FlatCardListDemo() {
    // utility class — invoked via main()
  }

  /**
   * Launches the demo.
   *
   * @param args unused
    * @version v0.1.0
    * @since v0.1.0
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(FlatCardListDemo::launch);
  }

  private static void launch() {
    FlatLightLaf.setup();

    final DefaultCardListModel<String> model = new DefaultCardListModel<>();
    for (int i = 1; i <= 8; i++) {
      model.add("Item " + i);
    }

    final CardAdapter<String> adapter =
        (item, index) ->
            new FlatCard()
                .setVariant(CardVariant.OUTLINED)
                .setHeader(item, "Index " + index)
                .setBody(new JLabel("Sample body for " + item));

    final FlatCardList<String> list =
        new FlatCardList<>(model, adapter)
            .setSelectionMode(CardSelectionMode.SINGLE)
            .setReorderable(true);

    final JFrame frame = new JFrame("FlatCardList demo");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(new JScrollPane(list), BorderLayout.CENTER);
    frame.setSize(640, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

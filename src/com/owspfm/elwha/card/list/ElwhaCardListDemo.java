package com.owspfm.elwha.card.list;

import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.ElwhaCard;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Minimal smoke-test entry point for {@link ElwhaCardList}.
 *
 * <p>Mirrors the role of {@code ElwhaCardDemo} in the parent package: a single-frame app with a
 * pre-populated model, default vertical layout, and single selection enabled. For an interactive
 * exploration, use the playground instead.
 *
 * <p>Run from the terminal:
 *
 * <pre>
 *   mvn -q exec:java \
 *     -Dexec.mainClass=com.owspfm.elwha.card.list.ElwhaCardListDemo
 * </pre>
 *
 * @author Charles Bryan
 * @version v0.1.0
 * @since v0.1.0
 */
public final class ElwhaCardListDemo {

  private ElwhaCardListDemo() {
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
    SwingUtilities.invokeLater(ElwhaCardListDemo::launch);
  }

  private static void launch() {
    FlatLightLaf.setup();

    final DefaultCardListModel<String> model = new DefaultCardListModel<>();
    for (int i = 1; i <= 8; i++) {
      model.add("Item " + i);
    }

    final CardAdapter<String> adapter =
        (item, index) ->
            new ElwhaCard()
                .setVariant(CardVariant.OUTLINED)
                .setHeader(item, "Index " + index)
                .setBody(new JLabel("Sample body for " + item));

    final ElwhaCardList<String> list =
        new ElwhaCardList<>(model, adapter)
            .setSelectionMode(CardSelectionMode.SINGLE)
            .setReorderable(true);

    final JFrame frame = new JFrame("ElwhaCardList demo");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(new JScrollPane(list), BorderLayout.CENTER);
    frame.setSize(640, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

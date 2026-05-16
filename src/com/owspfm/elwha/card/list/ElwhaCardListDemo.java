package com.owspfm.elwha.card.list;

import com.formdev.flatlaf.FlatLightLaf;
import com.owspfm.elwha.card.CardVariant;
import com.owspfm.elwha.card.ElwhaCard;
import com.owspfm.elwha.list.DefaultElwhaListModel;
import com.owspfm.elwha.list.ElwhaItemList;
import com.owspfm.elwha.list.ElwhaListAdapter;
import com.owspfm.elwha.list.SelectionMode;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Minimal smoke-test entry point for the unified {@link ElwhaItemList} when its items are {@link
 * ElwhaCard}s. Drop-in replacement for the previous {@code ElwhaCardListDemo} which exercised
 * {@code ElwhaCardList<T>}.
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

    final DefaultElwhaListModel<String> model = new DefaultElwhaListModel<>();
    for (int i = 1; i <= 8; i++) {
      model.add("Item " + i);
    }

    final ElwhaListAdapter<String> adapter =
        (item, index) ->
            new ElwhaCard()
                .setVariant(CardVariant.OUTLINED)
                .setHeader(item, "Index " + index)
                .setBody(new JLabel("Sample body for " + item));

    final ElwhaItemList<String> list =
        new ElwhaItemList<>(model, adapter)
            .setSelectionMode(SelectionMode.SINGLE)
            .setReorderable(true);

    final JFrame frame = new JFrame("ElwhaItemList<Card> demo");
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(new JScrollPane(list), BorderLayout.CENTER);
    frame.setSize(640, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

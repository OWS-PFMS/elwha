package com.owspfm.elwha.selectfield.playground;

import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * S2 demo (#375): the typed option model + single-select write-back as real API. A {@code
 * ElwhaSelectField<City>} over a small record list with a custom {@code display} renderer; a live
 * readout label is driven entirely by {@link ElwhaSelectField#addSelectionChangeListener} (it
 * updates on both menu picks and the programmatic buttons). The dogfooded {@link ElwhaButton}
 * controls call {@link ElwhaSelectField#setSelectedValue} — "Select Cairo" sets a value, "Clear"
 * passes {@code null} — proving the programmatic round-trip and the listener firing.
 *
 * <p>Run: {@code mvn -q compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.selectfield.playground.SelectFieldS2ModelDemo"}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class SelectFieldS2ModelDemo {

  private SelectFieldS2ModelDemo() {}

  /** A small domain record, to show the non-String typed model + display renderer. */
  private record City(String name, String country) {}

  /**
   * Launches the demo frame.
   *
   * @param args ignored
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.SYSTEM).build());

          final JFrame frame = new JFrame("ElwhaSelectField — S2 model");
          frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

          final List<City> cities =
              List.of(
                  new City("Cairo", "Egypt"),
                  new City("Lima", "Peru"),
                  new City("Oslo", "Norway"),
                  new City("Tokyo", "Japan"));

          final ElwhaSelectField<City> select = ElwhaSelectField.filled("City");
          select.setDisplayFunction(c -> c.name() + " (" + c.country() + ")");
          select.setOptions(cities);

          final JLabel readout = new JLabel("Selected: —");
          readout.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
          select.addSelectionChangeListener(
              c ->
                  readout.setText(
                      "Selected: " + (c == null ? "—" : c.name() + " / " + c.country())));

          final ElwhaButton selectCairo = ElwhaButton.outlinedButton("Select Cairo");
          selectCairo.addActionListener(e -> select.setSelectedValue(cities.get(0)));
          final ElwhaButton clear = ElwhaButton.outlinedButton("Clear");
          clear.addActionListener(e -> select.setSelectedValue(null));

          final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
          buttons.add(selectCairo);
          buttons.add(clear);

          final JPanel selectCell = new JPanel(new BorderLayout());
          selectCell.add(select, BorderLayout.NORTH);

          final JPanel root = new JPanel(new BorderLayout(0, 16));
          root.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 28));
          root.add(selectCell, BorderLayout.NORTH);
          final JPanel south = new JPanel(new BorderLayout(0, 12));
          south.add(readout, BorderLayout.NORTH);
          south.add(buttons, BorderLayout.SOUTH);
          root.add(south, BorderLayout.CENTER);

          frame.setContentPane(root);
          frame.setPreferredSize(new Dimension(440, 240));
          frame.pack();
          frame.setLocationRelativeTo(null);
          frame.setVisible(true);
        });
  }
}

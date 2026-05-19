package com.owspfm.elwha.button.playground;

import com.owspfm.elwha.button.ButtonGroup;
import com.owspfm.elwha.button.ButtonInteractionMode;
import com.owspfm.elwha.button.ButtonVariant;
import com.owspfm.elwha.button.ElwhaButton;
import com.owspfm.elwha.theme.ColorRole;
import com.owspfm.elwha.theme.ElwhaTheme;
import com.owspfm.elwha.theme.MaterialPalettes;
import com.owspfm.elwha.theme.Mode;
import com.owspfm.elwha.theme.TypeRole;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Story 5 (#118) scratch demo: visual + interactive smoketest for {@link ButtonGroup} — mutex
 * selection across N {@link ElwhaButton}s in {@link ButtonInteractionMode#SELECTABLE} mode.
 *
 * <p>Sections:
 *
 * <ol>
 *   <li><strong>Non-mandatory filter row</strong> — 3 {@link ButtonVariant#OUTLINED} selectable
 *       buttons ("All", "Active", "Archived") in a group. Clicking any one deselects the others;
 *       clicking the selected one again clears the group (selection → none).
 *   <li><strong>Mandatory view-mode row</strong> — 3 {@link ButtonVariant#FILLED_TONAL} selectable
 *       buttons ("List", "Grid", "Compact") in a mandatory group. Clicking the selected button is a
 *       no-op; one is always selected.
 *   <li><strong>Mandatory toggle live</strong> — checkbox flips {@code mandatory} on / off on the
 *       view-mode group at runtime, demonstrating the setter is hot.
 *   <li><strong>Programmatic setSelected</strong> — three "Select X" push-buttons that drive {@link
 *       ButtonGroup#setSelected(ElwhaButton)} so the group state can be flipped without clicking
 *       the members directly.
 *   <li><strong>Throw verifications</strong> — startup-time checks for the two contract throws:
 *       adding a CLICKABLE button must {@link IllegalArgumentException} and {@code
 *       setSelected(null)} on a mandatory group must {@link IllegalStateException}.
 * </ol>
 *
 * <p>Both groups have a live status label that updates from a {@link
 * ButtonGroup#addSelectionChangeListener} callback — proves the {@code PROPERTY_SELECTED_BUTTON}
 * event fires with the expected old/new button references.
 *
 * <p>Run: {@code mvn compile exec:java
 * -Dexec.mainClass="com.owspfm.elwha.button.playground.ButtonGroupDemo"}.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public final class ButtonGroupDemo {

  private final JLabel filterStatus = new JLabel("selected: (none)");
  private final JLabel viewModeStatus = new JLabel("selected: List");
  private final JLabel guardStatus = new JLabel(" ");

  private ButtonGroupDemo() {}

  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
        () -> {
          ElwhaTheme.install(
              ElwhaTheme.config().theme(MaterialPalettes.baseline()).mode(Mode.LIGHT).build());
          new ButtonGroupDemo().show();
        });
  }

  private void show() {
    final JFrame frame = new JFrame("ElwhaButton — Story 5 ButtonGroup Demo");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());

    frame.add(buildToolbar(), BorderLayout.NORTH);
    frame.add(buildBody(), BorderLayout.CENTER);

    guardStatus.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    guardStatus.setText(verifyContractThrows());
    frame.add(guardStatus, BorderLayout.SOUTH);

    frame.setSize(1000, 720);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  // -------------------------------------------------------------- toolbar

  private JPanel buildToolbar() {
    final JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
    bar.setBackground(ColorRole.SURFACE_CONTAINER_LOW.resolve());

    final JComboBox<Mode> modeBox = new JComboBox<>(new Mode[] {Mode.LIGHT, Mode.DARK});
    modeBox.setSelectedItem(Mode.LIGHT);
    modeBox.addActionListener(
        e -> {
          ElwhaTheme.install(ElwhaTheme.current().withMode((Mode) modeBox.getSelectedItem()));
          SwingUtilities.invokeLater(() -> SwingUtilities.windowForComponent(bar).repaint());
        });

    bar.add(new JLabel("Mode:"));
    bar.add(modeBox);
    bar.add(Box.createHorizontalStrut(20));
    bar.add(new JLabel("Two groups: non-mandatory filter row + mandatory view-mode row"));
    return bar;
  }

  // ---------------------------------------------------------------- body

  private JPanel buildBody() {
    final JPanel body = new JPanel();
    body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
    body.setBackground(ColorRole.SURFACE.resolve());
    body.add(buildFilterSection());
    body.add(buildViewModeSection());
    return body;
  }

  private JPanel buildFilterSection() {
    final JPanel section = section("Non-mandatory filter row — click the selected one to clear");
    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    row.setOpaque(false);

    final ElwhaButton all = newToggle("All", ButtonVariant.OUTLINED);
    final ElwhaButton active = newToggle("Active", ButtonVariant.OUTLINED);
    final ElwhaButton archived = newToggle("Archived", ButtonVariant.OUTLINED);

    final ButtonGroup filterGroup = new ButtonGroup().add(all).add(active).add(archived);
    filterGroup.addSelectionChangeListener(
        evt -> {
          final ElwhaButton nv = (ElwhaButton) evt.getNewValue();
          filterStatus.setText("selected: " + (nv == null ? "(none)" : nv.getText()));
        });

    row.add(all);
    row.add(active);
    row.add(archived);

    final JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    statusLine.setOpaque(false);
    filterStatus.setFont(TypeRole.LABEL_LARGE.resolve());
    filterStatus.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    statusLine.add(filterStatus);

    final ElwhaButton clearProg =
        new ElwhaButton("Clear (setSelected(null))").setVariant(ButtonVariant.TEXT);
    clearProg.addActionListener(e -> filterGroup.setSelected(null));
    statusLine.add(clearProg);

    section.add(row);
    section.add(statusLine);
    return section;
  }

  private JPanel buildViewModeSection() {
    final JPanel section =
        section("Mandatory view-mode row — clicking selected is a no-op; always one selected");

    final ElwhaButton list = newToggle("List", ButtonVariant.FILLED_TONAL);
    final ElwhaButton grid = newToggle("Grid", ButtonVariant.FILLED_TONAL);
    final ElwhaButton compact = newToggle("Compact", ButtonVariant.FILLED_TONAL);

    list.setSelected(true);

    final ButtonGroup viewModeGroup =
        new ButtonGroup().setMandatory(true).add(list).add(grid).add(compact);
    viewModeGroup.addSelectionChangeListener(
        evt -> {
          final ElwhaButton nv = (ElwhaButton) evt.getNewValue();
          viewModeStatus.setText("selected: " + (nv == null ? "(none)" : nv.getText()));
        });

    final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    row.setOpaque(false);
    row.add(list);
    row.add(grid);
    row.add(compact);

    final JPanel statusLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    statusLine.setOpaque(false);
    viewModeStatus.setFont(TypeRole.LABEL_LARGE.resolve());
    viewModeStatus.setForeground(ColorRole.ON_SURFACE_VARIANT.resolve());
    statusLine.add(viewModeStatus);

    final JCheckBox mandatoryToggle = new JCheckBox("Mandatory", true);
    mandatoryToggle.setOpaque(false);
    mandatoryToggle.addActionListener(
        e -> viewModeGroup.setMandatory(mandatoryToggle.isSelected()));
    statusLine.add(mandatoryToggle);

    final ElwhaButton progList =
        new ElwhaButton("setSelected(List)").setVariant(ButtonVariant.OUTLINED);
    progList.addActionListener(e -> viewModeGroup.setSelected(list));
    final ElwhaButton progGrid =
        new ElwhaButton("setSelected(Grid)").setVariant(ButtonVariant.OUTLINED);
    progGrid.addActionListener(e -> viewModeGroup.setSelected(grid));
    final ElwhaButton progCompact =
        new ElwhaButton("setSelected(Compact)").setVariant(ButtonVariant.OUTLINED);
    progCompact.addActionListener(e -> viewModeGroup.setSelected(compact));
    statusLine.add(progList);
    statusLine.add(progGrid);
    statusLine.add(progCompact);

    final ElwhaButton tryClear =
        new ElwhaButton("Try setSelected(null) — should throw when mandatory")
            .setVariant(ButtonVariant.TEXT);
    tryClear.addActionListener(
        e -> {
          try {
            viewModeGroup.setSelected(null);
            guardStatus.setText("UNEXPECTED: setSelected(null) did NOT throw on mandatory group");
          } catch (IllegalStateException expected) {
            guardStatus.setText(
                "OK — mandatory.setSelected(null) threw IllegalStateException: "
                    + expected.getMessage());
          }
        });

    section.add(row);
    section.add(statusLine);
    section.add(tryClear);
    return section;
  }

  // ----------------------------------------------------------- factories

  private static ElwhaButton newToggle(final String label, final ButtonVariant variant) {
    return new ElwhaButton(label)
        .setVariant(variant)
        .setInteractionMode(ButtonInteractionMode.SELECTABLE);
  }

  // ------------------------------------------------------------- ui glue

  private JPanel section(final String title) {
    final JPanel s = new JPanel();
    s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
    s.setOpaque(false);
    s.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    final JLabel l = new JLabel(title);
    l.setFont(TypeRole.TITLE_MEDIUM.resolve());
    l.setForeground(ColorRole.ON_SURFACE.resolve());
    l.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    s.add(l);
    return s;
  }

  // ------------------------------------------------------------ contract

  private static String verifyContractThrows() {
    boolean addClickableThrew = false;
    try {
      new ButtonGroup().add(new ElwhaButton("clickable"));
    } catch (IllegalArgumentException expected) {
      addClickableThrew = true;
    }
    boolean clearMandatoryThrew = false;
    try {
      new ButtonGroup().setMandatory(true).setSelected(null);
    } catch (IllegalStateException expected) {
      clearMandatoryThrew = true;
    }
    if (addClickableThrew && clearMandatoryThrew) {
      return "OK — add(CLICKABLE) throws IllegalArgumentException; mandatory.setSelected(null)"
          + " throws IllegalStateException.";
    }
    return String.format(
        Locale.ROOT,
        "FAILED — addClickableThrew=%s clearMandatoryThrew=%s",
        addClickableThrew,
        clearMandatoryThrew);
  }
}

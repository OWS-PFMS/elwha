package com.owspfm.elwha.showcase;

import com.owspfm.elwha.appbar.AppBarVariant;
import com.owspfm.elwha.appbar.ElwhaAppBar;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.iconbutton.ElwhaIconButton;
import com.owspfm.elwha.iconbutton.playground.IconButtonPlaygroundPanels;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.menu.ElwhaMenu;
import com.owspfm.elwha.menu.ElwhaMenuItem;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaAppBar} (story #462): a {@link
 * ComponentWorkbench} stage hosting a live bar in {@code BorderLayout.NORTH} over a scrollable
 * content stub wired as its scroll source — scroll the page and the bar lifts (and, for the
 * flexible variants, collapses) — with Variant / Subtitle / Centered / Long headline / Headline
 * lines / Nav icon / Action count / Overflow menu / Lift on scroll / Height allocated / Enabled /
 * RTL controls, and a state gallery stacking one bar per configuration (the lifted and collapsed
 * rows forced via the {@code setLifted} / {@code setCollapsedFraction} hooks).
 *
 * <p>The overflow control dogfoods {@link ElwhaMenu}: it appends a {@code more_vert} action whose
 * listener opens a menu anchored to the button — the class-Javadoc overflow recipe, live.
 *
 * <p><strong>Height allocated</strong> is the one control that is not a property of the bar. It
 * swaps the stage's own header slot between the documented {@code BorderLayout.NORTH} placement,
 * which honours the bar's preferred height, and a height-pinned wrapper that hands the bar less
 * than it asked for — the only honest way for a workbench to demonstrate the #525 degradation,
 * since the property being exercised belongs to the host rather than to the component.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
final class AppBarShowcasePanels {

  private static final String[] ACTION_NAMES = {"Favorite", "Edit", "Color palette", "Layers"};

  private static final String LONG_TITLE = "Quarterly revenue and operating expenses";

  private static final String ALLOCATION_PREFERRED = "Preferred (honoured)";

  private AppBarShowcasePanels() {}

  // The bar's slot in the stage. "Preferred" is the documented NORTH placement, which honours the
  // bar's preferred height; every other option pins a wrapper to a fixed height and hands the bar
  // all of it, which is how a host under-allocates the bar (#525) without ever asking what it
  // wanted. GridLayout does this to the specimen demos by construction.
  private static JComponent header(final ElwhaAppBar bar, final String allocation) {
    if (ALLOCATION_PREFERRED.equals(allocation)) {
      return bar;
    }
    final JPanel clamp = new JPanel(new BorderLayout());
    clamp.setOpaque(false);
    clamp.add(bar, BorderLayout.CENTER);
    clamp.setPreferredSize(new Dimension(0, parsePx(allocation)));
    return clamp;
  }

  private static int parsePx(final String allocation) {
    return Integer.parseInt(allocation.substring(0, allocation.indexOf(' ')));
  }

  private static Icon actionGlyph(final int i) {
    return switch (i % 4) {
      case 0 -> MaterialIcons.favorite();
      case 1 -> MaterialIcons.edit();
      case 2 -> MaterialIcons.palette();
      default -> MaterialIcons.layers();
    };
  }

  /** Builds the interactive Workbench (live bar + scrollable page + control rail + code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaSelectField<AppBarVariant> variantBox = ElwhaSelectField.outlined("Variant");
    variantBox.setOptions(List.of(AppBarVariant.values()));
    variantBox.setSelectedValue(AppBarVariant.SMALL);
    final ElwhaSelectField<Integer> countBox = ElwhaSelectField.outlined("Actions");
    countBox.setOptions(List.of(0, 1, 2, 3));
    countBox.setSelectedValue(2);
    final ElwhaCheckbox longTitleBox = new ElwhaCheckbox("Long headline");
    final ElwhaSelectField<Integer> maxLinesBox = ElwhaSelectField.outlined("Headline lines");
    maxLinesBox.setOptions(List.of(1, 2, 3));
    maxLinesBox.setSelectedValue(1);
    final ElwhaSelectField<String> allocationBox = ElwhaSelectField.outlined("Height allocated");
    allocationBox.setOptions(List.of(ALLOCATION_PREFERRED, "120 px", "96 px", "72 px", "64 px"));
    allocationBox.setSelectedValue(ALLOCATION_PREFERRED);
    final ElwhaCheckbox subtitleBox = new ElwhaCheckbox("Subtitle");
    final ElwhaCheckbox centeredBox = new ElwhaCheckbox("Title centered");
    final ElwhaCheckbox navBox = new ElwhaCheckbox("Navigation icon");
    navBox.setChecked(true);
    final ElwhaCheckbox overflowBox = new ElwhaCheckbox("Overflow menu action");
    final ElwhaCheckbox liftBox = new ElwhaCheckbox("Lift on scroll");
    liftBox.setChecked(true);
    final ElwhaCheckbox enabledBox = new ElwhaCheckbox("Enabled");
    enabledBox.setChecked(true);
    final ElwhaCheckbox rtlBox = new ElwhaCheckbox("Right-to-left");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("App bar");
    controls.addControl("", variantBox);
    controls.addControl("", subtitleBox);
    controls.addControl("", centeredBox);
    controls.addSection("Headline");
    controls.addControl("", longTitleBox);
    controls.addControl("", maxLinesBox);
    controls.addSection("Slots");
    controls.addControl("", navBox);
    controls.addControl("", countBox);
    controls.addControl("", overflowBox);
    controls.addSection("Behavior");
    controls.addControl("", liftBox);
    controls.addControl("", allocationBox);
    controls.addControl("", enabledBox);
    controls.addControl("", rtlBox);

    // --- Icon buttons facet (#318) ---
    // The bar's nav button and its actions are real ElwhaIconButton children with a public
    // accessor apiece, and the main column owns only whether they exist. Their own presentation is
    // otherwise unreachable once embedded, so it goes in a facet driving every button on the bar at
    // once — which is also how M3 reads a bar's icon buttons: one treatment, not per-slot styling.
    // The bar is rebuilt on every control change, so the editor re-applies to the fresh buttons at
    // the end of each apply rather than holding a reference to any of them.
    final ComponentWorkbench.Facet[] iconFacet = new ComponentWorkbench.Facet[1];
    final Runnable[] reapply = new Runnable[1];
    final IconButtonPlaygroundPanels.IconButtonEditor iconEditor =
        IconButtonPlaygroundPanels.buildIconButtonEditor(() -> reapply[0].run());

    final Runnable apply =
        () -> {
          final AppBarVariant variant =
              orDefault(variantBox.getSelectedValue(), AppBarVariant.SMALL);
          final int count = orDefault(countBox.getSelectedValue(), 2);

          final ElwhaAppBar bar = new ElwhaAppBar(variant);
          bar.setTitle(longTitleBox.isChecked() ? LONG_TITLE : "Inbox");
          bar.setTitleMaxLines(orDefault(maxLinesBox.getSelectedValue(), 1));
          if (subtitleBox.isChecked()) {
            bar.setSubtitle("Synced 5 minutes ago");
          }
          bar.setTitleCentered(centeredBox.isChecked());
          if (navBox.isChecked()) {
            bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
          }
          for (int i = 0; i < count; i++) {
            bar.addAction(actionGlyph(i), ACTION_NAMES[i % ACTION_NAMES.length], null);
          }
          if (overflowBox.isChecked()) {
            final ElwhaIconButton more =
                bar.addAction(MaterialIcons.moreVert(), "More options", null);
            final ElwhaMenu menu =
                ElwhaMenu.builder()
                    .addItem(ElwhaMenuItem.of("Settings"))
                    .addItem(ElwhaMenuItem.of("Help"))
                    .addItem(ElwhaMenuItem.of("Send feedback"))
                    .build();
            more.addActionListener(e -> menu.open(more));
          }

          final JPanel page = new JPanel();
          page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
          page.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
          for (int i = 1; i <= 40; i++) {
            page.add(new JLabel("Page content row " + i));
            page.add(Box.createVerticalStrut(12));
          }
          final JScrollPane scroller = new JScrollPane(page);
          scroller.setName("appBarShowcaseScroller");
          scroller.setBorder(BorderFactory.createEmptyBorder());
          scroller.getVerticalScrollBar().setUnitIncrement(16);

          iconEditor.applyTo(bar.getNavigationIcon());
          for (final ElwhaIconButton action : bar.getActions()) {
            iconEditor.applyTo(action);
          }
          if (iconFacet[0] != null) {
            iconFacet[0].setCode(
                "ElwhaIconButton action = bar.addAction(MaterialIcons.search(), \"Search\","
                    + " null);\n"
                    + iconEditor.code("action"));
          }

          bar.setScrollSource(scroller);
          bar.setLiftOnScroll(liftBox.isChecked());
          bar.setEnabled(enabledBox.isChecked());

          final String allocation =
              orDefault(allocationBox.getSelectedValue(), ALLOCATION_PREFERRED);
          final JPanel stage = new JPanel(new BorderLayout());
          stage.setOpaque(false);
          stage.add(header(bar, allocation), BorderLayout.NORTH);
          stage.add(scroller, BorderLayout.CENTER);
          stage.setPreferredSize(new Dimension(560, 330));
          stage.applyComponentOrientation(
              rtlBox.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          workbench.setStage(stage);
          workbench.setCode(
              renderCode(
                  variant,
                  subtitleBox.isChecked(),
                  centeredBox.isChecked(),
                  navBox.isChecked(),
                  count,
                  overflowBox.isChecked(),
                  liftBox.isChecked(),
                  enabledBox.isChecked(),
                  rtlBox.isChecked(),
                  longTitleBox.isChecked(),
                  orDefault(maxLinesBox.getSelectedValue(), 1),
                  allocation));
        };

    reapply[0] = apply;
    // Wrapped in a WorkbenchControls so the facet column insets the editor exactly like the main
    // column does — a raw editor handed to addFacet stretches full-width and reads left-shifted.
    final WorkbenchControls iconControls = new WorkbenchControls();
    iconControls.addControl("", iconEditor.panel());
    iconFacet[0] = workbench.addFacet("Icon buttons", iconControls);

    variantBox.addSelectionChangeListener(v -> apply.run());
    countBox.addSelectionChangeListener(v -> apply.run());
    maxLinesBox.addSelectionChangeListener(v -> apply.run());
    allocationBox.addSelectionChangeListener(v -> apply.run());
    longTitleBox.addActionListener(e -> apply.run());
    subtitleBox.addActionListener(e -> apply.run());
    centeredBox.addActionListener(e -> apply.run());
    navBox.addActionListener(e -> apply.run());
    overflowBox.addActionListener(e -> apply.run());
    liftBox.addActionListener(e -> apply.run());
    enabledBox.addActionListener(e -> apply.run());
    rtlBox.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  /** Builds the stacked per-configuration gallery (lift/collapse forced via the hooks). */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.setOpaque(false);
    stack.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    stack.add(
        galleryRow("Small — nav + title + actions", configured(AppBarVariant.SMALL, false, false)));
    stack.add(galleryRow("Small + subtitle", configured(AppBarVariant.SMALL, true, false)));
    stack.add(galleryRow("Small — centered", configured(AppBarVariant.SMALL, true, true)));
    stack.add(galleryRow("Small — lifted (content scrolled under)", lifted()));
    stack.add(
        galleryRow(
            "Medium flexible — expanded 112",
            configured(AppBarVariant.MEDIUM_FLEXIBLE, false, false)));
    stack.add(
        galleryRow(
            "Medium flexible + subtitle — 136",
            configured(AppBarVariant.MEDIUM_FLEXIBLE, true, false)));
    stack.add(
        galleryRow(
            "Large flexible — expanded 120",
            configured(AppBarVariant.LARGE_FLEXIBLE, false, false)));
    stack.add(
        galleryRow(
            "Large flexible + subtitle — 152",
            configured(AppBarVariant.LARGE_FLEXIBLE, true, false)));
    stack.add(galleryRow("Flexible collapsed to the strip (fraction 1)", collapsed()));
    stack.add(galleryRow("Medium flexible — headline wrapped to two lines (#478)", wrapped(false)));
    stack.add(galleryRow("Large flexible — headline wrapped, with subtitle (#478)", wrapped(true)));
    stack.add(
        allocationRow(
            "Large flexible under-allocated to 96 px — collapses, never overlaps (#525)", 96));
    stack.add(galleryRow("Disabled", disabled()));
    stack.add(Box.createVerticalGlue());
    return stack;
  }

  private static ElwhaAppBar configured(
      final AppBarVariant variant, final boolean subtitle, final boolean centered) {
    final ElwhaAppBar bar = new ElwhaAppBar(variant);
    bar.setTitle("Headline");
    if (subtitle) {
      bar.setSubtitle("Subtitle");
    }
    bar.setTitleCentered(centered);
    bar.setNavigationIcon(MaterialIcons.menu(), "Open navigation", null);
    bar.addAction(MaterialIcons.favorite(), "Favorite", null);
    bar.addAction(MaterialIcons.moreVert(), "More options", null);
    return bar;
  }

  private static ElwhaAppBar lifted() {
    final ElwhaAppBar bar = configured(AppBarVariant.SMALL, false, false);
    bar.setLifted(true);
    return bar;
  }

  private static ElwhaAppBar collapsed() {
    final ElwhaAppBar bar = configured(AppBarVariant.MEDIUM_FLEXIBLE, false, false);
    bar.setCollapsedFraction(1f);
    bar.setLifted(true);
    return bar;
  }

  private static ElwhaAppBar disabled() {
    final ElwhaAppBar bar = configured(AppBarVariant.SMALL, true, false);
    bar.setEnabled(false);
    return bar;
  }

  private static ElwhaAppBar wrapped(final boolean large) {
    final ElwhaAppBar bar =
        configured(
            large ? AppBarVariant.LARGE_FLEXIBLE : AppBarVariant.MEDIUM_FLEXIBLE, large, false);
    bar.setTitle(LONG_TITLE);
    bar.setTitleMaxLines(2);
    return bar;
  }

  private static JComponent galleryRow(final String title, final ElwhaAppBar bar) {
    return galleryRow(title, (JComponent) bar);
  }

  private static JComponent allocationRow(final String title, final int height) {
    final ElwhaAppBar bar = configured(AppBarVariant.LARGE_FLEXIBLE, true, false);
    return galleryRow(title, header(bar, height + " px"));
  }

  private static JComponent galleryRow(final String title, final JComponent content) {
    final JPanel row = new JPanel(new BorderLayout(0, 4));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
    final JLabel header = new JLabel(title);
    header.setFont(header.getFont().deriveFont(Font.BOLD));
    row.add(header, BorderLayout.NORTH);
    row.add(content, BorderLayout.CENTER);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 24));
    return row;
  }

  private static <T> T orDefault(final T value, final T fallback) {
    return value != null ? value : fallback;
  }

  private static String renderCode(
      final AppBarVariant variant,
      final boolean subtitle,
      final boolean centered,
      final boolean nav,
      final int count,
      final boolean overflow,
      final boolean lift,
      final boolean enabled,
      final boolean rtl,
      final boolean longTitle,
      final int maxLines,
      final String allocation) {
    final StringBuilder code = new StringBuilder(512);
    code.append("ElwhaAppBar bar = ElwhaAppBar.")
        .append(
            switch (variant) {
              case SMALL -> "small";
              case MEDIUM_FLEXIBLE -> "mediumFlexible";
              case LARGE_FLEXIBLE -> "largeFlexible";
            })
        .append("();\n");
    code.append("bar.setTitle(\"").append(longTitle ? LONG_TITLE : "Inbox").append("\");\n");
    if (maxLines > 1) {
      code.append("bar.setTitleMaxLines(").append(maxLines).append(");\n");
    }
    if (subtitle) {
      code.append("bar.setSubtitle(\"Synced 5 minutes ago\");\n");
    }
    if (centered) {
      code.append("bar.setTitleCentered(true);\n");
    }
    if (nav) {
      code.append("bar.setNavigationIcon(MaterialIcons.menu(), \"Open navigation\", e -> …);\n");
    }
    for (int i = 0; i < Math.min(count, 2); i++) {
      code.append("bar.addAction(MaterialIcons.")
          .append(i == 0 ? "favorite" : "edit")
          .append("(), \"")
          .append(ACTION_NAMES[i])
          .append("\", e -> …);\n");
    }
    if (count > 2) {
      code.append("// … ").append(count - 2).append(" more actions\n");
    }
    if (overflow) {
      code.append("// Overflow is consumer composition with ElwhaMenu:\n");
      code.append("ElwhaIconButton more =\n");
      code.append("    bar.addAction(MaterialIcons.moreVert(), \"More options\", null);\n");
      code.append("ElwhaMenu menu = ElwhaMenu.builder()\n");
      code.append("    .addItem(ElwhaMenuItem.of(\"Settings\")) /* … */ .build();\n");
      code.append("more.addActionListener(e -> menu.open(more));\n");
    }
    if (ALLOCATION_PREFERRED.equals(allocation)) {
      code.append("frame.add(bar, BorderLayout.NORTH);\n");
    } else {
      code.append("// A host that pins the bar's height under-allocates it — the bar renders at\n");
      code.append("// the collapse fraction that height implies rather than overlapping (#525).\n");
      code.append("JPanel clamp = new JPanel(new BorderLayout());\n");
      code.append("clamp.add(bar, BorderLayout.CENTER);\n");
      code.append("clamp.setPreferredSize(new Dimension(0, ")
          .append(parsePx(allocation))
          .append("));\n");
      code.append("frame.add(clamp, BorderLayout.NORTH);\n");
    }
    code.append("bar.setScrollSource(scroller);  // lift");
    if (variant.isFlexible()) {
      code.append(" + collapse");
    }
    code.append("\n");
    if (!lift) {
      code.append("bar.setLiftOnScroll(false);\n");
    }
    if (!enabled) {
      code.append("bar.setEnabled(false);\n");
    }
    if (rtl) {
      code.append("bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
    }
    return code.toString();
  }
}

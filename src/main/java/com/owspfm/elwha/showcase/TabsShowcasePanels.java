package com.owspfm.elwha.showcase;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.tabs.ElwhaTab;
import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.tabs.TabMode;
import com.owspfm.elwha.tabs.TabsVariant;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaTabs} (story #433): a {@link ComponentWorkbench}
 * stage hosting a live tab bar over a {@link CardLayout} content panel — the consumer recipe from
 * the class Javadoc, dogfooded — with Variant / Tab mode / Icon form / Tab count / Badge /
 * Auto-activate / Enabled / RTL controls (variant, icon form, and count rebuild the bar; the rest
 * apply live), and a state gallery stacking one bar per configuration with hover/pressed forced on
 * inner tabs via the gallery hooks.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
final class TabsShowcasePanels {

  private static final String[] GLYPHS = {
    "home",
    "favorite",
    "info",
    "palette",
    "layers",
    "grid_view",
    "edit",
    "delete",
    "help",
    "push_pin"
  };
  private static final String[] LABELS = {
    "Home", "Favorites", "About", "Palette", "Layers", "Grid", "Edit", "Trash", "Help", "Pinned"
  };

  private TabsShowcasePanels() {}

  /** Builds the interactive Workbench (live bar + CardLayout pages + control rail + code). */
  static JComponent buildWorkbench() {
    final ComponentWorkbench workbench = new ComponentWorkbench();

    final ElwhaSelectField<TabsVariant> variantBox = ElwhaSelectField.outlined("Variant");
    variantBox.setOptions(List.of(TabsVariant.values()));
    variantBox.setSelectedValue(TabsVariant.PRIMARY);
    final ElwhaSelectField<TabMode> modeBox = ElwhaSelectField.outlined("Tab mode");
    modeBox.setOptions(List.of(TabMode.values()));
    modeBox.setSelectedValue(TabMode.FIXED);
    final ElwhaSelectField<IconForm> iconBox = ElwhaSelectField.outlined("Icon form");
    iconBox.setOptions(List.of(IconForm.values()));
    iconBox.setSelectedValue(IconForm.NONE);
    final ElwhaSelectField<Integer> countBox = ElwhaSelectField.outlined("Tabs");
    countBox.setOptions(List.of(3, 4, 6, 10));
    countBox.setSelectedValue(4);
    final JCheckBox badgeBox = new JCheckBox("Badge on tab 2");
    final JCheckBox autoActivateBox = new JCheckBox("Auto-activate on focus");
    final JCheckBox enabledBox = new JCheckBox("Enabled", true);
    final JCheckBox rtlBox = new JCheckBox("Right-to-left");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Tabs");
    controls.addControl("", variantBox);
    controls.addControl("", modeBox);
    controls.addControl("", countBox);
    controls.addSection("Content");
    controls.addControl("", iconBox);
    controls.addControl("", badgeBox);
    controls.addSection("Behavior");
    controls.addControl("", autoActivateBox);
    controls.addControl("", enabledBox);
    controls.addControl("", rtlBox);

    // Variant / icon form / count are constructor-shaped, so apply rebuilds the bar + pages and
    // re-stages; live properties (mode, badge, auto-activate, enabled, RTL) are stamped onto the
    // fresh bar each pass. The previous active index carries across rebuilds where it fits.
    final int[] activeIndex = {0};
    final Runnable apply =
        () -> {
          final TabsVariant variant = orDefault(variantBox.getSelectedValue(), TabsVariant.PRIMARY);
          final TabMode mode = orDefault(modeBox.getSelectedValue(), TabMode.FIXED);
          final IconForm form = orDefault(iconBox.getSelectedValue(), IconForm.NONE);
          final int count = orDefault(countBox.getSelectedValue(), 4);

          final ElwhaTabs bar = new ElwhaTabs(variant);
          final JPanel pages = new JPanel(new CardLayout());
          pages.setName("tabsShowcasePages");
          for (int i = 0; i < count; i++) {
            bar.addTab(buildTab(form, i));
            pages.add(buildPage(i), String.valueOf(i));
          }
          bar.setTabMode(mode);
          bar.setAutoActivate(autoActivateBox.isSelected());
          if (badgeBox.isSelected() && count >= 2) {
            bar.getTabAt(1).setBadge(ElwhaBadge.large(3));
          }
          bar.setActiveTabIndex(Math.min(activeIndex[0], count - 1));
          activeIndex[0] = bar.getActiveTabIndex();
          bar.addChangeListener(
              e -> {
                activeIndex[0] = bar.getActiveTabIndex();
                ((CardLayout) pages.getLayout())
                    .show(pages, String.valueOf(bar.getActiveTabIndex()));
              });
          ((CardLayout) pages.getLayout()).show(pages, String.valueOf(bar.getActiveTabIndex()));
          bar.setEnabled(enabledBox.isSelected());

          final JPanel stage = new JPanel(new BorderLayout());
          stage.setOpaque(false);
          stage.add(bar, BorderLayout.NORTH);
          stage.add(pages, BorderLayout.CENTER);
          stage.setPreferredSize(new Dimension(520, 240));
          stage.applyComponentOrientation(
              rtlBox.isSelected()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          workbench.setStage(stage);
          workbench.setCode(
              renderCode(
                  variant,
                  mode,
                  form,
                  count,
                  badgeBox.isSelected(),
                  autoActivateBox.isSelected(),
                  enabledBox.isSelected(),
                  rtlBox.isSelected()));
        };

    variantBox.addSelectionChangeListener(v -> apply.run());
    modeBox.addSelectionChangeListener(v -> apply.run());
    iconBox.addSelectionChangeListener(v -> apply.run());
    countBox.addSelectionChangeListener(v -> apply.run());
    badgeBox.addActionListener(e -> apply.run());
    autoActivateBox.addActionListener(e -> apply.run());
    enabledBox.addActionListener(e -> apply.run());
    rtlBox.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  private static ElwhaTab buildTab(final IconForm form, final int i) {
    final String label = LABELS[i % LABELS.length];
    final MaterialIcons.Symbol glyph = MaterialIcons.symbol(GLYPHS[i % GLYPHS.length]);
    return switch (form) {
      case NONE -> ElwhaTab.of(label);
      case STACKED -> ElwhaTab.of(glyph, label);
      case INLINE -> {
        final ElwhaTab tab = ElwhaTab.of(glyph, label);
        tab.setInlineIcon(true);
        yield tab;
      }
      case ICON_ONLY -> ElwhaTab.iconOnly(glyph, label);
    };
  }

  private static JComponent buildPage(final int i) {
    final JLabel page =
        new JLabel(
            "Page " + (i + 1) + " — \"" + LABELS[i % LABELS.length] + "\"", SwingConstants.CENTER);
    page.setName("page-" + i);
    page.setFont(page.getFont().deriveFont(Font.PLAIN, 15f));
    return page;
  }

  /** Builds the stacked per-configuration gallery (hover/pressed forced via the hooks). */
  static JComponent buildGallery() {
    final JPanel stack = new JPanel();
    stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
    stack.setOpaque(false);
    stack.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

    stack.add(
        galleryRow(
            "Primary — active / hovered / pressed / rest",
            configuredBar(TabsVariant.PRIMARY, IconForm.NONE, false, false)));
    stack.add(
        galleryRow(
            "Primary stacked icons — 64px bar",
            configuredBar(TabsVariant.PRIMARY, IconForm.STACKED, false, false)));
    stack.add(
        galleryRow(
            "Primary inline icons",
            configuredBar(TabsVariant.PRIMARY, IconForm.INLINE, false, false)));
    stack.add(
        galleryRow("Secondary", configuredBar(TabsVariant.SECONDARY, IconForm.NONE, false, false)));
    stack.add(
        galleryRow(
            "Secondary with icons (always inline)",
            configuredBar(TabsVariant.SECONDARY, IconForm.STACKED, false, false)));
    stack.add(
        galleryRow(
            "Icon-only (accessible labels set)",
            configuredBar(TabsVariant.PRIMARY, IconForm.ICON_ONLY, false, false)));
    stack.add(galleryRow("Badged — count on the icon, dot trailing the label", badgedBar()));
    stack.add(
        galleryRow("Disabled", configuredBar(TabsVariant.PRIMARY, IconForm.NONE, true, false)));
    stack.add(
        galleryRow(
            "Right-to-left", configuredBar(TabsVariant.PRIMARY, IconForm.INLINE, false, true)));
    stack.add(galleryRow("Scrollable (wheel over the bar)", scrollableBar()));
    stack.add(Box.createVerticalGlue());
    return stack;
  }

  // One bar per configuration; tabs 1 and 2 carry the forced hover / pressed treatments.
  private static ElwhaTabs configuredBar(
      final TabsVariant variant, final IconForm form, final boolean disabled, final boolean rtl) {
    final ElwhaTabs bar = new ElwhaTabs(variant);
    final String[] stateLabels = {"Active", "Hovered", "Pressed", "Rest"};
    for (int i = 0; i < stateLabels.length; i++) {
      final ElwhaTab tab =
          switch (form) {
            case NONE -> ElwhaTab.of(stateLabels[i]);
            case STACKED -> ElwhaTab.of(MaterialIcons.symbol(GLYPHS[i]), stateLabels[i]);
            case INLINE -> {
              final ElwhaTab t = ElwhaTab.of(MaterialIcons.symbol(GLYPHS[i]), stateLabels[i]);
              t.setInlineIcon(true);
              yield t;
            }
            case ICON_ONLY -> ElwhaTab.iconOnly(MaterialIcons.symbol(GLYPHS[i]), stateLabels[i]);
          };
      bar.addTab(tab);
    }
    if (!disabled) {
      bar.getTabAt(1).setHovered(true);
      bar.getTabAt(2).setPressed(true);
    } else {
      bar.setEnabled(false);
    }
    if (rtl) {
      bar.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
    }
    return bar;
  }

  private static ElwhaTabs badgedBar() {
    final ElwhaTabs bar = ElwhaTabs.primary();
    final ElwhaTab inbox = ElwhaTab.of(MaterialIcons.symbol("home"), "Inbox");
    inbox.setBadge(ElwhaBadge.large(88));
    bar.addTab(inbox);
    final ElwhaTab updates = ElwhaTab.of("Updates");
    updates.setBadge(ElwhaBadge.small());
    bar.addTab(updates);
    bar.addTab("Archive");
    return bar;
  }

  private static ElwhaTabs scrollableBar() {
    final ElwhaTabs bar = ElwhaTabs.secondary();
    bar.setTabMode(TabMode.SCROLLABLE);
    for (int i = 1; i <= 12; i++) {
      bar.addTab("Topic " + i);
    }
    return bar;
  }

  private static JComponent galleryRow(final String title, final ElwhaTabs bar) {
    final JPanel row = new JPanel(new BorderLayout(0, 4));
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
    final JLabel header = new JLabel(title);
    header.setFont(header.getFont().deriveFont(Font.BOLD));
    row.add(header, BorderLayout.NORTH);
    row.add(bar, BorderLayout.CENTER);
    row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + 90));
    return row;
  }

  private static <T> T orDefault(final T value, final T fallback) {
    return value != null ? value : fallback;
  }

  /** The workbench's icon-form choice — the three icon placements plus none. */
  enum IconForm {
    NONE("No icons"),
    STACKED("Stacked (primary default)"),
    INLINE("Inline"),
    ICON_ONLY("Icon-only");

    private final String label;

    IconForm(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  private static String renderCode(
      final TabsVariant variant,
      final TabMode mode,
      final IconForm form,
      final int count,
      final boolean badge,
      final boolean autoActivate,
      final boolean enabled,
      final boolean rtl) {
    final StringBuilder code = new StringBuilder(512);
    code.append("ElwhaTabs tabs = ElwhaTabs.")
        .append(variant == TabsVariant.PRIMARY ? "primary" : "secondary")
        .append("();\n");
    for (int i = 0; i < Math.min(count, 3); i++) {
      code.append(
          switch (form) {
            case NONE -> "tabs.addTab(\"" + LABELS[i] + "\");\n";
            case STACKED ->
                "tabs.addTab(ElwhaTab.of(MaterialIcons.symbol(\""
                    + GLYPHS[i]
                    + "\"), \""
                    + LABELS[i]
                    + "\"));\n";
            case INLINE ->
                "ElwhaTab t"
                    + i
                    + " = ElwhaTab.of(MaterialIcons.symbol(\""
                    + GLYPHS[i]
                    + "\"), \""
                    + LABELS[i]
                    + "\");\nt"
                    + i
                    + ".setInlineIcon(true);\n"
                    + "tabs.addTab(t"
                    + i
                    + ");\n";
            case ICON_ONLY ->
                "tabs.addTab(ElwhaTab.iconOnly(MaterialIcons.symbol(\""
                    + GLYPHS[i]
                    + "\"), \""
                    + LABELS[i]
                    + "\"));\n";
          });
    }
    if (count > 3) {
      code.append("// … ").append(count - 3).append(" more tabs\n");
    }
    if (mode == TabMode.SCROLLABLE) {
      code.append("tabs.setTabMode(TabMode.SCROLLABLE);\n");
    }
    if (badge) {
      code.append("tabs.getTabAt(1).setBadge(ElwhaBadge.large(3));\n");
    }
    if (autoActivate) {
      code.append("tabs.setAutoActivate(true);\n");
    }
    if (!enabled) {
      code.append("tabs.setEnabled(false);\n");
    }
    if (rtl) {
      code.append("tabs.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
    }
    code.append("\n// Content switching is consumer composition — the CardLayout recipe:\n");
    code.append("JPanel pages = new JPanel(new CardLayout());\n");
    code.append("// pages.add(panel, \"0\"), \"1\", … per tab\n");
    code.append("tabs.addChangeListener(e -> ((CardLayout) pages.getLayout())\n");
    code.append("    .show(pages, String.valueOf(tabs.getActiveTabIndex())));");
    return code.toString();
  }
}

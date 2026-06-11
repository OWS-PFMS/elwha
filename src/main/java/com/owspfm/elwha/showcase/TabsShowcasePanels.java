package com.owspfm.elwha.showcase;

import com.owspfm.elwha.badge.ElwhaBadge;
import com.owspfm.elwha.badge.playground.BadgePlaygroundPanels;
import com.owspfm.elwha.checkbox.ElwhaCheckbox;
import com.owspfm.elwha.icons.MaterialIcons;
import com.owspfm.elwha.selectfield.ElwhaSelectField;
import com.owspfm.elwha.tabs.ElwhaTab;
import com.owspfm.elwha.tabs.ElwhaTabs;
import com.owspfm.elwha.tabs.TabMode;
import com.owspfm.elwha.tabs.TabsVariant;
import com.owspfm.elwha.theme.ColorRole;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The Elwha Showcase leaf surface for {@link ElwhaTabs} (story #433): a {@link ComponentWorkbench}
 * stage hosting a live tab bar over a {@link CardLayout} content panel — the consumer recipe from
 * the class Javadoc, dogfooded — with Variant / Tab mode / Icon form / Tab count / Auto-activate /
 * Enabled / RTL controls (variant, icon form, and count rebuild the bar; the rest apply live), and
 * a state gallery stacking one bar per configuration with hover/pressed forced on inner tabs via
 * the gallery hooks.
 *
 * <p><strong>Badge facet (the nav-rail pattern).</strong> Tabs are a composed badge host, so the
 * badge gets the reusable {@link BadgePlaygroundPanels#buildBadgeEditor} as a native Workbench
 * facet (the switcher reads Component | Badge | Surface) bound to the bar's second tab, with its
 * own code panel — not a parallel control in the Component column. The editor omits anchoring on
 * purpose: the tab owns it (icon corner with an icon, trailing edge label-only). The live badge
 * instance survives the bar rebuilds that variant / icon-form / count changes trigger.
 *
 * <p><strong>Variant-aware icon forms.</strong> The icon-form options follow the variant — "Stacked
 * icon" exists only for {@link TabsVariant#PRIMARY} (secondary tabs are always inline), and a
 * stacked selection maps to inline when the variant flips to secondary.
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
    iconBox.setOptions(iconFormsFor(TabsVariant.PRIMARY));
    iconBox.setSelectedValue(IconForm.NONE);
    final ElwhaSelectField<Integer> countBox = ElwhaSelectField.outlined("Tabs");
    countBox.setOptions(List.of(3, 4, 6, 10));
    countBox.setSelectedValue(4);
    final ElwhaCheckbox autoActivateBox = new ElwhaCheckbox("Auto-activate on focus");
    final ElwhaCheckbox enabledBox = new ElwhaCheckbox("Enabled");
    enabledBox.setChecked(true);
    final ElwhaCheckbox rtlBox = new ElwhaCheckbox("Right-to-left");

    final WorkbenchControls controls = workbench.controls();
    controls.addSection("Tabs");
    controls.addControl("", variantBox);
    controls.addControl("", modeBox);
    controls.addControl("", countBox);
    controls.addSection("Content");
    controls.addControl("", iconBox);
    controls.addSection("Behavior");
    controls.addControl("", autoActivateBox);
    controls.addControl("", enabledBox);
    controls.addControl("", rtlBox);

    // The badge facet's live state: one editor-owned badge instance, re-pinned onto the fresh
    // bar's second tab across every rebuild.
    final ElwhaTab[] badgedTab = new ElwhaTab[1];
    final ElwhaBadge[] badge = {ElwhaBadge.large(3)};

    // Variant / icon form / count are constructor-shaped, so apply rebuilds the bar + pages and
    // re-stages; live properties (mode, auto-activate, enabled, RTL) are stamped onto the fresh
    // bar each pass. The previous active index carries across rebuilds where it fits.
    final int[] activeIndex = {0};
    final Runnable apply =
        () -> {
          final TabsVariant variant = orDefault(variantBox.getSelectedValue(), TabsVariant.PRIMARY);
          final TabMode mode = orDefault(modeBox.getSelectedValue(), TabMode.FIXED);
          final IconForm form = orDefault(iconBox.getSelectedValue(), IconForm.NONE);
          final int count = orDefault(countBox.getSelectedValue(), 4);

          if (badgedTab[0] != null) {
            badgedTab[0].setBadge(null);
          }
          final ElwhaTabs bar = new ElwhaTabs(variant);
          final JPanel pages = new JPanel(new CardLayout());
          pages.setName("tabsShowcasePages");
          for (int i = 0; i < count; i++) {
            bar.addTab(buildTab(form, i));
            pages.add(buildPage(i), String.valueOf(i));
          }
          bar.setTabMode(mode);
          bar.setAutoActivate(autoActivateBox.isChecked());
          badgedTab[0] = bar.getTabAt(Math.min(1, count - 1));
          badgedTab[0].setBadge(badge[0]);
          bar.setActiveTabIndex(Math.min(activeIndex[0], count - 1));
          activeIndex[0] = bar.getActiveTabIndex();
          bar.addChangeListener(
              e -> {
                activeIndex[0] = bar.getActiveTabIndex();
                ((CardLayout) pages.getLayout())
                    .show(pages, String.valueOf(bar.getActiveTabIndex()));
              });
          ((CardLayout) pages.getLayout()).show(pages, String.valueOf(bar.getActiveTabIndex()));
          bar.setEnabled(enabledBox.isChecked());

          final JPanel stage = new JPanel(new BorderLayout());
          stage.setOpaque(false);
          stage.add(bar, BorderLayout.NORTH);
          stage.add(pages, BorderLayout.CENTER);
          stage.setPreferredSize(new Dimension(520, 240));
          stage.applyComponentOrientation(
              rtlBox.isChecked()
                  ? ComponentOrientation.RIGHT_TO_LEFT
                  : ComponentOrientation.LEFT_TO_RIGHT);
          workbench.setStage(stage);
          workbench.setCode(
              renderCode(
                  variant,
                  mode,
                  form,
                  count,
                  autoActivateBox.isChecked(),
                  enabledBox.isChecked(),
                  rtlBox.isChecked()));
        };

    // --- Badge facet (the nav-rail pattern: ElwhaShowcase #306 wiring) ---
    final BadgePlaygroundPanels.BadgeSlot badgeSlot =
        new BadgePlaygroundPanels.BadgeSlot() {
          @Override
          public ElwhaBadge get() {
            return badge[0];
          }

          @Override
          public void set(final ElwhaBadge next) {
            badge[0] = next;
            if (badgedTab[0] != null) {
              badgedTab[0].setBadge(next);
            }
          }
        };
    final ComponentWorkbench.Facet[] badgeFacet = new ComponentWorkbench.Facet[1];
    final JComponent badgeEditor =
        BadgePlaygroundPanels.buildBadgeEditor(
            badgeSlot,
            () -> {
              if (badgeFacet[0] != null) {
                badgeFacet[0].setCode(renderBadgeCode(badgeSlot));
              }
            });
    // Wrapped in a WorkbenchControls so the facet column centers the editor like the standalone
    // Badge Workbench does (a raw editor would stretch full-width and read left-shifted).
    final WorkbenchControls badgeControls = new WorkbenchControls();
    badgeControls.addControl("", badgeEditor);
    badgeFacet[0] = workbench.addFacet("Badge", badgeControls);
    badgeFacet[0].setCode(renderBadgeCode(badgeSlot));

    // The icon-form choices follow the variant: "Stacked icon" only exists for PRIMARY
    // (secondary tabs are always inline); a stacked pick degrades to inline on the flip.
    variantBox.addSelectionChangeListener(
        v -> {
          final TabsVariant variant = orDefault(variantBox.getSelectedValue(), TabsVariant.PRIMARY);
          final IconForm current = orDefault(iconBox.getSelectedValue(), IconForm.NONE);
          final List<IconForm> options = iconFormsFor(variant);
          iconBox.setOptions(options);
          iconBox.setSelectedValue(
              options.contains(current)
                  ? current
                  : (current == IconForm.STACKED ? IconForm.INLINE : IconForm.NONE));
          apply.run();
        });
    modeBox.addSelectionChangeListener(v -> apply.run());
    iconBox.addSelectionChangeListener(v -> apply.run());
    countBox.addSelectionChangeListener(v -> apply.run());
    autoActivateBox.addActionListener(e -> apply.run());
    enabledBox.addActionListener(e -> apply.run());
    rtlBox.addActionListener(e -> apply.run());
    apply.run();
    return workbench;
  }

  private static List<IconForm> iconFormsFor(final TabsVariant variant) {
    return variant == TabsVariant.PRIMARY
        ? List.of(IconForm.NONE, IconForm.STACKED, IconForm.INLINE, IconForm.ICON_ONLY)
        : List.of(IconForm.NONE, IconForm.INLINE, IconForm.ICON_ONLY);
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

  /** The workbench's icon-form choice — the icon placements available for the active variant. */
  enum IconForm {
    NONE("No icons"),
    STACKED("Stacked icon"),
    INLINE("Inline icon"),
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

  // Mirrors renderNavRailBadgeCode: the facet's code panel shows the badge construction + the
  // tab attach line; anchoring is the tab's own business.
  private static String renderBadgeCode(final BadgePlaygroundPanels.BadgeSlot slot) {
    final ElwhaBadge current = slot.get();
    if (current == null) {
      return "favorites.setBadge(null);";
    }
    final boolean small = current.getVariant() == ElwhaBadge.Variant.SMALL;
    final StringBuilder code = new StringBuilder(220);
    code.append("ElwhaBadge badge = ElwhaBadge.");
    code.append(small ? "small()" : "large(\"" + current.getContent() + "\")");
    if (current.getContainerColor() != ColorRole.ERROR) {
      code.append("\n    .withContainerColor(ColorRole.")
          .append(current.getContainerColor().name())
          .append(")");
    }
    if (!small && current.getLabelColor() != ColorRole.ON_ERROR) {
      code.append("\n    .withLabelColor(ColorRole.")
          .append(current.getLabelColor().name())
          .append(")");
    }
    code.append(
        ";\nfavorites.setBadge(badge);  // icon corner with an icon, trailing edge"
            + " label-only");
    return code.toString();
  }

  private static String renderCode(
      final TabsVariant variant,
      final TabMode mode,
      final IconForm form,
      final int count,
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
    if (autoActivate) {
      code.append("tabs.setAutoActivate(true);\n");
    }
    if (!enabled) {
      code.append("tabs.setEnabled(false);\n");
    }
    if (rtl) {
      code.append("tabs.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);\n");
    }
    code.append("// Badge: see the Badge facet — tabs anchor it themselves.\n");
    code.append("\n// Content switching is consumer composition — the CardLayout recipe:\n");
    code.append("JPanel pages = new JPanel(new CardLayout());\n");
    code.append("// pages.add(panel, \"0\"), \"1\", … per tab\n");
    code.append("tabs.addChangeListener(e -> ((CardLayout) pages.getLayout())\n");
    code.append("    .show(pages, String.valueOf(tabs.getActiveTabIndex())));");
    return code.toString();
  }
}

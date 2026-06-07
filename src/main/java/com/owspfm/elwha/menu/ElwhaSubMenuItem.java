package com.owspfm.elwha.menu;

import java.util.Objects;
import javax.swing.Icon;

/**
 * A menu item that hosts a nested {@link ElwhaMenu} — the M3 <strong>submenu</strong> trigger (epic
 * #322 V2). The one sanctioned sibling of {@link ElwhaMenuItem} (design §3, research §Q′): it
 * inherits the full slot anatomy, state painting, and accessibility of a menu item and adds a
 * nested menu that opens to the side without dismissing its parent.
 *
 * <p>Activating the item (click, Right-arrow, or — from S2 — a hover-intent dwell) opens its {@link
 * #getSubMenu() submenu} as a child overlay anchored beside this row; the parent menu stays open
 * and marks this item {@linkplain #isExpanded() expanded} while the submenu is up. The submenu and
 * its parent form an overlay <em>chain</em> (host design §2): a press outside every level closes
 * the whole chain, Escape/Left closes one level back to this opener, and selecting a leaf action
 * item closes the chain.
 *
 * <p><strong>Construction</strong> mirrors {@link ElwhaMenuItem} plus the nested menu — {@code
 * ElwhaSubMenuItem.of("Share", shareMenu)} or {@code ElwhaSubMenuItem.of(icon, "Share", shareMenu)}
 * — and the item is added to a parent menu through the usual {@link ElwhaMenu.Builder#addItem},
 * since an {@code ElwhaSubMenuItem} <em>is an</em> {@link ElwhaMenuItem}.
 *
 * <p>The trailing {@code ›} caret affordance, hover-intent timers, and side placement land in S2;
 * this S1 form wires the open/close chain mechanics and the expanded-state tracking the later
 * stories build on.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSubMenuItem extends ElwhaMenuItem {

  private ElwhaMenu subMenu;
  private boolean expanded;

  /**
   * Constructs a submenu item with a label and no leading icon.
   *
   * @param label the item label; required
   * @param subMenu the nested menu this item opens; required
   * @throws NullPointerException if {@code label} or {@code subMenu} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSubMenuItem(final String label, final ElwhaMenu subMenu) {
    this(null, label, subMenu);
  }

  /**
   * Constructs a submenu item with a leading icon and a label.
   *
   * @param leadingIcon the 20 dp leading icon, or {@code null} for none
   * @param label the item label; required
   * @param subMenu the nested menu this item opens; required
   * @throws NullPointerException if {@code label} or {@code subMenu} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaSubMenuItem(final Icon leadingIcon, final String label, final ElwhaMenu subMenu) {
    super(leadingIcon, label);
    this.subMenu = Objects.requireNonNull(subMenu, "subMenu");
  }

  /**
   * Creates a submenu item with a label.
   *
   * @param label the item label; required
   * @param subMenu the nested menu this item opens; required
   * @return a new submenu item
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaSubMenuItem of(final String label, final ElwhaMenu subMenu) {
    return new ElwhaSubMenuItem(label, subMenu);
  }

  /**
   * Creates a submenu item with a leading icon and a label.
   *
   * @param leadingIcon the 20 dp leading icon
   * @param label the item label; required
   * @param subMenu the nested menu this item opens; required
   * @return a new submenu item
   * @version v0.4.0
   * @since v0.4.0
   */
  public static ElwhaSubMenuItem of(
      final Icon leadingIcon, final String label, final ElwhaMenu subMenu) {
    return new ElwhaSubMenuItem(leadingIcon, label, subMenu);
  }

  /**
   * The nested menu this item opens.
   *
   * @return the submenu; never {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public ElwhaMenu getSubMenu() {
    return subMenu;
  }

  /**
   * Replaces the nested menu this item opens.
   *
   * @param subMenu the new submenu; required
   * @throws NullPointerException if {@code subMenu} is {@code null}
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSubMenu(final ElwhaMenu subMenu) {
    this.subMenu = Objects.requireNonNull(subMenu, "subMenu");
  }

  /**
   * Whether this item's submenu is currently open.
   *
   * @return {@code true} while the submenu is shown
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isExpanded() {
    return expanded;
  }

  // Pushed by the parent ElwhaMenu when this item's submenu opens / closes. Drives the active-state
  // shape-morph (S3) and the EXPANDED accessibility state (S4); S1 only tracks the flag.
  void setExpanded(final boolean expanded) {
    if (this.expanded == expanded) {
      return;
    }
    this.expanded = expanded;
    repaint();
  }
}

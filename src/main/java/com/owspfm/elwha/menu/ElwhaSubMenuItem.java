package com.owspfm.elwha.menu;

import com.owspfm.elwha.icons.MaterialIcons;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import javax.swing.Icon;
import javax.swing.Timer;

/**
 * A menu item that hosts a nested {@link ElwhaMenu} — the M3 <strong>submenu</strong> trigger (epic
 * #322 V2). The one sanctioned sibling of {@link ElwhaMenuItem} (design §3, research §Q′): it
 * inherits the full slot anatomy, state painting, and accessibility of a menu item and adds a
 * nested menu that opens to the side without dismissing its parent.
 *
 * <p>Activating the item — a <strong>hover-intent dwell</strong> (400 ms), a click, or Right-arrow
 * — opens its {@link #getSubMenu() submenu} as a child overlay anchored beside this row; the parent
 * menu stays open and marks this item {@linkplain #isExpanded() expanded} while the submenu is up.
 * Moving the pointer away closes it after a matching dwell unless the pointer crossed into the
 * submenu. The submenu and its parent form an overlay <em>chain</em> (host design §2): a press
 * outside every level closes the whole chain, Escape/Left closes one level back to this opener, and
 * selecting a leaf action item closes the chain.
 *
 * <p><strong>Anatomy.</strong> A trailing {@code ›} caret ({@link MaterialIcons#chevronRight()}) is
 * placed automatically as the submenu signifier and themes like every other glyph; the
 * trailing-icon slot is reserved for it, so {@link #setTrailingIcon(Icon)} is unsupported on a
 * submenu item.
 *
 * <p><strong>Construction</strong> mirrors {@link ElwhaMenuItem} plus the nested menu — {@code
 * ElwhaSubMenuItem.of("Share", shareMenu)} or {@code ElwhaSubMenuItem.of(icon, "Share", shareMenu)}
 * — and the item is added to a parent menu through the usual {@link ElwhaMenu.Builder#addItem},
 * since an {@code ElwhaSubMenuItem} <em>is an</em> {@link ElwhaMenuItem}.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ElwhaSubMenuItem extends ElwhaMenuItem {

  /** M3 {@code md-sub-menu} hover-open/close debounce (default 400 ms). */
  static final int HOVER_DELAY_MS = 400;

  private ElwhaMenu subMenu;
  private boolean expanded;
  private ElwhaMenu ownerMenu;
  private final Timer openTimer;
  private final Timer closeTimer;

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
    // The trailing slot is the submenu caret (the signifier); set via super to bypass the
    // disallowed
    // public setter.
    super.setTrailingIcon(MaterialIcons.chevronRight(ICON_SIZE_PX));

    this.openTimer = new Timer(HOVER_DELAY_MS, e -> requestOpen());
    this.openTimer.setRepeats(false);
    this.closeTimer = new Timer(HOVER_DELAY_MS, e -> requestCloseIfPointerAway());
    this.closeTimer.setRepeats(false);
    installHoverIntent();
  }

  // Pushed by the owning ElwhaMenu at build time so hover-intent can route open/close requests.
  void attachOwner(final ElwhaMenu owner) {
    this.ownerMenu = owner;
  }

  private void requestOpen() {
    if (ownerMenu != null) {
      ownerMenu.requestOpenSubMenu(this);
    }
  }

  // Hover-away dwell elapsed: only collapse if the pointer is neither over this opener nor anywhere
  // in the open submenu chain — so crossing the opener↔submenu boundary doesn't flicker it shut.
  private void requestCloseIfPointerAway() {
    if (ownerMenu == null) {
      return;
    }
    final Point screen =
        MouseInfo.getPointerInfo() != null ? MouseInfo.getPointerInfo().getLocation() : null;
    final boolean overOpener =
        isShowing()
            && screen != null
            && new Rectangle(getLocationOnScreen(), getSize()).contains(screen);
    if (!overOpener && !subMenu.isPointerInSubChain(screen)) {
      ownerMenu.requestCloseSubMenu(this);
    }
  }

  private void installHoverIntent() {
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseEntered(final MouseEvent e) {
            closeTimer.stop();
            if (isEnabled() && !expanded) {
              openTimer.restart();
            }
          }

          @Override
          public void mouseExited(final MouseEvent e) {
            openTimer.stop();
            if (expanded) {
              closeTimer.restart();
            }
          }
        });
  }

  @Override
  public void removeNotify() {
    openTimer.stop();
    closeTimer.stop();
    super.removeNotify();
  }

  /**
   * Unsupported on a submenu item — the trailing slot is reserved for the {@code ›} caret that
   * signifies the nested menu. Set a leading icon instead.
   *
   * @param icon ignored
   * @throws UnsupportedOperationException always
   * @version v0.4.0
   * @since v0.4.0
   */
  @Override
  public void setTrailingIcon(final Icon icon) {
    throw new UnsupportedOperationException(
        "ElwhaSubMenuItem reserves the trailing slot for the submenu caret");
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

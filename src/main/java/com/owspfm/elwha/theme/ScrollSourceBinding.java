package com.owspfm.elwha.theme;

import java.util.Objects;
import javax.swing.BoundedRangeModel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;

/**
 * Shared scroll-source plumbing for scroll-aware components: binds to a {@link JScrollPane}'s
 * vertical scrollbar {@link BoundedRangeModel} and delivers {@code (value, delta)} updates to one
 * listener, managing the attach/detach lifecycle across source swaps.
 *
 * <p>This is deliberately <em>only</em> the plumbing — what a component does with the updates is
 * its own response semantics: {@code ElwhaFabAnchor} reacts to scroll <em>direction</em>
 * (hide/shrink), {@code ElwhaAppBar} to the absolute <em>offset</em> (lift, collapse). Mirrors the
 * {@link RipplePainter}/{@link MorphAnimator} shared-machinery precedent (design {@code
 * elwha-appbar-design.md} §8, settling the #269 cross-component note from epic #287).
 *
 * <p>Attachment is explicit: callers invoke {@link #attach()} when their response is armed (and
 * typically from {@code addNotify()}) and {@link #detach()} when it is not (and from {@code
 * removeNotify()}). {@link #setSource(JScrollPane)} detaches from the old source; the caller
 * decides whether to re-attach.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public final class ScrollSourceBinding {

  /**
   * Receives scroll updates from the bound model.
   *
   * @author Charles Bryan
   * @version v0.4.0
   * @since v0.4.0
   */
  @FunctionalInterface
  public interface ScrollListener {

    /**
     * Called when the bound vertical scrollbar model's value changes.
     *
     * @param value the new scroll value
     * @param delta the change from the previous value; never zero
     * @version v0.4.0
     * @since v0.4.0
     */
    void scrolled(int value, int delta);
  }

  private final ScrollListener listener;
  private final ChangeListener changeListener = e -> onChange();

  private JScrollPane source;
  private BoundedRangeModel attachedModel;
  private int prevValue;

  /**
   * Creates a binding delivering updates to the given listener.
   *
   * @param listener the scroll listener; required
   * @version v0.4.0
   * @since v0.4.0
   */
  public ScrollSourceBinding(final ScrollListener listener) {
    this.listener = Objects.requireNonNull(listener, "listener");
  }

  /**
   * Sets the scroll source, detaching from any previous one. Does not re-attach — the caller
   * decides via {@link #attach()}.
   *
   * @param source the scroll source, or {@code null} to clear
   * @version v0.4.0
   * @since v0.4.0
   */
  public void setSource(final JScrollPane source) {
    if (source == this.source) {
      return;
    }
    detach();
    this.source = source;
  }

  /**
   * The current scroll source, or {@code null}.
   *
   * @return the source
   * @version v0.4.0
   * @since v0.4.0
   */
  public JScrollPane getSource() {
    return source;
  }

  /**
   * Attaches to the source's vertical scrollbar model. No-op without a source or when already
   * attached; the attach itself fires no update (the next model change does).
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void attach() {
    if (source == null || attachedModel != null) {
      return;
    }
    attachedModel = source.getVerticalScrollBar().getModel();
    prevValue = attachedModel.getValue();
    attachedModel.addChangeListener(changeListener);
  }

  /**
   * Detaches from the bound model, if attached.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  public void detach() {
    if (attachedModel != null) {
      attachedModel.removeChangeListener(changeListener);
      attachedModel = null;
    }
  }

  /**
   * Whether the binding is currently attached to a model.
   *
   * @return {@code true} while attached
   * @version v0.4.0
   * @since v0.4.0
   */
  public boolean isAttached() {
    return attachedModel != null;
  }

  /**
   * The source's current vertical scroll value — readable whether or not the binding is attached;
   * {@code 0} without a source.
   *
   * @return the scroll value
   * @version v0.4.0
   * @since v0.4.0
   */
  public int value() {
    if (attachedModel != null) {
      return attachedModel.getValue();
    }
    return source != null ? source.getVerticalScrollBar().getValue() : 0;
  }

  private void onChange() {
    if (attachedModel == null) {
      return;
    }
    final int value = attachedModel.getValue();
    final int delta = value - prevValue;
    if (delta == 0) {
      return;
    }
    prevValue = value;
    listener.scrolled(value, delta);
  }
}

package com.owspfm.elwha.appbar;

import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A real, laid-out {@link JScrollPane} over a tall view, driven through its viewport.
 *
 * <p>The obvious shortcut — {@code pane.getVerticalScrollBar().setValue(200)} on a bare scroll pane
 * — does not work. A scroll bar with no view behind it has a range of {@code [0, 0]}, so the write
 * is clamped straight back to zero and the binding never sees a scroll at all. Worse, it can appear
 * to work briefly and then be undone the next time the pane re-syncs itself with its viewport.
 *
 * <p>So this fixture builds the real thing: a view with a genuine preferred size, a viewport sized
 * smaller than it, and an explicit layout pass so the scroll pane's UI can compute the scrollbar's
 * range from the view/extent relationship. Scrolling then goes through {@link
 * javax.swing.JViewport#setViewPosition}, exactly as a user's wheel or drag would, and the
 * scrollbar model follows because the pane's own change handler syncs it.
 */
final class ScrollFixture {

  /** The scrollable content's height — far taller than the viewport, so there is real travel. */
  static final int VIEW_HEIGHT = 3000;

  /** The viewport's height. */
  static final int VIEWPORT_HEIGHT = 400;

  /** The viewport's width. */
  static final int VIEWPORT_WIDTH = 500;

  private final JScrollPane pane;

  private ScrollFixture() {
    final JPanel view = new JPanel();
    view.setPreferredSize(new Dimension(VIEWPORT_WIDTH, VIEW_HEIGHT));
    view.setSize(VIEWPORT_WIDTH, VIEW_HEIGHT);
    pane = new JScrollPane(view);
    pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    pane.setSize(VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    layOutFully();
  }

  /** Builds a laid-out scroll pane parked at the top of its content. */
  static ScrollFixture create() {
    return new ScrollFixture();
  }

  /** The pane to hand to {@code setScrollSource}. */
  JScrollPane pane() {
    return pane;
  }

  /**
   * Scrolls the viewport to {@code y} pixels down the content, the way a wheel or drag would, and
   * returns the scroll value the bound model actually reports.
   */
  int scrollTo(final int y) {
    pane.getViewport().setViewPosition(new Point(0, y));
    layOutFully();
    return pane.getVerticalScrollBar().getValue();
  }

  /** The scroll value the app bar's binding reads. */
  int value() {
    return pane.getVerticalScrollBar().getValue();
  }

  // Container.validate() is a no-op without a peer, so the tree is laid out by hand — the scroll
  // pane's UI needs the viewport's extent and view sizes to be real before it can compute a range.
  private void layOutFully() {
    pane.doLayout();
    pane.getViewport().doLayout();
    final JComponent view = (JComponent) pane.getViewport().getView();
    view.doLayout();
    pane.doLayout();
  }
}

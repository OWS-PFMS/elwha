package com.owspfm.elwha.navrail;

import com.owspfm.elwha.icons.MaterialIcons;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;

/**
 * Shared scaffolding for the navigation-rail tests.
 *
 * <p><strong>No font scaffolding needed.</strong> The rail and its destinations resolve their
 * {@code TypeRole} unconditionally, like {@code ElwhaAppBar}, {@code ElwhaTab} and {@code
 * ElwhaBadge} (#628), so an unparented rail measures and paints exactly as a mounted one does.
 * Before that ruling they read the inherited {@link java.awt.Component#getFont()} first, which is
 * {@code null} on an unparented component — so these tests had to be read as approximating a state
 * a running app is never in. They no longer do.
 */
final class RailFixture {

  private RailFixture() {}

  /** A destination with a bundled glyph and a label. */
  static ElwhaNavRailDestination destination(final String label) {
    return ElwhaNavRailDestination.of(MaterialIcons.symbol("home"), label);
  }

  /** {@code count} destinations, labelled in order. */
  static List<ElwhaNavRailDestination> destinations(final int count) {
    final List<ElwhaNavRailDestination> list = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      list.add(destination("Item " + i));
    }
    return list;
  }

  /** A rail populated with {@code count} destinations and laid out at its preferred size. */
  static ElwhaNavigationRail laidOut(final ElwhaNavigationRail rail, final int count) {
    rail.setPrimary(destinations(count));
    rail.setSize(rail.getPreferredSize());
    rail.doLayout();
    return rail;
  }

  /**
   * Re-lays out the rail and delivers the resize notifications an anchored badge listens for.
   *
   * <p>{@code Component.setBounds} <em>posts</em> {@code COMPONENT_RESIZED} to the event queue
   * rather than dispatching it, so inside an on-EDT test a layout pass never reaches the badge
   * anchor and the badge stays where the previous geometry put it. Dispatching explicitly puts the
   * event through the same {@code Component.dispatchEvent} pipeline the toolkit would; only the
   * delivery timing is made synchronous.
   */
  static void relayout(final ElwhaNavigationRail rail) {
    rail.setSize(rail.getPreferredSize());
    rail.doLayout();
    for (final ElwhaNavRailDestination destination : rail.getPrimary()) {
      destination.dispatchEvent(
          new java.awt.event.ComponentEvent(
              destination, java.awt.event.ComponentEvent.COMPONENT_RESIZED));
    }
  }

  /**
   * Mounts the rail in a hand-laid-out root pane so anchored badges have a {@link JLayeredPane} to
   * live on. {@code Container.validate} is a no-op without a peer, so bounds are assigned by hand.
   */
  static JRootPane mount(final JComponent rail, final int width, final int height) {
    final JRootPane root = new JRootPane();
    root.setSize(width + 200, height + 200);
    root.getLayeredPane().setBounds(0, 0, width + 200, height + 200);
    root.getContentPane().setLayout(null);
    root.getContentPane().setBounds(0, 0, width + 200, height + 200);
    final JPanel holder = new JPanel(null);
    holder.setBounds(20, 30, width + 160, height + 160);
    root.getContentPane().add(holder);
    rail.setBounds(0, 0, width, height);
    holder.add(rail);
    rail.doLayout();
    return root;
  }
}

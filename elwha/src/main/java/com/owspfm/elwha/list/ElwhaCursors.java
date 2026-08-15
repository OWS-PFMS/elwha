package com.owspfm.elwha.list;

import java.awt.Cursor;

/**
 * The Elwha drag cursors — an open-hand <em>grab</em> and a closed-fist <em>grabbing</em> — as
 * plain AWT {@link Cursor} objects any component can wear.
 *
 * <p><strong>Why it exists.</strong> AWT ships only {@link Cursor#HAND_CURSOR} (a pointing finger,
 * which desktops read as "this is a link") and {@link Cursor#MOVE_CURSOR} (a four-direction arrow).
 * Neither is the gesture vocabulary every modern desktop uses for direct manipulation: an open hand
 * meaning <em>this surface can be picked up</em>, and a closed fist meaning <em>a drag is in
 * flight</em>. {@link ElwhaItemList} paints those two states for its own drag-reorder affordance;
 * this class is the same pair, published so a consumer's hand-built draggable surfaces — a tab
 * strip, a chip row, a splitter, a canvas handle — read identically to the library's.
 *
 * <p><strong>Usage.</strong> Both cursors are ordinary {@code Cursor} values, so they go wherever
 * Swing takes one:
 *
 * <pre>{@code
 * // at rest, or on hover: this surface can be picked up
 * handle.setCursor(ElwhaCursors.grab());
 *
 * // from the press that starts a drag until the release that ends it
 * handle.setCursor(ElwhaCursors.grabbing());
 * }</pre>
 *
 * <p><strong>Ask again rather than caching.</strong> Call these methods at the point of use. The
 * cursors are built lazily and cached internally, so repeated calls are cheap, and the cache is
 * dropped whenever the returned instances would have stopped rendering — macOS drops the OS-side
 * association for custom cursors across a Spaces or Mission Control transition, and a {@code
 * Cursor} object held across one survives as an object while painting nothing. Re-applying a field
 * you stored at construction time reinstates the dead instance; calling {@code grab()} again gets a
 * live one.
 *
 * <p><strong>The body colour follows the platform pointer, never the theme.</strong> No desktop OS
 * flips its pointer when the theme changes — macOS ships a black arrow with a white outline in both
 * appearances, Windows a white arrow with a black outline — so the hand does the same: a dark body
 * with a light halo on macOS and Linux, a light body with a dark halo on Windows. The halo plays
 * the outline's role, keeping the hand legible on either ground. Nothing here reacts to {@link
 * com.owspfm.elwha.theme.ElwhaTheme}, and that is deliberate rather than an omission.
 *
 * <p><strong>Geometry.</strong> Both states share one hotspot on the knuckle line, so the artwork
 * does not shift when a press swaps grab for grabbing. Artwork is authored on a 32&nbsp;px grid and
 * scaled to whatever size the toolkit reports as best for the display.
 *
 * <p><strong>Degradation.</strong> Neither method returns {@code null} and neither throws. Where
 * the bundled artwork cannot be decoded, a painted hand silhouette stands in; where the toolkit
 * refuses custom cursors outright, or the JVM is headless, the result is {@link
 * Cursor#MOVE_CURSOR}. A consumer never has to branch on availability.
 *
 * @author Charles Bryan
 * @version v1.2.0
 * @since v1.2.0
 */
public final class ElwhaCursors {

  private ElwhaCursors() {}

  /**
   * Returns the open-hand <em>grab</em> cursor, which signals that a surface can be picked up.
   *
   * <p>Wear it on a draggable surface at rest — on hover, or permanently if the whole component is
   * a handle. Swap to {@link #grabbing()} for the duration of a drag.
   *
   * @return the grab cursor, or {@link Cursor#MOVE_CURSOR} where custom cursors are unavailable;
   *     never {@code null}
   * @version v1.2.0
   * @since v1.2.0
   */
  public static Cursor grab() {
    return ReorderCursors.grab();
  }

  /**
   * Returns the closed-fist <em>grabbing</em> cursor, which signals a drag in flight.
   *
   * <p>Wear it from the press that starts a drag until the release that ends it, then return to
   * {@link #grab()}. It shares its hotspot with the grab cursor, so the swap does not shift the
   * artwork under the pointer.
   *
   * @return the grabbing cursor, or {@link Cursor#MOVE_CURSOR} where custom cursors are
   *     unavailable; never {@code null}
   * @version v1.2.0
   * @since v1.2.0
   */
  public static Cursor grabbing() {
    return ReorderCursors.grabbing();
  }
}

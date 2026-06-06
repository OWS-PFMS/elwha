package com.owspfm.elwha.menu;

/**
 * The vertical-menu layout of an {@link ElwhaMenu} (M3 "Configurations", research §H). Mirrors M3's
 * exact nouns per the terminology lock (design §P).
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
public enum Layout {

  /**
   * Flat list — every item in one group, no separators.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  STANDARD,

  /**
   * Partitioned list — items bundled into groups separated by a {@link Separator}.
   *
   * @version v0.4.0
   * @since v0.4.0
   */
  GROUPED
}

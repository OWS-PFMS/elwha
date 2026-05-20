package com.owspfm.elwha.card;

/**
 * The two M3-sanctioned thumbnail shapes for {@link ElwhaCardThumbnail}. {@link #CIRCULAR} matches
 * the avatar / contact-card pattern; {@link #SQUARE} matches the photo / object-list pattern.
 *
 * @author Charles Bryan
 * @version v0.2.0
 * @since v0.2.0
 */
public enum ThumbnailShape {
  /** Round avatar — clipped to a circle. The default for portrait / avatar use. */
  CIRCULAR,

  /** Square photo — full bleed of the thumbnail container. */
  SQUARE
}

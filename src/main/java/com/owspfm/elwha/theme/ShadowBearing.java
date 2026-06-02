package com.owspfm.elwha.theme;

import java.awt.Insets;

/**
 * The uniform accessor for a shadowed primitive's M3 elevation-halo reserve. Every elevated Elwha
 * primitive ({@code ElwhaSurface} and its {@code ElwhaCard} subclass, {@code ElwhaButton}, {@code
 * ElwhaFab}) reserves space around its visible body so the key+ambient shadow stack never clips
 * against the component bounds; this interface exposes that reserve through a single getter so
 * placement helpers can ask "what is your halo?" without knowing the concrete primitive type.
 *
 * <p>The returned {@link Insets} are the per-edge halo a placement helper backs out of the
 * component bounds when it pins the <em>visible body</em> (not the padded component) to a spec
 * margin — {@link com.owspfm.elwha.fab.ElwhaFabAnchor} is the canonical consumer. Depending on
 * {@code ShadowBearing} rather than a concrete type is what lets a single anchor serve every
 * shadowed primitive.
 *
 * <p>This mirrors the {@code IconBearing} pattern (the placement contract {@code ElwhaBadgeAnchor}
 * consumes): one narrow accessor interface, implemented by every primitive that conforms, queried
 * by the placement helper. The <em>contract</em> is uniform; the <em>mechanism</em> for where the
 * reserve lives is allowed to differ by role — a container (`ElwhaSurface` family) carries it in
 * {@code getInsets()} and lays out children inside it, while a self-painting leaf ({@code
 * ElwhaButton}, {@code ElwhaFab}) bakes it into {@code getPreferredSize()} and translates the body
 * manually. See {@code docs/development/component-api-conventions.md} §8 for the full doctrine,
 * including the worst-case-elevation reserve-sizing rule and the {@code getMaximumSize} trap.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
public interface ShadowBearing {

  /**
   * Returns this primitive's reserved shadow-halo insets — the per-edge space it pads around its
   * visible body for the M3 elevation shadow. A placement helper subtracts these from the component
   * bounds to recover the visible body rect. Implementations size the reserve for the worst-case
   * elevation the primitive can actually paint (see conventions §8); a flat / non-elevated variant
   * returns zero-width insets {@code (0, 0, 0, 0)} (no halo).
   *
   * <p>Implementations must return a fresh {@link Insets} (or a defensive copy) each call — the
   * reserve is internal state and callers must not be able to mutate it.
   *
   * @return the reserved halo insets (never {@code null}); mutating the returned instance must not
   *     affect the primitive
   * @version v0.4.0
   * @since v0.4.0
   */
  Insets getShadowInsets();
}

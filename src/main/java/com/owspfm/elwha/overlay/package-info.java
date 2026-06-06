/**
 * Shared in-window overlay-host plumbing — the {@link
 * com.owspfm.elwha.overlay.AbstractElwhaOverlay} base that mounts a surface on the host frame's
 * {@link javax.swing.JLayeredPane}, drives the entrance/exit motion, manages focus (modal trap or
 * light-dismiss), and tears the overlay down. The Elwha dialog ({@code com.owspfm.elwha.dialog}),
 * the M3 menu ({@code com.owspfm.elwha.menu}), and the pending side-sheet (#308) all subclass it
 * rather than each duplicating the layered-pane glue.
 *
 * <p><strong>Library-internal.</strong> Types here are {@code public} only to cross the package
 * boundary into the component packages that consume them; they are not part of the supported
 * consumer API and may change without a deprecation cycle.
 *
 * @author Charles Bryan (cfb3@uw.edu)
 * @version v0.4.0
 * @since v0.4.0
 */
package com.owspfm.elwha.overlay;

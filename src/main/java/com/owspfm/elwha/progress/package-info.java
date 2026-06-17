/**
 * The Elwha Material 3 Expressive progress indicators — epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/467">#467</a>. {@link
 * com.owspfm.elwha.progress.ElwhaLinearProgressIndicator} and {@link
 * com.owspfm.elwha.progress.ElwhaCircularProgressIndicator} are dedicated {@link
 * javax.swing.JComponent}s over a shared {@link
 * com.owspfm.elwha.progress.AbstractElwhaProgressIndicator} base, backed by a {@link
 * javax.swing.BoundedRangeModel}, painting the updated-M3 anatomy (primary active indicator,
 * secondary-container track, 4px track-active gap, linear stop indicator) plus the Expressive wavy
 * shape — <em>not</em> {@code JProgressBar} subclasses or {@code ProgressBarUI} delegates (design
 * §2). Spec lives in {@code docs/research/elwha-progress-indicator-design.md} + {@code
 * elwha-progress-indicator-research.md}.
 *
 * @author Charles Bryan
 * @version v0.4.0
 * @since v0.4.0
 */
package com.owspfm.elwha.progress;

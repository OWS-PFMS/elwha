/**
 * The Elwha Material 3 Expressive progress indicators. {@link
 * com.owspfm.elwha.progress.ElwhaLinearProgressIndicator} and {@link
 * com.owspfm.elwha.progress.ElwhaCircularProgressIndicator} are dedicated {@link
 * javax.swing.JComponent}s over a shared {@link
 * com.owspfm.elwha.progress.AbstractElwhaProgressIndicator} base, backed by a {@link
 * javax.swing.BoundedRangeModel}, painting the updated-M3 anatomy (primary active indicator,
 * secondary-container track, 4px track-active gap, linear stop indicator) plus the Expressive wavy
 * shape — <em>not</em> {@code JProgressBar} subclasses or {@code ProgressBarUI} delegates (design
 * §2).
 *
 * <p>Design: {@code docs/research/elwha-progress-indicator-design.md}; research: {@code
 * elwha-progress-indicator-research.md}. Epic <a
 * href="https://github.com/OWS-PFMS/elwha/issues/467">#467</a>.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.4.0
 */
package com.owspfm.elwha.progress;

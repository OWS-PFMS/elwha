package com.owspfm.elwha.theme;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * The light/dark mode a {@link Config} installs under.
 *
 * <p>{@link #LIGHT} and {@link #DARK} are concrete; {@link #SYSTEM} follows the operating system's
 * appearance and is resolved to a concrete mode at install time via {@link #resolved()}. Per {@code
 * elwha-theme-install-api.md} §6, v1 detects the OS appearance only at install time — a live
 * OS-change listener is a deliberately-deferred fast follow.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.1.0
 */
public enum Mode {

  /**
   * The light palette of the installed {@link Theme}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  LIGHT,
  /**
   * The dark palette of the installed {@link Theme}.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  DARK,
  /**
   * Follow the operating system's appearance — resolved to {@link #LIGHT} or {@link #DARK} at
   * install time.
   *
   * @version v0.1.0
   * @since v0.1.0
   */
  SYSTEM;

  /** How long an OS-appearance probe may run before it is killed and read as "unknown". */
  private static final long PROBE_TIMEOUT_MS = 500L;

  /**
   * Returns whether this mode is already concrete ({@link #LIGHT} or {@link #DARK}) rather than
   * needing OS resolution.
   *
   * @return {@code true} for {@link #LIGHT} and {@link #DARK}, {@code false} for {@link #SYSTEM}
   * @version v0.1.0
   * @since v0.1.0
   */
  public boolean isConcrete() {
    return this != SYSTEM;
  }

  /**
   * Resolves this mode to a concrete {@link #LIGHT} or {@link #DARK}.
   *
   * <p>{@link #LIGHT} and {@link #DARK} return themselves. {@link #SYSTEM} performs a best-effort,
   * install-time OS appearance check (macOS and Windows are detected; any other platform, or a
   * failed probe, falls back to {@link #LIGHT}).
   *
   * <p>The check shells out, so it is bounded: a probe that has not answered within half a second
   * is killed and read as a failure. A stalled {@code defaults} or {@code reg} therefore costs the
   * caller that half-second and a {@link #LIGHT} theme, not an indefinite block — which matters
   * because {@code ElwhaTheme.install} runs this on the Event Dispatch Thread.
   *
   * @return {@link #LIGHT} or {@link #DARK}
   * @version v0.5.0
   * @since v0.1.0
   */
  public Mode resolved() {
    if (isConcrete()) {
      return this;
    }
    return detectSystemDark() ? DARK : LIGHT;
  }

  // Shell-out probes rather than a FlatLaf call: FlatLaf 3.5 exposes no cross-platform OS
  // appearance API (isLafDark() reports the installed LAF, not the OS). The cost is acceptable —
  // this runs once per install, never per paint.
  private static boolean detectSystemDark() {
    String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    try {
      if (os.contains("mac")) {
        String value = runProbe("defaults", "read", "-g", "AppleInterfaceStyle");
        return value != null && value.toLowerCase(Locale.ROOT).contains("dark");
      }
      if (os.contains("win")) {
        String value =
            runProbe(
                "reg",
                "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v",
                "AppsUseLightTheme");
        return value != null && value.contains("0x0");
      }
    } catch (RuntimeException probeFailed) {
      return false;
    }
    return false;
  }

  // Bounded exactly like MorphAnimator's gsettings probe: wait with a deadline, kill on expiry,
  // and only then drain the pipe — reading first would reintroduce the unbounded block, since a
  // stalled child holds its output stream open. Both probes print a single short line, so the
  // output cannot outgrow the pipe buffer while the process runs.
  private static String runProbe(String... command) {
    Process process = null;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
      if (!process.waitFor(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        return null;
      }
      return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception probeFailed) {
      if (probeFailed instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      if (process != null) {
        process.destroyForcibly();
      }
      return null;
    }
  }
}

package com.owspfm.ui.components.theme;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * The light/dark mode a {@link Config} installs under.
 *
 * <p>{@link #LIGHT} and {@link #DARK} are concrete; {@link #SYSTEM} follows the operating system's
 * appearance and is resolved to a concrete mode at install time via {@link #resolved()}. Per {@code
 * flatcomp-theme-install-api.md} §6, v1 detects the OS appearance only at install time — a live
 * OS-change listener is a deliberately-deferred fast follow.
 *
 * @author Charles Bryan
 * @version v0.1.0
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
   * @return {@link #LIGHT} or {@link #DARK}
   * @version v0.1.0
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

  private static String runProbe(String... command) {
    try {
      Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
      String output;
      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          builder.append(line).append('\n');
        }
        output = builder.toString();
      }
      process.waitFor();
      return output;
    } catch (Exception probeFailed) {
      if (probeFailed instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return null;
    }
  }
}

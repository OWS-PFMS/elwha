package com.owspfm.elwha.testkit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Scoped capture of a single component class's {@code java.util.logging} output — the sanctioned
 * way to assert an advisory (e.g. the missing-accessible-name warning) fires or stays silent.
 *
 * <p>{@link SuiteLogSilencer} pins the {@code com.owspfm.elwha} threshold to {@code SEVERE} for the
 * whole suite, so opening a capture temporarily drops the specific logger to {@link Level#ALL},
 * detaches it from parent handlers (nothing reaches the console), and records every published
 * message. {@link #close()} restores the inherited state — use try-with-resources so a failing
 * assertion cannot leak the widened level into later tests.
 *
 * @author Charles Bryan
 * @version v1.1.0
 * @since v1.1.0
 */
public final class LogCapture implements AutoCloseable {

  private final Logger logger;
  private final Handler handler;
  private final List<LogRecord> records = new CopyOnWriteArrayList<>();

  private LogCapture(final Logger logger) {
    this.logger = logger;
    this.handler =
        new Handler() {
          @Override
          public void publish(final LogRecord record) {
            records.add(record);
          }

          @Override
          public void flush() {}

          @Override
          public void close() {}
        };
    logger.setLevel(Level.ALL);
    logger.setUseParentHandlers(false);
    logger.addHandler(this.handler);
  }

  /**
   * Opens a capture on the logger named after {@code owner} — the class an Elwha advisory logs
   * through.
   *
   * @param owner the component class whose logger to capture
   * @return the open capture; close it (try-with-resources) to restore the logger
   * @version v1.1.0
   * @since v1.1.0
   */
  public static LogCapture of(final Class<?> owner) {
    return new LogCapture(Logger.getLogger(owner.getName()));
  }

  /**
   * Returns the messages published since the capture opened, in publication order.
   *
   * @return the captured message strings
   * @version v1.1.0
   * @since v1.1.0
   */
  public List<String> messages() {
    return records.stream().map(LogRecord::getMessage).toList();
  }

  /**
   * Restores the captured logger: handler removed, level back to inherited, parent handlers
   * reattached.
   *
   * @version v1.1.0
   * @since v1.1.0
   */
  @Override
  public void close() {
    logger.removeHandler(handler);
    logger.setLevel(null);
    logger.setUseParentHandlers(true);
  }
}

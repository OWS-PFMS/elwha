package com.owspfm.elwha.testkit;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs every test and lifecycle method on the Swing event dispatch thread, so component
 * construction and mutation in tests obey the same single-thread discipline the library promises
 * consumers. Register via {@code @ExtendWith(EdtInterceptor.class)}. Adapted from the EDT
 * interceptor example in the JUnit user guide (intercepting-invocations).
 *
 * <p>Not for {@code gui}-tagged Cacio tests: those drive a real event pipeline with {@code
 * java.awt.Robot}, which must run <em>off</em> the EDT.
 *
 * @author Charles Bryan
 * @version v0.5.0
 * @since v0.5.0
 */
public final class EdtInterceptor implements InvocationInterceptor {

  /**
   * Creates the interceptor; instantiated reflectively by JUnit.
   *
   * @version v0.5.0
   * @since v0.5.0
   */
  public EdtInterceptor() {}

  @Override
  public void interceptTestMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  @Override
  public void interceptTestTemplateMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  @Override
  public void interceptBeforeEachMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  @Override
  public void interceptAfterEachMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  @Override
  public void interceptBeforeAllMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  @Override
  public void interceptAfterAllMethod(
      final Invocation<Void> invocation,
      final ReflectiveInvocationContext<Method> invocationContext,
      final ExtensionContext extensionContext)
      throws Throwable {
    onEdt(invocation);
  }

  private static void onEdt(final Invocation<Void> invocation) throws Throwable {
    if (SwingUtilities.isEventDispatchThread()) {
      invocation.proceed();
      return;
    }
    final AtomicReference<Throwable> thrown = new AtomicReference<>();
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            invocation.proceed();
          } catch (final Throwable t) {
            thrown.set(t);
          }
        });
    if (thrown.get() != null) {
      throw thrown.get();
    }
  }
}

package edelta.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.xtext.xbase.testing.OnTheFlyJavaCompiler2;
import org.junit.After;
import org.junit.Test;

import edelta.interpreter.EdeltaInterpreterTimeoutGuard;

/** Tests timeout delivery, cancellation races and the thread's interrupt state. */
public class EdeltaInterpreterTimeoutGuardTest {

	@After
	public void clearInterrupt() {
		Thread.interrupted();
	}

	@Test
	public void testZeroDisablesTimeout() throws InterruptedException {
		try (var guard = new EdeltaInterpreterTimeoutGuard(0)) {
			assertThat(new CountDownLatch(1).await(50, TimeUnit.MILLISECONDS)).isFalse();
		}
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
	}

	@Test
	public void testTimeoutInterruptsBlockingCode() {
		try (var guard = new EdeltaInterpreterTimeoutGuard(10)) {
			assertThatThrownBy(() -> new CountDownLatch(1).await(30, TimeUnit.SECONDS))
				.isInstanceOf(InterruptedException.class);
		}
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
	}

	@Test
	public void testTimeoutDoesNotInterruptSubsequentCompilation() {
		try (var guard = new EdeltaInterpreterTimeoutGuard(30000)) {
			// Fire explicitly to test cleanup without depending on timer scheduling.
			guard.run();
			assertThat(Thread.currentThread().isInterrupted()).isTrue();
		}
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
		var compiler = new OnTheFlyJavaCompiler2(getClass().getClassLoader());
		assertThat(compiler.compileToClass("foo.Example", "package foo; public class Example {}")
			.getName()).isEqualTo("foo.Example");
	}

	@Test
	public void testCallbackAfterCloseDoesNotInterrupt() {
		var guard = new EdeltaInterpreterTimeoutGuard(30000);
		guard.close();
		// Simulate the timer having dequeued the callback just before close.
		guard.run();
		assertThat(Thread.currentThread().isInterrupted()).isFalse();
	}

	@Test
	public void testPreservesInterruptAlreadySetOnEntry() {
		Thread.currentThread().interrupt();
		try (var guard = new EdeltaInterpreterTimeoutGuard(30000)) {
			guard.run();
		}
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}

	@Test
	public void testPreservesExternalInterruptWhenThereWasNoTimeout() {
		try (var guard = new EdeltaInterpreterTimeoutGuard(30000)) {
			Thread.currentThread().interrupt();
		}
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
	}
}

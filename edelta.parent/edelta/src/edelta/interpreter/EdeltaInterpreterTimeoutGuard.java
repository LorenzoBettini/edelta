package edelta.interpreter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Interrupts the creating thread when interpretation exceeds its timeout.
 * Must be closed on that same thread, including when evaluation throws.
 * Closing and firing are synchronized so that a timeout cannot interrupt later
 * work on the interpreter thread. An interrupt already set on entry is preserved;
 * otherwise, a timeout's interrupt is consumed when interpretation finishes.
 */
public final class EdeltaInterpreterTimeoutGuard extends TimerTask implements AutoCloseable {

	private final Thread interpreterThread = Thread.currentThread();
	private final boolean interruptedOnEntry = interpreterThread.isInterrupted();
	private final Timer timer = new Timer("Edelta interpreter timeout", true);
	private boolean closed;
	private boolean timedOut;

	/** Starts the timeout in milliseconds; zero disables it, as with Thread.join(0). */
	public EdeltaInterpreterTimeoutGuard(final int timeout) {
		if (timeout != 0) {
			timer.schedule(this, timeout);
		}
	}

	/** Interrupts interpretation unless the guard has already been closed. */
	@Override
	public synchronized void run() {
		if (!closed) {
			timedOut = true;
			interpreterThread.interrupt();
		}
	}

	/** Cancels the timer and consumes its interrupt without clearing an earlier one. */
	@Override
	public synchronized void close() {
		closed = true;
		timer.cancel();
		if (timedOut && !interruptedOnEntry) {
			Thread.interrupted();
		}
	}
}

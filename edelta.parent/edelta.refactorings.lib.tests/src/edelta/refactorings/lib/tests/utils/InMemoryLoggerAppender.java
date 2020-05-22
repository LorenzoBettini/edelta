/**
 * 
 */
package edelta.refactorings.lib.tests.utils;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Appends logging events in a String.
 * 
 * @author Lorenzo Bettini
 *
 */
public class InMemoryLoggerAppender extends AppenderSkeleton {

	private StringBuilder builder = new StringBuilder();

	@Override
	public void close() {
		// nothing to close
	}

	@Override
	public boolean requiresLayout() {
		return false;
	}

	@Override
	protected void append(LoggingEvent event) {
		builder.append(
			event.getLevel() + ": " + event.getMessage().toString()
			+ System.lineSeparator());
	}

	public String getResult() {
		return builder.toString();
	}
}

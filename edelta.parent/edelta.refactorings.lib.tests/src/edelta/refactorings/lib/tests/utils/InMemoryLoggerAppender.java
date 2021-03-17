/**
 * 
 */
package edelta.refactorings.lib.tests.utils;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Appends logging events in a String. Make sure you specify the "\n" line
 * delimiter when comparing with directly with strings containing "\n" in Java
 * tests, while don't specify any delimiter (so that we use the default one
 * {@link System#lineSeparator()}) when using this in Xtend tests using
 * multi-line strings (which automatically rely on {@link System#lineSeparator()}).
 * 
 * @author Lorenzo Bettini
 *
 */
public class InMemoryLoggerAppender extends AppenderSkeleton {

	private StringBuilder builder = new StringBuilder();
	private String lineSeparator = System.lineSeparator();

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
			+ lineSeparator);
	}

	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	public String getResult() {
		return builder.toString();
	}
}

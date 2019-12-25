package edelta.maven.plugin;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.xtext.maven.MavenLog4JConfigurator;

/**
 * Customization to avoid passing a null {@link Throwable},
 * which generates a {@link NullPointerException} with log4j.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaMavenLog4JConfigurator extends MavenLog4JConfigurator {

	@Override
	protected AppenderSkeleton createMojoLogAppender(final Log log) {
		return new AppenderSkeleton() {

			@Override
			protected void append(LoggingEvent event) {
				if (event.getMessage() == null) {
					return;
				}
				Throwable throwable = getThrowable(event);
				if (throwable != null) {
					if (Level.DEBUG == event.getLevel()) {
						log.debug((CharSequence) event.getMessage(), throwable);
					} else if (Level.INFO == event.getLevel()) {
						log.info((CharSequence) event.getMessage(), throwable);
					} else if (Level.WARN == event.getLevel()) {
						log.warn((CharSequence) event.getMessage(), throwable);
					} else if (Level.ERROR == event.getLevel()) {
						log.error((CharSequence) event.getMessage(), throwable);
					}
				} else {
					if (Level.DEBUG == event.getLevel()) {
						log.debug((CharSequence) event.getMessage());
					} else if (Level.INFO == event.getLevel()) {
						log.info((CharSequence) event.getMessage());
					} else if (Level.WARN == event.getLevel()) {
						log.warn((CharSequence) event.getMessage());
					} else if (Level.ERROR == event.getLevel()) {
						log.error((CharSequence) event.getMessage());
					}
				}
			}

			public void close() {
			}

			private Throwable getThrowable(LoggingEvent event) {
				return event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;
			}

			public boolean requiresLayout() {
				return false;
			}
		};
	}
}

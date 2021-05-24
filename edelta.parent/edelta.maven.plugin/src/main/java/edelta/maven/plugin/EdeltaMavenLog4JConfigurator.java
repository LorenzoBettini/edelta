package edelta.maven.plugin;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.eclipse.xtext.maven.MavenLog4JConfigurator;

/**
 * Customization to avoid passing a null {@link Throwable},
 * which generates a {@link NullPointerException} with some loggers,
 * like {@link SystemStreamLog}, being used with Maven Testing Harness.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaMavenLog4JConfigurator extends MavenLog4JConfigurator {

	private final class AppenderSkeletonCustom extends AppenderSkeleton {
		private final Log log;

		private AppenderSkeletonCustom(Log log) {
			this.log = log;
		}

		@Override
		protected void append(LoggingEvent event) {
			if (event.getMessage() == null) {
				return;
			}
			var throwable = getThrowable(event);
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

		@Override
		public void close() {
			// nothing to do
		}

		private Throwable getThrowable(LoggingEvent event) {
			return event.getThrowableInformation() != null ? event.getThrowableInformation().getThrowable() : null;
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}
	}

	@Override
	protected AppenderSkeleton createMojoLogAppender(final Log log) {
		return new AppenderSkeletonCustom(log);
	}
}

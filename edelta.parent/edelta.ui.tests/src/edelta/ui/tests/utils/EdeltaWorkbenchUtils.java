package edelta.ui.tests.utils;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

public class EdeltaWorkbenchUtils {

	private static final int RETRIES = 3;

	private EdeltaWorkbenchUtils() {
		// cannot instantiate
	}

	/**
	 * Retries if we get a {@link CoreException} while waiting for build
	 * 
	 * @throws CoreException
	 */
	public static void waitForBuildWithRetries() throws CoreException {
		var i = 0;
		while (true) {
			try {
				ResourcesPlugin
					.getWorkspace()
					.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
				return;
			} catch (CoreException e) {
				if (++i < RETRIES) {
					System.err.println("*** EXCEPTION WAITING FOR BUILD ***");
					e.printStackTrace();
				} else
					throw e;
			}
		}
	}
}
package edelta.dependency.analyzer.ui.internal;

import java.io.IOException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Utility methods for safety checks and exception handling.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDependencyAnalyzerUiUtils {

	@FunctionalInterface
	public interface IFileRunnable {
		void accept(IFile workspaceFile) throws IOException, CoreException;
	}

	private EdeltaDependencyAnalyzerUiUtils() {
		// only static methods
	}

	public static void executeOnIFileSelection(ExecutionEvent event, IFileRunnable consumer) throws ExecutionException {
		var selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection instanceof IStructuredSelection) {
			var firstElement = ((IStructuredSelection) selection).getFirstElement();
			if (firstElement instanceof IFile) {
				try {
					consumer.accept((IFile) firstElement);
				} catch (IOException | CoreException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
			}
		}
	}
}

package edelta.dependency.analyzer.ui.internal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
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
		void accept(IFile workspaceFile, IProgressMonitor monitor) throws IOException, CoreException;
	}

	private EdeltaDependencyAnalyzerUiUtils() {
		// only static methods
	}

	public static void executeOnIFileSelection(ExecutionEvent event, IFileRunnable consumer) throws ExecutionException {
		var selection = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().getSelection();
		if (selection instanceof IStructuredSelection structuredSelection) {
			var firstElement = structuredSelection.getFirstElement();
			if (firstElement instanceof IFile file) {
				try {
					var service = PlatformUI.getWorkbench().getProgressService();
					service.run(false, false, new WorkspaceModifyOperation() {
						@Override
						protected void execute(IProgressMonitor monitor)
								throws CoreException, InvocationTargetException, InterruptedException {
							try {
								consumer.accept(file, monitor);
							} catch (IOException e) {
								throw new CoreException(
										new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
							}
						}
					});
				} catch (InvocationTargetException | InterruptedException e) {
					throw new ExecutionException(e.getMessage(), e);
				}
			}
		}
	}
}

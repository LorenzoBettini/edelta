package edelta.ui.tests.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Programmatically import existing projects into the workspace
 * for UI tests.
 * 
 * @author Lorenzo Bettini
 *
 */
public class ProjectImportUtil {

	/**
	 * Imports an existing project into the running workspace for SWTBot tests.
	 * 
	 * IMPORTANT: projects to be imported are expected to be located in the
	 * "../" + projectName folder.
	 * 
	 * @param projectName
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	public static IProject importProject(String projectName) throws CoreException, InvocationTargetException, InterruptedException {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		String projectToImportPath = path + "/../" +  projectName;
		IProject project = importProject(new File(projectToImportPath), projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		return project;
	}

	private static IProject importProject(final File projectPath, final String projectName) throws CoreException, InvocationTargetException, InterruptedException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		ImportOperation importOperation = new ImportOperation(
				project.getFullPath(), // relative to the workspace
				projectPath, // absolute path
				FileSystemStructureProvider.INSTANCE,
				s -> IOverwriteQuery.ALL);
		// this means: copy the imported project into workspace
		importOperation.setCreateContainerStructure(false);
		importOperation.run(new NullProgressMonitor());
		return project;
	}
}

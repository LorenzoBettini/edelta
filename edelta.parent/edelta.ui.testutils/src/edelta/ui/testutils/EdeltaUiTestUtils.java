package edelta.ui.testutils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Utilities for UI testing.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaUiTestUtils {

	private EdeltaUiTestUtils() {
		// only static methods
	}

	/**
	 * Imports an existing project into the running workspace for SWTBot tests.
	 * 
	 * IMPORTANT: the project path is meant to be relative to the current directory.
	 * 
	 * @param projectPath
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
	public static IJavaProject importJavaProject(String projectPath)
			throws CoreException, InvocationTargetException, InterruptedException {
		return JavaCore.create(importProject(projectPath));
	}

	/**
	 * Imports an existing project into the running workspace for SWTBot tests.
	 * 
	 * IMPORTANT: the project path is meant to be relative to the current directory.
	 * 
	 * @param projectPath
	 * @return
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws InvocationTargetException
	 */
	public static IProject importProject(String projectPath)
			throws CoreException, InvocationTargetException, InterruptedException {
		String projectName = new File(projectPath).getName();
		IProject project = getProjectFromWorkspace(projectName);
		if (project.isAccessible())
			return project;
		System.out.println("*** IMPORTING PROJECT: " + projectPath); // NOSONAR
		File currDir = new File("./");
		String path = currDir.getAbsolutePath();
		String projectToImportPath = String.format("%s/%s", path, projectPath);
		project = importProject(new File(projectToImportPath), projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		return project;
	}

	private static IProject importProject(final File projectPath, final String projectName) throws InvocationTargetException, InterruptedException {
		IProject project = getProjectFromWorkspace(projectName);
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

	private static IProject getProjectFromWorkspace(final String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}
}

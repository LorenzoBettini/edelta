package edelta.library.compilation;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.junit.BeforeClass;
import org.junit.Test;

public class EdeltaProjectCompilationTest {

	@BeforeClass
	public static void init() throws Exception {
		// needed when building with Tycho, otherwise, dependencies
		// in the MANIFEST of the created project will not be visible
		PDETargetPlatformUtils.setTargetPlatform();

		if (PlatformUI.getWorkbench().getIntroManager().getIntro() != null) {
			PlatformUI.getWorkbench().getIntroManager().closeIntro(
					PlatformUI.getWorkbench().getIntroManager().getIntro());
		}
	}

	@Test
	public void compileProject() throws CoreException, InterruptedException {
		File currDir = new File(".");
		String path = currDir.getAbsolutePath();
		String parentProject = "edelta.parent";
		int pos = path.lastIndexOf(parentProject);
		String baseDirectory = path.substring(0, pos + parentProject.length());
		String projectName = "edelta.refactorings.lib";
		String projectDirectory = baseDirectory + "/" + projectName;
		IProject project = importProject(new File(projectDirectory), projectName);
		project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
		IResourcesSetupUtil.fullBuild();
		IResourcesSetupUtil.assertNoErrorsInWorkspace();
	}

	private static IProject importProject(final File baseDirectory, final String projectName) throws CoreException {
		IProjectDescription description = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(new Path(baseDirectory.getAbsolutePath() + "/.project"));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null);
		return project;
	}
}

package edelta.library.compilation;

import java.io.File;
import java.util.Arrays;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Utility class that runs Eclipse, import the projects that we want to
 * compile and compile it with the Edelta compiler, so that java._trace files
 * are generated (and later included in the jar during the build).
 * 
 * Using xtext-maven-plugin does not seem to work since it does not
 * find the Ecore files.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaProjectCompilationTest {

	@BeforeClass
	public static void init() throws Exception {
		// needed when building with Tycho, otherwise, dependencies
		// in the MANIFEST of the created project will not be visible
		PDETargetPlatformUtils.setTargetPlatform();

		if (PlatformUI.getWorkbench().getIntroManager().getIntro() != null) {
			PlatformUI.getWorkbench().getIntroManager()
					.closeIntro(PlatformUI.getWorkbench().getIntroManager().getIntro());
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
		assertJavaTraceFiles(project);
	}

	private static IProject importProject(final File baseDirectory, final String projectName) throws CoreException {
		IProjectDescription description = ResourcesPlugin.getWorkspace()
				.loadProjectDescription(new Path(baseDirectory.getAbsolutePath() + "/.project"));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
		project.create(description, null);
		project.open(null);
		return project;
	}

	private void assertJavaTraceFiles(IProject project) throws CoreException {
		IFolder folder = project.getFolder("src-gen");
		assertJavaTraceFiles(folder);
	}

	private void assertJavaTraceFiles(IFolder folder) throws CoreException {
		IResource[] members = folder.members();
		Arrays.
			stream(members).
			forEach(
				m -> {
					if (m instanceof IFolder) {
						IFolder f = (IFolder) m;
						try {
							assertJavaTraceFiles(f);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					} else if (m.getName().endsWith(".java")) {
						String resourceName = m.getName();
						String javaFileName = resourceName.substring(0, resourceName.lastIndexOf("."));
						Assert.assertTrue(
							"no trace file for " + m.getName(),
							Arrays.stream(members).anyMatch(
									candidate ->
									candidate.getName().equals("." + javaFileName + ".java._trace")
									)
							);
					}
				}
			);
	}

}

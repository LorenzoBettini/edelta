package edelta.ui.tests.utils;

import static java.util.stream.Collectors.toList;
import static org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.addSourceFolder;
import static org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.findJavaProject;
import static org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.makeJava8Compliant;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.join;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.size;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtext.ui.XtextProjectHelper;
import org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil;
import org.eclipse.xtext.ui.util.PluginProjectFactory;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.Assert;

import com.google.inject.Inject;

/**
 * Utility class for creating a Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 */
public class PluginProjectHelper {
	@Inject
	private PluginProjectFactory projectFactory;

	public IJavaProject createJavaPluginProject(final String projectName, final List<String> requiredBundles,
			final List<String> additionalSrcFolders) throws JavaModelException, CoreException {
		projectFactory.setProjectName(projectName);
		// sometimes we get:
		// ERROR org.eclipse.xtext.ui.util.JavaProjectFactory - Build path contains
		// duplicate entry: 'src' for project 'test'
		// Java Model Exception: Java Model Status [Build path contains duplicate entry:
		// 'src' for project 'test']
		// probably due to some missing synchronization?
		// thus we don't do:
		// projectFactory.addFolders((additionalSrcFolders+#["src"]).toList);
		// but we add source folders later, which also triggers workspace build for each
		// added source folder
		projectFactory.addBuilderIds(JavaCore.BUILDER_ID, "org.eclipse.pde.ManifestBuilder",
				"org.eclipse.pde.SchemaBuilder", XtextProjectHelper.BUILDER_ID);
		projectFactory.addProjectNatures(JavaCore.NATURE_ID, "org.eclipse.pde.PluginNature",
				XtextProjectHelper.NATURE_ID);
		projectFactory.addRequiredBundles(requiredBundles);
		final IProject result = projectFactory.createProject(new NullProgressMonitor(), null);
		makeJava8Compliant(JavaCore.create(result));
		final IJavaProject javaProject = findJavaProject(projectName);
		addSourceFolder(javaProject, "src");
		for (String folder : additionalSrcFolders) {
			addSourceFolder(javaProject, folder);
		}
		return javaProject;
	}

	public void assertNoErrors() throws CoreException {
		final Iterable<IMarker> markers = getErrorMarkers();
		Assert.assertEquals(
			("unexpected errors:\n" + 
				join(map(markers, it -> {
					try {
						return it.getAttribute(IMarker.LOCATION) + ", " + it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				}), "\n")), 0, size(markers));
	}

	public void assertErrors(final CharSequence expected) throws CoreException {
		final Iterable<IMarker> markers = getErrorMarkers();
		assertEqualsStrings(expected,
				join(map(markers, it -> {
					try {
						return it.getAttribute(IMarker.MESSAGE);
					} catch (CoreException e) {
						throw Exceptions.sneakyThrow(e);
					}
				}), "\n"));
	}

	public Iterable<IMarker> getErrorMarkers() throws CoreException {
		return Stream.of(IResourcesSetupUtil.root().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE))
			.filter(it -> it.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
			.collect(toList());
	}

	protected void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}

}

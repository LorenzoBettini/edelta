package edelta.ui.tests.utils

import com.google.inject.Inject
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.util.List
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IResource
import org.eclipse.core.runtime.NullProgressMonitor
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.JavaCore
import org.eclipse.xtext.junit4.ui.util.JavaProjectSetupUtil
import org.eclipse.xtext.ui.XtextProjectHelper
import org.eclipse.xtext.ui.util.PluginProjectFactory

import static org.eclipse.xtext.junit4.ui.util.IResourcesSetupUtil.*
import static org.junit.Assert.*

/**
 * Utility class for creating a Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 * 
 */
class PluginProjectHelper {

	@Inject PluginProjectFactory projectFactory

	def IJavaProject createJavaPluginProject(String projectName, List<String> requiredBundles) {
		projectFactory.setProjectName(projectName);
		projectFactory.addFolders(newArrayList("src"));
		projectFactory.addBuilderIds(JavaCore.BUILDER_ID, "org.eclipse.pde.ManifestBuilder",
			"org.eclipse.pde.SchemaBuilder", XtextProjectHelper.BUILDER_ID);
		projectFactory.addProjectNatures(
			JavaCore.NATURE_ID,
			"org.eclipse.pde.PluginNature",
			XtextProjectHelper.NATURE_ID
		);
		projectFactory.addRequiredBundles(requiredBundles);
		val result = projectFactory.createProject(new NullProgressMonitor(), null);
		JavaProjectSetupUtil.makeJava5Compliant(JavaCore.create(result));
		return JavaProjectSetupUtil.findJavaProject(projectName);
	}

	def assertNoErrors() {
		val markers = getErrorMarkers()
		assertEquals(
			"unexpected errors:\n" +
				markers.map[getAttribute(IMarker.LOCATION) + ", " + getAttribute(IMarker.MESSAGE)].join("\n"),
			0,
			markers.size
		)
	}

	def assertErrors(CharSequence expected) {
		val markers = getErrorMarkers()
		assertEqualsStrings(
			expected,
			markers.map[getAttribute(IMarker.MESSAGE)].join("\n")
		)
	}

	def getErrorMarkers() {
		root.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE).
			filter[
				getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR
			]
	}

	def protected assertEqualsStrings(CharSequence expected, CharSequence actual) {
		assertEquals(expected.toString().replaceAll("\r", ""), actual.toString());
	}

	def void clearJdtIndex() {
		val jdtMetadata = JavaCore.getPlugin().getStateLocation().toFile();
		var success = false;
		try {
			cleanFolder(jdtMetadata);
			success = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		System.err.println("Clean up index " + jdtMetadata.getAbsolutePath() + ": " + success);
	}

	def void cleanFolder(File parentFolder) throws FileNotFoundException {
		cleanFolder(parentFolder, [true]);
	}

	def void cleanFolder(File parentFolder, FileFilter myFilter) throws FileNotFoundException {
		if (!parentFolder.exists()) {
			throw new FileNotFoundException(parentFolder.getAbsolutePath());
		}
		val File[] contents = parentFolder.listFiles(myFilter);
		for (var j = 0; j < contents.length; j++) {
			val File file = contents.get(j);
			if (file.isDirectory()) {
				cleanFolder(file, myFilter);
			} else {
				file.delete();
			}
		}
	}

}

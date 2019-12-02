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
import org.eclipse.xtext.ui.XtextProjectHelper
import org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil
import org.eclipse.xtext.ui.util.PluginProjectFactory

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.*
import static org.eclipse.xtext.ui.testing.util.JavaProjectSetupUtil.*
import static org.junit.Assert.*

/**
 * Utility class for creating a Plug-in project for testing.
 * 
 * @author Lorenzo Bettini
 * 
 */
class PluginProjectHelper {

	@Inject PluginProjectFactory projectFactory

	def IJavaProject createJavaPluginProject(String projectName, List<String> requiredBundles, List<String> additionalSrcFolders) {
		projectFactory.setProjectName(projectName);
		// sometimes we get:
		// ERROR org.eclipse.xtext.ui.util.JavaProjectFactory  - Build path contains duplicate entry: 'src' for project 'test'
		// Java Model Exception: Java Model Status [Build path contains duplicate entry: 'src' for project 'test']
		// probably due to some missing synchronization?
		// Since additionalSrcFolders contain crucial elements for our tests, like the .ecore files
		// better to add additionalSrcFolders before 'src', so that at least the additionalSrcFolders
		// are part of the project used during tests.
		// thus we don't do:
		// projectFactory.addFolders((additionalSrcFolders+#["src"]).toList);
		// but we add source folders later, which also triggers workspace build for each added source folder
		projectFactory.addBuilderIds(JavaCore.BUILDER_ID, "org.eclipse.pde.ManifestBuilder",
			"org.eclipse.pde.SchemaBuilder", XtextProjectHelper.BUILDER_ID);
		projectFactory.addProjectNatures(
			JavaCore.NATURE_ID,
			"org.eclipse.pde.PluginNature",
			XtextProjectHelper.NATURE_ID
		);
		projectFactory.addRequiredBundles(requiredBundles);
		val result = projectFactory.createProject(new NullProgressMonitor(), null);
		makeJava8Compliant(JavaCore.create(result));
		val javaProject = JavaProjectSetupUtil.findJavaProject(projectName)
		addSourceFolder(javaProject, "src")
		additionalSrcFolders.forEach[folder | addSourceFolder(javaProject, folder)]
		return javaProject;
	}

	def static void makeJava8Compliant(IJavaProject javaProject) {
		val options= javaProject.getOptions(false);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
		options.put(JavaCore.COMPILER_PB_ASSERT_IDENTIFIER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_PB_ENUM_IDENTIFIER, JavaCore.ERROR);
		options.put(JavaCore.COMPILER_CODEGEN_INLINE_JSR_BYTECODE, JavaCore.ENABLED);
		options.put(JavaCore.COMPILER_LOCAL_VARIABLE_ATTR, JavaCore.GENERATE);
		options.put(JavaCore.COMPILER_LINE_NUMBER_ATTR, JavaCore.GENERATE);
		options.put(JavaCore.COMPILER_SOURCE_FILE_ATTR, JavaCore.GENERATE);
		options.put(JavaCore.COMPILER_CODEGEN_UNUSED_LOCAL, JavaCore.PRESERVE);
		javaProject.setOptions(options);
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

	def static void clearJdtIndex() {
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

	def static void cleanFolder(File parentFolder) throws FileNotFoundException {
		cleanFolder(parentFolder, [true]);
	}

	def static void cleanFolder(File parentFolder, FileFilter myFilter) throws FileNotFoundException {
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

package edelta.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.junit.Assert;

/**
 * Utilities for testing.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTestUtils {

	private static final boolean OS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

	private EdeltaTestUtils() {

	}

	/**
	 * Removes all contents of the specified directory and of the first level
	 * possible subdirectories, skipping ".gitignore" and possible further
	 * subdirectories.
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void cleanDirectoryAndFirstSubdirectories(String directory) throws IOException {
		File dir = new File(directory);
		for (File file : dir.listFiles())
			if (file.isDirectory())
				cleanDirectory(directory + "/" + file.getName());
		cleanDirectory(directory);
	}

	/**
	 * Removes all contents of the specified directory, skipping ".gitignore"
	 * and possible subdirectories.
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void cleanDirectory(String directory) throws IOException {
		File dir = new File(directory);
		for (File file : dir.listFiles())
			if (!file.isDirectory() && !file.getName().equals(".gitignore"))
				Files.delete(file.toPath());
	}

	/**
	 * Removes all contents of the specified directory, skipping ".gitignore".
	 * 
	 * @param directory
	 * @throws IOException
	 */
	public static void cleanDirectoryRecursive(String directory) throws IOException {
		File dir = new File(directory);
		var listFiles = dir.listFiles();
		if (listFiles == null)
			return;
		for (File file : listFiles) {
			if (!file.isDirectory() && !file.getName().equals(".gitignore"))
				Files.delete(file.toPath());
			if (file.isDirectory()) {
				cleanDirectoryRecursive(directory + "/" + file.getName());
				Files.delete(file.toPath());
			}
		}
	}

	public static String loadFile(String file) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(file));
		return new String(encoded, Charset.defaultCharset());
	}

	/**
	 * Compares the string contents of the two files, given their paths,
	 * {@link Assert#assertEquals(Object, Object)}
	 * 
	 * @param pathOfExpectedContents
	 * @param pathOfActualContents
	 * @throws IOException
	 */
	public static void assertFilesAreEquals(String pathOfExpectedContents, String pathOfActualContents) throws IOException {
		assertFilesAreEquals(null, pathOfExpectedContents, pathOfActualContents);
	}

	/**
	 * Compares the string contents of the two files, given their paths,
	 * {@link Assert#assertEquals(String, Object, Object)}
	 * 
	 * @param message
	 * @param pathOfExpectedContents
	 * @param pathOfActualContents
	 * @throws IOException
	 */
	public static void assertFilesAreEquals(String message, String pathOfExpectedContents, String pathOfActualContents) throws IOException {
		assertEquals(
			message,
			removeCR(loadFile(pathOfExpectedContents)),
			removeCR(loadFile(pathOfActualContents)));
	}

	/**
	 * Compares the string contents of the file, given its path, with the specified
	 * string using {@link Assert#assertEquals(Object, Object)}
	 * 
	 * @param path
	 * @param expectedContents
	 * @throws IOException
	 */
	public static void assertFileContents(String path, String expectedContents) throws IOException {
		assertEquals(
			removeCR(expectedContents),
			removeCR(loadFile(path)));
	}

	/**
	 * Makes sure that there are no OS dependent line endings,
	 * for example CR on Windows.
	 * 
	 * @param s
	 * @return
	 */
	public static String removeCR(String s) {
		if (!OS_WINDOWS)
			return s;
		return s.replace("\r", "");
	}

	/**
	 * See {@link FileUtils#copyDirectory(File, File)}
	 * 
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	public static void copyDirectory(String source, String dest) throws IOException {
		FileUtils.copyDirectory(new File(source), new File(dest));
	}

	/**
	 * @see #assertResourcesAreValid(Collection)
	 * 
	 * @param resources
	 */
	public static void assertResourcesAreValid(Resource... resources) {
		assertResourcesAreValid(List.of(resources));
	}

	/**
	 * Asserts that the given resources are valid, i.e., that they do not contain
	 * any validation errors. This is done by validating each EObject in the
	 * resources and checking the severity of the resulting {@link Diagnostic}
	 * object. If any EObject is not valid, the test fails with a message containing
	 * the validation errors.
	 * 
	 * @see Diagnostician#validate(EObject)
	 * @see Diagnostic#getSeverity()
	 * 
	 * @param resources
	 */
	public static void assertResourcesAreValid(Collection<Resource> resources) {
		var diagnosticMessages = new StringBuilder();
		for (var resource : resources) {
			for (var eObject : resource.getContents()) {
				var diagnostic = Diagnostician.INSTANCE.validate(eObject);
				if (diagnostic.getSeverity() != Diagnostic.OK) {
					prettyDiagnostic(diagnosticMessages, diagnostic, "");
				}
			}
		}
		if (!diagnosticMessages.isEmpty()) {
			fail(diagnosticMessages.toString());
		}
	}

	/**
	 * Pretty prints the diagnostic messages of the given diagnostic and its
	 * children, indenting them with the specified string.
	 * 
	 * @param diagnosticMessages
	 * @param diagnostic
	 * @param indent
	 */
	public static void prettyDiagnostic(StringBuilder diagnosticMessages, Diagnostic diagnostic, String indent) {
		diagnosticMessages.append(indent);
		diagnosticMessages.append(diagnostic.getMessage() + "\n");
		for (var child : diagnostic.getChildren()) {
			prettyDiagnostic(diagnosticMessages, child, indent + "  ");
		}
	}
}

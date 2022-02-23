package edelta.testutils;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;

/**
 * Utilities for testing.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTestUtils {

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
		for (File file : dir.listFiles()) {
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
		return s.replace("\r", "");
	}
}

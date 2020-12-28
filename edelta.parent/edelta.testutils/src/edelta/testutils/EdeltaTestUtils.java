package edelta.testutils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
	 * Removes all contents of the specified directory, skipping ".gitignore"
	 * and possible subdirectories.
	 * 
	 * @param directory
	 */
	public static void cleanDirectory(String directory) {
		File dir = new File(directory);
		for (File file : dir.listFiles())
			if (!file.isDirectory() && !file.getName().equals(".gitignore"))
				assertTrue("File not deleted " + file.getAbsolutePath(), file.delete());
	}

	public static String loadFile(String file) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(file));
		return new String(encoded, Charset.defaultCharset());
	}

	/**
	 * Compares the two files as strings using
	 * {@link Assert#assertEquals(Object, Object)}
	 * 
	 * @param fileWithExpectedContents
	 * @param fileWithActualContents
	 * @throws IOException
	 */
	public static void compareFileContents(String fileWithExpectedContents, String fileWithActualContents) throws IOException {
		assertEquals(removeCR(loadFile(fileWithExpectedContents)), removeCR(loadFile(fileWithActualContents)));
	}

	/**
	 * Compares the file contents with the specified string using
	 * {@link Assert#assertEquals(Object, Object)}
	 * 
	 * @param file1
	 * @param expectedContents
	 * @throws IOException
	 */
	public static void compareSingleFileContents(String file1, String expectedContents) throws IOException {
		assertEquals(removeCR(expectedContents), removeCR(loadFile(file1)));
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

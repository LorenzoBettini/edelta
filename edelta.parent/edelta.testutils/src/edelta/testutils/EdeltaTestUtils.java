package edelta.testutils;

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
				file.delete();
	}

	public static String loadFile(String file) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(file));
		return new String(encoded, Charset.defaultCharset());
	}

	/**
	 * Compares the two files as strings using
	 * {@link Assert#assertEquals(Object, Object)}
	 * 
	 * @param file1
	 * @param file2
	 * @throws IOException
	 */
	public static void compareFileContents(String file1, String file2) throws IOException {
		Assert.assertEquals(loadFile(file1), loadFile(file2));
	}
}

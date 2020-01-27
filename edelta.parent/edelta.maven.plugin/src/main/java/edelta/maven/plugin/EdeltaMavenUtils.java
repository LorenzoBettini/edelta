package edelta.maven.plugin;

import java.io.File;

import org.eclipse.xtext.util.Strings;

import com.google.common.base.Predicate;

public class EdeltaMavenUtils {

	private EdeltaMavenUtils() {
		// only static methods
	}

	public static Predicate<String> emptyStringFilter() {
		return input -> !Strings.isEmpty(input.trim());
	}

	public static File createTempDir(String tmpClassDirectory) {
		File tmpDir = new File(tmpClassDirectory);
		if (!tmpDir.mkdirs() && !tmpDir.exists()) {
			throw new IllegalArgumentException("Couldn't create directory '" + tmpClassDirectory + "'.");
		}
		return tmpDir;
	}

}

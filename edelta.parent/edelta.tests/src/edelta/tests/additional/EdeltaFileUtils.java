package edelta.tests.additional;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class EdeltaFileUtils {

	public static CharSequence readFileAsString(String file) throws IOException {
		return readFileAsString(new File(file));
	}

	public static CharSequence readFileAsString(File file) throws IOException {
		byte[] buffer = new byte[(int) file.length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(file));
			f.read(buffer);
		} finally {
			if (f != null)
				try {
					f.close();
				} catch (IOException ignored) {
				}
		}
		return new String(buffer).replaceAll("\\r", "");
	}
}

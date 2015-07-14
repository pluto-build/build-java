package build.pluto.buildjava.test.simple;

import java.io.File;

public class FileUtils {

	public static void touch(File file) {
		file.setLastModified(System.currentTimeMillis());
	}

}

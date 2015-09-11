package build.pluto.buildjava.compiler;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface JavaCompiler extends Serializable {
	public static class JavaCompilerResult {
		private final Map<File, ? extends Collection<File>> generatedFiles;
		private final Collection<File> loadedFiles;

		public JavaCompilerResult(Map<File, ? extends Collection<File>> generatedFiles, Collection<File> loadedFiles) {
			this.generatedFiles = Collections.unmodifiableMap(generatedFiles);
			this.loadedFiles = Collections.unmodifiableCollection(loadedFiles);
		}
		public Map<File, ? extends Collection<File>> getGeneratedFiles() {
			return generatedFiles;
		}
		public Collection<File> getLoadedFiles() {
			return loadedFiles;
		}
	}

	public JavaCompilerResult compile(
			Collection<File> sourceFiles,
			File targetDir, 
			Collection<File> sourcePath,
			Collection<File> classPath,
			Collection<String> additionalArguments) throws Exception;
}

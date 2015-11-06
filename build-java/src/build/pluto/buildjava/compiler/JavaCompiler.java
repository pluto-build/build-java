package build.pluto.buildjava.compiler;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;

public interface JavaCompiler extends Serializable {
	public JavaCompilerResult compile(
			Collection<File> sourceFiles,
			File targetDir, 
			Collection<File> sourcePath,
			Collection<File> classPath,
			Collection<String> additionalArguments) throws Exception;
}

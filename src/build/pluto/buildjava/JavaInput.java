package build.pluto.buildjava;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import build.pluto.buildjava.compiler.JavaCompiler;
import build.pluto.dependency.IMetaBuildingEnabled;
import build.pluto.dependency.Origin;

public class JavaInput implements Serializable, IMetaBuildingEnabled {
	public static enum Compiler {
		JAVAC, ECLIPSE;
	}
	
	private static final long serialVersionUID = -8905198283548748809L;
	private final List<File> inputFiles;
	private final File targetDir;
	private final List<File> sourcePath;
	private final List<File> classPath;
	private final Collection<String> additionalArgs;
	private final String sourceRelease;
	private final String targetRelease;
	private final JavaCompiler compiler;
	private final Origin filesOrigin;

	public JavaInput(
			List<File> inputFiles,
			File targetDir,
			List<File> sourcePath, 
			List<File> classPath,
			Collection<String> additionalArgs,
			String sourceRelease, 
			String targetRelease,
			Origin filesOrigin,
			JavaCompiler compiler) {
		if (sourcePath == null || sourcePath.isEmpty()) {
			throw new IllegalArgumentException("Provide at least one source path!");
		}
		
		List<File> absoluteInputFiles = new ArrayList<>(inputFiles.size());
		for (File f : inputFiles)
			absoluteInputFiles.add(f.getAbsoluteFile());
		this.inputFiles = Collections.unmodifiableList(absoluteInputFiles);
		
		this.targetDir = (targetDir != null ? targetDir : new File(".")).getAbsoluteFile();
		this.sourcePath = sourcePath;
		this.classPath = (classPath == null || classPath.isEmpty()) ? Collections.singletonList(this.targetDir) : classPath;
		this.additionalArgs = additionalArgs;
		this.sourceRelease = sourceRelease;
		this.targetRelease = targetRelease;
		this.filesOrigin = filesOrigin;
		this.compiler = compiler;
	}

	public JavaInput(List<File> inputFile, File targetDir, File sourcePath, Origin filesOrigin, JavaCompiler compiler) {
		this(inputFile, targetDir, Collections.singletonList(sourcePath), Collections.singletonList(targetDir), null, null, null, filesOrigin, compiler);
	}
	public JavaInput(File inputFile, File targetDir, File sourcePath, Origin filesOrigin, JavaCompiler compiler) {
		this(Collections.singletonList(inputFile), targetDir, Collections.singletonList(sourcePath), Collections.singletonList(targetDir), null, null, null, filesOrigin, compiler);
	}

	@Override
	public void setMetaBuilding(boolean metaBuilding) {
		// Do nothing for now... Implementation is currently only in the
		// hotswap branch
	}

	public List<File> getInputFiles() {
		return inputFiles;
	}

	public Collection<String> getAdditionalArgs() {
		return additionalArgs;
	}

	public File getTargetDir() {
		return targetDir;
	}

	public List<File> getSourcePath() {
		return sourcePath;
	}

	public List<File> getClassPath() {
		return classPath;
	}
	
	public String getSourceRelease() {
		return sourceRelease;
	}

	public String getTargetRelease() {
		return targetRelease;
	}
	
	public JavaCompiler getCompiler() {
		return compiler;
	}
	
	public Origin getFilesOrigin() {
		return filesOrigin;
	}

	@Override
	public String toString() {
		return "JavaInput(input=" + inputFiles + ", target=" + targetDir + ", sourcePath=" + sourcePath + ", classPath=" + classPath + ", args="
				+ additionalArgs + ", origin=" + filesOrigin
				;
	}

}
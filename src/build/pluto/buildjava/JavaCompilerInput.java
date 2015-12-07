package build.pluto.buildjava;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import build.pluto.buildjava.compiler.IJavaCompiler;
import build.pluto.buildjava.compiler.JavacCompiler;
import build.pluto.dependency.IMetaBuildingEnabled;
import build.pluto.dependency.Origin;

public class JavaCompilerInput implements Serializable, IMetaBuildingEnabled {
	private static final long serialVersionUID = -8905198283548748809L;

	public final List<File> sourceFiles;
	public final File targetDir;
	public final List<File> sourcePath;
	public final List<File> classPath;
	public final Collection<String> additionalArgs;
	public final String sourceRelease;
	public final String targetRelease;
	public final Origin sourceOrigin;
	public final Origin classOrigin;
	public final IJavaCompiler compiler;

	private JavaCompilerInput(Builder builder) {
		if (builder.sourcePath == null || builder.sourcePath.isEmpty()) {
			throw new IllegalArgumentException("Source path may not be empty.");
		}
		
		List<File> absoluteInputFiles = new ArrayList<>(builder.inputFiles.size());
		for (File f : builder.inputFiles)
			absoluteInputFiles.add(f.getAbsoluteFile());
		this.sourceFiles = Collections.unmodifiableList(absoluteInputFiles);
		
		this.targetDir = (builder.targetDir != null ? builder.targetDir : new File(".")).getAbsoluteFile();
		this.sourcePath = builder.sourcePath;
		this.classPath = (builder.classPath == null || builder.classPath.isEmpty()) ? Collections.singletonList(this.targetDir) : builder.classPath;
		this.additionalArgs = builder.additionalArgs;
		this.sourceRelease = builder.sourceRelease;
		this.targetRelease = builder.targetRelease;
		this.sourceOrigin = builder.sourceOrigin;
		this.classOrigin = builder.classOrigin;
		this.compiler = builder.compiler == null ? JavacCompiler.instance : builder.compiler;
	}

	@Override
	public void setMetaBuilding(boolean metaBuilding) {
		// Do nothing for now... Implementation is currently only in the
		// hotswap branch
	}

	@Override
	public String toString() {
		return "JavaCompilerInput(input=" + sourceFiles + ", target=" + targetDir + ", sourcePath=" + sourcePath + ", classPath=" + classPath + ", args="
				+ additionalArgs + ", origin=" + sourceOrigin
				+ ")";
	}

	
	public static Builder Builder() { return new Builder(); }
	public static class Builder {
		private List<File> inputFiles = new ArrayList<>();
		private File targetDir;
		private List<File> sourcePath = new ArrayList<>();
		private List<File> classPath = new ArrayList<>();
		private Collection<String> additionalArgs = new ArrayList<>();
		private String sourceRelease;
		private String targetRelease;
		private IJavaCompiler compiler;
		private Origin sourceOrigin;
		private Origin classOrigin;
		
		public JavaCompilerInput get() { 
			return new JavaCompilerInput(this);
		}
		
		public Builder addInputFiles(File... files) {
			for (File f : files)
				inputFiles.add(f);
			return this;
		}
		public Builder addInputFiles(Iterable<File> files) {
			for (File f : files)
				inputFiles.add(f);
			return this;
		}
		
		public Builder setTargetDir(File targetDir) {
			this.targetDir = targetDir;
			return this;
		}
		
		public Builder addSourcePaths(File... sourcePaths) {
			for (File f : sourcePaths)
				sourcePath.add(f);
			return this;
		}
		public Builder addSourcePaths(Iterable<File> sourcePaths) {
			for (File f : sourcePaths)
				sourcePath.add(f);
			return this;
		}
		
		public Builder addClassPaths(File... classPaths) {
			for (File f : classPaths)
				classPath.add(f);
			return this;
		}
		public Builder addClassPaths(Iterable<File> classPaths) {
			for (File f : classPaths)
				classPath.add(f);
			return this;
		}
		
		public Builder addAdditionalArgs(String... args) {
			for (String arg : args)
				additionalArgs.add(arg);
			return this;
		}
		public Builder addAdditionalArgs(Iterable<String> args) {
			for (String arg : args)
				additionalArgs.add(arg);
			return this;
		}
		
		public Builder setSourceRelease(String sourceRelease) {
			this.sourceRelease = sourceRelease;
			return this;
		}
		
		public Builder setTargetRelease(String targetRelease) {
			this.targetRelease = targetRelease;
			return this;
		}
		
		public Builder setCompiler(IJavaCompiler compiler) {
			this.compiler = compiler;
			return this;
		}
		
		public Builder setSourceOrigin(Origin sourceOrigin) {
			this.sourceOrigin = sourceOrigin;
			return this;
		}
		
		public Builder setClassOrigin(Origin classOrigin) {
			this.classOrigin = classOrigin;
			return this;
		}
	}
}
package build.pluto.buildjava;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import build.pluto.builder.BuildRequest;
import build.pluto.buildjava.util.ListUtils;
import build.pluto.dependency.IMetaBuildingEnabled;
import build.pluto.util.AbsoluteComparedFile;

public class JavaInput implements Serializable, IMetaBuildingEnabled {
	private static final long serialVersionUID = -8905198283548748809L;
	private final List<File> inputFiles;
	private final File targetDir;
	private final List<File> sourcePath;
	private final List<File> classPath;
	private final String[] additionalArgs;
	private final List<BuildRequest<?, ?, ?, ?>> injectedDependencies;

	public JavaInput(List<File> inputFile, File targetDir, List<File> sourcePath, List<File> classPath, String[] additionalArgs,
			List<BuildRequest<?, ?, ?, ?>> requiredUnits) {
		Objects.requireNonNull(inputFile);
		if (sourcePath == null || sourcePath.isEmpty()) {
			throw new IllegalArgumentException("Provide at least one source path!");
		}
		this.inputFiles = inputFile;
		this.targetDir = targetDir != null ? targetDir : new File(".");
		this.sourcePath = sourcePath;
		this.classPath = (classPath == null || classPath.isEmpty()) ? Collections.singletonList(this.targetDir) : classPath;
		this.additionalArgs = additionalArgs;
		this.injectedDependencies = requiredUnits;
	}
	public JavaInput(File inputFile, File targetDir, List<File> sourcePath, List<File> classPath, String[] additionalArgs,
			List<BuildRequest<?, ?, ?, ?>> requiredUnits) {
		Objects.requireNonNull(inputFile);
		if (sourcePath == null || sourcePath.isEmpty()) {
			throw new IllegalArgumentException("Provide at least one source path!");
		}
		this.inputFiles = Collections.singletonList(inputFile);
		this.targetDir = targetDir != null ? targetDir : new File(".");
		this.sourcePath = sourcePath;
		this.classPath = (classPath == null || classPath.isEmpty()) ? Collections.singletonList(this.targetDir) : classPath;
		this.additionalArgs = additionalArgs;
		this.injectedDependencies = requiredUnits;
	}

	public JavaInput(List<File> inputFile, File targetDir, File sourcePath) {
		this(inputFile, targetDir, Collections.singletonList(sourcePath), Collections.singletonList(targetDir), null, null);
	}
	public JavaInput(File inputFile, File targetDir, File sourcePath) {
		this(Collections.singletonList(inputFile), targetDir, Collections.singletonList(sourcePath), Collections.singletonList(targetDir), null, null);
	}

	@Override
	public void setMetaBuilding(boolean metaBuilding) {
		// Do nothing for now... Implementation is currently only in the
		// hotswap branch
	}

	public List<File> getInputFiles() {
		return inputFiles;
	}

	public String[] getAdditionalArgs() {
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

	public List<BuildRequest<?, ?, ?, ?>> getInjectedDependencies() {
		if (injectedDependencies == null)
			return Collections.emptyList();
		else
			return injectedDependencies;
	}

	private static <T> boolean arraysEqualsEmptyEqNull(T[] arr1, T[] arr2) {
		if ((arr1 == null || arr1.length == 0) && (arr2 == null || arr2.length == 0))
			return true;
		if (arr1 == null || arr2 == null)
			return false;
		return Arrays.equals(arr1, arr2);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JavaInput) {
			JavaInput other = (JavaInput) obj;
			if (inputFiles.size() != other.inputFiles.size())
				return false;
			for (int i = 0; i < inputFiles.size(); i++)
				if (!AbsoluteComparedFile.equals(inputFiles.get(i), other.inputFiles.get(i)))
					return false;
			if (!AbsoluteComparedFile.equals(targetDir, other.targetDir))
				return false;
			if (!ListUtils.equals(sourcePath, other.sourcePath))
				return false;
			if (!ListUtils.equals(classPath, other.classPath))
				return false;
			if (!arraysEqualsEmptyEqNull(additionalArgs, other.additionalArgs))
				return false;
			if (!ListUtils.equalsEmptyEqNull(injectedDependencies, other.injectedDependencies))
				return false;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "JavaInput(input=" + inputFiles + ", target=" + targetDir + ", sourcePath=" + sourcePath + ", classPath=" + classPath + ", args="
				+ Arrays.toString(additionalArgs) + ", deps=" + injectedDependencies;
	}

}
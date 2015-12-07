package build.pluto.buildjava;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import build.pluto.dependency.Origin;

public class JavaRunnerInput implements Serializable {
	private static final long serialVersionUID = -8905198283548748809L;

	public final String description;
	public final File workingDir;
	public final String mainClass;
	public final List<File> classPath;
	public final Origin classOrigin;
	public final Collection<String> vmArgs;
	public final Collection<String> programArgs;

	private JavaRunnerInput(Builder builder) {
		this.description = builder.description;
		this.mainClass = Objects.requireNonNull(builder.mainClass, "Name of main class is missing.");

		if (builder.workingDir == null)
			this.workingDir = new File(".").getAbsoluteFile();
		else
			this.workingDir = builder.workingDir;
		
		if (builder.classPath == null || builder.classPath.isEmpty())
			this.classPath = Collections.singletonList(workingDir.getAbsoluteFile());
		else
			this.classPath = Collections.unmodifiableList(builder.classPath);
		this.vmArgs = Collections.unmodifiableCollection(builder.vmArgs);
		this.programArgs = Collections.unmodifiableCollection(builder.programArgs);
		this.classOrigin = builder.classOrigin;
	}

	@Override
	public String toString() {
		return "JavaRunnerInput(mainClass=" + mainClass + ", classPath=" + classPath + ", args=" + vmArgs + ")";
	}

	
	public static Builder Builder() { return new Builder(); }
	public static class Builder {
		private String description;
		private File workingDir;
		private String mainClass;
		private List<File> classPath = new ArrayList<>();
		private Collection<String> vmArgs = new ArrayList<>();
		private Collection<String> programArgs = new ArrayList<>();
		private Origin classOrigin;
		
		public JavaRunnerInput get() { 
			return new JavaRunnerInput(this);
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
		
		public Builder addVMArgs(String... args) {
			for (String arg : args)
				vmArgs.add(arg);
			return this;
		}
		public Builder addVMArgs(Iterable<String> args) {
			for (String arg : args)
				vmArgs.add(arg);
			return this;
		}

		public Builder addProgramArgs(String... args) {
			for (String arg : args)
				programArgs.add(arg);
			return this;
		}
		public Builder addProgramArgs(Iterable<String> args) {
			for (String arg : args)
				programArgs.add(arg);
			return this;
		}

		public Builder setMainClass(String mainClass) {
			this.mainClass = mainClass;
			return this;
		}
		
		public Builder setDescription(String description) {
			this.description = description;
			return this;
		}
		
		public Builder setWorkingDir(File workingDir) {
			this.workingDir = workingDir;
			return this;
		}
		
		public Builder setClassOrigin(Origin classOrigin) {
			this.classOrigin = classOrigin;
			return this;
		}
	}
}
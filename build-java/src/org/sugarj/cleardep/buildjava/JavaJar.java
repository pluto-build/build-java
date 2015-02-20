package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sugarj.cleardep.SimpleCompilationUnit;
import org.sugarj.cleardep.build.BuildContext;
import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.CommandExecution;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

public class JavaJar extends Builder<BuildContext, JavaJar.Input, SimpleCompilationUnit> {

	public static BuilderFactory<BuildContext, Input, SimpleCompilationUnit, JavaJar> factory = new BuilderFactory<BuildContext, Input, SimpleCompilationUnit, JavaJar>() {
		@Override
		public JavaJar makeBuilder(BuildContext context) { return new JavaJar(context); }
	};
	
	public static enum Mode { Create, List, Extract, Update, GenIndex;
		public String option() {
			switch (this) {
			case Create: return "c";
			case List: return "t";
			case Extract: return "x";
			case Update: return "u";
			case GenIndex: return "i";
			default: return "";
			}
		}
	}
	
	public static class Input {
		public final Mode mode;
		public final Path jarPath;
		public final Path manifestPath;
		public final Path baseDir;
		public final String[] files;
		public final RequirableCompilationUnit[] requiredUnits;
		public Input(
				Mode mode,
				Path jarPath,
				Path manifestPath,
				Path baseDir,
				String[] files,
				RequirableCompilationUnit[] requiredUnits) {
			this.mode = mode;
			this.jarPath = jarPath;
			this.manifestPath = manifestPath;
			this.baseDir = baseDir;
			this.files = files;
			this.requiredUnits = requiredUnits;
		}
	}
	
	public JavaJar(BuildContext context) {
		super(context);
	}

	@Override
	protected String taskDescription(Input input) {
		return "Compile Stratego code";
	}
	
	@Override
	protected Path persistentPath(Input input) {
		int hash = Arrays.hashCode(input.files);
		if (input.jarPath != null)
			return FileCommands.addExtension(input.jarPath, hash + ".dep");
		if (input.manifestPath != null)
			return FileCommands.addExtension(input.manifestPath, "jar." + hash + ".dep");
		if (input.baseDir != null)
			return FileCommands.addExtension(input.baseDir, "jar." + hash + ".dep");
		return new AbsolutePath("./jar." + hash + ".dep");
	}

	@Override
	protected Class<SimpleCompilationUnit> resultClass() {
		return SimpleCompilationUnit.class;
	}

	@Override
	protected Stamper defaultStamper() { return LastModifiedStamper.instance; }

	@Override
	protected void build(SimpleCompilationUnit result, Input input) throws IOException {
		if (input.requiredUnits != null)
			for (RequirableCompilationUnit req : input.requiredUnits)
				result.addModuleDependency(req.require());
		
		List<String> flags = new ArrayList<>();
		List<String> args = new ArrayList<>();
		
		flags.add(input.mode.option());
		
		if (input.manifestPath != null) {
			result.addExternalFileDependency(input.manifestPath);
			flags.add("m");
			args.add(input.manifestPath.getAbsolutePath());
		}
		
		if (input.jarPath != null) {
			flags.add("f");
			args.add(input.jarPath.getAbsolutePath());
		}
		
		if (input.baseDir != null) {
			args.add("-C");
			args.add(input.baseDir.getAbsolutePath());
		}
		
		for (String s : input.files) {
			Path p;
			if (AbsolutePath.acceptable(s))
				p = new AbsolutePath(s);
			else if (input.baseDir != null)
				p = new RelativePath(input.baseDir, s);
			else
				p = new AbsolutePath("./" + s);
			
			args.add(p.getAbsolutePath());
			
			if (p.getFile().isDirectory())
				for (Path file : FileCommands.listFilesRecursive(p))
					result.addExternalFileDependency(file);
			else
				result.addExternalFileDependency(p);
		}

		String[] command = new String[1 + flags.size() + args.size()];
		command[0] = "jar";
		int i = 1;
		for (String flag : flags) {
			command[i] = flag;
			i++;
		}
		for (String arg : args) {
			command[i] = arg;
			i++;
		}
		new CommandExecution(true).execute(command);
		
		if (input.jarPath != null)
			result.addGeneratedFile(input.jarPath);
	}
}

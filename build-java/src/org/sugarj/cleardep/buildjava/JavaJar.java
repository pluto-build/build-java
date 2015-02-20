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
import org.sugarj.common.StringCommands;
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
		
		public String modeForPath() {
			switch (this) {
			case Create: return "generate";
			case List: return "list";
			case Extract: return "extract";
			case Update: return "generate";
			case GenIndex: return "index";
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
		String mode = input.mode.modeForPath();
		if (input.jarPath != null)
			return FileCommands.addExtension(input.jarPath, mode + "." +"dep");
		int hash = Arrays.hashCode(input.files);
		if (input.manifestPath != null)
			return FileCommands.addExtension(input.manifestPath, "jar." + mode + "." + hash + ".dep");
		if (input.baseDir != null)
			return FileCommands.addExtension(input.baseDir, "jar." + mode + "." + hash + ".dep");
		return new AbsolutePath("./jar." + mode + "." + hash + ".dep");
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
			result.addExternalFileDependency(input.baseDir);
			args.add(input.baseDir.getAbsolutePath());
			if (!FileCommands.exists(input.baseDir))
				return;
		}
		
		boolean foundSourceFile = false;
		for (String s : input.files) {
			Path base;
			if (AbsolutePath.acceptable(s))
				base = null;
			else if (input.baseDir != null)
				base = input.baseDir;
			else
				base = new AbsolutePath(".");
				
			Path p;
			if (base == null) {
				p = new AbsolutePath(s);
				base = p;
			}
			else
				p = new RelativePath(base, s);
			
			if (p.getFile().isDirectory())
				for (Path file : FileCommands.listFilesRecursive(p)) {
					result.addExternalFileDependency(file);
					RelativePath rel = FileCommands.getRelativePath(base, file);
					args.add(rel.getRelativePath());
					foundSourceFile = true;
				}
			else {
				result.addExternalFileDependency(p);
				if (FileCommands.exists(p)) {
					args.add(s);
					foundSourceFile = true;
				}
			}
		}
		
		if ((input.mode == Mode.Create || input.mode == Mode.Update) && !foundSourceFile)
			return;
		
		String[] command = new String[1 + 1 + args.size()];
		command[0] = "jar";
		command[1] = StringCommands.printListSeparated(flags, "");
		int i = 2;
		for (String arg : args) {
			command[i] = arg;
			i++;
		}
		
		try {
			new CommandExecution(true).execute(command);
		} finally {
			if (input.jarPath != null)
				result.addGeneratedFile(input.jarPath);
		}
	}
}

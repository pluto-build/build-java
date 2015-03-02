package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.sugarj.cleardep.None;
import org.sugarj.cleardep.build.BuildRequest;
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

public class JavaJar extends Builder<JavaJar.Input, None> {

	public static BuilderFactory<Input, None, JavaJar> factory = new BuilderFactory<Input, None, JavaJar>() {
		private static final long serialVersionUID = -3699710145857628029L;

		@Override
		public JavaJar makeBuilder(Input input) { return new JavaJar(input); }
	};
	
	public static enum Mode { Create, List, Extract, Update, GenIndex, CreateOrUpdate;
		
		public String modeForPath() {
			switch (this) {
			case Create: return "generate";
			case List: return "list";
			case Extract: return "extract";
			case Update: return "generate";
			case GenIndex: return "index";
			case CreateOrUpdate: return "generate";
			default: return "";
			}
		}
	}
	
	public String option(Mode m) {
		switch (m) {
		case Create: return "c";
		case List: return "t";
		case Extract: return "x";
		case Update: return "u";
		case GenIndex: return "i";
		case CreateOrUpdate: return FileCommands.fileExists(input.jarPath) ? option(Mode.Update) : option(Mode.Create);
		default: return "";
		}
	}

	
	public static class Input implements Serializable {
		private static final long serialVersionUID = -6951002448963322561L;
		public final Mode mode;
		public final Path jarPath;
		public final Path manifestPath;
		public final Path[] files;
		public final BuildRequest<?,?,?,?>[] requiredUnits;
		public Input(
				Mode mode,
				Path jarPath,
				Path manifestPath,
				Path[] files,
				BuildRequest<?,?,?,?>[] requiredUnits) {
			this.mode = mode;
			this.jarPath = jarPath;
			this.manifestPath = manifestPath;
			this.files = files;
			this.requiredUnits = requiredUnits;
		}
	}
	
	private JavaJar(Input input) {
		super(input);
	}

	@Override
	protected String taskDescription() {
		switch (input.mode) {
		case Create:
		case Update: return "Generate JAR file";
		case Extract: return "Extract files from JAR file";
		case List: return "List table of contents of JAR file";
		case GenIndex: return "Create index information from JAR file";
		default: return "";
		}
	}
	
	@Override
	protected Path persistentPath() {
		String mode = input.mode.modeForPath();
		if (input.jarPath != null)
			return FileCommands.addExtension(input.jarPath, mode + "." +"dep");
		int hash = Arrays.hashCode(input.files);
		if (input.manifestPath != null)
			return FileCommands.addExtension(input.manifestPath, "jar." + mode + "." + hash + ".dep");
		return new AbsolutePath("./jar." + mode + "." + hash + ".dep");
	}

	@Override
	protected Stamper defaultStamper() { return LastModifiedStamper.instance; }

	@Override
	protected None build() throws IOException {
		if (input.requiredUnits != null)
			for (BuildRequest<?,?,?,?> req : input.requiredUnits)
				require(req);
		
		List<String> flags = new ArrayList<>();
		List<String> args = new ArrayList<>();
		
		flags.add(option(input.mode));
		
		if (input.manifestPath != null) {
			requires(input.manifestPath);
			flags.add("m");
			args.add(input.manifestPath.getAbsolutePath());
		}
		
		if (input.jarPath != null) {
			flags.add("f");
			args.add(input.jarPath.getAbsolutePath());
		}
		
		for (Path f : input.files) {
			requires(f);
			if (f instanceof AbsolutePath)
				args.add(f.getAbsolutePath());
			else if (f instanceof RelativePath) {
				RelativePath frel = (RelativePath) f;
				args.add("-C");
				args.add(frel.getBasePath().getAbsolutePath());
				args.add(frel.getRelativePath());
			}
		}
		
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
				generates(input.jarPath);
		}
		return None.val;
	}
}

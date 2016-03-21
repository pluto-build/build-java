package build.pluto.buildjava;

import static org.sugarj.common.Log.log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.Log;
import org.sugarj.common.StringCommands;

import build.pluto.builder.Builder;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.builder.factory.BuilderFactoryFactory;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;
import build.pluto.stamp.LastModifiedStamper;

public class JavaRunner extends Builder<JavaRunnerInput, Out<ExecutionResult>> {

	public JavaRunner(JavaRunnerInput input) {
		super(input);
	}

	public final static BuilderFactory<JavaRunnerInput, Out<ExecutionResult>, JavaRunner> factory = BuilderFactoryFactory.of(JavaRunner.class, JavaRunnerInput.class);

	
	@Override
	protected String description(JavaRunnerInput input) {
		if (input.description != null)
			return input.description;
		return "Run Java class " + input.mainClass + " with arguments " + input.programArgs;
	}

	@Override
	public File persistentPath(JavaRunnerInput input) {
		return new File(input.workingDir, "run." + input.mainClass + input.programArgs.hashCode() + ".dep");
	}

	@Override
	protected Out<ExecutionResult> build(JavaRunnerInput input) throws Throwable {
		String classPathString = StringCommands.printListSeparated(input.classPath, File.pathSeparator);
		
		boolean verboseInput = input.vmArgs != null && (input.vmArgs.contains("-verbose") || input.vmArgs.contains("-verbose:class"));
		
		List<String> command = new ArrayList<>();
		command.add("java");
		if (input.vmArgs != null)
			command.addAll(input.vmArgs);
		if (!verboseInput)
			command.add("-verbose:class");
		command.add("-cp");
		command.add(classPathString);
		command.add(input.mainClass);
		if (input.programArgs != null)
			command.addAll(input.programArgs);
		
		requireBuild(input.classOrigin);
		
		Log.log.log(StringCommands.printListSeparated(command, " "), Log.DETAIL);
		try {
	    	ExecutionResult er = Exec.run(input.workingDir, command.toArray(new String[command.size()]));
	    	String[] outMsgs = installDependencies(er.outMsgs, !verboseInput);
	    	return OutputPersisted.of(new ExecutionResult(er.cmds, outMsgs, er.errMsgs));
		} catch (ExecutionError e) {
			String[] outMsgs = installDependencies(e.outMsgs, !verboseInput);
			if (verboseInput)
				throw e;
			else {
				String msg = e.getMessage().substring(0, e.getMessage().length() - log.commandLineAsString(e.cmds).length() - 2);
				throw new ExecutionError(msg, e.cmds, outMsgs, e.errMsgs);
			}
		}
	}

	private final String jarprefix = "[Opened";
	private final String fileprefix = "[Loaded ";
	private final String filesplit = " from file:";
	private String[] installDependencies(String[] outMsgs, boolean removeVerbose) {
		List<String> out = new ArrayList<>();
		for (String line : outMsgs) {
			boolean lineIsVerbose = false;
			if (line.startsWith(jarprefix)) {
				lineIsVerbose = true;
				String jar = line.substring(jarprefix.length() + 1, line.length() - 1);
				require(new File(jar), LastModifiedStamper.instance);
			}
			else if (line.startsWith(fileprefix)) {
				lineIsVerbose = true;
				if (line.contains(filesplit)) {
					String trim = line.substring(fileprefix.length(), line.length() - 1);
					String[] classAndPath = trim.split(filesplit);
					String clazz = classAndPath[0];
					String dir = classAndPath[1];
					File f = new File(dir, clazz.replace('.', File.separatorChar) + ".class");
					require(f, LastModifiedStamper.instance);
				}
			}

			if (removeVerbose && !lineIsVerbose)
				out.add(line);
		}
		
		if (removeVerbose)
			return out.toArray(new String[out.size()]);
		else
			return outMsgs;
	}

}

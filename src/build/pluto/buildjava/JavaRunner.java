package build.pluto.buildjava;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.sugarj.common.Exec;
import org.sugarj.common.Exec.ExecutionError;
import org.sugarj.common.Exec.ExecutionResult;
import org.sugarj.common.StringCommands;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.BuilderFactoryFactory;
import build.pluto.output.Out;
import build.pluto.output.OutputPersisted;

public class JavaRunner extends Builder<JavaRunnerInput, Out<ExecutionResult>> {

	public JavaRunner(JavaRunnerInput input) {
		super(input);
	}

	public final static BuilderFactory<JavaRunnerInput, Out<ExecutionResult>, JavaRunner> factory = BuilderFactoryFactory.of(JavaRunner.class, JavaRunnerInput.class);

	
	@Override
	protected String description(JavaRunnerInput input) {
		if (input.description != null)
			return input.description;
		return "Run Java class " + input.mainClass;
	}

	@Override
	public File persistentPath(JavaRunnerInput input) {
		return new File(input.workingDir, "run." + input.mainClass + ".dep");
	}

	@Override
	protected Out<ExecutionResult> build(JavaRunnerInput input) throws Throwable {
		String classPathString = StringCommands.printListSeparated(input.classPath, File.pathSeparator);
		
		List<String> command = new ArrayList<>();
		command.add("java");
		command.add("-cp");
		command.add(classPathString);
		if (input.vmArgs != null)
			command.addAll(input.vmArgs);
		command.add(input.mainClass);
		if (input.programArgs != null)
			command.addAll(input.programArgs);
		
		try {
	    	ExecutionResult er = Exec.run(input.workingDir, command.toArray(new String[command.size()]));
			return OutputPersisted.of(er);
    	} catch (ExecutionError e) {
    		String msg = StringCommands.printListSeparated(e.outMsgs, "\n") + StringCommands.printListSeparated(e.errMsgs, "\n");
    		String cmd = StringCommands.printListSeparated(e.cmds, " ");
    		reportError("Execution failed>>>\n" + cmd + "\n" + msg);
    		report("<<<Execution failed");
    		setState(State.FAILURE);
    		return null;
    	}
	}

}

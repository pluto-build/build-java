package org.sugarj.cleardep.buildjava;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.buildjava.JavaBuilder.Input;
import org.sugarj.cleardep.buildjava.util.Environment;
import org.sugarj.cleardep.buildjava.util.FileExtensionFilter;
import org.sugarj.cleardep.buildjava.util.SugarLangProjectEnvironment;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;
import org.sugarj.common.path.RelativePath;

/**
 * updates editors to show newly built results
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class EclipseJavaBuilder extends IncrementalProjectBuilder {

	protected IProject[] build(int kind, Map<String, String> args,
			IProgressMonitor monitor) {
		System.out.println("Starting build...");
		BuildManager manager = new BuildManager();

		InitConsole();
		
		try {
			List<JavaBuilder.Input> inputs = makeInputs(getProject());
			for (Input input : inputs) {
				manager.require(JavaBuilder.factory.makeBuilder(input, manager));
			}
			getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} finally {
			monitor.done();
		}
		return null;
	}
	
	private void InitConsole() {
		Log.out = EclipseConsole.getOutputPrintStream();
		Log.err = EclipseConsole.getErrorPrintStream();
		Log.log.setLoggingLevel(Log.ALWAYS);
		EclipseConsole.activateConsoleOnce();
	}

	private List<Input> makeInputs(IProject project) throws JavaModelException {
		Environment env = SugarLangProjectEnvironment.makeProjectEnvironment(project);

		List<Path> files = new ArrayList<Path>();
		for (Path sp: env.getSourcePath()) {
			for (Path p: FileCommands.listFilesRecursive(sp, new FileExtensionFilter("java"))) {
				files.add(p);
			}
		}
		
		List<Input> inputs = new ArrayList<Input>();
		
		for (Path p: files) {
			JavaBuilder.Input input = new Input(Arrays.asList(p), env.getRoot(), env.getBin(), env.getSourcePath(),
					env.getIncludePath(), null, null, true);
			inputs.add(input);
		}

		return inputs;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		// TODO: Implement clean
		System.out.println("Starting clean...");
		InitConsole();
	}
}

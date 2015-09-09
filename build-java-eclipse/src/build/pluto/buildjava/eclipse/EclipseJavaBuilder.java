package build.pluto.buildjava.eclipse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.sugarj.common.FileCommands;
import org.sugarj.common.Log;

import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.buildjava.JavaBuilder;
import build.pluto.buildjava.JavaInput;
import build.pluto.buildjava.util.FileExtensionFilter;
import build.pluto.output.None;

/**
 * updates editors to show newly built results
 * 
 * @author Sebastian Erdweg <seba at informatik uni-marburg de>
 */
public class EclipseJavaBuilder extends IncrementalProjectBuilder {

	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) {
		System.out.println("Starting build...");

		InitConsole();

		try {
			List<BuildRequest<?, None, ?, ?>> requests = new ArrayList<>();
			for (JavaInput input : makeInputs(getProject()))
				requests.add(JavaBuilder.request(input));
				
			BuildManagers.buildAll(requests);

			getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
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

	private List<JavaInput> makeInputs(IProject project) throws JavaModelException {
		Environment env = SugarLangProjectEnvironment.makeProjectEnvironment(project);

		List<JavaInput> inputs = new ArrayList<>();
		
		for (File sourcePath : env.getSourcePath())
			for (Path p : FileCommands.listFilesRecursive(sourcePath.toPath(), new FileExtensionFilter("java")))
				inputs.add(new JavaInput(
						p.toFile(), 
						env.getBin(), 
						env.getSourcePath(), 
						env.getIncludePath(), 
						new String[] { "-source", env.getJavaComplianceLevel() },
						null));
		
		return inputs;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		System.out.println("Starting clean...");
		InitConsole();

		Log.log.beginTask("Starting clean", Log.ALWAYS);

		Environment env = SugarLangProjectEnvironment.makeProjectEnvironment(getProject());

		try {
			FileCommands.delete(env.getBin());
			FileCommands.createDir(env.getBin());
			getProject().refreshLocal(IProject.DEPTH_INFINITE, monitor);
		} catch (IOException e) {
			Log.err.println("Clean failed...");
		}
		monitor.done();

		Log.log.endTask();
	}
}

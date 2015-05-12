package build.pluto.buildjava.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import build.pluto.buildjava.JavaBuilder.Input;
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
			List<JavaBuilder.Input> inputs = makeInputs(getProject());
			@SuppressWarnings("unchecked")
			BuildRequest<?, None, ?, ?>[] reqs = (BuildRequest<?, None, ?, ?>[]) new BuildRequest[inputs.size()];
			int i = 0;
			for (Input input : inputs) {
				reqs[i] = new BuildRequest<>(JavaBuilder.factory, input);
				i++;
			}

			BuildManagers.buildAll(reqs);

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

	private List<Input> makeInputs(IProject project) throws JavaModelException {
		Environment env = SugarLangProjectEnvironment.makeProjectEnvironment(project);

		Stream<File> files = env.getSourcePath().stream()
				.flatMap((org.sugarj.common.path.Path sp) -> FileCommands.streamFiles(sp.getFile(), new FileExtensionFilter("java")));

		List<Input> inputs = files.map(
				(File p) -> new JavaBuilder.Input(new File[] { p }, env.getBin().getFile(), env.getSourcePath().toArray(new File[0]), env.getIncludePath()
						.toArray(new File[0]), new String[] { "-source", env.getJavaComplianceLevel() }, null, true)).collect(Collectors.toList());

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

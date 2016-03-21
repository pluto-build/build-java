package build.pluto.buildjava;

import java.util.ArrayList;
import java.util.Objects;

import build.pluto.builder.BuildAtOnceCycleHandler;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.factory.BuilderFactory;
import build.pluto.output.None;

/**
 * Specializes the {@link BuildAtOnceCycleHandler} for Java.
 * 
 * @author moritzlichter
 *
 */
public class JavaCycleHandler extends
		BuildAtOnceCycleHandler<JavaCompilerInput, None, JavaCompiler, BuilderFactory<ArrayList<JavaCompilerInput>, None, JavaCompiler>> {

	protected JavaCycleHandler(BuildCycle cycle, BuilderFactory<ArrayList<JavaCompilerInput>, None, JavaCompiler> builderFactory) {
		super(cycle, builderFactory);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean canBuildCycle(BuildCycle cycle) {
		if (!super.canBuildCycle(cycle)) {
			return false;
		}
		// Java builder needs not only all components in the cycle be a java
		// compilation task, but also that they have same compiler args and
		// target directory
		JavaCompilerInput initialInput = ((ArrayList<JavaCompilerInput>) cycle.getInitial().input).get(0);
		
		for (BuildRequest<?, ?, ?, ?> req : cycle.getCycleComponents())
			for (JavaCompilerInput input : (ArrayList<JavaCompilerInput>) req.input)
				if (!input.targetDir.equals(initialInput.targetDir)
					|| !Objects.equals(input.additionalArgs, initialInput.additionalArgs))
					return false;
		
		return true;
	}

}

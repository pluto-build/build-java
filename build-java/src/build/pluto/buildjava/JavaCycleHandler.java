package build.pluto.buildjava;

import java.util.ArrayList;
import java.util.Arrays;

import build.pluto.builder.BuildAtOnceCycleHandler;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.output.None;

/**
 * Specializes the {@link BuildAtOnceCycleHandler} for Java.
 * 
 * @author moritzlichter
 *
 */
public class JavaCycleHandler extends
		BuildAtOnceCycleHandler<JavaInput, None, JavaBuilder, BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder>> {

	protected JavaCycleHandler(BuildCycle cycle, BuilderFactory<ArrayList<JavaInput>, None, JavaBuilder> builderFactory) {
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
		JavaInput initialInput = ((ArrayList<JavaInput>) cycle.getInitial().input).get(0);
		return cycle
				.getCycleComponents()
				.stream()
				.flatMap((BuildRequest<?, ?, ?, ?> r) -> ((ArrayList<JavaInput>) r.input).stream())
				.allMatch(
						(JavaInput otherInput) -> otherInput.getTargetDir().equals(initialInput.getTargetDir())
								&& Arrays.equals(otherInput.getAdditionalArgs(), initialInput.getAdditionalArgs()));
	}

}

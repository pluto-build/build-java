package build.pluto.buildjava;

import java.util.ArrayList;
import java.util.Arrays;

import build.pluto.builder.BuildAtOnceCycleSupport;
import build.pluto.builder.BuildCycle;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.buildjava.JavaBuilder.Input;
import build.pluto.output.None;

/**
 * Specializes the {@link BuildAtOnceCycleSupport} for Java.
 * 
 * @author moritzlichter
 *
 */
public class JavaCycleSupport extends
		BuildAtOnceCycleSupport<JavaBuilder.Input, None, JavaBuilder, BuilderFactory<ArrayList<JavaBuilder.Input>, None, JavaBuilder>> {

	protected JavaCycleSupport(BuildCycle cycle, BuilderFactory<ArrayList<Input>, None, JavaBuilder> builderFactory) {
		super(cycle, builderFactory);
	}

	@Override
	public boolean canBuildCycle() {
		if (!super.canBuildCycle()) {
			return false;
		}
		// Java builder needs not only all components in the cycle be a java
		// compilation task, but also that they have same compiler args and
		// target directory
		JavaBuilder.Input initialInput = (JavaBuilder.Input) this.cycle.getInitial().input;
		return this.cycle
				.getCycleComponents()
				.stream()
				.map((BuildRequest<?, ?, ?, ?> r) -> (JavaBuilder.Input) r.input)
				.allMatch(
						(JavaBuilder.Input otherInput) -> otherInput.targetDir.equals(initialInput.targetDir)
								&& Arrays.equals(otherInput.additionalArgs, initialInput.additionalArgs));
	}

}

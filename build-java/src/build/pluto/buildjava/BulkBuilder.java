package build.pluto.buildjava;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import build.pluto.BuildUnit;
import build.pluto.builder.BuildRequest;
import build.pluto.builder.Builder;
import build.pluto.builder.BuilderFactory;
import build.pluto.dependency.FileRequirement;
import build.pluto.dependency.Requirement;
import build.pluto.output.Output;
import build.pluto.stamp.LastModifiedStamper;
import build.pluto.stamp.Stamp;

public abstract class BulkBuilder<In extends Serializable, Out extends Output, SubIn extends Serializable> extends Builder<In, Out> {

	public BulkBuilder(In input) {
		super(input);
	}

	private Map<SubIn, List<Requirement>> tracedRequired = new HashMap<>();
	private Map<SubIn, Set<File>> tracedProvided = new HashMap<>();
	
	protected abstract Collection<File> requiredFiles(In input);
	
	protected abstract Collection<SubIn> splitInput(In input, Set<File> changedFiles);
	
	protected abstract BuildRequest<
		? extends SubIn,
		? extends Output, 
		? extends Builder<SubIn, ? extends Output>, 
		? extends BuilderFactory<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>>> 
		makeSubRequest(SubIn subInput);
	
	/**
	 * Calls `require(SubIn, File)` to register the required files for each subinput.
	 * Calls `provide(SubIn, File)` to register the provided files for each subinput.
	 */
	protected abstract Out buildBulk(In input, Collection<SubIn> splitInput, Set<File> changedFiles) throws Throwable;
	
	protected void require(SubIn subInput, File file, Stamp stamp) {
		require(subInput, new FileRequirement(file, stamp));
	}
	protected void require(SubIn subInput, File file) {
		require(subInput, new FileRequirement(file, defaultStamper().stampOf(file)));
	}
	protected void require(SubIn subInput, Requirement req) {
		List<Requirement> files = tracedRequired.get(subInput);
		if (files == null) {
			files = new ArrayList<>();
			tracedRequired.put(subInput, files);
		}
		files.add(req);
	}

	protected void provide(SubIn subInput, File file) {
		Set<File> files = tracedProvided.get(subInput);
		if (files == null) {
			files = new HashSet<>();
			tracedProvided.put(subInput, files);
		}
		files.add(file);
	}

	@Override
	protected Out build(In input) throws Throwable {
		Set<File> changedFiles = new HashSet<>(requiredFiles(input));
		if (getPreviousBuildUnit() != null)
			for (Requirement req : getPreviousBuildUnit().getRequirements())
				if (req instanceof FileRequirement) {
					FileRequirement freq = (FileRequirement) req;
					if (changedFiles.contains(freq.file) && freq.isConsistent())
						changedFiles.remove(freq.file);
				}

		Collection<SubIn> splitInput = splitInput(input, changedFiles);
		
		Out out = buildBulk(input, splitInput, changedFiles);
		
		for (SubIn subInput : splitInput) 
			makeShadowBuildUnit(subInput);
		
		return out;
	}

	private void makeShadowBuildUnit(SubIn subInput) throws IOException {
		BuildRequest<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>, ? extends BuilderFactory<? extends SubIn, ? extends Output, ? extends Builder<SubIn, ? extends Output>>> 
			req = makeSubRequest(subInput);
		Builder<SubIn, ? extends Output> builder = req.createBuilder();
		File path = builder.persistentPath(subInput);
		
		BuildUnit<? extends Output> unit = BuildUnit.create(path, req);
		
		List<Requirement> required = tracedRequired.get(subInput);
		if (required != null)
			for (Requirement r : required)
				unit.requireOther(r);
		
		Set<File> provided = tracedProvided.get(subInput);
		if (provided != null)
			for (File f : provided)
				unit.generates(f, LastModifiedStamper.instance.stampOf(f));
		
		unit.setState(BuildUnit.State.SUCCESS);
		
		unit.write();
	}
}

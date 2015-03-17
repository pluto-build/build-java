import java.io.IOException;
import java.io.Serializable;

import org.sugarj.cleardep.build.Builder;
import org.sugarj.cleardep.build.BuilderFactory;
import org.sugarj.cleardep.output.None;
import org.sugarj.cleardep.stamp.LastModifiedStamper;
import org.sugarj.cleardep.stamp.Stamper;
import org.sugarj.common.Log;
import org.sugarj.common.path.AbsolutePath;
import org.sugarj.common.path.Path;


public class SomeBuilder extends Builder<SomeBuilder.SomeInput, None> {

	public static class SomeInput implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
	}
	
	public static BuilderFactory<SomeInput, None, SomeBuilder> factory = new BuilderFactory<SomeInput, None, SomeBuilder>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2193786625546374284L;

		@Override
		public SomeBuilder makeBuilder(SomeInput input) {
			return new SomeBuilder(input);
		}
	};
	
	public SomeBuilder(
			SomeInput input) {
		super(input);
	}

	@Override
	protected None build() throws IOException {
		Log.log.log("XYA SomeBuilder does stuff...", Log.ALWAYS);
		return null;
	}

	@Override
	protected Stamper defaultStamper() {
		return LastModifiedStamper.instance;
	} 

	@Override
	protected Path persistentPath() {
		return new AbsolutePath("./someBuilder.dep");
	}

	@Override
	protected String description() {
		return "Some Builder";
	}

}

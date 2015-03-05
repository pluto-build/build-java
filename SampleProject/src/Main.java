import java.io.IOException;

import org.sugarj.cleardep.build.BuildManager;
import org.sugarj.cleardep.build.BuildRequest;


public class Main {

	public static void main(String[] args) throws IOException {
		BuildManager.build(new BuildRequest<>(SomeBuilder.factory, new SomeBuilder.SomeInput()));
	}
}
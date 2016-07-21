package plugin;

import java.util.logging.Logger;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import extractors.LogExtractor;

public class Main implements IApplication{
	private static Logger log = Logger.getLogger("Main");

	@Override
	public Object start(IApplicationContext arg0) throws Exception {
		log.info("Up and Running");
		LogExtractor.extract();
		return EXIT_OK;
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}

}

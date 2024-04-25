package redecker.mickey.webcnc.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class creates a global exception handler to log uncaught exceptions for
 * debugging purposes
 * 
 * @author Mickey Redecker
 *
 */
public class GlobalExceptionHandler implements Thread.UncaughtExceptionHandler {

	private static final Logger logger = LogManager.getLogger(GlobalExceptionHandler.class);

	@Override
	public void uncaughtException(Thread thread, Throwable exception) {
		logger.error(exception);
	}

}

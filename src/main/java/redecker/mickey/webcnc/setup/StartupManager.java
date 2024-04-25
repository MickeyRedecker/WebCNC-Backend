package redecker.mickey.webcnc.setup;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import redecker.mickey.webcnc.netstate.NetworkStateCache;
import redecker.mickey.webcnc.switchcomms.manager.SwitchCommsManager;
import redecker.mickey.webcnc.switchcredstore.SwitchCredStore;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This class provides the startup procedure for the WebCNC
 * 
 * It sets launch parameters to their default or specified values
 * 
 * It fetches all switch credentials from the switchCredStore, retrieves their
 * TSN / LLDP information via the switchComms and stores it in the netStateCache
 * 
 * @author Mickey Redecker
 *
 */
@Component
public class StartupManager implements ApplicationRunner {

	private static final Logger logger = LogManager.getLogger(StartupManager.class);

	@Override
	public void run(ApplicationArguments args) throws Exception {

		logger.info("Startup procedure starting");
		WebcncApplication.netstatecache = new NetworkStateCache();
		WebcncApplication.switchcredstore = new SwitchCredStore("./switchconfig.txt");
		WebcncApplication.switchComms = new SwitchCommsManager();

		// set password
		String password = "admin"; // Default password
		if (args.containsOption("password")) {
			List<String> values = args.getOptionValues("password");
			if (values != null && !values.isEmpty()) {
				password = values.get(0);
			}
		}
		WebcncApplication.password = password;

		// set switchConnectionRetries
		Integer switchConnectionRetries = 3; // default switchConnectionTimeout
		if (args.containsOption("switchConnectionRetries")) {
			List<String> values = args.getOptionValues("switchConnectionRetries");
			if (values != null && !values.isEmpty()) {
				try {
					switchConnectionRetries = Integer.parseInt(values.get(0));
					if (switchConnectionRetries < 1 || switchConnectionRetries > 100) {
						switchConnectionRetries = 3000;
						logger.warn("Invalid switchConnectionRetries value provided, using default value: " + switchConnectionRetries);
					}
				} catch (NumberFormatException e) {
					logger.warn("Invalid switchConnectionRetries value provided, using default value: " + switchConnectionRetries);
				}
			}
		}
		WebcncApplication.switchConnectionRetries = switchConnectionRetries;

		// set switchConnectionTimeout
		Integer switchConnectionTimeout = 3000; // default switchConnectionTimeout
		if (args.containsOption("switchConnectionTimeout")) {
			List<String> values = args.getOptionValues("switchConnectionTimeout");
			if (values != null && !values.isEmpty()) {
				try {
					switchConnectionTimeout = Integer.parseInt(values.get(0));
					if (switchConnectionTimeout < 1 || switchConnectionTimeout > 1000000) {
						switchConnectionTimeout = 3000;
						logger.warn("Invalid switchConnectionTimeout value provided, using default value: " + switchConnectionTimeout);
					}
				} catch (NumberFormatException e) {
					logger.warn("Invalid switchConnectionTimeout value provided, using default value: " + switchConnectionTimeout);
				}
			}
		}
		WebcncApplication.switchConnectionTimeout = switchConnectionTimeout;

		// fetch switch info from persistent storage
		List<SwitchInfo> switchInfoList = WebcncApplication.switchcredstore.getAllSwitchInfo();
		List<Switch> switches = new LinkedList<Switch>();

		// fetch switch data for each switch
		for (SwitchInfo switchInfo : switchInfoList) {
			try {
				Switch newSwitch = WebcncApplication.switchComms.getNewSwitchInformation(switchInfo);
				switches.add(newSwitch);
				logger.info("Switch " + newSwitch.getSwitchIdentifier() + " data retrieved successfully");
			}
			// save unreachable dummy switch
			catch (CommsException e) {
				
				Switch newSwitch = Switch.makeUnreachableDummy(switchInfo);
				switches.add(newSwitch);
				logger.warn("Couldn´t retrieve data for switch " + newSwitch.getSwitchIdentifier()
						+ " , unreachable dummy switch added");
			}
		}
		WebcncApplication.netstatecache.replaceAllSwitches(switches);
		logger.info("Startup procedure finished");

	}
}

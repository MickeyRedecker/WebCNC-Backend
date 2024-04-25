package redecker.mickey.webcnc.restcontroller;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This class provides an API call to reload the backend from the frontend
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/reloadbackend")
public class ReloadBackendController {

	private static final Logger logger = LogManager.getLogger(ReloadBackendController.class);

	/**
	 * This function re-fetches all TSN and LLDP data from the stored switches,
	 * stores it in the netstatecache and sends it to the frontend
	 * 
	 * @param passwordHeader The WebCNC password for permission control
	 * @return A list of all switches with the new LLDP / TSN information
	 */
	@PostMapping
	public ResponseEntity<String> reloadBackend(@RequestHeader("webcncpassword") String passwordHeader) {

		logger.info("reloadbackend API call received");

		if (!passwordHeader.equals(WebcncApplication.password)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
		}

		// fetch switch info from persistent storage
		List<SwitchInfo> switchInfoList = WebcncApplication.switchcredstore.getAllSwitchInfo();
		List<Switch> switches = new LinkedList<Switch>();

		// fetch data for each switch
		for (SwitchInfo switchInfo : switchInfoList) {
			try {
				Switch newSwitch = WebcncApplication.switchComms.getNewSwitchInformation(switchInfo);
				switches.add(newSwitch);
			}
			// save unreachable dummy switch
			catch (CommsException e) {
				logger.warn("Couldn´t reach " + switchInfo.getSwitchIdentifier() + ", adding unreachable dummy switch");
				
				Switch newSwitch = Switch.makeUnreachableDummy(switchInfo);
				switches.add(newSwitch);
			}
		}
		WebcncApplication.netstatecache.replaceAllSwitches(switches);

		return ResponseEntity.ok("Backend reloaded successfully");
	}

}

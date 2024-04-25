package redecker.mickey.webcnc.restcontroller;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This class provides an API call to add new switches from the frontend
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/addswitch")
public class AddSwitchController {

	private static final Logger logger = LogManager.getLogger(AddSwitchController.class);

	/**
	 * This function receives SNMPv3 credentials of a switch from the frontend to
	 * create a new switch and retrieve its TSN and LLDP data
	 * 
	 * @param newSwitchInfo  the SNMPv3 credentials of the new switch
	 * @param passwordHeader the WebCNC password for permission control
	 * @return The new switch data or an error message
	 */
	@PutMapping
	public ResponseEntity<?> addSwitchController(@RequestBody SwitchInfo newSwitchInfo,
			@RequestHeader("webcncpassword") String passwordHeader) {

		logger.info("addswitch API call received");

		if (!passwordHeader.equals(WebcncApplication.password)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
		}

		try {
			// make sure the identifier is unique
			List<String> identifiers = WebcncApplication.netstatecache.getAllSwitchIdentifiers();
			for (String id : identifiers) {
				if (newSwitchInfo.getSwitchIdentifier().equals(id)) {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.body("A switch with this identifier already exists");
				}
			}

			// fetch data for new switch
			Switch newSwitch = WebcncApplication.switchComms.getNewSwitchInformation(newSwitchInfo);

			// save new switch in netstatecache
			WebcncApplication.netstatecache.addSwitch(newSwitch);

			// save new switch info to persistent storage
			WebcncApplication.switchcredstore.addSwitchToConfig(newSwitch);

			// send new switch to frontend
			return ResponseEntity.ok(newSwitch);
		}

		catch (CommsException e) {
			logger.catching(e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Couldn´t reach switch");
		} catch (Exception e) {
			logger.catching(e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unknown error occured");
		}

	}

}

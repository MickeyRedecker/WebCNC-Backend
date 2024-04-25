package redecker.mickey.webcnc.restcontroller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;

/**
 * This class provides an API call to remove a switch permanently from the
 * backend
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/removeswitch")
public class RemoveSwitchController {

	private static final Logger logger = LogManager.getLogger(RemoveSwitchController.class);

	/**
	 * Deletes a switch permanently from the backend
	 * 
	 * @param switchIdentifier The switch to delete
	 * @param passwordHeader   The WebCNC password for permission control
	 * @return an acknowledgement or error message
	 */
	@DeleteMapping
	public ResponseEntity<String> deleteSwitch(@RequestHeader("removeswitch") String switchIdentifier,
			@RequestHeader("webcncpassword") String passwordHeader) {

		logger.info("removeswitch API call received for switch " + switchIdentifier);
		if (!passwordHeader.equals(WebcncApplication.password)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
		}

		try {

			WebcncApplication.switchcredstore.removeSwitchFromConfig(switchIdentifier);
			boolean isDeleted = WebcncApplication.netstatecache.removeSwitch(switchIdentifier);

			if (isDeleted) {
				return ResponseEntity.ok().body("Switch successfully deleted");
			} else {
				// Handle the case where the switch could not be found or not deleted for some
				// reason
				return ResponseEntity.badRequest().body("Error: Switch does not exist in the backend");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.internalServerError().body("An unknown error occurred");
		}
	}

}

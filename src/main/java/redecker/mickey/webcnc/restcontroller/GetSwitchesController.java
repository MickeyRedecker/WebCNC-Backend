package redecker.mickey.webcnc.restcontroller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;

/**
 * This class provides an API call to fetch all switch information from the
 * backend to the frontend
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/getswitches")
public class GetSwitchesController {

	private static final Logger logger = LogManager.getLogger(GetSwitchesController.class);

	/**
	 * This function sends the stored switch information for all switches (except
	 * the SNMPv3 credentials) to the frontend
	 * 
	 * @param passwordHeader The WebCNC password for permission control
	 * @return A list of all switches and their TSN / LLDP information
	 */
	@GetMapping
	public ResponseEntity<?> getSwitches(@RequestHeader("webcncpassword") String passwordHeader) {

		logger.info("getswitches API call received");

		// Check if the password is correct
		if (!passwordHeader.equals(WebcncApplication.password)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
		}

		return ResponseEntity.ok(WebcncApplication.netstatecache.getAllSwitches());
	}

}

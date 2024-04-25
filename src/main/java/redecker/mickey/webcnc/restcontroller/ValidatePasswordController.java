package redecker.mickey.webcnc.restcontroller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;

/**
 * This class provides an API call to validate the password of the WebCNC
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/validatepassword")
public class ValidatePasswordController {

	private static final Logger logger = LogManager.getLogger(ValidatePasswordController.class);

	/**
	 * This function is used to validate a received password against the WebCNC
	 * password
	 * 
	 * @param password The received password to validate
	 * @return an acknowledgement or error message
	 */
	@PostMapping
	public ResponseEntity<String> validatePassword(@RequestBody String password) {
		logger.info("validatepassword API call received");
		if (WebcncApplication.password.equals(password)) {
			return ResponseEntity.ok("Password is correct");
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
		}
	}

}

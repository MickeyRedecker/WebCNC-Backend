package redecker.mickey.webcnc.restcontroller;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import redecker.mickey.webcnc.setup.WebcncApplication;
import redecker.mickey.webcnc.types.Port;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This class provides an API call to save a new TSN configuration for a port
 * and apply it on the switch
 * 
 * @author Mickey Redecker
 *
 */
@RestController
@RequestMapping("/webcnc/api/saveport")
public class SavePortController {

	private static final Logger logger = LogManager.getLogger(SavePortController.class);

	/**
	 * Saves a new TSN configuration to the specified port
	 * 
	 * @param port           The port with its new TSN configuration
	 * @param passwordHeader The WebCNC password for permission control
	 * @return An acknowledgement or error message
	 */
	@PostMapping
	public ResponseEntity<String> savePortConfig(@RequestBody Port port,
			@RequestHeader("webcncpassword") String passwordHeader) {
		logger.info("saveport API call received");
		if (!passwordHeader.equals(WebcncApplication.password)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password is incorrect");
		}
		try {
			// ensure that switch exists
			boolean switchExists = false;
			List<String> identifiers = WebcncApplication.netstatecache.getAllSwitchIdentifiers();
			for (String id : identifiers) {
				if (id.equals(port.getSwitchIdentifier())) {
					switchExists = true;
				}
			}

			if (!switchExists) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This switch does not exist in the backend");
			}
			
			Switch portSwitch = WebcncApplication.netstatecache.getSwitch(port.getSwitchIdentifier());
			WebcncApplication.switchComms.setPortParameters(port, portSwitch);
			
			//update netStateCache
			List<Port> newPorts = new LinkedList<Port>();
			for(Port oldPort: portSwitch.getTsnPorts()) {
				if(oldPort.getPortNumber().intValue() != port.getPortNumber().intValue()) {
					newPorts.add(oldPort);
				}
				else {
					newPorts.add(port);
				}
			}
			portSwitch.setTsnPorts(newPorts);
			WebcncApplication.netstatecache.replaceSwitch(portSwitch);
			
			return ResponseEntity.ok("Port configuration saved successfully.");
		} catch (CommsException e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body("Couldn´t set values on switch: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("an unknown error occured");
		}
	}

}

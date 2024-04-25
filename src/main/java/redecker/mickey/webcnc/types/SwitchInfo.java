package redecker.mickey.webcnc.types;

/**
 * This class represents the information required to gather TSN and LLDP data
 * from the switch via SNMPv3
 * 
 * @author Mickey Redecker
 *
 */
public class SwitchInfo {

	private String switchIdentifier; //a unique identifier of the switch. This atribute is required by most other components.
	private String address; // must be of form a:b:c:d where a, b, c, d between 0 and 255
	private Integer port; // must be a number > 0
	private String authUserName;
	private String authAlgorithm;
	private String authPassword;
	private String encryptAlgorithm;
	private String encryptPassword;
	private String tsnPortsString; // is of form n,m,o,... where n, m, o are positive numbers

	/**
	 * 
	 * @param switchIdentifier a unique identifier
	 * @param address          the IP address of the switch
	 * @param port             The SNMP-UDP port of the switch
	 * @param authUserName     the SNMPv3 authentication user name
	 * @param authAlgorithm    the SNMPv3 authentication algorithm
	 * @param authPassword     the SNMPv3 authentication password
	 * @param encryptAlgorithm the SNMPv3 encryption Algorithm
	 * @param encryptPassword  the SNMPv3 encryption password
	 * @param tsnPortsString   a String indicating the TSN-capable ports on the
	 *                         switch
	 */
	public SwitchInfo(String switchIdentifier, String address, Integer port, String authUserName, String authAlgorithm,
			String authPassword, String encryptAlgorithm, String encryptPassword, String tsnPortsString) {

		if (switchIdentifier == null) {
			throw new IllegalArgumentException("switchIdentifier cannot be null");
		} else if (!checkForIpAddress(address)) {
			throw new IllegalArgumentException("Invalid address format");
		} else if (port <= 0) {
			throw new IllegalArgumentException("port must be a number greater than 0");
		} else if (authUserName == null) {
			throw new IllegalArgumentException("authUerName cannot be null");
		} else if (authAlgorithm == null) {
			throw new IllegalArgumentException("authAlgorithm cannot be null");
		} else if (authPassword == null) {
			throw new IllegalArgumentException("authPassword cannot be null");
		} else if (encryptAlgorithm == null) {
			throw new IllegalArgumentException("encryptAlgorithm cannot be null");
		} else if (encryptPassword == null) {
			throw new IllegalArgumentException("encryptPassword cannot be null");
		} else if (!checkForValidTSNPortsList(tsnPortsString)) {
			throw new IllegalArgumentException("TSN Ports List has invalid format");
		}

		this.switchIdentifier = switchIdentifier;
		this.address = address;
		this.port = port;
		this.authUserName = authUserName;
		this.authAlgorithm = authAlgorithm;
		this.authPassword = authPassword;
		this.encryptAlgorithm = encryptAlgorithm;
		this.encryptPassword = encryptPassword;
		this.tsnPortsString = tsnPortsString;
	}

	// getters and setters
	public String getTSNPortsString() {
		return tsnPortsString;
	}

	public void setTSNPortsString(String newPortsString) {
		if (!checkForValidTSNPortsList(tsnPortsString)) {
			throw new IllegalArgumentException("TSN Ports List has invalid format");
		}
		this.tsnPortsString = newPortsString;
	}

	public String getEncryptPassword() {
		return encryptPassword;
	}

	public void setEncryptPassword(String encryptPassword) {
		this.encryptPassword = encryptPassword;
	}

	public String getEncryptAlgorithm() {
		return encryptAlgorithm;
	}

	public void setEncryptAlgorithm(String encryptAlgorithm) {
		this.encryptAlgorithm = encryptAlgorithm;
	}

	public String getAuthUserName() {
		return authUserName;
	}

	public void setAuthUserName(String authUserName) {
		this.authUserName = authUserName;
	}

	public String getSwitchIdentifier() {
		return switchIdentifier;
	}

	public void setSwitchIdentifier(String switchIdentifier) {
		if (switchIdentifier == null)
			throw new IllegalArgumentException("switchIdentifier cannot be null");
		this.switchIdentifier = switchIdentifier;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		if (!checkForIpAddress(address))
			throw new IllegalArgumentException("Invalid address format");
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		if (port <= 0)
			throw new IllegalArgumentException("port must be a number greater than 0");
		this.port = port;
	}

	public String getAuthAlgorithm() {
		return authAlgorithm;
	}

	public void setAuthAlgorithm(String authAlgorithm) {
		if (authAlgorithm == null)
			throw new IllegalArgumentException("authAlgorithm cannot be null");
		this.authAlgorithm = authAlgorithm;
	}

	public String getAuthPassword() {
		return authPassword;
	}

	public void setAuthPassword(String authPassword) {
		if (authPassword == null)
			throw new IllegalArgumentException("authPassword cannot be null");
		this.authPassword = authPassword;
	}

	/**
	 * Tests if a String is a valid tsnPortsString
	 * 
	 * @param address the String to test
	 * @return true if it is a valid tsnPortsString, false else
	 */
	private boolean checkForValidTSNPortsList(String portsString) {
		if (portsString == null) {
			return false;
		}
		if (portsString.trim().equals("")) {
			return true;
		}
		String[] portNumbers = portsString.split(",");

		for (String portNumberString : portNumbers) {
			try {
				int portNumber = Integer.valueOf(portNumberString);
				if (portNumber < 0) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests if a String is a valid IP address
	 * 
	 * @param address the String to test
	 * @return true if it is a valid IP address, false else
	 */
	private boolean checkForIpAddress(String address) {
		if (address == null) {
			return false;
		}
		String[] addressElements = address.split("\\.");
		// IP address consists of 4 elements
		if (addressElements.length == 4) {
			try {
				// check if any element is invalid, return false
				for (String addressElementString : addressElements) {
					int addressElement = Integer.valueOf(addressElementString);
					if (addressElement < 0) {
						return false;
					} else if (addressElement > 255) {
						return false;
					}
				}
			} catch (NumberFormatException e) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}
}

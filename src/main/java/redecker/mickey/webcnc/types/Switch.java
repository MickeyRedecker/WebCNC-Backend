package redecker.mickey.webcnc.types;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import redecker.mickey.webcnc.types.enums.AuthAlgorithm;
import redecker.mickey.webcnc.types.enums.EncryptionAlgorithm;

/**
 * This class represents a TSN capable switch with a unique identifier, its SNMP
 * credentials, its TSN ports, and network system name information about itself
 * and neighbors that were gathered via LLDP by the switch
 * 
 * @author Mickey Redecker
 *
 */
public class Switch {

	private String switchIdentifier; //a unique identifier of the switch. This atribute is required by most other components.
	private String address; // The IP-address of the switch, must be of form a.b.c.d where a, b, c, d
							// between 0 and 255
	private String sysname;
	private List<String> neighborSysNames; // list positions of sysnames, port ids and localport must match
	private List<String> neighborPortIds;
	private List<Integer> neighborLocalPorts;
	@JsonIgnore
	private Integer port; // the SNMP-UDP-port of the switch, must be a number > 0
	@JsonIgnore
	private String authUserName;
	@JsonIgnore
	private AuthAlgorithm authAlgorithm;
	@JsonIgnore
	private String authPassword;
	@JsonIgnore
	private EncryptionAlgorithm encryptAlgorithm;
	@JsonIgnore
	private String encryptPassword;
	private List<Port> tsnPorts;
	private boolean reachable;

	/**
	 * 
	 * @param switchIdentifier   a unique identifier
	 * @param address            The IP address of the switch
	 * @param port               The UDPÜ port of the switch
	 * @param sysname            The switches system name
	 * @param neighborSysNames   A list of system names of the switches neighbor
	 *                           devices, list position must match local port and
	 *                           port id
	 * @param neighborPortIds    A list of port ids (usually MAC Adresses) of the
	 *                           switches neighbor devices, list position must match
	 *                           local port and sysname
	 * @param neighborLocalPorts A list of the switches port numbers to which the
	 *                           switches neighbor devices are connected, list
	 *                           position must match sysnames and port id
	 * @param authUserName       the SNMPv3 authentication user name
	 * @param authAlgorithm      the SNMPv3 authentication algorithm
	 * @param authPassword       the SNMPv3 authentication password
	 * @param encryptAlgorithm   the SNMPv3 encryption Algorithm
	 * @param encryptPassword    the SNMPv3 encryption password
	 * @param TSNPorts           a list of TSN-capable Ports of the switch
	 * @param isReachable        Indicates whether the switch is reachable or not
	 */
	public Switch(String switchIdentifier, String address, Integer port, String sysname, List<String> neighborSysNames,
			List<String> neighborPortIds, List<Integer> neighborLocalPorts, String authUserName,
			AuthAlgorithm authAlgorithm, String authPassword, EncryptionAlgorithm encryptAlgorithm,
			String encryptPassword, List<Port> TSNPorts, boolean isReachable) {

		if (switchIdentifier == null) {
			throw new IllegalArgumentException("switchIdentifier cannot be null");
		} else if (!checkForIpAddress(address)) {
			throw new IllegalArgumentException("Invalid address format");
		} else if (port <= 0) {
			throw new IllegalArgumentException("port must be a number greater than 0");
		} else if (sysname == null) {
			throw new IllegalArgumentException("sysname cannot be null");
		} else if (neighborSysNames == null) {
			throw new IllegalArgumentException("neighborSysNames cannot be null");
		} else if (neighborLocalPorts == null) {
			throw new IllegalArgumentException("neighborLocalPorts cannot be null");
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
		} else if (TSNPorts == null) {
			throw new IllegalArgumentException("TSNPorts cannot be null");
		}

		this.switchIdentifier = switchIdentifier;
		this.address = address;
		this.port = port;
		this.sysname = sysname;
		this.neighborSysNames = neighborSysNames;
		this.neighborPortIds = neighborPortIds;
		this.neighborLocalPorts = neighborLocalPorts;
		this.authUserName = authUserName;
		this.authAlgorithm = authAlgorithm;
		this.authPassword = authPassword;
		this.encryptAlgorithm = encryptAlgorithm;
		this.encryptPassword = encryptPassword;
		this.tsnPorts = TSNPorts;
		this.reachable = isReachable;
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

	// getters and setters with guard statements
	public String getEncryptPassword() {
		return encryptPassword;
	}

	public void setEncryptPassword(String encryptPassword) {
		this.encryptPassword = encryptPassword;
	}

	public EncryptionAlgorithm getEncryptAlgorithm() {
		return encryptAlgorithm;
	}

	public void setEncryptAlgorithm(EncryptionAlgorithm encryptAlgorithm) {
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

	public String getSysname() {
		return sysname;
	}

	public void setSysname(String sysname) {
		if (sysname == null)
			throw new IllegalArgumentException("sysname cannot be null");
		this.sysname = sysname;
	}

	public List<String> getNeighborSysNames() {

		// Deep copy neighborSysNames
		List<String> deepCopyNeighborSysNames = new LinkedList<>();
		for (String neighbor : neighborSysNames) {
			deepCopyNeighborSysNames.add(neighbor);
		}
		return deepCopyNeighborSysNames;
	}

	public void setNeighborSysNames(List<String> neighborSysNames) {
		if (neighborSysNames == null)
			throw new IllegalArgumentException("neighborSysNames cannot be null");
		this.neighborSysNames = neighborSysNames;
	}

	public List<String> getNeighborPortIds() {

		// Deep copy neighborPortIds
		List<String> deepCopyNeighborPortIds = new LinkedList<>();
		for (String neighbor : neighborPortIds) {
			deepCopyNeighborPortIds.add(neighbor);
		}
		return deepCopyNeighborPortIds;
	}

	public void setNeighborPortIds(List<String> neighborPortIds) {
		if (neighborPortIds == null)
			throw new IllegalArgumentException("neighborMacAdresses cannot be null");
		this.neighborSysNames = neighborPortIds;
	}

	public List<Integer> getNeighborLocalPorts() {

		// Deep copy neighborLocalPorts
		List<Integer> deepCopyNeighborLocalPorts = new LinkedList<>();
		for (Integer neighbor : neighborLocalPorts) {
			deepCopyNeighborLocalPorts.add(neighbor);
		}
		return deepCopyNeighborLocalPorts;
	}

	public void setNeighborLocalPorts(List<Integer> neighborLocalPorts) {
		if (neighborLocalPorts == null)
			throw new IllegalArgumentException("neighborLocalPorts cannot be null");
		this.neighborLocalPorts = neighborLocalPorts;
	}

	public List<Port> getTsnPorts() {

		// Deep copy TSNPorts
		List<Port> deepCopyTSNPorts = new LinkedList<>();
		for (Port port : tsnPorts) {
			Port newPort = port.makeDeepCopy();
			deepCopyTSNPorts.add(newPort);
		}
		return deepCopyTSNPorts;
	}

	public void setTsnPorts(List<Port> TSNPorts) {
		if (TSNPorts == null)
			throw new IllegalArgumentException("TSNPorts cannot be null");
		this.tsnPorts = TSNPorts;
	}

	public boolean isReachable() {
		return reachable;
	}

	public void setReachable(boolean isReachable) {
		this.reachable = isReachable;
	}

	public AuthAlgorithm getAuthAlgorithm() {
		return authAlgorithm;
	}

	public void setAuthAlgorithm(AuthAlgorithm authAlgorithm) {
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
	 * Creates a deepcopy of the Switch
	 * 
	 * @return deepcopy of the object
	 */
	public Switch makeDeepCopy() {

		// Deep copy neighborSysNames
		List<String> deepCopyNeighborSysNames = new LinkedList<>();
		for (String neighbor : neighborSysNames) {
			deepCopyNeighborSysNames.add(neighbor);
		}

		// Deep copy neighborPortIds
		List<String> deepCopyNeighborPortIds = new LinkedList<>();
		for (String neighbor : neighborPortIds) {
			deepCopyNeighborPortIds.add(neighbor);
		}

		// Deep copy neighborLocalPorts
		List<Integer> deepCopyNeighborLocalPorts = new LinkedList<>();
		for (Integer neighbor : neighborLocalPorts) {
			deepCopyNeighborLocalPorts.add(neighbor);
		}

		// Deep copy TSNPorts
		List<Port> deepCopyTSNPorts = new LinkedList<>();
		for (Port port : tsnPorts) {
			Port newPort = port.makeDeepCopy();
			deepCopyTSNPorts.add(newPort);
		}

		return new Switch(this.getSwitchIdentifier(), this.getAddress(), this.getPort(), this.getSysname(),
				deepCopyNeighborSysNames, deepCopyNeighborPortIds, deepCopyNeighborLocalPorts, this.authUserName,
				this.authAlgorithm, this.authPassword, this.encryptAlgorithm, this.encryptPassword, deepCopyTSNPorts,
				this.isReachable());
	}
	
	/**
	 * Generates an unreachable dummy Switch-Object based on the persistently stored Information from a SwitchInfo-Objekt
	 * 
	 * @param switchInfo The SwitchInfo used to generate the dummy switch
	 * @return a dummy switch
	 */
	public static Switch makeUnreachableDummy(SwitchInfo switchInfo) {
		// gather data from switchInfo to populate dummy switch
		String switchIdentifier = switchInfo.getSwitchIdentifier();
		String address = switchInfo.getAddress();
		Integer port = switchInfo.getPort();
		String authUserName = switchInfo.getAuthUserName();
		String authAlgorithmString = switchInfo.getAuthAlgorithm();
		String authPassword = switchInfo.getAuthPassword();
		String encryptAlgorithmString = switchInfo.getEncryptAlgorithm();
		String encryptPassword = switchInfo.getEncryptAlgorithm();

		// parse auth Algorithm
		AuthAlgorithm authAlgorithm;
		if (authAlgorithmString.equals("MD5")) {
			authAlgorithm = AuthAlgorithm.MD5;
		} else {
			authAlgorithm = AuthAlgorithm.SHA1;
		}

		// parse Encryption Algorithm
		EncryptionAlgorithm encryptAlgorithm;
		if (encryptAlgorithmString.equals("DES")) {
			encryptAlgorithm = EncryptionAlgorithm.DES;
		} else {
			encryptAlgorithm = EncryptionAlgorithm.AES128;
		}

		Switch newSwitch = new Switch(switchIdentifier, address, port, "", new LinkedList<String>(),
				new LinkedList<String>(), new LinkedList<Integer>(), authUserName, authAlgorithm, authPassword,
				encryptAlgorithm, encryptPassword, new LinkedList<Port>(), false);
		return newSwitch;
	}

}

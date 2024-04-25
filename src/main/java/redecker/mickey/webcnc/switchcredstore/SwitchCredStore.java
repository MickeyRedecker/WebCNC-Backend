package redecker.mickey.webcnc.switchcredstore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import redecker.mickey.webcnc.types.Port;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.enums.AuthAlgorithm;
import redecker.mickey.webcnc.types.enums.EncryptionAlgorithm;

/**
 * This class implements the ISwitchCredStore interface with a simple text file
 * as persistent storage
 * 
 * It ensures thread-safe operation with the use of a monitor
 * 
 * @author Mickey Redecker
 *
 */
public class SwitchCredStore implements ISwitchCredStore {

	private static final Logger logger = LogManager.getLogger(SwitchCredStore.class);

	File file;

	public SwitchCredStore(String switchConfigFilePath) {
		file = new File(switchConfigFilePath);
	}

	@Override
	public synchronized List<SwitchInfo> getAllSwitchInfo() {

		List<SwitchInfo> gatheredSwitchData = new LinkedList<SwitchInfo>();

		// if switches file doesn´t exist, create and fill with examples
		if (!file.exists()) {
			try {
				file.createNewFile();
				BufferedWriter writer = new BufferedWriter(new FileWriter(file));

				writer.write("# This is an example file for configuring switches");
				writer.newLine();
				writer.write("# Each instance should be formatted as shown below");
				writer.newLine();
				writer.write("# switch identifiers can only contain alphanumeric characters");
				writer.newLine();
				writer.write("# Lines starting with # are not interpreted as switch data");
				writer.newLine();
				writer.newLine();
				writer.write("# switchIdentifier1:");
				writer.newLine();
				writer.write("# address = 192.0.0.1");
				writer.newLine();
				writer.write("# snmpport = 161");
				writer.newLine();
				writer.write("# authUserName = mySNMPUserName");
				writer.newLine();
				writer.write("# authAlgorithm = MD5");
				writer.newLine();
				writer.write("# authPassWord = mySNMPAuthPassword");
				writer.newLine();
				writer.write("# encryptAlgorithm = DES");
				writer.newLine();
				writer.write("# encryptPassword = myEncryptionPassword");
				writer.newLine();
				writer.write("# tsnPorts = 1,2,3,4");
				writer.newLine();
				writer.newLine();
				writer.write("# switchIdentifier2:");
				writer.newLine();
				writer.write("# address = 192.0.0.2");
				writer.newLine();
				writer.write("# snmpport = 161");
				writer.newLine();
				writer.write("# authUserName = myOtherSNMPUserName");
				writer.newLine();
				writer.write("# authAlgorithm = SHA1");
				writer.newLine();
				writer.write("# authPassWord = myOtherSNMPAuthPassword2");
				writer.newLine();
				writer.write("# encryptAlgorithm = AES128");
				writer.newLine();
				writer.write("# encryptPassword = myOtherEncryptionPassword2");
				writer.newLine();
				writer.write("# tsnPorts = 1,2,5,6");
				writer.newLine();

				writer.close();
			} catch (IOException e) {
				logger.catching(e);
				throw new IllegalStateException("issue reading/writing to switch config file");
			}
		}

		// file exists, read switch data
		else {
			try {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String line;
				String readIdentifier = null;
				String readAddress = null;
				Integer readPort = null;
				String readAuthUserName = null;
				String readAuthAlgorithm = null;
				String readAuthPassword = null;
				String readEncryptAlgorithm = null;
				String readEncryptPassword = null;
				String readTSNPortsString = null;
				Boolean firstSwitch = true;

				while ((line = br.readLine()) != null) {

					// skip comented line
					if (line.startsWith("#")) {
						continue;
					}
					// new switch section starts
					if (line.contains(":") && !line.contains("#")) {

						// check if previous read switch data needs to be saved
						if (firstSwitch) {
							firstSwitch = false;
						}

						// not the first switch in file, check and save previous switch
						else {

							// check if switch ID is unique
							for (SwitchInfo previousSwitch : gatheredSwitchData) {
								if (readIdentifier == previousSwitch.getSwitchIdentifier()) {
									throw new IllegalStateException(
											"config file contains duplicate switch identifiers");
								}
							}

							// check if all switch data has been retrieved
							if (readIdentifier == null | readAddress == null | readPort == null
									| readAuthUserName == null | readAuthAlgorithm == null | readAuthPassword == null
									| readEncryptAlgorithm == null | readEncryptPassword == null
									| readTSNPortsString == null) {
								if (readIdentifier == null) {
									throw new IllegalStateException("config file is invalid");
								} else {
									throw new IllegalStateException(
											"config file is invalid at section " + readIdentifier);
								}
							}

							// save previous switch
							SwitchInfo newSwitchInfo = new SwitchInfo(readIdentifier, readAddress, readPort,
									readAuthUserName, readAuthAlgorithm, readAuthPassword, readEncryptAlgorithm,
									readEncryptPassword, readTSNPortsString);
							gatheredSwitchData.add(newSwitchInfo);

						}

						// read identifier, reset other values
						String[] parts = line.split(":");
						if (parts.length < 1) {
							throw new IllegalStateException("config file invalid");
						}
						readIdentifier = parts[0].trim();
						readAddress = null;
						readPort = null;
						readAuthUserName = null;
						readAuthAlgorithm = null;
						readAuthPassword = null;
						readEncryptAlgorithm = null;
						readEncryptPassword = null;
						readTSNPortsString = null;
					}

					// value within switch section
					else if (line.contains("=")) {
						String[] parts = line.split("=", 2);
						if (parts.length < 1) {
							throw new IllegalStateException("config file has invalid switch parameter line");
						}

						// determine which value is beind read, process accordingly
						String parameter = parts[0].trim().toLowerCase();
						switch (parameter) {

						case "address":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid address line");
							}
							if (!checkForIpAddress(parts[1].trim())) {
								throw new IllegalStateException("config file has invalid ip address in address line");
							}
							readAddress = parts[1].trim();
							break;

						case "snmpport":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid snmpport line");
							}
							try {
								readPort = Integer.valueOf(parts[1].trim());
							} catch (NumberFormatException e) {
								throw new IllegalStateException("config file contains invalid snmpport parameter");
							}
							break;

						case "authusername":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid authUserName line");
							}
							readAuthUserName = parts[1].trim();
							break;

						case "authalgorithm":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid authAlgorithm line");
							}
							if (parts[1].trim().equals("MD5") && parts[1].trim().equals("SHA1")) {
								throw new IllegalStateException("config file contains unsupported AuthAlgorithm");
							}
							readAuthAlgorithm = parts[1].trim();
							break;

						case "authpassword":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid authPassword line");
							}
							readAuthPassword = parts[1].trim();
							break;

						case "encryptalgorithm":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid encryptAlgorithm line");
							}
							if (parts[1].trim().equals("DES") && parts[1].trim().equals("AES128")) {
								throw new IllegalStateException("config file contains unsupported encryptAlgorithm");
							}
							readEncryptAlgorithm = parts[1].trim();
							break;

						case "encryptpassword":
							if (parts.length != 2) {
								throw new IllegalStateException("config file has invalid encryptPassword line");
							}
							readEncryptPassword = parts[1].trim();
							break;

						case "tsnports":
							if (parts.length == 1) {
								readTSNPortsString = "";
							} else if (parts.length == 2) {
								if (parts[1] == null) {
									readTSNPortsString = "";
									break;
								}
								if (!checkForValidTSNPortsList(parts[1].trim())) {
									throw new IllegalStateException("config file contains invalid tsnPorts line");
								}
								readTSNPortsString = parts[1].trim();
							} else {
								throw new IllegalStateException("config file contains invalid tsnPorts line");
							}
							break;

						}

					}

				}

				// save last read switch
				if (!firstSwitch) {

					// check if switch ID is unique
					for (SwitchInfo previousSwitch : gatheredSwitchData) {
						if (readIdentifier.equals(previousSwitch.getSwitchIdentifier())) {
							throw new IllegalStateException("config file contains duplicate switch identifiers");
						}
					}

					// check if all switch data has been retrieved
					if (readIdentifier == null | readAddress == null | readPort == null | readAuthUserName == null
							| readAuthAlgorithm == null | readAuthPassword == null | readEncryptAlgorithm == null
							| readEncryptPassword == null | readTSNPortsString == null) {
						if (readIdentifier == null) {
							throw new IllegalStateException("config file is invalid");
						} else {
							throw new IllegalStateException("config file is invalid at section " + readIdentifier);
						}
					}

					// save previous switch
					SwitchInfo newSwitchInfo = new SwitchInfo(readIdentifier, readAddress, readPort, readAuthUserName,
							readAuthAlgorithm, readAuthPassword, readEncryptAlgorithm, readEncryptPassword,
							readTSNPortsString);
					gatheredSwitchData.add(newSwitchInfo);

				}
				br.close();
			} catch (IOException e) {
				logger.catching(e);
				throw new IllegalStateException("issue reading/writing to switch config file");
			}
		}
		return gatheredSwitchData;
	}

	@Override
	public synchronized void addSwitchToConfig(Switch switchToAdd) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

			// prepare values that are not available strings already
			String authAlgorithm;
			if (switchToAdd.getAuthAlgorithm() == AuthAlgorithm.MD5) {
				authAlgorithm = "MD5";
			} else {
				authAlgorithm = "SHA1";
			}
			String encryptAlgorithm;
			if (switchToAdd.getEncryptAlgorithm() == EncryptionAlgorithm.DES) {
				encryptAlgorithm = "DES";
			} else {
				encryptAlgorithm = "AES128";
			}
			String tsnPorts = "";
			for (Port port : switchToAdd.getTsnPorts()) {
				if (tsnPorts.equals("")) {
					tsnPorts = port.getPortNumber().toString();
				} else {
					tsnPorts = tsnPorts + "," + port.getPortNumber().toString();
				}
			}

			// append values to switch config file
			writer.newLine();
			writer.write(switchToAdd.getSwitchIdentifier() + ":");
			writer.newLine();
			writer.write("address = " + switchToAdd.getAddress());
			writer.newLine();
			writer.write("snmpport = " + switchToAdd.getPort().toString());
			writer.newLine();
			writer.write("authUserName = " + switchToAdd.getAuthUserName());
			writer.newLine();
			writer.write("authAlgorithm = " + authAlgorithm);
			writer.newLine();
			writer.write("authPassWord = " + switchToAdd.getAuthPassword());
			writer.newLine();
			writer.write("encryptAlgorithm = " + encryptAlgorithm);
			writer.newLine();
			writer.write("encryptPassword = " + switchToAdd.getEncryptPassword());
			writer.newLine();
			writer.write("tsnPorts = " + tsnPorts);
			writer.newLine();

			writer.close();
		} catch (IOException e) {
			logger.catching(e);
			throw new IllegalStateException("issue accessing files when trying to add switch to switch config");
		}
	}

	@Override
	public synchronized void removeSwitchFromConfig(String switchIdentifier) {
		if (!file.exists()) {
			throw new IllegalStateException("switch config file not found when trying to remove switch!");
		}
		File tempFile = new File(file.getPath() + ".tmp");

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

			String line;
			Boolean removeSwitchSection = false;
			Boolean foundSwitch = false;

			// copy over lines that are not in the section of the to-be-deleted switch
			while ((line = reader.readLine()) != null) {

				// when reading new switches section, set flag based on if this is the switch to
				// be removed
				if (!line.startsWith("#")) {
					if (line.contains(":")) {
						String[] parts = line.split(":");
						if (parts.length > 0) {
							if (parts[0].trim().equals(switchIdentifier.trim())) {
								removeSwitchSection = true;
								foundSwitch = true;
							} else {
								removeSwitchSection = false;
							}
						}

					}
				}

				// copy line if flag is false
				if (!removeSwitchSection) {
					writer.write(line);
					writer.newLine();
				}
			}
			reader.close();
			writer.close();
			File newFile = new File(file.getPath());
			file.delete();
			tempFile.renameTo(newFile);
			file = newFile;
			if (foundSwitch) {
				logger.info("removed switch " + switchIdentifier + " from switch config file");
			} else {
				logger.info(
						"couldn´t remove switch " + switchIdentifier + " from switch config file, switch not found");
			}
		} catch (IOException e) {
			logger.catching(e);
			throw new IllegalStateException("issue accessing files when trying to remove switch from switch config");
		}
	}

	// checks if a String represents an IPv4 address
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

}

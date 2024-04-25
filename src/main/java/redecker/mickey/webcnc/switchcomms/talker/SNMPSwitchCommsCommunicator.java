package redecker.mickey.webcnc.switchcomms.talker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.UserTarget;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import redecker.mickey.webcnc.setup.WebcncApplication;
import redecker.mickey.webcnc.types.GCLEntry;
import redecker.mickey.webcnc.types.LLDPDataTuple;
import redecker.mickey.webcnc.types.PTPTimeTuple;
import redecker.mickey.webcnc.types.enums.AuthAlgorithm;
import redecker.mickey.webcnc.types.enums.EncryptionAlgorithm;
import redecker.mickey.webcnc.types.exceptions.PDUErrorException;
import redecker.mickey.webcnc.types.exceptions.PDUNullException;
import redecker.mickey.webcnc.types.exceptions.ResponseNullException;
import redecker.mickey.webcnc.types.exceptions.CommsException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class provides low-level SNMP operations on a switch that get or set a
 * single value on the switch
 * 
 * It gets/sets TSN-parameters defined in IEEE 802.1Qbv and LLDP information
 * about the switch and its neighbors
 * 
 * The class uses the SNMP4J library for the SNMP communication with the switch
 * 
 * @author Mickey Redecker
 *
 */
public class SNMPSwitchCommsCommunicator implements ISwitchCommsCommunicator {

	private String ipAddress;
	private Integer snmpPort;
	private Integer portNumber;
	private String authUserName;
	private AuthAlgorithm authAlgorithm;
	private String authPassword;
	private EncryptionAlgorithm encryptAlgorithm;
	private String encryptPassword;

	private static final Logger logger = LogManager.getLogger(SNMPSwitchCommsCommunicator.class);

	/**
	 * creates the SwitchCommsCommunicator with the SNMPv3 credentials of the target
	 * switch
	 * 
	 * @param ipAddress
	 * @param snmpPort
	 * @param portNumber
	 * @param authUserName
	 * @param authAlgorithm
	 * @param authPassword
	 * @param encryptAlgorithm
	 * @param encryptPassword
	 */
	public SNMPSwitchCommsCommunicator(String ipAddress, Integer snmpPort, Integer portNumber, String authUserName,
			AuthAlgorithm authAlgorithm, String authPassword, EncryptionAlgorithm encryptAlgorithm,
			String encryptPassword) {

		if (checkForIpAddress(ipAddress)) {
			throw new IllegalArgumentException("ipAddress has invalid format");
		}
		this.ipAddress = ipAddress;
		this.snmpPort = snmpPort;
		this.portNumber = portNumber;
		this.authUserName = authUserName;
		this.authAlgorithm = authAlgorithm;
		this.authPassword = authPassword;
		this.encryptAlgorithm = encryptAlgorithm;
		this.encryptPassword = encryptPassword;
	}

	@Override
	public List<GCLEntry> getOperControlList() throws CommsException {

		List<GCLEntry> gcl = new LinkedList<GCLEntry>();

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.7.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						OctetString octetStringResponse = (OctetString) responsePDU.getVariableBindings().get(0)
								.getVariable();
						byte[] responseGCL = octetStringResponse.getValue();

						// every 7 bytes represent a GCL entry
						int numberOfEntries = responseGCL.length / 7;

						// create a GCLEntry for each GCL entry
						for (int i = 0; i < numberOfEntries; i++) {
							byte[] tlv = new byte[7];

							// copy over corresponding bytes from response
							for (int byteCounter = 0; byteCounter < 7; byteCounter++) {
								tlv[byteCounter] = responseGCL[i * 7 + byteCounter];
							}

							// calculate gateStates List
							List<Boolean> gateStates = new LinkedList<Boolean>();
							for (int counter = 0; counter < 8; counter++) {
								// select which bit to copy over
								int bitSelector = 1 << (7 - counter);
								// retrieve gate bit
								if ((tlv[2] & bitSelector) != 0) {
									gateStates.add(true);
								} else {
									gateStates.add(false);
								}
							}

							// Retrieve interval time
							long intervalTime = 0L; // Initialize your long
							intervalTime |= ((long) tlv[3] & 0xFF) << 24;
							intervalTime |= ((long) tlv[4] & 0xFF) << 16;
							intervalTime |= ((long) tlv[5] & 0xFF) << 8;
							intervalTime |= ((long) tlv[6] & 0xFF);

							// generate new gcl entry identifier
							long maxSafeInteger = 9007199254740991L; //max number int in typescript
							Random random = new Random();
							Long id = (long) (random.nextDouble() * maxSafeInteger);

							// make new GCL Entry and add it to list
							GCLEntry newEntry = new GCLEntry(id, gateStates, intervalTime);
							gcl.add(newEntry);

						}

					} else {
						throw new PDUErrorException("getOperControlList | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getOperControlList");
				}
			} else {
				throw new ResponseNullException("getOperControlList");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return gcl;
	}

	@Override
	public void setAdminControlList(List<GCLEntry> gcl) throws CommsException {

		int length = gcl.size();
		byte[] allTLVs = new byte[7 * length]; // each TLV is 7 bytes

		// make TLVs for each entry one by one and add them to allTLVs
		for (int i = 0; i < length; i++) {

			// a new TLV for one gcl entry
			byte[] tlv = new byte[7]; // 1 byte for operation, 1 byte for length, 1 byte for GateState, 4 bytes for
										// TimeInterval
			tlv[0] = (byte) 0; // operation setGateStates
			tlv[1] = (byte) 5; // length is always 5 for operation setGateStates

			// make byte for gateStates
			byte[] gateStatesByte = new byte[1]; // One byte array with a single byte
			List<Boolean> gateStates = gcl.get(i).getGateStates();

			// go through all gateStates and set the corresponding bit
			for (int counter = 0; counter < 8; counter++) {
				if (gateStates.get(counter)) {
					gateStatesByte[0] |= 1 << (7 - counter);
				}
			}
			tlv[2] = gateStatesByte[0]; // add gateStates to tlv

			// 4th to 7th byte represents the interval time
			long timeInNs = gcl.get(i).getTimeInNs();
			tlv[3] = (byte) (timeInNs >> 24);
			tlv[4] = (byte) (timeInNs >> 16);
			tlv[5] = (byte) (timeInNs >> 8);
			tlv[6] = (byte) (timeInNs);

			// add tlv to allTLVs
			for (int byteCounter = 0; byteCounter < 7; byteCounter++) {
				allTLVs[7 * i + byteCounter] = tlv[byteCounter];
			}

		}

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.6.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new OctetString(allTLVs));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminControlList | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminControlList");
				}
			} else {
				throw new ResponseNullException("setAdminControlList");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public void setAdminControlListLength(List<GCLEntry> gcl) throws CommsException {

		Long length = (long) gcl.size();

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.4.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new UnsignedInteger32(length));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminControlListLength | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminControlListLength");
				}
			} else {
				throw new ResponseNullException("setAdminControlListLength");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public Long getOperCycleTimeNumerator() throws CommsException {

		Long numerator = 0L;

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.10.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						numerator = responsePDU.getVariableBindings().get(0).getVariable().toLong();

					} else {

						throw new PDUErrorException("getOperCycleTimeNumerator | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getOperCycleTimeNumerator");
				}
			} else {
				throw new ResponseNullException("getOperCycleTimeNumerator");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return numerator;
	}

	@Override
	public void setAdminCycleTimeNumerator(Long cycleTimeNumerator) throws CommsException {
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.8.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new UnsignedInteger32(cycleTimeNumerator));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminCycleTimeNumerator | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminCycleTimeNumerator");
				}
			} else {
				throw new ResponseNullException("setAdminCycleTimeNumerator");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public Long getOperCycleTimeDenominator() throws CommsException {

		Long denominator = 0L;

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.11.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						denominator = responsePDU.getVariableBindings().get(0).getVariable().toLong();

					} else {

						throw new PDUErrorException("getOperCycleTimeDenominator | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getOperCycleTimeDenominator");
				}
			} else {
				throw new ResponseNullException("getOperCycleTimeDenominator");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return denominator;
	}

	@Override
	public void setAdminCycleTimeDenominator(Long cycleTimeDenominator) throws CommsException {
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.9.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new UnsignedInteger32(cycleTimeDenominator));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminCycleTimeDenominator | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminCycleTimeDenominator");
				}
			} else {
				throw new ResponseNullException("setAdminCycleTimeDenominator");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public Long getOperCycleTimeExtension() throws CommsException {

		Long extension = 0L;

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.13.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						extension = responsePDU.getVariableBindings().get(0).getVariable().toLong();

					} else {

						throw new PDUErrorException("getOperCycleTimeExtension | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getOperCycleTimeExtension");
				}
			} else {
				throw new ResponseNullException("getOperCycleTimeExtension");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return extension;

	}

	@Override
	public void setAdminCycleTimeExtension(Long cycleTimeExtension) throws CommsException {
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.12.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new UnsignedInteger32(cycleTimeExtension));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminCycleTimeExtension | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminCycleTimeExtension");
				}
			} else {
				throw new ResponseNullException("setAdminCycleTimeExtension");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public PTPTimeTuple getOperBaseTime() throws CommsException {

		PTPTimeTuple time = new PTPTimeTuple();
		time.nanoseconds = 0L;
		time.seconds = 0L;

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.15.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						OctetString octetStringResponse = (OctetString) responsePDU.getVariableBindings().get(0)
								.getVariable();
						byte[] reponseTime = octetStringResponse.getValue();

						// extract seconds and nanoseconds from response
						long seconds = 0;
						for (int i = 0; i < 6; i++) {
							seconds = (seconds << 8) | (reponseTime[i] & 0xFF);
						}
						// Extract nanoseconds from response
						Long nanoseconds = 0L;
						for (int i = 6; i < 10; i++) {
							nanoseconds = (nanoseconds << 8) | (reponseTime[i] & 0xFF);
						}
						time.nanoseconds = nanoseconds;
						time.seconds = seconds;

					} else {

						throw new PDUErrorException("getOperBaseTime | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getOperBaseTime");
				}
			} else {
				throw new ResponseNullException("getOperBaseTime");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return time;
	}

	@Override
	public void setAdminBaseTime(PTPTimeTuple timeTuple) throws CommsException {

		long seconds = timeTuple.seconds;
		long nanoseconds = timeTuple.nanoseconds;

		ByteBuffer bytesToSet = ByteBuffer.allocate(10);
		bytesToSet.order(ByteOrder.BIG_ENDIAN);

		// remove top 16 bits of seconds, split lower 48 in 32 and 16
		int higher32secondBits = (int) (seconds >> 16);
		bytesToSet.putInt(higher32secondBits);
		short lower16SecondBits = (short) (seconds & 0xFFFF);
		bytesToSet.putShort(lower16SecondBits);

		// Add lower 32 bits of nanoseconds to byte buffer
		bytesToSet.putInt(6, (int) (nanoseconds & 0xFFFFFFFFL));

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.14.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new OctetString(bytesToSet.array()));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setAdminBaseTime | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setAdminBaseTime");
				}
			} else {
				throw new ResponseNullException("setAdminBaseTime");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public void setConfigChange() throws CommsException {
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.16.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, new Integer32(1));
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setConfigChange | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setConfigChange");
				}
			} else {
				throw new ResponseNullException("setConfigChange");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}
	
	@Override
	public boolean getGateEnabled() throws CommsException {
		
		boolean gateEnabled = false;
		
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.111.2.802.1.1.30.1.2.1.1.1.1." + portNumber.toString())));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						Integer gateEnabledInt = responsePDU.getVariableBindings().get(0).getVariable().toInt();
						if(gateEnabledInt == 1) {
							gateEnabled = true;
						}
						else {
							gateEnabled = false;
						}

					} else {

						throw new PDUErrorException("getGateEnabled | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getGateEnabled");
				}
			} else {
				throw new ResponseNullException("getGateEnabled");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return gateEnabled;

	}

	@Override
	public void setGateEnabled(boolean gateEnabled) throws CommsException {
		Integer32 value;
		if(gateEnabled) {
			value = new Integer32(1);
		}
		else {
			value = new Integer32(2);
		}
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			OID oid = new OID("1.3.111.2.802.1.1.30.1.2.1.1.1.1." + portNumber.toString());
			VariableBinding varBind = new VariableBinding(oid, value);
			pdu.add(varBind);
			pdu.setType(PDU.SET);

			ResponseEvent response = snmp.set(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
					} else {

						throw new PDUErrorException("setGateEnabled | Error Status = " + errorStatus
								+ " | Error index = " + errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("setGateEnabled");
				}
			} else {
				throw new ResponseNullException("setGateEnabled");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}

	}

	@Override
	public String getSysname() throws CommsException {

		String sysName = "";
		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// Create PDU
			ScopedPDU pdu = new ScopedPDU();
			pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.5.0")));
			pdu.setType(ScopedPDU.GET);

			ResponseEvent response = snmp.get(pdu, target);

			// Process Agent Response
			if (response != null) {
				PDU responsePDU = response.getResponse();

				if (responsePDU != null) {
					int errorStatus = responsePDU.getErrorStatus();
					int errorIndex = responsePDU.getErrorIndex();
					String errorStatusText = responsePDU.getErrorStatusText();

					if (errorStatus == PDU.noError) {
						logger.debug("Snmp Response = " + responsePDU.getVariableBindings());
						sysName = responsePDU.getVariableBindings().get(0).getVariable().toString();
					} else {

						throw new PDUErrorException("getSysName | Error Status = " + errorStatus + " | Error index = "
								+ errorIndex + " | Error Status Text = " + errorStatusText);
					}
				} else {
					throw new PDUNullException("getSysName");
				}
			} else {
				throw new ResponseNullException("getSysName");
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return sysName;
	}

	@Override
	public List<LLDPDataTuple> getLLDPRemData() throws CommsException {

		List<LLDPDataTuple> remData = new LinkedList<LLDPDataTuple>();

		Snmp snmp = null;
		try {
			TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
			snmp = new Snmp(transport);

			SecurityProtocols sp = makeSecurityProtocols();

			USM users = new USM(sp, new OctetString(MPv3.createLocalEngineID()), 0);
			SecurityModels.getInstance().addSecurityModel(users);

			addSNMPUser(snmp);

			// Set the target and listen to responses
			UserTarget target = makeNewTarget();
			snmp.listen();

			// OIDs for lldpRemSysName and lldpRemPortId
			OID[] columns = new OID[] { new OID("1.0.8802.1.1.2.1.4.1.1.9"), new OID("1.0.8802.1.1.2.1.4.1.1.7") };
			TableUtils tableUtils = new TableUtils(snmp, new tablePDUFactory());
			List<TableEvent> events = tableUtils.getTable(target, columns, null, null);

			for (TableEvent event : events) {
				if (event.isError()) {
					logger.error("Error: " + event.getErrorMessage());
					throw new PDUErrorException("getLLDPRemSysNames | " + event.getErrorMessage());
				} else {
					VariableBinding sysNameBinding = event.getColumns()[0]; // lldpRemSysName
					VariableBinding portIdBinding = event.getColumns()[1]; // lldpRemPortId
					if (sysNameBinding != null && portIdBinding != null) {
						LLDPDataTuple data = new LLDPDataTuple();
						data.remSysName = sysNameBinding.getVariable().toString();
						data.remPortId = portIdBinding.getVariable().toString();
						// second last OID number is the local port number (used for indexing the lldp
						// remote table rows)
						data.localPortNumber = sysNameBinding.getOid().get(sysNameBinding.getOid().size() - 2);
						remData.add(data);
					}
				}
			}
		} catch (IOException e) {
			logger.catching(e);
		} finally {
			if (snmp != null) {
				try {
					snmp.close();
				} catch (IOException e) {
					logger.catching(e);
				}
			}
		}
		return remData;
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
		String[] addressElements = address.split(".");
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

	private SecurityProtocols makeSecurityProtocols() {
		// activate appropriate security protocol
		SecurityProtocols sp = SecurityProtocols.getInstance();
		if (authAlgorithm == AuthAlgorithm.MD5) {
			sp.addAuthenticationProtocol(new AuthMD5());
		} else {
			sp.addAuthenticationProtocol(new AuthSHA());
		}
		return sp;
	}

	private UserTarget makeNewTarget() {
		UserTarget target = new UserTarget();
		target.setAddress(GenericAddress.parse("udp:" + ipAddress + "/" + snmpPort.toString()));
		target.setVersion(SnmpConstants.version3);
		target.setSecurityLevel(SecurityLevel.AUTH_PRIV);
		target.setSecurityName(new OctetString(authUserName));
		target.setRetries(WebcncApplication.switchConnectionRetries);
		target.setTimeout(WebcncApplication.switchConnectionTimeout);
		return target;
	}

	private void addSNMPUser(Snmp snmp) {
		// add user depending on which authentification and encryption variant is used
		if (authAlgorithm == AuthAlgorithm.MD5 && encryptAlgorithm == EncryptionAlgorithm.DES) {
			snmp.getUSM().addUser(new OctetString(authUserName), new UsmUser(new OctetString(authUserName), AuthMD5.ID,
					new OctetString(authPassword), PrivDES.ID, new OctetString(encryptPassword)));
		} else if (authAlgorithm == AuthAlgorithm.MD5 && encryptAlgorithm == EncryptionAlgorithm.AES128) {
			snmp.getUSM().addUser(new OctetString(authUserName), new UsmUser(new OctetString(authUserName), AuthMD5.ID,
					new OctetString(authPassword), PrivAES128.ID, new OctetString(encryptPassword)));
		} else if (authAlgorithm == AuthAlgorithm.SHA1 && encryptAlgorithm == EncryptionAlgorithm.DES) {
			snmp.getUSM().addUser(new OctetString(authUserName), new UsmUser(new OctetString(authUserName), AuthSHA.ID,
					new OctetString(authPassword), PrivDES.ID, new OctetString(encryptPassword)));
		} else if (authAlgorithm == AuthAlgorithm.SHA1 && encryptAlgorithm == EncryptionAlgorithm.AES128) {
			snmp.getUSM().addUser(new OctetString(authUserName), new UsmUser(new OctetString(authUserName), AuthSHA.ID,
					new OctetString(authPassword), PrivAES128.ID, new OctetString(encryptPassword)));
		}
	}

	// factory to make new PDUs for retrieving LLDPRemTable data
	private static class tablePDUFactory implements PDUFactory {

		@Override
		public PDU createPDU(org.snmp4j.Target<?> target) {
			PDU pdu = new ScopedPDU();
			pdu.setType(PDU.GETNEXT);
			return pdu;
		}

		@Override
		public PDU createPDU(MessageProcessingModel messageProcessingModel) {
			PDU pdu = new ScopedPDU();
			pdu.setType(PDU.GETNEXT);
			return pdu;
		}

	}

}

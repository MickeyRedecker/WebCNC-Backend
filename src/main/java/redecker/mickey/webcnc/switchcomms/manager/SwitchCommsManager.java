package redecker.mickey.webcnc.switchcomms.manager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import redecker.mickey.webcnc.switchcomms.talker.ISwitchCommsCommunicator;
import redecker.mickey.webcnc.switchcomms.talker.SNMPSwitchCommsCommunicator;
import redecker.mickey.webcnc.types.GCLEntry;
import redecker.mickey.webcnc.types.LLDPDataTuple;
import redecker.mickey.webcnc.types.PTPTimeTuple;
import redecker.mickey.webcnc.types.Port;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.enums.AuthAlgorithm;
import redecker.mickey.webcnc.types.enums.EncryptionAlgorithm;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This class provides high-level operations on switches via SNMP
 * 
 * It uses low-level functions provided by a SwitchCommsCommunicator to set and
 * get values from a switch via SNMPv3
 * 
 * It orchestrates the low-level calls to achieve the desired high-level
 * operation and ensures thread safety via a monitor
 * 
 * @author Mickey Redecker
 *
 */
public class SwitchCommsManager implements ISwitchCommsManager {

	@Override
	public synchronized Switch getNewSwitchInformation(SwitchInfo switchInfo) throws CommsException {

		// retrieve data from switchInfo
		String identifier = switchInfo.getSwitchIdentifier();
		String address = switchInfo.getAddress();
		Integer port = switchInfo.getPort();
		String authUserName = switchInfo.getAuthUserName();
		AuthAlgorithm authAlgorithm;
		if (switchInfo.getAuthAlgorithm().trim().equalsIgnoreCase("MD5")) {
			authAlgorithm = AuthAlgorithm.MD5;
		} else if (switchInfo.getAuthAlgorithm().trim().equalsIgnoreCase("SHA1")) {
			authAlgorithm = AuthAlgorithm.SHA1;
		} else {
			throw new IllegalArgumentException(
					"AuthAlgorithm " + switchInfo.getAuthAlgorithm().trim() + " not supported");
		}
		String authPassword = switchInfo.getAuthPassword();
		EncryptionAlgorithm encryptAlgorithm;
		if (switchInfo.getEncryptAlgorithm().trim().equalsIgnoreCase("DES")) {
			encryptAlgorithm = EncryptionAlgorithm.DES;
		} else if (switchInfo.getEncryptAlgorithm().trim().equalsIgnoreCase("AES128")) {
			encryptAlgorithm = EncryptionAlgorithm.AES128;
		} else {
			throw new IllegalArgumentException(
					"EncryptionAlgorithm " + switchInfo.getEncryptAlgorithm().trim() + " not supported");
		}
		String encryptPassword = switchInfo.getEncryptPassword();

		// parse tsn port numbers
		List<Integer> tsnPortNumbers = new LinkedList<Integer>();
		String tsnPortsString = switchInfo.getTSNPortsString();
		if (!tsnPortsString.trim().equals("")) {
			String[] parts = tsnPortsString.split(",");
			try {
				// check if any element is invalid, return false
				for (String part : parts) {
					int portNumber = Integer.valueOf(part);
					tsnPortNumbers.add(portNumber);
					if (portNumber < 0) {
						throw new IllegalArgumentException("tsnPortsString contains negative port number");
					}
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("tsnPortsString is of invalid format");
			}
		}
		// make tsn port numbers unique and sort ascending
		HashSet<Integer> tsnPortNumbersSet = new HashSet<Integer>(tsnPortNumbers);
		tsnPortNumbers = new LinkedList<Integer>(tsnPortNumbersSet);
		Collections.sort(tsnPortNumbers);
		// all data from switchInfo retrieved

		// use talker to get sysname and lldp data from switch

		ISwitchCommsCommunicator sysNamesTalker = new SNMPSwitchCommsCommunicator(address, port, 1, authUserName,
				authAlgorithm, authPassword, encryptAlgorithm, encryptPassword);
		String sysName = sysNamesTalker.getSysname();
		List<LLDPDataTuple> lldpRemData = sysNamesTalker.getLLDPRemData();
		Collections.sort(lldpRemData, new Comparator<LLDPDataTuple>() {
            @Override
            public int compare(LLDPDataTuple data1, LLDPDataTuple data2) {
                return Integer.compare(data1.localPortNumber, data2.localPortNumber);
            }
        });
		List<String> lldpRemSysNames = new LinkedList<String>();
		List<String> lldpRemPortIds = new LinkedList<String>();
		List<Integer> lldpRemLocalPortNumbers = new LinkedList<Integer>();
		for (LLDPDataTuple data : lldpRemData) {
			lldpRemSysNames.add(data.remSysName);
			lldpRemPortIds.add(data.remPortId);
			lldpRemLocalPortNumbers.add(data.localPortNumber);
		}

		// for each port of the switch, use talker to retrieve TSN information
		List<Port> TSNPorts = new LinkedList<Port>();
		for (Integer tsnPortNumber : tsnPortNumbers) {
			ISwitchCommsCommunicator tsnTalker = new SNMPSwitchCommsCommunicator(address, port, tsnPortNumber, authUserName,
					authAlgorithm, authPassword, encryptAlgorithm, encryptPassword);

			Long cycleTimeNumerator = tsnTalker.getOperCycleTimeNumerator();
			Long cycleTimeDenominator = tsnTalker.getOperCycleTimeDenominator();
			Long cycleTimeNumeratorNS = cycleTimeNumerator * 1000000000;
			Long cycleTime;
			if (cycleTimeDenominator == 0L) {
				cycleTime = 0L;
			} else {
				cycleTime = cycleTimeNumeratorNS / cycleTimeDenominator;
			}

			Long cycleTimeExtension = tsnTalker.getOperCycleTimeExtension();

			PTPTimeTuple timeTuple = tsnTalker.getOperBaseTime();
			boolean gateEnabled = tsnTalker.getGateEnabled();
			Long ptpSeconds = timeTuple.seconds;
			Long nanoseconds = timeTuple.nanoseconds;

			// convert ptpSeconds to year/month/day/hour/minute/second
			Instant instant = Instant.ofEpochSecond(ptpSeconds);
			LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
			Integer year = dateTime.getYear();
			Integer month = dateTime.getMonthValue();
			Integer day = dateTime.getDayOfMonth();
			Integer hour = dateTime.getHour();
			Integer minute = dateTime.getMinute();
			Integer second = dateTime.getSecond();

			List<GCLEntry> gateControlList = tsnTalker.getOperControlList();

			Port newPort = new Port(tsnPortNumber, identifier, cycleTime, cycleTimeExtension, year, month, day, hour,
					minute, second, nanoseconds, gateControlList, gateEnabled);
			TSNPorts.add(newPort);
		}

		Switch newSwitch = new Switch(identifier, address, port, sysName, lldpRemSysNames, lldpRemPortIds,
				lldpRemLocalPortNumbers, authUserName, authAlgorithm, authPassword, encryptAlgorithm, encryptPassword,
				TSNPorts, true);
		return newSwitch;

	}

	@Override
	public synchronized Switch getUpdatedSwitch(Switch oldSwitch) throws CommsException {

		// retrieve data from oldSwitch
		String identifier = oldSwitch.getSwitchIdentifier();
		String address = oldSwitch.getAddress();
		Integer port = oldSwitch.getPort();
		String authUserName = oldSwitch.getAuthUserName();
		AuthAlgorithm authAlgorithm = oldSwitch.getAuthAlgorithm();
		String authPassword = oldSwitch.getAuthPassword();
		EncryptionAlgorithm encryptAlgorithm = oldSwitch.getEncryptAlgorithm();
		String encryptPassword = oldSwitch.getEncryptPassword();
		List<Port> oldTSNPorts = oldSwitch.getTsnPorts();
		// all data from switchInfo retrieved

		// use talker to get sysname and lldp data from switch
		ISwitchCommsCommunicator sysNamesTalker = new SNMPSwitchCommsCommunicator(address, port, 1, authUserName,
				authAlgorithm, authPassword, encryptAlgorithm, encryptPassword);
		String sysName = sysNamesTalker.getSysname();
		List<LLDPDataTuple> lldpRemData = sysNamesTalker.getLLDPRemData();
		Collections.sort(lldpRemData, new Comparator<LLDPDataTuple>() {
            @Override
            public int compare(LLDPDataTuple data1, LLDPDataTuple data2) {
                return Integer.compare(data1.localPortNumber, data2.localPortNumber);
            }
        });
		List<String> lldpRemSysNames = new LinkedList<String>();
		List<String> lldpRemPortIds = new LinkedList<String>();
		List<Integer> lldpRemLocalPortNumbers = new LinkedList<Integer>();
		for (LLDPDataTuple data : lldpRemData) {
			lldpRemSysNames.add(data.remSysName);
			lldpRemPortIds.add(data.remPortId);
			lldpRemLocalPortNumbers.add(data.localPortNumber);
		}

		// for each port of the switch, use talker to retrieve TSN information
		List<Port> newTSNPorts = new LinkedList<Port>();
		for (Port oldTSNPort : oldTSNPorts) {
			Integer tsnPortNumber = oldTSNPort.getPortNumber();
			ISwitchCommsCommunicator tsnTalker = new SNMPSwitchCommsCommunicator(address, port, tsnPortNumber, authUserName,
					authAlgorithm, authPassword, encryptAlgorithm, encryptPassword);

			Long cycleTimeNumerator = tsnTalker.getOperCycleTimeNumerator();
			Long cycleTimeDenominator = tsnTalker.getOperCycleTimeDenominator();
			Long cycleTimeNumeratorNS = cycleTimeNumerator * 1000000000;
			Long cycleTime;
			if (cycleTimeDenominator == 0L) {
				cycleTime = 0L;
			} else {
				cycleTime = cycleTimeNumeratorNS / cycleTimeDenominator;
			}

			Long cycleTimeExtension = tsnTalker.getOperCycleTimeExtension();

			PTPTimeTuple timeTuple = tsnTalker.getOperBaseTime();
			boolean gateEnabled = tsnTalker.getGateEnabled();
			Long ptpSeconds = timeTuple.seconds;
			Long nanoseconds = timeTuple.nanoseconds;

			// convert ptpSeconds to year/month/day/hour/minute/second
			Instant instant = Instant.ofEpochSecond(ptpSeconds);
			LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
			Integer year = dateTime.getYear();
			Integer month = dateTime.getMonthValue();
			Integer day = dateTime.getDayOfMonth();
			Integer hour = dateTime.getHour();
			Integer minute = dateTime.getMinute();
			Integer second = dateTime.getSecond();

			List<GCLEntry> gateControlList = tsnTalker.getOperControlList();

			Port newPort = new Port(tsnPortNumber, identifier, cycleTime, cycleTimeExtension, year, month, day, hour,
					minute, second, nanoseconds, gateControlList, gateEnabled);
			newTSNPorts.add(newPort);
		}

		Switch newSwitch = new Switch(identifier, address, port, sysName, lldpRemSysNames, lldpRemPortIds,
				lldpRemLocalPortNumbers, authUserName, authAlgorithm, authPassword, encryptAlgorithm, encryptPassword,
				newTSNPorts, true);
		return newSwitch;
	}

	@Override
	public synchronized void setPortParameters(Port newPort, Switch oldSwitch) throws CommsException {

		// retrieve data from oldSwitch
		String address = oldSwitch.getAddress();
		Integer port = oldSwitch.getPort();
		String authUserName = oldSwitch.getAuthUserName();
		AuthAlgorithm authAlgorithm = oldSwitch.getAuthAlgorithm();
		String authPassword = oldSwitch.getAuthPassword();
		EncryptionAlgorithm encryptAlgorithm = oldSwitch.getEncryptAlgorithm();
		String encryptPassword = oldSwitch.getEncryptPassword();
		// all data from switchInfo retrieved

		// set new TSN parameters on switch
		ISwitchCommsCommunicator tsnTalker = new SNMPSwitchCommsCommunicator(address, port, newPort.getPortNumber(),
				authUserName, authAlgorithm, authPassword, encryptAlgorithm, encryptPassword);

		// calculate PTPTimeTuple for setting AdminBaseTime
		Integer year = newPort.getStartYear();
		Integer month = newPort.getStartMonth();
		Integer day = newPort.getStartDay();
		Integer hour = newPort.getStartHour();
		Integer minute = newPort.getStartMinute();
		Integer second = newPort.getStartSecond();
		Long nanosecond = newPort.getStartNanosecond();
		LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second);
		Instant instant = dateTime.toInstant(ZoneOffset.UTC);
		Long ptpSeconds = instant.getEpochSecond();

		// set AdminBaseTime
		PTPTimeTuple newPTPTime = new PTPTimeTuple();
		newPTPTime.seconds = ptpSeconds;
		newPTPTime.nanoseconds = nanosecond;
		tsnTalker.setAdminBaseTime(newPTPTime);

		// set other tsn parameters
		tsnTalker.setAdminControlListLength(newPort.getGateControlList());
		tsnTalker.setAdminControlList(newPort.getGateControlList());
		tsnTalker.setAdminCycleTimeDenominator(1000000000L); // use cycle time value as nanoseconds
		tsnTalker.setAdminCycleTimeNumerator(newPort.getCycleTime());
		tsnTalker.setAdminCycleTimeExtension(newPort.getCycleTimeExtension());
		tsnTalker.setGateEnabled(newPort.getGateEnabled());
		tsnTalker.setConfigChange();

	}

}

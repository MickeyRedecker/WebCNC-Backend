package redecker.mickey.webcnc.types;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 *
 * 
 *         This class represents a TSN capable port on a TSN switch.
 * 
 *         It represents the parameters of a TSN time-aware shaper
 * 
 *         Instead of replicating the data structures defined in IEEE 802.1Qbv,
 *         it stores the information in a more human-readable format for easier
 *         frontend processing For example, PTPTimeValues are instead
 *         represented as Year, Month, Day, Hour, Minute, Second and Nanosecond.
 *			
 * @author Mickey Redecker
 */
public class Port {

	private Integer portNumber; // positive
	private String switchIdentifier; // unique
	private Long cycleTime; // positive, in ns, splits into numerator and denominator on switch
	private Long cycleTimeExtension; // positive
	private Integer startYear;// positive >= 1970, <= 8921373
	private Integer startMonth;// positive < 13
	private Integer startDay;// positive < 32
	private Integer startHour;// positive < 24
	private Integer startMinute;// positive < 60
	private Integer startSecond;// positive < 60
	private Long startNanosecond;// positive < 1 000 000 000
	private List<GCLEntry> gateControlList;
	private boolean gateEnabled;

	/**
	 * 
	 * @param portNumber         The number of the port on the switch
	 * @param switchIdentifier   The unique identifier of the switch that this port
	 *                           belongs to
	 * @param cycleTime          The TSN cycle time for the port
	 * @param cycleTimeExtension The TSN cycle time extension for the port
	 * @param startYear          The start year of the current configuration
	 * @param startMonth         The start month of the current configuration
	 * @param startDay           The start day of the current configuration
	 * @param startHour          The start hour of the current configuration
	 * @param startMinute        The start minute of the current configuration
	 * @param startSecond        The start second of the current configuration
	 * @param startNanosecond    The start nanosecond of the current configuration
	 * @param gateControlList    A list of GCLEntries representing the gate control
	 *                           list of the port
	 * @param gateEnabled        Indicates if TSN is activated on the port
	 */
	public Port(Integer portNumber, String switchIdentifier, Long cycleTime, Long cycleTimeExtension, Integer startYear,
			Integer startMonth, Integer startDay, Integer startHour, Integer startMinute, Integer startSecond,
			Long startNanosecond, List<GCLEntry> gateControlList, boolean gateEnabled) {

		// guard statements to check for invalid values
		if (portNumber <= 0) {
			throw new IllegalArgumentException("portNumber must be positive and greater than 0");
		} else if (switchIdentifier == null) {
			throw new IllegalArgumentException("switchidentifier can´t be null");
		} else if (cycleTime < 0) {
			throw new IllegalArgumentException("cycleTime must be positive");
		} else if (cycleTimeExtension < 0) {
			throw new IllegalArgumentException("cycleTimeExtender must be positive");
		} else if (startYear < 1970 || startYear > 8921373) {
			throw new IllegalArgumentException("startYear must be between 1970 and 8921373");
		} else if (startMonth < 1 || startMonth > 12) {
			throw new IllegalArgumentException("startMonth must be between 1 and 12");
		} else if (!validateDate(startYear, startMonth, startDay)) {
			throw new IllegalArgumentException("startDay is invalid for the given date");
		} else if (startHour < 0 || startHour > 23) {
			throw new IllegalArgumentException("startHour must be between 0 and 23");
		} else if (startMinute < 0 || startMinute > 59) {
			throw new IllegalArgumentException("startMinute must be between 0 and 59");
		} else if (startSecond < 0 || startSecond > 59) {
			throw new IllegalArgumentException("startSecond must be between 0 and 59");
		} else if (startNanosecond < 0 || startNanosecond >= 4294967296L) {
			throw new IllegalArgumentException("startNanosecond must be between 0 and 4294967296");
		} else if (gateControlList == null) {
			throw new IllegalArgumentException("gateControlList can´t be null");
		}

		this.portNumber = portNumber;
		this.switchIdentifier = switchIdentifier;
		this.cycleTime = cycleTime;
		this.cycleTimeExtension = cycleTimeExtension;
		this.startYear = startYear;
		this.startMonth = startMonth;
		this.startDay = startDay;
		this.startHour = startHour;
		this.startMinute = startMinute;
		this.startSecond = startSecond;
		this.startNanosecond = startNanosecond;
		this.gateControlList = gateControlList;
		this.gateEnabled = gateEnabled;
	}

	// getters and setters with guard statements
	public Integer getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(Integer portNumber) {
		if (portNumber < 0) {
			throw new IllegalArgumentException("portNumber must be positive");
		}
		this.portNumber = portNumber;
	}

	public String getSwitchIdentifier() {
		return switchIdentifier;
	}

	public void setSwitchIdentifier(String switchIdentifier) {
		if (switchIdentifier == null) {
			throw new IllegalArgumentException("switchidentifier can´t be null");
		}
		this.switchIdentifier = switchIdentifier;
	}

	public Long getCycleTime() {
		return cycleTime;
	}

	public void setCycleTime(Long cycleTime) {
		if (cycleTime <= 0) {
			throw new IllegalArgumentException("cycleTime is negative");
		}
		this.cycleTime = cycleTime;
	}

	public Long getCycleTimeExtension() {
		return cycleTimeExtension;
	}

	public void setCycleTimeExtension(Long cycleTimeExtender) {
		if (cycleTimeExtender <= 0) {
			throw new IllegalArgumentException("cycleTimeExtender is negative");
		}
		this.cycleTimeExtension = cycleTimeExtender;
	}

	public Integer getStartYear() {
		return startYear;
	}

	public void setStartYear(Integer startYear) {
		if (startYear < 1970 || startYear > 8921373) {
			throw new IllegalArgumentException("startYear must be between 1970 and 8921373");
		}
		this.startYear = startYear;
	}

	public Integer getStartMonth() {
		return startMonth;
	}

	public void setStartMonth(Integer startMonth) {
		if (startMonth < 1 || startMonth > 12) {
			throw new IllegalArgumentException("startMonth must be between 1 and 12");
		}
		this.startMonth = startMonth;
	}

	public Integer getStartDay() {
		return startDay;
	}

	public void setStartDay(Integer startDay) {
		if (!validateDate(this.startYear, this.startMonth, startDay)) {
			throw new IllegalArgumentException("startDay must be between 1 and 31");
		}
		this.startDay = startDay;
	}

	public Integer getStartHour() {
		return startHour;
	}

	public void setStartHour(Integer startHour) {
		if (startHour < 0 || startHour > 23) {
			throw new IllegalArgumentException("startHour must be between 0 and 23");
		}
		this.startHour = startHour;
	}

	public Integer getStartMinute() {
		return startMinute;
	}

	public void setStartMinute(Integer startMinute) {
		if (startMinute < 0 || startMinute > 59) {
			throw new IllegalArgumentException("startMinute must be between 0 and 59");
		}
		this.startMinute = startMinute;
	}

	public Integer getStartSecond() {
		return startSecond;
	}

	public void setStartSecond(Integer startSecond) {
		if (startSecond < 0 || startSecond > 59) {
			throw new IllegalArgumentException("startSecond must be between 0 and 59");
		}
		this.startSecond = startSecond;
	}

	public Long getStartNanosecond() {
		return startNanosecond;
	}

	public void setStartNanosecond(Long startNanosecond) {
		if (startNanosecond > 4294967296L) {
			throw new IllegalArgumentException("timeInNs must be below 4294967296");
		}
		this.startNanosecond = startNanosecond;
	}

	public List<GCLEntry> getGateControlList() {
		return gateControlList;
	}

	public void setGateControlList(List<GCLEntry> gateControlList) {
		if (gateControlList == null) {
			throw new IllegalArgumentException("gateControlList can´t be null");
		}
		this.gateControlList = gateControlList;
	}
	
	public boolean getGateEnabled() {
		return gateEnabled;
	}
	
	public boolean setGateEnabled() {
		return gateEnabled;
	}

	/**
	 * Creates a deepcopy of the Port
	 * 
	 * @return deepcopy of the object
	 */
	public Port makeDeepCopy() {

		List<GCLEntry> copiedGateControlList = new LinkedList<GCLEntry>();
		for (GCLEntry entry : this.gateControlList) {
			GCLEntry deepCopyEntry = entry.makeDeepCopy();
			copiedGateControlList.add(deepCopyEntry);
		}

		return new Port(this.getPortNumber(), this.getSwitchIdentifier(), this.getCycleTime(),
				this.getCycleTimeExtension(), this.getStartYear(), this.getStartMonth(), this.getStartDay(),
				this.getStartHour(), this.getStartMinute(), this.getStartSecond(), this.getStartNanosecond(),
				copiedGateControlList, this.getGateEnabled());
	}

	private static boolean checkLeapYear(int year) {
		return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
	}

	public static boolean validateDate(int year, int month, int day) {
		int[] maxDays = { 31, checkLeapYear(year) ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 }; // max days for
																										// each month of
																										// the year

		if (month < 1 || month > 12) {
			return false;
		} else if (day < 1 || day > maxDays[month - 1]) {
			return false;
		} else {
			return true;
		}
	}
}

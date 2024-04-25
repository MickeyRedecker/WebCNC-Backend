package redecker.mickey.webcnc.switchcomms.manager;

import redecker.mickey.webcnc.types.Port;
import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This interfaces defines high-level operations on switches via SNMP
 * 
 * It uses low-level functions provided by an object implementing the
 * ISwitchCommsCommunicator interface to set and get values from a switch via
 * SNMPv3
 * 
 * Any class implementing this interface shall ensure thread-safe execution of
 * all switch operations
 * 
 * @author Mickey Redecker
 *
 */
public interface ISwitchCommsManager {

	/**
	 * A function that takes new switch information and gathers TSN and LLDP data
	 * from the switch using the ISwitchCommsCommunicator interface
	 * 
	 * @param switchInfo the SNMPv3 credentials of the switch
	 * @return returns a Switch object with all TSN and LLDP data gathered from the
	 *         switch
	 * @throws CommsException If an error occurs during SNMP calls to the switch
	 */
	public Switch getNewSwitchInformation(SwitchInfo switchInfo) throws CommsException;

	/**
	 * A function that gathers new TSN and LLDP data from an already known switch
	 * using the ISwitchCommsCommunicator interface
	 * 
	 * @param oldSwitch The switch to gather updated data from
	 * @return A Switch object with the new TSN and LLDP data gathered from the
	 *         switch
	 * @throws CommsException If an error occurs during SNMP calls to the switch
	 */
	public Switch getUpdatedSwitch(Switch oldSwitch) throws CommsException;

	/**
	 * A function that sets new TSN parameters for a port on a known switch using
	 * the ISwitchCommsCommunicator interface
	 * 
	 * @param newPort   the new port with TSN parameters to apply on the switch
	 * @param oldSwitch the target switch housing the TSN port
	 * @throws CommsException If an error occurs during SNMP calls to the switch
	 */
	public void setPortParameters(Port newPort, Switch oldSwitch) throws CommsException;
}

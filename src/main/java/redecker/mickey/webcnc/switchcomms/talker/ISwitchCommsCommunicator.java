package redecker.mickey.webcnc.switchcomms.talker;

import java.util.List;

import redecker.mickey.webcnc.types.GCLEntry;
import redecker.mickey.webcnc.types.LLDPDataTuple;
import redecker.mickey.webcnc.types.PTPTimeTuple;
import redecker.mickey.webcnc.types.exceptions.CommsException;

/**
 * This interface defines low-level SNMP operations on a switch that get or set
 * a single value on the switch
 * 
 * It gets/sets TSN-parameters defined in IEEE 802.1Qbv and LLDP information
 * about the switch and its neighbors
 * 
 * Thread-safe operation has to be ensured by the SwitchCommsManager, so
 * thread-safety is not a concern here
 * 
 * @author Mickey Redecker
 *
 */
public interface ISwitchCommsCommunicator {
	/**
	 * Get the operControlList parameter from the switch
	 * 
	 * @return a List of GCLEntries representing the gate control list
	 * @throws CommsException
	 */
	public List<GCLEntry> getOperControlList() throws CommsException;

	/**
	 * Set the adminControlList on the switch
	 * 
	 * @param gcl the gate control list to apply on the switch
	 * @throws CommsException
	 */
	public void setAdminControlList(List<GCLEntry> gcl) throws CommsException;

	/**
	 * Set the adminControlListLength on the switch
	 * 
	 * @param gcl the gate control list whose length shall be set
	 * @throws CommsException
	 */
	public void setAdminControlListLength(List<GCLEntry> gcl) throws CommsException;

	/**
	 * Gets the operCycleTimeNumerator parameter from the switch
	 * 
	 * @return Long representing the cycle time numerator
	 * @throws CommsException
	 */
	public Long getOperCycleTimeNumerator() throws CommsException;

	/**
	 * Sets the AdminCycleTimeNumerator on the switch
	 * 
	 * @param cycleTimeNumerator The numerator value to be set
	 * @throws CommsException
	 */
	public void setAdminCycleTimeNumerator(Long cycleTimeNumerator) throws CommsException;

	/**
	 * Get the opcerCycleTimeDenominator from the switch
	 * 
	 * @return the cycle time denominator
	 * @throws CommsException
	 */
	public Long getOperCycleTimeDenominator() throws CommsException;

	/**
	 * Set the AdminCycleTimeDenominator on the switch
	 * 
	 * @param cycleTimeDenominator the denominator value to be set
	 * @throws CommsException
	 */
	public void setAdminCycleTimeDenominator(Long cycleTimeDenominator) throws CommsException;

	/**
	 * Get the OperCycleTimeExtension value from the switch
	 * 
	 * @return Long representing the cycle time extension
	 * @throws CommsException
	 */
	public Long getOperCycleTimeExtension() throws CommsException;

	/**
	 * Set the AdminCycleTimeExtension value on the switch
	 * 
	 * @param cycleTimeExtension The extension value to be set
	 * @throws CommsException
	 */
	public void setAdminCycleTimeExtension(Long cycleTimeExtension) throws CommsException;

	/**
	 * Gets the operBaseTime value from the switch
	 * 
	 * @return PTPTimeTuple containing the seconds and nanoseconds of the
	 *         OperBaseTime
	 * @throws CommsException
	 */
	public PTPTimeTuple getOperBaseTime() throws CommsException;

	/**
	 * Sets the AdminBaseTime value on the switch
	 * 
	 * @param timeTuple PTPTimeTuple containing the seconds and nanoseconds to be
	 *                  set
	 * @throws CommsException
	 */
	public void setAdminBaseTime(PTPTimeTuple timeTuple) throws CommsException;

	/**
	 * Sets the configChange parameter of the switch to 1 This applies the Admin
	 * parameters as the new active or pending configuration on the switch
	 * 
	 * @throws CommsException
	 */
	public void setConfigChange() throws CommsException;
	
	/**
	 * Gets the gateEnabled parameter of the port
	 * 
	 * @return boolean indicating the gateEnabled state
	 * @throws CommsException
	 */
	boolean getGateEnabled() throws CommsException;
	
	/**
	 * sets the gateEnabled parameter of the port
	 * 
	 * @param gateEnabled the state to set the parameter to
	 * @throws CommsException
	 */
	void setGateEnabled(boolean gateEnabled) throws CommsException;
	
	/**
	 * Gets the system name of the switch
	 * 
	 * @return The switches system name
	 * @throws CommsException
	 */
	public String getSysname() throws CommsException;

	/**
	 * Gets a list of all neighbors system names, neighbor port ids and the local
	 * port number gathered via LLDP
	 * 
	 * @return List containing the neighbors system names, neighbor port ids and
	 *         local port number
	 * @throws CommsException
	 */
	public List<LLDPDataTuple> getLLDPRemData() throws CommsException;



}

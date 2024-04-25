package redecker.mickey.webcnc.switchcredstore;

import java.util.List;

import redecker.mickey.webcnc.types.Switch;
import redecker.mickey.webcnc.types.SwitchInfo;

/**
 * This interface defines operations on the persistent storage of switch
 * credentials
 * 
 * Any class implementing this feature has to ensure thread-safe operation on
 * the chosen persistent storage method
 * 
 * @author Mickey Redecker
 *
 */
public interface ISwitchCredStore {

	/**
	 * This function reads all persistently stored switch SNMPv3 credentials from
	 * the storage medium and returns it as a list
	 * 
	 * @return List of SwitchInfo objects with their SNMPv3 credentials
	 */
	public List<SwitchInfo> getAllSwitchInfo();

	/**
	 * This function adds the SNMPv3 credentials of a switch to the persistent
	 * storage
	 * 
	 * @param switchToAdd The switch whose information is to be stored
	 */
	public void addSwitchToConfig(Switch switchToAdd);

	/**
	 * This function permanently removes a switches credentials from the persistent
	 * storage
	 * 
	 * @param switchIdentifier The identifier of the switch to be removed
	 */
	public void removeSwitchFromConfig(String switchIdentifier);

}

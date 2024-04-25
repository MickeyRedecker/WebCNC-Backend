package redecker.mickey.webcnc.netstate;

import java.util.List;

import redecker.mickey.webcnc.types.Switch;

/**
 * This interface defines operations on the NetworkStateCache
 * 
 * The netstatecache is used to store and perform operations on the information
 * aquired about the network state with its participants and configurations.
 * 
 * The netstatecache also to ensure thread-safe operation on its storage
 * solution
 * 
 * @author Mickey Redecker
 *
 */
public interface INetworkStateCache {

	/**
	 * This functions returns a list of all Switches
	 * 
	 * @return List of all switches
	 */
	public List<Switch> getAllSwitches();

	/**
	 * This function returns a single switch based on an identifier
	 * 
	 * @param identifier The identifier of the requested switch
	 * @return The Switch with the specified identifier
	 */
	public Switch getSwitch(String identifier);

	/**
	 * Adds a new switch to the netstatecache
	 * 
	 * @param newSwitch The switch to add
	 * @return Indicates whether the operation was successful or not
	 */
	public Boolean addSwitch(Switch newSwitch);

	/**
	 * Removes a switch from the netstatecache
	 * 
	 * @param identifier the identifier of the switch to remove
	 * @return Indicates whether the operation was successful or not
	 */
	public Boolean removeSwitch(String identifier);

	/**
	 * Replaces a switch with the specified switch based on a matching identifier
	 * 
	 * @param newSwitch The new switch to add
	 * @return Indicates whether the operation was successful or not
	 */
	public Boolean replaceSwitch(Switch newSwitch);

	/**
	 * Returns a list of all stored switch identifiers
	 * 
	 * @return List of switch identifiers (Strings)
	 */
	public List<String> getAllSwitchIdentifiers();

	/**
	 * Returns a list of all switches whose reachable attribute is set to true
	 * 
	 * @return List of reachable Switches
	 * 
	 */
	public List<Switch> getReachableSwitches();

	/**
	 * Returns a list of all switches whose reachable attribute is set to false
	 * 
	 * @return List of unreachable Switches
	 * 
	 */
	public List<Switch> getUnreachableSwitches();

	/**
	 * This function replaces all stored switches with the provided list of switches
	 * All previously stored information is lost
	 * 
	 * @param newSwitchList The new list of Switches to be stored
	 * @return Indicates whether the operation was successful or not
	 */
	public Boolean replaceAllSwitches(List<Switch> newSwitchList);
}

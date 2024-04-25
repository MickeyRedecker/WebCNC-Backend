package redecker.mickey.webcnc.netstate;

import java.util.LinkedList;
import java.util.List;

import redecker.mickey.webcnc.types.Switch;

/**
 * This class implements the INetworkStateCache interface with in-memory storage
 * 
 * It ensures thread-safe operation with the use of a monitor
 * 
 * @author Mickey Redecker
 *
 */
public class NetworkStateCache implements INetworkStateCache {

	private List<Switch> switches;

	public NetworkStateCache() {
		this.switches = new LinkedList<Switch>();
	}

	@Override
	public synchronized List<Switch> getAllSwitches() {
		List<Switch> deepCopySwitches = new LinkedList<Switch>();
		for (Switch currentSwitch : switches) {
			deepCopySwitches.add(currentSwitch.makeDeepCopy());
		}
		return deepCopySwitches;
	}

	@Override
	public synchronized Switch getSwitch(String identifier) {
		for (Switch currentSwitch : switches) {
			if (currentSwitch.getSwitchIdentifier().equals(identifier)) {
				return currentSwitch.makeDeepCopy();
			}
		}
		return null;
	}

	@Override
	// return type indicates if switch could be added
	// returns false if switch with identifier already exists
	public synchronized Boolean addSwitch(Switch newSwitch) {
		for (Switch currentSwitch : switches) {
			if (currentSwitch.getSwitchIdentifier().equals(newSwitch.getSwitchIdentifier())) {
				return false;
			}
		}
		switches.add(newSwitch.makeDeepCopy());
		return true;
	}

	@Override
	// return value indicates if switch has been found or not
	public synchronized Boolean removeSwitch(String identifier) {
		for (Switch currentSwitch : switches) {
			if (currentSwitch.getSwitchIdentifier().equals(identifier)) {
				switches.remove(currentSwitch);
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized List<String> getAllSwitchIdentifiers() {

		List<String> identifiers = new LinkedList<String>();
		for (Switch currentSwitch : switches) {
			identifiers.add(currentSwitch.getSwitchIdentifier());
		}
		return identifiers;

	}

	@Override
	public synchronized List<Switch> getReachableSwitches() {

		List<Switch> deepCopySwitches = new LinkedList<Switch>();
		for (Switch currentSwitch : switches) {
			if (currentSwitch.isReachable()) {
				deepCopySwitches.add(currentSwitch.makeDeepCopy());
			}
		}
		return deepCopySwitches;
	}

	@Override
	public synchronized List<Switch> getUnreachableSwitches() {
		List<Switch> deepCopySwitches = new LinkedList<Switch>();
		for (Switch currentSwitch : switches) {
			if (!currentSwitch.isReachable()) {
				deepCopySwitches.add(currentSwitch.makeDeepCopy());
			}
		}
		return deepCopySwitches;
	}

	@Override
	// return value indicates if replacement was successfull
	// returns false if new Switch List contains non-unique identifiers
	public synchronized Boolean replaceAllSwitches(List<Switch> newSwitchList) {
		List<Switch> deepCopySwitches = new LinkedList<Switch>();
		List<String> newIdentifiers = new LinkedList<String>(); // used to check if identifiers are unique
		for (Switch currentSwitch : newSwitchList) {
			if (newIdentifiers.contains(currentSwitch.getSwitchIdentifier())) {
				return false;
			}
			newIdentifiers.add(currentSwitch.getSwitchIdentifier());
			deepCopySwitches.add(currentSwitch.makeDeepCopy());
		}
		switches = deepCopySwitches;
		return true;
	}

	@Override
	// return value indicates if switch has been found and replaced
	// returning false means the switch wasn´t found and hasn´t been inserted
	public Boolean replaceSwitch(Switch newSwitch) {
		
		for (Switch currentSwitch : switches) {
			if (currentSwitch.getSwitchIdentifier().equals(newSwitch.getSwitchIdentifier())) {
				int positionInList = switches.indexOf(currentSwitch);
				switches.remove(currentSwitch);
				switches.add(positionInList, newSwitch.makeDeepCopy());
				return true;
			}
		}
		return false;

	}

}

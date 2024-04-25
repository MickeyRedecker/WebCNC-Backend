package redecker.mickey.webcnc.types;

import java.util.LinkedList;
import java.util.List;

/**
 * 
 * 
 * 
 *         This class represents a single entry in a gate control list on a TSN
 *         switch
 * 
 *         It contains a random identifier, a list of gate states and the time
 *         how long the entry will be active per cycle.
 *         
 *         @author Mickey Redecker
 *
 */
public class GCLEntry {

	private Long entryIdentifier; // random identifier, positive
	private List<Boolean> gateStates; // 8 entries represent Gate 0-7
	private Long timeInNs; // positive < 4294967296

	/**
	 * Creates a new GCLEntry
	 * 
	 * @param entryIdentifier A random unique identifier
	 * @param gateStates      a list of 8 boolean gate states
	 * @param timeInNs        the time which the entry will be active for per cycle
	 */
	public GCLEntry(Long entryIdentifier, List<Boolean> gateStates, Long timeInNs) {

		// guard statements to check for invalid values
		if (gateStates.size() != 8) {
			throw new IllegalArgumentException("gateState List size != 8");
		} else if (entryIdentifier < 0) {
			throw new IllegalArgumentException("entryIdentifier is negative");
		} else if (timeInNs > 4294967296L) {
			throw new IllegalArgumentException("timeInNs must be below 4294967296");
		}

		this.entryIdentifier = entryIdentifier;
		this.gateStates = gateStates;
		this.timeInNs = timeInNs;
	}

	// getters and setters with guard statements
	public Long getEntryIdentifier() {
		return entryIdentifier;
	}

	public void setEntryIdentifier(Long entryIdentifier) {
		if (entryIdentifier < 0) {
			throw new IllegalArgumentException("entryIdentifier is negative");
		}
		this.entryIdentifier = entryIdentifier;
	}

	public List<Boolean> getGateStates() {
		return gateStates;
	}

	public void setGateStates(List<Boolean> gateStates) {
		if (gateStates.size() != 8) {
			throw new IllegalArgumentException("gateState List size != 8");
		}
		this.gateStates = gateStates;
	}

	public Long getTimeInNs() {
		return timeInNs;
	}

	public void setTimeInNs(Long timeInNs) {
		if (timeInNs > 4294967296L) {
			throw new IllegalArgumentException("timeInNs must be below 4294967296");
		}
		this.timeInNs = timeInNs;
	}

	/**
	 * Creates a deepcopy of the GCLEntry
	 * 
	 * @return deepcopy of the object
	 */
	public GCLEntry makeDeepCopy() {

		List<Boolean> deepCopyGateStates = new LinkedList<Boolean>();
		for (Boolean gateState : gateStates) {
			deepCopyGateStates.add(gateState);
		}

		return new GCLEntry(this.getEntryIdentifier(), deepCopyGateStates, this.getTimeInNs());
	}
}

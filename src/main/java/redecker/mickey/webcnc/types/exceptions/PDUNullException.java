package redecker.mickey.webcnc.types.exceptions;

/**
 * 
 * 
 * 
 *         This exception is thrown if there is no response PDU after an SNMP
 *         request
 *         
 *         @author Mickey Redecker
 * 
 *
 */
public class PDUNullException extends PDUException {
	public PDUNullException(String errorMessage) {
		super(errorMessage);
	}
}

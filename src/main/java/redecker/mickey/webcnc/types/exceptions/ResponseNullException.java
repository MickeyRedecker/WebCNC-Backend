package redecker.mickey.webcnc.types.exceptions;

/**
 * 
 * 
 * 
 *         This exception is thrown if there is no SNMP response after a request
 *         
 *         @author Mickey Redecker
 * 
 *
 */
public class ResponseNullException extends CommsException {
	public ResponseNullException(String errorMessage) {
		super(errorMessage);
	}
}

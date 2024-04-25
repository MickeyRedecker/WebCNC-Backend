package redecker.mickey.webcnc.types.exceptions;

/**
 * 
 * 
 * 
 *         This exception is never thrown. It is the supertype for both
 *         PDUErrorException and PDUNullException.
 *         
 *         @author Mickey Redecker
 * 
 *
 */
public class PDUException extends CommsException {
	public PDUException(String errorMessage) {
		super(errorMessage);
	}
}

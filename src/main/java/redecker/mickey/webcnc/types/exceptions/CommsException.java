package redecker.mickey.webcnc.types.exceptions;

/**
 * 
 * 
 * 
 *         This exception is never thrown. It is the supertype for
 *         ResponseNullException and PDUException. All exceptions that extend
 *         this exception shall only contain non-sensitive information in their
 *         messages that can be displayed to the user in the frontend.
 *         
 *         @author Mickey Redecker
 * 
 *
 */
public class CommsException extends Exception {
	public CommsException(String errorMessage) {
		super(errorMessage);
	}
}

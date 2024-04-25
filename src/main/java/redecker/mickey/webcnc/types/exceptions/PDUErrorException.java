package redecker.mickey.webcnc.types.exceptions;

/**
 * 
 * 
 * 
 *         This exception is thrown if a PDU response contains information about
 *         an error
 *         
 *         @author Mickey Redecker
 * 
 *
 */
public class PDUErrorException extends PDUException {
	public PDUErrorException(String errorMessage) {
		super(errorMessage);
	}
}

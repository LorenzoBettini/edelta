package edelta.interpreter;

/**
 * Subclasses of this exception will always be rethrown by the safe interpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 */
	public EdeltaInterpreterRuntimeException(String message) {
		super(message);
	}

}
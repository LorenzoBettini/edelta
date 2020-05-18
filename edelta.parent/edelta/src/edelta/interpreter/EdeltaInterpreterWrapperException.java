package edelta.interpreter;

/**
 * Wraps a {@link Throwable} found in the result of interpretation
 */
public class EdeltaInterpreterWrapperException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final Exception exception;

	public EdeltaInterpreterWrapperException(Exception exception) {
		super(exception);
		this.exception = exception;
	}

	public Exception getException() {
		return exception;
	}
}
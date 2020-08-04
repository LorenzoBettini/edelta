/**
 * 
 */
package edelta.interpreter;

import org.apache.log4j.Logger;

import edelta.edelta.EdeltaProgram;

/**
 * An interpreter that swallows all {@link RuntimeException}s except for
 * {@link EdeltaInterpreterRuntimeException}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaSafeInterpreter extends EdeltaInterpreter {

	private static final Logger LOG = Logger.getLogger(EdeltaSafeInterpreter.class);

	@Override
	@SuppressWarnings("all") // avoid warning for nested try block
	public void evaluateModifyEcoreOperations(EdeltaProgram program) {
		try {
			try {
				super.evaluateModifyEcoreOperations(program);
			} catch (EdeltaInterpreterWrapperException e) {
				throw e.getException();
			}
		} catch (EdeltaInterpreterRuntimeException e) {
			throw e;
		} catch (RuntimeException e) {
			LOG.debug("while interpreting", e);
		} catch (Exception e) {
			LOG.warn("result of interpreting", e);
		}
	}

}

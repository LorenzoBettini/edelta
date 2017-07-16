/**
 * 
 */
package edelta.interpreter;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;

import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression;

/**
 * An interpreter that swallows all {@link RuntimeException}s except for
 * {@link EdeltaInterpreterRuntimeException}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaSafeInterpreter extends EdeltaInterpreter {

	private static final Logger LOG = Logger.getLogger(EdeltaSafeInterpreter.class);

	/**
	 * This exception will always be rethrown by the safe interpreter.
	 * 
	 * @author Lorenzo Bettini
	 *
	 */
	public static class EdeltaInterpreterRuntimeException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		/**
		 * @param message
		 */
		public EdeltaInterpreterRuntimeException(String message) {
			super(message);
		}

	}

	@Override
	public IEvaluationResult run(EdeltaEcoreBaseEClassManipulationWithBlockExpression exp, EClass eClass,
			JvmGenericType jvmGenericType, List<EPackage> packages) {
		try {
			IEvaluationResult result = super.run(exp, eClass, jvmGenericType, packages);
			if (result != null) {
				Throwable exception = result.getException();
				if (exception != null) {
					if (exception instanceof EdeltaInterpreterRuntimeException) {
						throw (EdeltaInterpreterRuntimeException) exception;
					}
					LOG.debug("result of interpreting", exception);
					exception.printStackTrace();
				}
			}
			return result;
		} catch (RuntimeException e) {
			if (e instanceof EdeltaInterpreterRuntimeException) {
				throw (EdeltaInterpreterRuntimeException) e;
			}
			LOG.debug("while interpreting", e);
		}
		return null;
	}

}

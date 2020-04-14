/**
 * 
 */
package edelta.interpreter;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmGenericType;

import edelta.edelta.EdeltaModifyEcoreOperation;

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
	 * Subclasses of this exception will always be rethrown by the safe interpreter.
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
	@SuppressWarnings("all") // avoid warning for nested try block
	public void run(Iterable<EdeltaModifyEcoreOperation> ops,
			Map<String, EPackage> nameToCopiedEPackageMap,
			JvmGenericType jvmGenericType, List<EPackage> ePackages) {
		try {
			try {
				super.run(ops, nameToCopiedEPackageMap, jvmGenericType, ePackages);
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

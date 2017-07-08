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
 * An interpreter that swallows {@link RuntimeException}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaSafeInterpreter extends EdeltaInterpreter {

	private static final Logger LOG = Logger.getLogger(EdeltaSafeInterpreter.class);

	@Override
	public IEvaluationResult run(EdeltaEcoreBaseEClassManipulationWithBlockExpression exp, EClass eClass,
			JvmGenericType jvmGenericType, List<EPackage> packages) {
		try {
			IEvaluationResult result = super.run(exp, eClass, jvmGenericType, packages);
			if (result != null) {
				Throwable exception = result.getException();
				if (exception != null) {
					LOG.debug("result of interpreting", exception);
					exception.printStackTrace();
				}
			}
			return result;
		} catch (RuntimeException e) {
			LOG.debug("while interpreting", e);
		}
		return null;
	}

}

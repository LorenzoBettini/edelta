/**
 * 
 */
package edelta.interpreter;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression;

/**
 * An interpreter that swallows {@link RuntimeException}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaSafeInterpreter implements IEdeltaInterpreter {

	private static final Logger LOG = Logger.getLogger(EdeltaSafeInterpreter.class);

	@Inject private EdeltaInterpreter delegate;

	@Override
	public IEvaluationResult run(EdeltaEcoreBaseEClassManipulationWithBlockExpression exp, EClass eClass,
			JvmGenericType jvmGenericType) {
		try {
			IEvaluationResult result = delegate.run(exp, eClass, jvmGenericType);
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
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void setInterpreterTimeout(int interpreterTimeout) {
		delegate.setInterpreterTimeout(interpreterTimeout);
	}

}

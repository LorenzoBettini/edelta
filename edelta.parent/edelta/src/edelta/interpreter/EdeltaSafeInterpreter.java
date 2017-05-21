/**
 * 
 */
package edelta.interpreter;

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

	@Inject private EdeltaInterpreter delegate;

	@Override
	public IEvaluationResult run(EdeltaEcoreBaseEClassManipulationWithBlockExpression exp, EClass eClass,
			JvmGenericType jvmGenericType) {
		try {
			return delegate.run(exp, eClass, jvmGenericType);
		} catch (RuntimeException e) {
			// swallow runtime exceptions
		}
		return null;
	}

}

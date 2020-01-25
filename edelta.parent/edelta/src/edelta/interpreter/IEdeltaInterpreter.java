/**
 * 
 */
package edelta.interpreter;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.xbase.interpreter.IEvaluationResult;

import edelta.edelta.EdeltaModifyEcoreOperation;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	IEvaluationResult run(EdeltaModifyEcoreOperation op, EPackage ePackage,
			JvmGenericType jvmGenericType, List<EPackage> ePackages);

	void setInterpreterTimeout(int interpreterTimeout);

	public void setClassLoader(ClassLoader classLoader);
}

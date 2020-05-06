/**
 * 
 */
package edelta.interpreter;

import java.util.List;

import org.eclipse.xtext.util.CancelIndicator;

import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.lib.AbstractEdelta;
import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	void evaluateModifyEcoreOperations(EdeltaProgram program,
			EdeltaCopiedEPackagesMap copiedEPackagesMap);

	void setInterpreterTimeout(int interpreterTimeout);

	void setClassLoader(ClassLoader classLoader);

	Object evaluateEdeltaOperation(AbstractEdelta other, EdeltaProgram program, EdeltaOperation edeltaOperation,
			List<Object> argumentValues, CancelIndicator indicator);
}

/**
 * 
 */
package edelta.interpreter;

import edelta.edelta.EdeltaProgram;
import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	void evaluateModifyEcoreOperations(EdeltaProgram program,
			EdeltaCopiedEPackagesMap copiedEPackagesMap);

	void setInterpreterTimeout(int interpreterTimeout);

	public void setClassLoader(ClassLoader classLoader);
}

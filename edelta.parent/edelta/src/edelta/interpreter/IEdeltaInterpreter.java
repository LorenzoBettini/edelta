/**
 * 
 */
package edelta.interpreter;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;

import edelta.edelta.EdeltaProgram;
import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	void evaluateModifyEcoreOperations(EdeltaProgram program,
			EdeltaCopiedEPackagesMap copiedEPackagesMap,
			List<EPackage> ePackages);

	void setInterpreterTimeout(int interpreterTimeout);

	public void setClassLoader(ClassLoader classLoader);
}

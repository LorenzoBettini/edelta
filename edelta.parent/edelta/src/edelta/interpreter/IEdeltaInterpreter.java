/**
 * 
 */
package edelta.interpreter;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmGenericType;

import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.util.EdeltaCopiedEPackagesMap;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	void run(Iterable<EdeltaModifyEcoreOperation> ops,
			EdeltaCopiedEPackagesMap copiedEPackagesMap,
			JvmGenericType jvmGenericType, List<EPackage> ePackages);

	void setInterpreterTimeout(int interpreterTimeout);

	public void setClassLoader(ClassLoader classLoader);
}

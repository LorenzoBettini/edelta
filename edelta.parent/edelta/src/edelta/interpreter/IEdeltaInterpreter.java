/**
 * 
 */
package edelta.interpreter;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.common.types.JvmGenericType;

import edelta.edelta.EdeltaModifyEcoreOperation;

/**
 * @author Lorenzo Bettini
 *
 */
public interface IEdeltaInterpreter {

	void run(Iterable<EdeltaModifyEcoreOperation> ops,
			Map<String, EPackage> nameToCopiedEPackageMap,
			JvmGenericType jvmGenericType, List<EPackage> ePackages);

	void setInterpreterTimeout(int interpreterTimeout);

	public void setClassLoader(ClassLoader classLoader);
}

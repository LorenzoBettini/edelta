/**
 * 
 */
package edelta.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.scoping.XbaseQualifiedNameProvider;

import edelta.edelta.EdeltaProgram;
import edelta.util.EdeltaModelUtil;

/**
 * Deals with the case of a program without an explicit package name
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaQualifiedNameProvider extends XbaseQualifiedNameProvider {

	@Override
	public QualifiedName getFullyQualifiedName(EObject obj) {
		if (obj instanceof EdeltaProgram program) {
			var packageName = program.getName();
			packageName = packageName != null ? packageName : "edelta";
			return getConverter().toQualifiedName(
				packageName + "." +
					program.eResource().getURI().trimFileExtension().lastSegment());
		}
		return super.getFullyQualifiedName(obj);
	}

	/**
	 * We have to avoid possible eContainer cycles in {@link EPackage}
	 * subpackages.
	 */
	@Override
	protected QualifiedName computeFullyQualifiedNameFromNameAttribute(EObject obj) {
		if (obj instanceof EPackage ePackage && EdeltaModelUtil.hasCycleInSuperPackage(ePackage)) {
			// avoid StackOverflowError
			return getConverter().toQualifiedName(ePackage.getName());
		}
		return super.computeFullyQualifiedNameFromNameAttribute(obj);
	}
}

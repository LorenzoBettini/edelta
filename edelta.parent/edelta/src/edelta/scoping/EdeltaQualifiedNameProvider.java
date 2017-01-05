/**
 * 
 */
package edelta.scoping;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.scoping.XbaseQualifiedNameProvider;

import edelta.edelta.EdeltaProgram;

/**
 * Deals with the case of a program without an explicit package name
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaQualifiedNameProvider extends XbaseQualifiedNameProvider {

	@Override
	public QualifiedName getFullyQualifiedName(EObject obj) {
		if (obj instanceof EdeltaProgram) {
			EdeltaProgram program = (EdeltaProgram) obj;
			String packageName = program.getName();
			packageName = packageName != null ? packageName : "edelta";
			return getConverter().toQualifiedName(
				packageName + "." +
					program.eResource().getURI().trimFileExtension().lastSegment());
		}
		return super.getFullyQualifiedName(obj);
	}
}

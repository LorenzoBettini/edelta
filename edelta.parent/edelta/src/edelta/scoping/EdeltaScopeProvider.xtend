/*
 * generated by Xtext 2.10.0
 */
package edelta.scoping

import edelta.edelta.EdeltaPackage
import edelta.edelta.EdeltaProgram
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.scoping.Scopes
import org.eclipse.xtext.scoping.impl.FilteringScope

/**
 * This class contains custom scoping description.
 * 
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#scoping
 * on how and when to use it.
 */
class EdeltaScopeProvider extends AbstractEdeltaScopeProvider {

	override getScope(EObject context, EReference reference) {
		if (reference == EdeltaPackage.Literals.EDELTA_ECLASS_EXPRESSION__ECLASS) {
			val prog = EcoreUtil2.getContainerOfType(context, EdeltaProgram)
			return Scopes.scopeFor(
				prog.metamodels.map [
					EClassifiers
				].flatten
			)
		} else if (reference == EdeltaPackage.Literals.EDELTA_PROGRAM__METAMODELS) {
			return new FilteringScope(delegateGetScope(context, reference)) [
				"false".equals(getUserData("nsURI"))
			]
		}
		super.getScope(context, reference)
	}

}

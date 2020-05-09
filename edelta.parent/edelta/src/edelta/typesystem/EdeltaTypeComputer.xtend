package edelta.typesystem

import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {

	override void computeTypes(XExpression e, ITypeComputationState state) {
		switch (e) {
			EdeltaEcoreReferenceExpression: _computeTypes(e, state)
			default: super.computeTypes(e, state)
		}
	}

	def void _computeTypes(EdeltaEcoreReferenceExpression e, ITypeComputationState state) {
		val enamedelement = e.reference?.enamedelement;
		if (enamedelement === null) {
			state.acceptActualType(getPrimitiveVoid(state))
			return
		}
		if (enamedelement.eIsProxy) {
			// if it's unresolved, but there's a type expectation, then
			// we assign to this reference the expected type: this way
			// we will only get an error due to unresolved reference
			// and not an addition type mismatch error, which would be
			// superfluous
			val expectation = state.expectations.findFirst[expectedType !== null]
			if (expectation !== null) {
				// for unresolved proxies the type must be at least ENamedElement
				val atLeast = getRawTypeForName(ENamedElement, state)
				val expectedType = expectation.expectedType
				if (atLeast.isAssignableFrom(expectedType)) {
					state.acceptActualType(expectedType)
					return
				}
			}
			state.acceptActualType(
				getRawTypeForName(ENamedElement, state)
			)
		} else {
			state.acceptActualType(
				getRawTypeForName(enamedelement.eClass.instanceTypeName, state)
			)
		}
	}

}

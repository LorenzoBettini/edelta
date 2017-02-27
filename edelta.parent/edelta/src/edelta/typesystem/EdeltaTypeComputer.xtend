package edelta.typesystem

import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.xtext.xbase.XExpression
import edelta.edelta.EdeltaEcoreReference

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {

	override void computeTypes(XExpression e, ITypeComputationState state) {
		switch (e) {
			EdeltaEcoreCreateEClassExpression: _computeTypes(e, state)
			EdeltaEcoreCreateEAttributeExpression: _computeTypes(e, state)
			EdeltaEcoreReferenceExpression: _computeTypes(e, state)
			EdeltaEcoreReference: _computeTypes(e, state)
			default: super.computeTypes(e, state)
		}
	}

	def void _computeTypes(EdeltaEcoreCreateEClassExpression e, ITypeComputationState state) {
		for (ecoreRefSuperType : e.ecoreReferenceSuperTypes) {
			state.
				withExpectation(getRawTypeForName(EClass, state)).
				computeTypes(ecoreRefSuperType)
		}
		state.acceptActualType(getRawTypeForName(EClass, state))
	}

	def void _computeTypes(EdeltaEcoreCreateEAttributeExpression e, ITypeComputationState state) {
		val etypeRef = e.ecoreReferenceDataType
		state.
			withExpectation(getRawTypeForName(EDataType, state)).
			computeTypes(etypeRef)
		state.acceptActualType(getRawTypeForName(EAttribute, state))
	}

	def void _computeTypes(EdeltaEcoreReferenceExpression e, ITypeComputationState state) {
		val enamedelement = e.reference?.enamedelement;
		if (enamedelement === null) {
			state.acceptActualType(getPrimitiveVoid(state))
			return
		}
		val result = state.withNonVoidExpectation.computeTypes(e.reference)
		state.acceptActualType(result.actualExpressionType)
	}

	def void _computeTypes(EdeltaEcoreReference e, ITypeComputationState state) {
		val enamedelement = e.enamedelement;
		val type = switch (enamedelement) {
			case enamedelement === null || enamedelement.eIsProxy: {
				// if it's unresolved, but there's a type expectation, then
				// we assign to this reference the expected type: this way
				// we will only get an error due to unresolved reference
				// and not an addition type mismatch error, which would be
				// superfluous
				val expectation = state.expectations.findFirst[expectedType !== null]
				if (expectation !== null) {
					state.acceptActualType(expectation.expectedType)
					return
				}
				ENamedElement
			}
			EPackage: EPackage
			EClass: EClass
			EEnum: EEnum
			EDataType: EDataType
			EReference: EReference
			EEnumLiteral: EEnumLiteral
			default: EAttribute
		}
		state.acceptActualType(getRawTypeForName(type, state))
	}
}

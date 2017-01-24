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

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {

	def dispatch void computeTypes(EdeltaEcoreCreateEClassExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClass, state))
	}

	def dispatch void computeTypes(EdeltaEcoreCreateEAttributeExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EAttribute, state))
	}

	def dispatch void computeTypes(EdeltaEcoreReferenceExpression e, ITypeComputationState state) {
		val enamedelement = e.reference?.enamedelement;
		if (enamedelement === null) {
			state.acceptActualType(getPrimitiveVoid(state))
			return
		}
		val type = switch (enamedelement) {
			case enamedelement.eIsProxy: ENamedElement
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

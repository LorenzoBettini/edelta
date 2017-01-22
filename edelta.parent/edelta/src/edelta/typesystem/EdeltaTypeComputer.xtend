package edelta.typesystem

import edelta.edelta.EdeltaEAttributeExpression
import edelta.edelta.EdeltaEClassExpression
import edelta.edelta.EdeltaEClassifierExpression
import edelta.edelta.EdeltaEDataTypeExpression
import edelta.edelta.EdeltaEFeatureExpression
import edelta.edelta.EdeltaEReferenceExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EEnumLiteral
import org.eclipse.emf.ecore.ENamedElement

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {
	def dispatch void computeTypes(EdeltaEClassifierExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClassifier, state))
	}

	def dispatch void computeTypes(EdeltaEClassExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClass, state))
	}

	def dispatch void computeTypes(EdeltaEDataTypeExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EDataType, state))
	}

	def dispatch void computeTypes(EdeltaEFeatureExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EStructuralFeature, state))
	}

	def dispatch void computeTypes(EdeltaEAttributeExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EAttribute, state))
	}

	def dispatch void computeTypes(EdeltaEReferenceExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EReference, state))
	}

	def dispatch void computeTypes(EdeltaEcoreCreateEClassExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClass, state))
	}

	def dispatch void computeTypes(EdeltaEcoreCreateEAttributeExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EAttribute, state))
	}

	def dispatch void computeTypes(EdeltaEcoreReferenceExpression e, ITypeComputationState state) {
		val enamedelement = e.reference.enamedelement;
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

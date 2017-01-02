package edelta.typesystem

import edelta.edelta.EdeltaEAttributeExpression
import edelta.edelta.EdeltaEClassExpression
import edelta.edelta.EdeltaEClassifierExpression
import edelta.edelta.EdeltaEDataTypeExpression
import edelta.edelta.EdeltaEFeatureExpression
import edelta.edelta.EdeltaEReferenceExpression
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EReference

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
}

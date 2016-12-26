package edelta.typesystem

import edelta.edelta.EdeltaEClassExpression
import edelta.edelta.EdeltaEClassifierExpression
import edelta.edelta.EdeltaEDataTypeExpression
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState

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
}

package edelta.typesystem

import com.google.inject.Inject
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReferenceExpression
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {

	@Inject extension EdeltaEcoreTypeHelper

	override void computeTypes(XExpression e, ITypeComputationState state) {
		switch (e) {
			EdeltaEcoreCreateEClassExpression: _computeTypes(e, state)
			EdeltaEcoreCreateEAttributeExpression: _computeTypes(e, state)
			EdeltaEcoreReferenceExpression: _computeTypes(e, state)
			default: super.computeTypes(e, state)
		}
	}

	def void _computeTypes(EdeltaEcoreCreateEClassExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClass, state))
	}

	def void _computeTypes(EdeltaEcoreCreateEAttributeExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EAttribute, state))
	}

	def void _computeTypes(EdeltaEcoreReferenceExpression e, ITypeComputationState state) {
		val enamedelement = e.reference?.enamedelement;
		if (enamedelement === null) {
			state.acceptActualType(getPrimitiveVoid(state))
			return
		}
		val type = enamedelement.correspondingENamedElement
		state.acceptActualType(getRawTypeForName(type, state))
	}
}

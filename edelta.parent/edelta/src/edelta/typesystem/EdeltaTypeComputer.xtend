package edelta.typesystem

import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer
import edelta.edelta.EdeltaEClassExpression
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState
import org.eclipse.emf.ecore.EClass

class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {
	def dispatch void computeTypes(EdeltaEClassExpression e, ITypeComputationState state) {
		state.acceptActualType(getRawTypeForName(EClass, state))
	}
}
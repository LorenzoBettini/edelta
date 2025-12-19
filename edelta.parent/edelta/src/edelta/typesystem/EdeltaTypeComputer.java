package edelta.typesystem;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.findFirst;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * Custom type computer for typing our ecoreref() expressions
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaTypeComputer extends XbaseWithAnnotationsTypeComputer {
	@Override
	public void computeTypes(final XExpression e, final ITypeComputationState state) {
		if (e instanceof EdeltaEcoreReferenceExpression ecoreReferenceExpression) {
			computeTypesOfEdeltaEcoreReferenceExpression(ecoreReferenceExpression, state);
		} else {
			super.computeTypes(e, state);
		}
	}

	public void computeTypesOfEdeltaEcoreReferenceExpression(final EdeltaEcoreReferenceExpression e,
			final ITypeComputationState state) {
		var reference = e.getArgument();
		ENamedElement enamedelement = null;
		if (reference != null) {
			enamedelement = reference.getElement();
		}
		if (enamedelement == null || enamedelement.eIsProxy()) {
			// if it's unresolved, but there's a type expectation, then
			// we assign to this reference the expected type, if the expected type is
			// an ENamedElement: this way we will only get an error due to unresolved reference
			// and not an addition type mismatch error, which would be superfluous
			final ITypeExpectation expectation =
				findFirst(state.getExpectations(), it -> it.getExpectedType() != null);
			if (expectation != null) {
				final var atLeast =
						getRawTypeForName(ENamedElement.class, state);
				final var expectedType = expectation.getExpectedType();
				if (atLeast.isAssignableFrom(expectedType)) {
					state.acceptActualType(expectedType);
					return;
				}
			}
			// even if it's not resolved or not specified, in case of no expectations
			// for sure we can say that in the end it will be an ENamedElement,
			// i.e., the supertype of all Ecore elements.
			state.acceptActualType(getRawTypeForName(ENamedElement.class, state));
		} else {
			state.acceptActualType(getRawTypeForName(
					enamedelement.eClass().getInstanceTypeName(), state));
		}
	}
}

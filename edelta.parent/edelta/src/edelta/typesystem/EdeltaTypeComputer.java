package edelta.typesystem;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.findFirst;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.annotations.typesystem.XbaseWithAnnotationsTypeComputer;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationState;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeExpectation;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

import edelta.edelta.EdeltaEcoreReference;
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
		if (e instanceof EdeltaEcoreReferenceExpression) {
			computeTypesOfEdeltaEcoreReferenceExpression((EdeltaEcoreReferenceExpression) e, state);
		} else {
			super.computeTypes(e, state);
		}
	}

	public void computeTypesOfEdeltaEcoreReferenceExpression(final EdeltaEcoreReferenceExpression e,
			final ITypeComputationState state) {
		EdeltaEcoreReference reference = e.getReference();
		ENamedElement enamedelement = null;
		if (reference != null) {
			enamedelement = reference.getEnamedelement();
		}
		if (enamedelement == null) {
			state.acceptActualType(getPrimitiveVoid(state));
			return;
		}
		if (enamedelement.eIsProxy()) {
			// if it's unresolved, but there's a type expectation, then
			// we assign to this reference the expected type: this way
			// we will only get an error due to unresolved reference
			// and not an addition type mismatch error, which would be uperfluous
			final ITypeExpectation expectation =
				findFirst(state.getExpectations(), it -> it.getExpectedType() != null);
			if (expectation != null) {
				final LightweightTypeReference atLeast =
						getRawTypeForName(ENamedElement.class, state);
				final LightweightTypeReference expectedType = expectation.getExpectedType();
				if (atLeast.isAssignableFrom(expectedType)) {
					state.acceptActualType(expectedType);
					return;
				}
			}
			state.acceptActualType(getRawTypeForName(ENamedElement.class, state));
		} else {
			state.acceptActualType(getRawTypeForName(
					enamedelement.eClass().getInstanceTypeName(), state));
		}
	}
}

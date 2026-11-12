package edelta.resource.derivedstate;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

import java.util.Set;

import org.eclipse.xtext.naming.QualifiedName;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * Additional information on {@link EdeltaEcoreReferenceExpression}
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreReferenceExpressionState {

	private final EdeltaENamedElementXExpressionMap enamedElementXExpressionMap = new EdeltaENamedElementXExpressionMap();
	private EdeltaAccessibleElements ecoreReferenceExpressionAccessibleElements = new EdeltaAccessibleElements();
	// Snapshots contain independent EObjects, so intersect their qualified names.
	private Set<QualifiedName> accessibleInEveryEvaluation;

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap() {
		return enamedElementXExpressionMap;
	}

	public EdeltaAccessibleElements getAccessibleElements() {
		return ecoreReferenceExpressionAccessibleElements;
	}

	/**
	 * Returns elements from the latest snapshot whose names were present in every
	 * observed evaluation of this syntactic reference. An unevaluated reference
	 * has no accessible elements.
	 */
	public EdeltaAccessibleElements getAccessibleElementsInEveryEvaluation() {
		if (accessibleInEveryEvaluation == null) {
			return new EdeltaAccessibleElements();
		}
		return ecoreReferenceExpressionAccessibleElements.stream()
			.filter(e -> accessibleInEveryEvaluation.contains(e.getQualifiedName()))
			.collect(toCollection(EdeltaAccessibleElements::new));
	}

	public void setAccessibleElements(EdeltaAccessibleElements accessibleElements) {
		ecoreReferenceExpressionAccessibleElements = accessibleElements;
		final var names = accessibleElements.stream()
			.map(EdeltaAccessibleElement::getQualifiedName)
			.collect(toSet());
		if (accessibleInEveryEvaluation == null) {
			accessibleInEveryEvaluation = names;
		} else {
			accessibleInEveryEvaluation.retainAll(names);
		}
	}
}

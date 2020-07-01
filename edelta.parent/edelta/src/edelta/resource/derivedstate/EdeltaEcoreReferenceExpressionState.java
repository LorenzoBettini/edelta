package edelta.resource.derivedstate;

import edelta.edelta.EdeltaEcoreReferenceExpression;

/**
 * Additional information on {@link EdeltaEcoreReferenceExpression}
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaEcoreReferenceExpressionState {

	private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap = new EdeltaENamedElementXExpressionMap();
	private EdeltaEcoreReferenceExpressionAccessibleElements ecoreReferenceExpressionAccessibleElements = new EdeltaEcoreReferenceExpressionAccessibleElements();

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap() {
		return enamedElementXExpressionMap;
	}

	public EdeltaEcoreReferenceExpressionAccessibleElements getAccessibleElements() {
		return ecoreReferenceExpressionAccessibleElements;
	}
}

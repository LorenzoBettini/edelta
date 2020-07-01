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
	private EdeltaAccessibleElements ecoreReferenceExpressionAccessibleElements = new EdeltaAccessibleElements();

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap() {
		return enamedElementXExpressionMap;
	}

	public EdeltaAccessibleElements getAccessibleElements() {
		return ecoreReferenceExpressionAccessibleElements;
	}

	public void setAccessibleElements(EdeltaAccessibleElements accessibleElements) {
		ecoreReferenceExpressionAccessibleElements = accessibleElements;
	}
}

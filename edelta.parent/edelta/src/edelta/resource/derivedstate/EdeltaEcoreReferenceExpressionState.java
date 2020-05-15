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

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap() {
		return enamedElementXExpressionMap;
	}
}

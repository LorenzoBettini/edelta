package edelta.resource.derivedstate;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.xtext.resource.XtextResource;

/**
 * Additional derived state installable in an {@link XtextResource}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDerivedState extends AdapterImpl {
	private EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap();
	private EdeltaEcoreReferenceStateMap ecoreReferenceStateMap = new EdeltaEcoreReferenceStateMap();
	private EdeltaEcoreReferenceExpressionStateMap ecoreReferenceExpressionStateMap = new EdeltaEcoreReferenceExpressionStateMap();
	private EdeltaENamedElementXExpressionMap enamedElementXExpressionMap = new EdeltaENamedElementXExpressionMap();
	private EdeltaUnresolvedEcoreReferences unresolvedEcoreReferences = new EdeltaUnresolvedEcoreReferences();
	private EdeltaModifiedElements modifiedElements = new EdeltaModifiedElements();

	@Override
	public boolean isAdapterForType(final Object type) {
		return EdeltaDerivedState.class == type;
	}

	public EdeltaCopiedEPackagesMap getCopiedEPackagesMap() {
		return copiedEPackagesMap;
	}

	public EdeltaEcoreReferenceStateMap getEcoreReferenceStateMap() {
		return ecoreReferenceStateMap;
	}

	public EdeltaEcoreReferenceExpressionStateMap getEcoreReferenceExpressionStateMap() {
		return ecoreReferenceExpressionStateMap;
	}

	public EdeltaENamedElementXExpressionMap getEnamedElementXExpressionMap() {
		return enamedElementXExpressionMap;
	}

	public EdeltaUnresolvedEcoreReferences getUnresolvedEcoreReferences() {
		return unresolvedEcoreReferences;
	}

	public EdeltaModifiedElements getModifiedElements() {
		return modifiedElements;
	}

	public void clear() {
		copiedEPackagesMap.clear();
		ecoreReferenceStateMap.clear();
		ecoreReferenceExpressionStateMap.clear();
		enamedElementXExpressionMap.clear();
		unresolvedEcoreReferences.clear();
		modifiedElements.clear();
	}

}
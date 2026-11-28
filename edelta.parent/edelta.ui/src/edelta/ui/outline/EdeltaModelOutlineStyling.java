package edelta.ui.outline;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.XExpression;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;

/**
 * Determines whether a final preview row has direct, verified change evidence.
 * Styling is independent of whether one source expression can be selected.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaModelOutlineStyling {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private EdeltaModelPropertyRows propertyRows;

	/** Property styling and the two independently selected navigation candidates. */
	public record PropertyChange(boolean changed, XExpression ecoreExpression,
			XExpression migrationExpression) {}

	/**
	 * Combines Ecore row evidence and the exact final EObject/feature migration token.
	 *
	 * @param program the current Edelta program
	 * @param owner the final preview EObject
	 * @param feature the feature represented by the row
	 * @return styling evidence and navigation candidates for the property
	 */
	public PropertyChange propertyChange(EdeltaProgram program, EObject owner,
			EStructuralFeature feature) {
		var source = program.eResource();
		var original = derivedStateHelper.getCopiedEPackagesMap(source).getOriginal(feature);
		var ecore = EdeltaModelOutlineNavigation.featureChange(feature, original,
			derivedStateHelper.getModifiedFeatures(source));
		var created = !(original instanceof EStructuralFeature)
			&& derivedStateHelper.getLastResponsibleExpression(program, feature) != null;
		var migration = derivedStateHelper.getModelPreview(source)
			.getMigrationExpression(owner, feature);
		return new PropertyChange(ecore.visibleChange() || created || migration != null,
			ecore.expression(), migration);
	}

	/**
	 * Checks direct class-name evidence or eligible rows on this object only.
	 *
	 * @param program the current Edelta program
	 * @param modelElement the final preview EObject
	 * @param classExpression the justified class-name navigation expression, if any
	 * @return whether the object row is proven changed
	 */
	public boolean isChangedObject(EdeltaProgram program, EObject modelElement,
			XExpression classExpression) {
		if (classExpression != null) {
			return true;
		}
		for (var feature : modelElement.eClass().getEAllStructuralFeatures()) {
			if (propertyRows.isProperty(feature)
					&& propertyChange(program, modelElement, feature).changed()) {
				return true;
			}
		}
		return false;
	}
}

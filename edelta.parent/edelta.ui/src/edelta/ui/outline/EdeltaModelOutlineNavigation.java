package edelta.ui.outline;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.xbase.XExpression;

import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaModifiedFeatures;

/**
 * Selects only source regions justified by changes to the displayed Ecore definition.
 * It does not attribute migrations of individual model values.
 *
 * @author Lorenzo Bettini
 */
public final class EdeltaModelOutlineNavigation {
	private EdeltaModelOutlineNavigation() {}

	/**
	 * Returns the expression for an EClass name change that explains the current type label.
	 *
	 * @param eClass the evolving instance class
	 * @param original its imported Ecore counterpart, if any
	 * @param nameChange the recorded change to the EClass's name
	 * @param responsible the last expression recorded for the named element
	 * @return the justified expression, or {@code null}
	 */
	public static XExpression classExpression(EClass eClass, EObject original,
			EdeltaModifiedFeatures.Change nameChange, XExpression responsible) {
		if (!(original instanceof EClass originalClass)
				|| Objects.equals(originalClass.getName(), eClass.getName())) return null;
		if (nameChange == null || nameChange.initial()) return null;
		return nameChange.expression() == responsible ? responsible : null;
	}

	/**
	 * Returns an expression only when one recorded Ecore metadata change explains a visible row.
	 * Type-only changes are omitted because the row does not display its declared type.
	 *
	 * @param feature the evolving Ecore feature represented by the row
	 * @param original its imported Ecore counterpart, if any
	 * @param changes feature-specific Ecore provenance from the current derived state
	 * @return the single justified expression, or {@code null}
	 */
	public static XExpression featureExpression(EStructuralFeature feature, EObject original,
			EdeltaModifiedFeatures changes) {
		if (!(original instanceof EStructuralFeature originalFeature)) return null;
		var candidates = new ArrayList<XExpression>();
		var trustworthy = addVisibleChange(candidates, feature, originalFeature, changes,
			EcorePackage.Literals.ENAMED_ELEMENT__NAME);
		if (feature instanceof EReference) {
			trustworthy &= addVisibleChange(candidates, feature, originalFeature, changes,
				EcorePackage.Literals.ETYPED_ELEMENT__LOWER_BOUND);
			trustworthy &= addVisibleChange(candidates, feature, originalFeature, changes,
				EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND);
		} else if (feature.isMany() != originalFeature.isMany()) {
			// Attribute bounds are not printed; only a changed scalar/list shape is visible.
			trustworthy &= addVisibleChange(candidates, feature, originalFeature, changes,
				EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND);
		}
		return trustworthy && candidates.size() == 1 ? candidates.getFirst() : null;
	}

	private static boolean addVisibleChange(ArrayList<XExpression> candidates,
			EStructuralFeature feature, EStructuralFeature original, EdeltaModifiedFeatures changes,
			EStructuralFeature metadata) {
		if (Objects.equals(feature.eGet(metadata), original.eGet(metadata))) return true;
		var change = changes.get(feature, metadata);
		if (change == null || change.initial()) return false;
		candidates.add(change.expression());
		return true;
	}

	/**
	 * Uses a region only when its expression belongs to the current user's program.
	 *
	 * @param program the current program in the editor
	 * @param expression the Ecore provenance candidate, if any
	 * @param declaration the model input path region
	 * @param regions resolves significant regions in the current source
	 * @return the expression region when trustworthy, otherwise {@code declaration}
	 */
	public static ITextRegion sourceRegion(EdeltaProgram program, XExpression expression,
			ITextRegion declaration, Function<XExpression, ITextRegion> regions) {
		return sourceRegion(program, expression, null, declaration, regions);
	}

	/**
	 * Uses the first trustworthy expression, preserving Ecore navigation priority.
	 *
	 * @param program the current editor program
	 * @param primary Ecore metadata expression
	 * @param secondary attribute-migration expression
	 * @param declaration declaring model path region
	 * @param regions resolves source regions
	 * @return the first eligible expression region, or the declaration
	 */
	public static ITextRegion sourceRegion(EdeltaProgram program, XExpression primary,
			XExpression secondary, ITextRegion declaration,
			Function<XExpression, ITextRegion> regions) {
		var region = validRegion(program, primary, regions);
		if (region != null) return region;
		region = validRegion(program, secondary, regions);
		return region == null ? declaration : region;
	}

	private static ITextRegion validRegion(EdeltaProgram program, XExpression expression,
			Function<XExpression, ITextRegion> regions) {
		if (expression == null || program.eResource() == null
				|| expression.eResource() != program.eResource()
				|| !EcoreUtil.isAncestor(program, expression)) return null;
		var region = regions.apply(expression);
		return region != null && region.getLength() > 0 ? region : null;
	}
}

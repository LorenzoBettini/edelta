package edelta.ui.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * Builds finite, display-only property rows from an evolved Ecore object.
 *
 * @author Lorenzo Bettini
 */
public class EdeltaEcorePropertyRows {

	private static final int MAX_VALUES = 5;

	public record Row(EStructuralFeature feature, String label) {}

	/** Returns whether the object has displayable properties without reading their values. */
	public boolean hasRows(EObject owner) {
		return owner.eClass().getEAllStructuralFeatures().stream().anyMatch(this::isProperty);
	}

	/** Returns writable properties in Ecore feature order, including unset values. */
	public List<Row> get(EObject owner, Function<EObject, String> objectLabel) {
		var rows = new ArrayList<Row>();
		for (var feature : owner.eClass().getEAllStructuralFeatures()) {
			if (isProperty(feature)) {
				rows.add(new Row(feature,
					feature.getName() + " = " + format(owner.eGet(feature), objectLabel)));
			}
		}
		return rows;
	}

	private boolean isProperty(EStructuralFeature feature) {
		if (!feature.isChangeable() || feature.isDerived() || feature.isTransient() ||
				feature.isVolatile()) {
			return false;
		}
		return !(feature instanceof EReference reference &&
			(reference.isContainment() || reference.isContainer()));
	}

	private String format(Object value, Function<EObject, String> objectLabel) {
		if (value instanceof Collection<?> values) {
			var labels = new ArrayList<String>();
			var iterator = values.iterator();
			while (iterator.hasNext() && labels.size() < MAX_VALUES) {
				labels.add(format(iterator.next(), objectLabel));
			}
			if (iterator.hasNext()) {
				labels.add("... (+" + (values.size() - MAX_VALUES) + ")");
			}
			return "[" + String.join(", ", labels) + "]";
		}
		if (value instanceof EObject object) {
			return objectLabel.apply(object);
		}
		return value == null ? "<unset>" : value.toString();
	}
}

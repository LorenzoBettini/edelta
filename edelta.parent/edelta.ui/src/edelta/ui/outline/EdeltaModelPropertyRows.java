package edelta.ui.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.util.InternalEList;

/** Display-only rows for final preview objects; reference reads never resolve proxies.
 * @author Lorenzo Bettini
 */
public class EdeltaModelPropertyRows {
	private static final int MAX_VALUES = 5;
	private static final int MAX_SCALAR = 120;

	/** One leaf property in metamodel feature order. */
	public record Row(EStructuralFeature feature, String label) {}

	/** Whether the object has properties besides its containment hierarchy. */
	public boolean hasRows(EObject owner) {
		return owner.eClass().getEAllStructuralFeatures().stream().anyMatch(this::isProperty);
	}

	/** Formats attributes and non-containment references from the current preview object. */
	public List<Row> get(EObject owner) {
		var rows = new ArrayList<Row>();
		for (var feature : owner.eClass().getEAllStructuralFeatures()) {
			if (!isProperty(feature)) continue;
			var value = feature.isUnsettable() && !owner.eIsSet(feature)
				? null : owner.eGet(feature, false);
			var name = feature.getName();
			if (feature instanceof EReference reference) {
				name += " [" + reference.getLowerBound() + ".." +
					(reference.getUpperBound() == -1 ? "*" : reference.getUpperBound()) + "]";
			}
			rows.add(new Row(feature, name + " = " + format(value, feature instanceof EReference,
				owner)));
		}
		return rows;
	}

	/** Whether the feature is displayed as a property rather than a containment child. */
	public boolean isProperty(EStructuralFeature feature) {
		if (feature instanceof EAttribute) return true;
		var reference = (EReference) feature;
		return !reference.isContainment() && !reference.isContainer();
	}

	private String format(Object value, boolean reference, EObject owner) {
		if (value instanceof Collection<?> values) {
			var labels = new ArrayList<String>();
			var iterator = values instanceof InternalEList<?> internal
				? internal.basicIterator() : values.iterator();
			while (iterator.hasNext() && labels.size() < MAX_VALUES) {
				labels.add(formatOne(iterator.next(), reference, owner));
			}
			if (iterator.hasNext()) labels.add("... (+" + (values.size() - MAX_VALUES) + ")");
			return "[" + String.join(", ", labels) + "]";
		}
		return formatOne(value, reference, owner);
	}

	private String formatOne(Object value, boolean reference, EObject owner) {
		if (value == null) return "<unset>";
		if (reference) return targetLabel((EObject) value, owner);
		var text = value instanceof String ? "\"" + value + "\"" : value.toString();
		return text.length() <= MAX_SCALAR ? text : text.substring(0, MAX_SCALAR) + "...";
	}

	private String targetLabel(EObject target, EObject owner) {
		var label = target.eClass().getName();
		var proxy = target.eIsProxy();
		if (!proxy) {
			var name = target.eClass().getEAllAttributes().stream()
				.filter(attribute -> "name".equals(attribute.getName()) && !attribute.isMany())
				.findFirst().orElse(null);
			var value = name == null ? null : target.eGet(name, false);
			if (value != null && !value.toString().isBlank()) label += " " + value;
			else {
				var id = EcoreUtil.getID(target);
				if (id != null && !id.isBlank()) label += " #" + id;
			}
		}
		URI uri = proxy ? ((InternalEObject) target).eProxyURI()
			: EcoreUtil.getURI(target);
		var ownerURI = owner.eResource() == null ? null : owner.eResource().getURI();
		var prefix = uri.trimFragment().equals(ownerURI) ? "" : uri.lastSegment();
		label += " (" + prefix + "#" + uri.fragment() + ")";
		return label;
	}
}

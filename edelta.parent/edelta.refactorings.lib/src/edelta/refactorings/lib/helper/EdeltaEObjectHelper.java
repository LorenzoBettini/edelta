package edelta.refactorings.lib.helper;

import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

import edelta.lib.EdeltaEcoreUtil;

public class EdeltaEObjectHelper {

	public String represent(EObject o) {
		if (o == null)
			return null;
		return o.eClass().getEAllAttributes().stream()
			.map(attr -> {
				var value = o.eGet(attr);
				if (value != null) {
					return attr.getName() + " = " + value;
				} else {
					return null;
				}
			})
			.filter(Objects::nonNull)
			.collect(Collectors.joining(", "));
	}

	public String positionInContainter(EObject o) {
		var container = o.eContainer();
		if (container != null) {
			var containingFeature = o.eContainingFeature();
			if (containingFeature.isMany()) {
				var list = EdeltaEcoreUtil.getValueAsList(container, containingFeature);
				return (list.indexOf(o) + 1) + " / " + list.size();
			}
		}
		return "";
	}

}

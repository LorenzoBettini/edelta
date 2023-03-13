package edelta.refactorings.lib.helper;

import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaUtils;

public class EdeltaEObjectHelper {

	public String represent(Object o) {
		if (o == null)
			return null;
		if (o instanceof EObject) {
			EObject eObject = (EObject) o;
			var eClass = eObject.eClass();
			return
					EdeltaUtils.getEObjectRepr(eClass) + "{" +
					eClass.getEAllAttributes().stream()
					.map(attr -> {
						var value = eObject.eGet(attr);
						if (value != null) {
							return attr.getName() + " = " + value;
						} else {
							return null;
						}
					})
					.filter(Objects::nonNull)
					.collect(Collectors.joining(", ")) + "}";
			
		}
		return o.toString();
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

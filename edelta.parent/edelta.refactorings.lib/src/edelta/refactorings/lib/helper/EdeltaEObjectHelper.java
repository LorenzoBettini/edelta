package edelta.refactorings.lib.helper;

import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;

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

}

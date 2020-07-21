package edelta.resource.derivedstate;

import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.xtext.naming.QualifiedName;

/**
 * Represents an accessible {@link ENamedElement} with its {@link QualifiedName}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaAccessibleElement {

	private ENamedElement element;
	private QualifiedName qualifiedName;

	public EdeltaAccessibleElement(ENamedElement element, QualifiedName qualifiedName) {
		this.element = element;
		this.qualifiedName = qualifiedName;
	}

	public ENamedElement getElement() {
		return element;
	}

	public QualifiedName getQualifiedName() {
		return qualifiedName;
	}

}

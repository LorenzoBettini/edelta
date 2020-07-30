/**
 * 
 */
package edelta.ui.contentassist;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.EObjectDescription;

/**
 * Takes into consideration possible ambiguous elements and also stores the
 * fully {@link QualifiedName}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaContentAssistEObjectDescription extends EObjectDescription {

	private QualifiedName fullyQualifiedName;
	private boolean ambiguous;

	/**
	 * @param qualifiedName
	 * @param element
	 * @param userData
	 */
	public EdeltaContentAssistEObjectDescription(QualifiedName qualifiedName,
			QualifiedName fullyQualifiedName,
			EObject element,
			boolean ambiguous) {
		super(qualifiedName, element, null);
		this.fullyQualifiedName = fullyQualifiedName;
		this.ambiguous = ambiguous;
	}

	@Override
	public QualifiedName getQualifiedName() {
		return fullyQualifiedName;
	}

	public boolean isAmbiguous() {
		return ambiguous;
	}
}

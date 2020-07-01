package edelta.resource.derivedstate;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.xtext.naming.QualifiedName;

/**
 * The list of available elements by their {@link QualifiedName}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaAccessibleElements extends ArrayList<QualifiedName> {

	private static final long serialVersionUID = 1L;

	public EdeltaAccessibleElements() {
		super();
	}

	public EdeltaAccessibleElements(Collection<? extends QualifiedName> c) {
		super(c);
	}

}

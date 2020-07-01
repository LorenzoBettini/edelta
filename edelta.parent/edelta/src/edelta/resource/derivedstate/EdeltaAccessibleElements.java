package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashSet;

import org.eclipse.xtext.naming.QualifiedName;

/**
 * The available elements by their {@link QualifiedName}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaAccessibleElements extends HashSet<QualifiedName> {

	private static final long serialVersionUID = 1L;

	public EdeltaAccessibleElements() {
		super();
	}

	public EdeltaAccessibleElements(Collection<? extends QualifiedName> c) {
		super(c);
	}

}

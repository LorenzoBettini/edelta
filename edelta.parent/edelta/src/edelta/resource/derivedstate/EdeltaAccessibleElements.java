package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashSet;

/**
 * The accessible elements in a given context.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaAccessibleElements extends HashSet<EdeltaAccessibleElement> {

	private static final long serialVersionUID = 1L;

	public EdeltaAccessibleElements() {
		super();
	}

	public EdeltaAccessibleElements(Collection<? extends EdeltaAccessibleElement> c) {
		super(c);
	}

}

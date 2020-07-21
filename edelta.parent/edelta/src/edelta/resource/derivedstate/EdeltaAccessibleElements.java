package edelta.resource.derivedstate;

import java.util.Collection;
import java.util.HashSet;

/**
 * A collection of {@link EdeltaAccessibleElement}.
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

package edelta.resource.derivedstate;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A collection of {@link EdeltaAccessibleElement}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaAccessibleElements extends ArrayList<EdeltaAccessibleElement> {

	private static final long serialVersionUID = 1L;

	public EdeltaAccessibleElements() {
		super();
	}

	public EdeltaAccessibleElements(Collection<? extends EdeltaAccessibleElement> c) {
		super(c);
	}

}

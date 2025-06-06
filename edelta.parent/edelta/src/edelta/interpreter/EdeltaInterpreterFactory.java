package edelta.interpreter;

import org.eclipse.emf.ecore.resource.Resource;

/**
 * Creates an {@link EdeltaInterpreter}.
 * 
 * @author Lorenzo Bettini
 *
 */
public interface EdeltaInterpreterFactory {

	/**
	 * The {@link Resource} can be used to retrieve important information, like the
	 * Java project in the UI.
	 * 
	 * @param resource can be used to retrieve important information, like the Java
	 *                 project in the UI.
	 * @return
	 */
	EdeltaInterpreter create(Resource resource);

}
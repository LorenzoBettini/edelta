/**
 * 
 */
package edelta.interpreter;

import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Creates an {@link EdeltaInterpreter}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDefaultInterpreterFactory implements EdeltaInterpreterFactory {

	@Inject
	private Provider<EdeltaInterpreter> provider;

	/**
	 * The {@link Resource} can be used to retrieve important information, like the
	 * Java project in the UI.
	 * 
	 * @param resource can be used to retrieve important information, like the Java
	 *                 project in the UI.
	 * @return
	 */
	@Override
	public EdeltaInterpreter create(Resource resource) {
		return provider.get();
	}
}

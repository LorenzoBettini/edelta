/**
 * 
 */
package edelta.interpreter;

import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.Inject;
import com.google.inject.Provider;

import edelta.interpreter.internal.EdeltaInterpreterConfigurator;

/**
 * Creates an {@link IEdeltaInterpreter} and configures it with
 * {@link EdeltaInterpreterConfigurator}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaInterpreterFactory {

	@Inject
	private Provider<EdeltaInterpreter> provider;

	@Inject
	private EdeltaInterpreterConfigurator configurator;

	public EdeltaInterpreter create(Resource resource) {
		var interpreter = provider.get();
		configurator.configureInterpreter(interpreter, resource);
		return interpreter;
	}
}

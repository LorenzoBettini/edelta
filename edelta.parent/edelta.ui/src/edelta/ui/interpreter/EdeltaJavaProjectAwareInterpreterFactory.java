package edelta.ui.interpreter;

import org.eclipse.emf.ecore.resource.Resource;

import com.google.inject.Inject;

import edelta.interpreter.EdeltaInterpreter;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.ui.interpreter.internal.EdeltaJavaProjectAwareInterpreterConfigurator;

/**
 * Creates an {@link EdeltaInterpreter} and configures it with
 * {@link EdeltaJavaProjectAwareInterpreterConfigurator}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaJavaProjectAwareInterpreterFactory extends EdeltaInterpreterFactory {

	@Inject
	private EdeltaJavaProjectAwareInterpreterConfigurator configurator;

	@Override
	public EdeltaInterpreter create(Resource resource) {
		var interpreter = super.create(resource);
		configurator.configureInterpreter(interpreter, resource);
		return interpreter;
	}
}

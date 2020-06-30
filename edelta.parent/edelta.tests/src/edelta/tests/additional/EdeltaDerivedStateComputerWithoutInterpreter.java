/**
 * 
 */
package edelta.tests.additional;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import edelta.edelta.EdeltaProgram;
import edelta.resource.EdeltaDerivedStateComputer;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;

/**
 * Avoids the derived state computer run the interpreter since the tests in this
 * class must concern interpreter only and we don't want side effects from the
 * derived state computer running the interpreter.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaDerivedStateComputerWithoutInterpreter extends EdeltaDerivedStateComputer {

	@Override
	protected void runInterpreter(EdeltaProgram program, EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		// No interpreter is run
	}

	@Override
	protected void recordEcoreReferenceOriginalENamedElement(Resource resource) {
		// No recording is done
	}

	@Override
	protected void copyEPackages(List<EPackage> packages, EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		packages.stream()
			.forEach(p -> p.eAdapters().add(new EdeltaEContentAdapter()));
		super.copyEPackages(packages, copiedEPackagesMap);
	}
}

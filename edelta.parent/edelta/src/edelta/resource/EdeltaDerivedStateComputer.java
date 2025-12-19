package edelta.resource;

import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterHelper;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.scoping.EdeltaOriginalENamedElementRecorder;

@Singleton
public class EdeltaDerivedStateComputer extends JvmModelAssociator {
	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private IReferableElementsUnloader.GenericUnloader unloader;

	@Inject
	private EdeltaInterpreterFactory interpreterFactory;

	@Inject
	private EdeltaInterpreterHelper interpreterHelper;

	@Inject
	private EdeltaOriginalENamedElementRecorder originalENamedElementRecorder;

	@Override
	public void installDerivedState(final DerivedStateAwareResource resource, final boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase);
		final var program = (EdeltaProgram) resource.getContents().get(0);
		if ((!preIndexingPhase)) {
			final var modifyEcoreOperations = interpreterHelper
					.filterOperations(program.getModifyEcoreOperations());
			if (modifyEcoreOperations.isEmpty()) {
				return;
			}
			// make sure packages of the program are copied
			copyEPackages(program);
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource);
			// run the interpreter
			runInterpreter(program);
		}
	}

	protected void copyEPackages(EdeltaProgram program) {
		derivedStateHelper.copyEPackages(program);
	}

	protected void runInterpreter(final EdeltaProgram program) {
		interpreterFactory.create(program.eResource())
			.evaluateModifyEcoreOperations(program);
	}

	protected void recordEcoreReferenceOriginalENamedElement(final Resource resource) {
		final var references =
			getAllContentsOfType(resource.getContents().get(0),
				EdeltaEcoreReferenceExpression.class);
		for (var r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.getArgument());
		}
	}

	@Override
	public void discardDerivedState(final DerivedStateAwareResource resource) {
		final var copiedEPackagesMap = derivedStateHelper.getCopiedEPackagesMap(resource);
		final var derivedState = derivedStateHelper.getOrInstallAdapter(resource);
		unloadDerivedPackages(copiedEPackagesMap);
		super.discardDerivedState(resource);
		derivedState.clear();
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected void unloadDerivedPackages(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		for (final var p : copiedEPackagesMap.values()) {
			unloader.unloadRoot(p);
		}
	}
}

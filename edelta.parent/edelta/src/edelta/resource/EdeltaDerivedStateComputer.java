package edelta.resource;

import static com.google.common.collect.Iterables.filter;
import static org.eclipse.xtext.xbase.lib.IteratorExtensions.toIterable;

import java.util.List;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaProgram;
import edelta.interpreter.EdeltaInterpreterFactory;
import edelta.interpreter.EdeltaInterpreterHelper;
import edelta.lib.EdeltaEcoreUtil;
import edelta.resource.derivedstate.EdeltaCopiedEPackagesMap;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.scoping.EdeltaOriginalENamedElementRecorder;

@Singleton
public class EdeltaDerivedStateComputer extends JvmModelAssociator {
	@Inject
	private EdeltaDerivedStateHelper derivedState;

	@Inject
	private IReferableElementsUnloader.GenericUnloader unloader;

	@Inject
	private EdeltaInterpreterFactory interpreterFactory;

	@Inject
	private EdeltaInterpreterHelper interpreterHelper;

	@Inject
	private EdeltaOriginalENamedElementRecorder originalENamedElementRecorder;

	private EdeltaCopiedEPackagesMap getCopiedEPackagesMap(final Resource resource) {
		return this.derivedState.getCopiedEPackagesMap(resource);
	}

	@Override
	public void installDerivedState(final DerivedStateAwareResource resource, final boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase);
		final EdeltaProgram program = (EdeltaProgram) resource.getContents().get(0);
		if ((!preIndexingPhase)) {
			final List<EdeltaModifyEcoreOperation> modifyEcoreOperations = interpreterHelper
					.filterOperations(program.getModifyEcoreOperations());
			if (modifyEcoreOperations.isEmpty()) {
				return;
			}
			final EdeltaCopiedEPackagesMap copiedEPackagesMap = this.getCopiedEPackagesMap(resource);
			for (EdeltaModifyEcoreOperation op : modifyEcoreOperations) {
				// make sure packages under modification are copied
				this.getOrAddDerivedStateEPackage(op.getEpackage(), copiedEPackagesMap);
			}
			// we must add the copied EPackages to the resource
			resource.getContents().addAll(copiedEPackagesMap.values());
			// record original ecore references before running the interpreter
			this.recordEcoreReferenceOriginalENamedElement(resource);
			// run the interpreter
			this.runInterpreter(program, copiedEPackagesMap);
		}
	}

	protected void runInterpreter(final EdeltaProgram program, final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		this.interpreterFactory.create(program.eResource()).evaluateModifyEcoreOperations(program, copiedEPackagesMap);
	}

	protected void recordEcoreReferenceOriginalENamedElement(final Resource resource) {
		final Iterable<EdeltaEcoreReferenceExpression> references =
			filter(toIterable(resource.getAllContents()),
				EdeltaEcoreReferenceExpression.class);
		for (EdeltaEcoreReferenceExpression r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.getReference());
		}
	}

	protected EPackage getOrAddDerivedStateEPackage(final EPackage originalEPackage,
			final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		return copiedEPackagesMap.computeIfAbsent(originalEPackage.getName(),
			key -> EdeltaEcoreUtil.copyENamedElement(originalEPackage));
	}

	@Override
	public void discardDerivedState(final DerivedStateAwareResource resource) {
		final EdeltaCopiedEPackagesMap copiedEPackagesMap = this.getCopiedEPackagesMap(resource);
		this.unloadDerivedPackages(copiedEPackagesMap);
		super.discardDerivedState(resource);
		copiedEPackagesMap.clear();
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected void unloadDerivedPackages(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		for (final EPackage p : copiedEPackagesMap.values()) {
			this.unloader.unloadRoot(p);
		}
	}
}

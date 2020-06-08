package edelta.resource;

import static java.util.stream.Collectors.toList;
import static org.eclipse.xtext.EcoreUtil2.getAllContentsOfType;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.ENamedElement;
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
			final var modifyEcoreOperations = interpreterHelper
					.filterOperations(program.getModifyEcoreOperations());
			if (modifyEcoreOperations.isEmpty()) {
				return;
			}
			final var copiedEPackagesMap = this.getCopiedEPackagesMap(resource);
			// make sure packages under modification are copied
			copyEPackages(modifyEcoreOperations.stream()
				.map(EdeltaModifyEcoreOperation::getEpackage)
				.distinct()
				.collect(toList()), copiedEPackagesMap);
			// we must add the copied EPackages to the resource
			resource.getContents().addAll(copiedEPackagesMap.values());
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource);
			// run the interpreter
			runInterpreter(program, copiedEPackagesMap);
		}
	}

	protected void copyEPackages(List<EPackage> packages, EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		var copies = EdeltaEcoreUtil.copyEPackages(packages);
		copiedEPackagesMap.putAll(
			copies.stream()
				.collect(Collectors.toMap(ENamedElement::getName, Function.identity()))
		);
	}

	protected void runInterpreter(final EdeltaProgram program, final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		this.interpreterFactory.create(program.eResource()).evaluateModifyEcoreOperations(program, copiedEPackagesMap);
	}

	protected void recordEcoreReferenceOriginalENamedElement(final Resource resource) {
		final var references =
			getAllContentsOfType(resource.getContents().get(0),
				EdeltaEcoreReferenceExpression.class);
		for (var r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.getReference());
		}
	}

	@Override
	public void discardDerivedState(final DerivedStateAwareResource resource) {
		final var copiedEPackagesMap = this.getCopiedEPackagesMap(resource);
		unloadDerivedPackages(copiedEPackagesMap);
		super.discardDerivedState(resource);
		copiedEPackagesMap.clear();
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected void unloadDerivedPackages(final EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		for (final var p : copiedEPackagesMap.values()) {
			this.unloader.unloadRoot(p);
		}
	}
}

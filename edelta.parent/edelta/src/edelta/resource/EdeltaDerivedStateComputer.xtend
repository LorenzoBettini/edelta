package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaProgram
import edelta.interpreter.EdeltaInterpreterFactory
import edelta.interpreter.EdeltaInterpreterHelper
import edelta.lib.EdeltaEcoreUtil
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import edelta.services.IEdeltaEcoreModelAssociations
import edelta.util.EdeltaCopiedEPackagesMap
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader.GenericUnloader
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator implements IEdeltaEcoreModelAssociations {

	@Inject EdeltaDerivedStateHelper derivedState

	@Inject GenericUnloader unloader

	@Inject EdeltaInterpreterFactory interpreterFactory

	@Inject EdeltaInterpreterHelper interpreterHelper

	@Inject EdeltaOriginalENamedElementRecorder originalENamedElementRecorder

	override getCopiedEPackagesMap(Resource resource) {
		derivedState.getCopiedEPackagesMap(resource)
	}

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		val program = resource.contents.head as EdeltaProgram
		if (!preIndexingPhase) {
			val modifyEcoreOperations = 
				interpreterHelper.filterOperations(program.modifyEcoreOperations)
			if (modifyEcoreOperations.empty)
				return

			val copiedEPackagesMap = resource.copiedEPackagesMap

			for (epackage : modifyEcoreOperations.map[epackage]) {
				// make sure packages under modification are copied
				getOrAddDerivedStateEPackage(epackage, copiedEPackagesMap)
			}
			// we must add the copied EPackages to the resource
			resource.contents += copiedEPackagesMap.values
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource)
			// run the interpreter
			runInterpreter(
				program,
				copiedEPackagesMap
			)
		}
	}

	protected def void runInterpreter(EdeltaProgram program,
		EdeltaCopiedEPackagesMap copiedEPackagesMap
	) {
		interpreterFactory.create(program.eResource).evaluateModifyEcoreOperations(
			program,
			copiedEPackagesMap
		)
	}

	protected def void recordEcoreReferenceOriginalENamedElement(Resource resource) {
		val references = resource.allContents.
			toIterable.filter(EdeltaEcoreReferenceExpression).toList
		for (r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.reference)
		}
	}

	def protected getOrAddDerivedStateEPackage(EPackage originalEPackage,
			EdeltaCopiedEPackagesMap copiedEPackagesMap
	) {
		val referredEPackageName = originalEPackage.name
		var copiedEPackage = copiedEPackagesMap.get(referredEPackageName)
		if (copiedEPackage === null) {
			copiedEPackage = EdeltaEcoreUtil.copyENamedElement(originalEPackage)
			copiedEPackagesMap.put(referredEPackageName, copiedEPackage)
		}
		return copiedEPackage
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		val copiedEPackagesMap = resource.copiedEPackagesMap
		unloadDerivedPackages(copiedEPackagesMap)
		super.discardDerivedState(resource)
		copiedEPackagesMap.clear
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected def void unloadDerivedPackages(EdeltaCopiedEPackagesMap copiedEPackagesMap) {
		for (p : copiedEPackagesMap.values) {
			unloader.unloadRoot(p)
		}
	}

}

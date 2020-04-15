package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaProgram
import edelta.interpreter.IEdeltaInterpreter
import edelta.interpreter.internal.EdeltaInterpreterConfigurator
import edelta.lib.EdeltaEcoreUtil
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import edelta.services.IEdeltaEcoreModelAssociations
import edelta.util.EdeltaCopiedEPackagesMap
import java.util.List
import java.util.Map
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.Constants
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader.GenericUnloader
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator implements IEdeltaEcoreModelAssociations {

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	var String languageName;

	@Inject GenericUnloader unloader

	@Inject IEdeltaInterpreter interpreter

	@Inject EdeltaInterpreterConfigurator interpreterConfigurator

	@Inject EdeltaOriginalENamedElementRecorder originalENamedElementRecorder

	static class EdeltaDerivedStateAdapter extends AdapterImpl {
		var EdeltaCopiedEPackagesMap copiedEPackagesMap = new EdeltaCopiedEPackagesMap

		override boolean isAdapterForType(Object type) {
			return EdeltaDerivedStateAdapter === type;
		}
	}

	def protected EdeltaDerivedStateAdapter getOrInstallAdapter(Resource resource) {
		if (resource instanceof XtextResource) {
			val resourceLanguageName = resource.getLanguageName();
			if (resourceLanguageName == languageName) {
				var adapter = EcoreUtil.getAdapter(resource.eAdapters(), EdeltaDerivedStateAdapter) as EdeltaDerivedStateAdapter
				if (adapter === null) {
					adapter = new EdeltaDerivedStateAdapter();
					resource.eAdapters().add(adapter);
				}
				return adapter
			}
		}
		return new EdeltaDerivedStateAdapter
	}

	def protected copiedEPackagesMap(Resource resource) {
		getOrInstallAdapter(resource).copiedEPackagesMap
	}

	override copiedEPackages(Resource resource) {
		copiedEPackagesMap(resource).values
	}

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		val program = resource.contents.head as EdeltaProgram
		if (!preIndexingPhase) {
			val modifyEcoreOperations = program.modifyEcoreOperations.filter[epackage !== null]
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
			// configure and run the interpreter
			interpreterConfigurator.configureInterpreter(interpreter, resource)
			val packages = (copiedEPackagesMap.values + program.metamodels).toList
			runInterpreter(
				program,
				copiedEPackagesMap,
				packages
			)
		}
	}

	protected def void runInterpreter(EdeltaProgram program,
		EdeltaCopiedEPackagesMap copiedEPackagesMap,
		List<EPackage> packages
	) {
		interpreter.evaluateModifyEcoreOperations(
			program,
			copiedEPackagesMap,
			packages
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
			Map<String, EPackage> nameToCopiedEPackageMap
	) {
		val referredEPackageName = originalEPackage.name
		var copiedEPackage = nameToCopiedEPackageMap.get(referredEPackageName)
		if (copiedEPackage === null) {
			copiedEPackage = EdeltaEcoreUtil.copyENamedElement(originalEPackage)
			nameToCopiedEPackageMap.put(referredEPackageName, copiedEPackage)
		}
		return copiedEPackage
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		val nameToCopiedEPackageMap = resource.copiedEPackagesMap
		unloadDerivedPackages(nameToCopiedEPackageMap)
		super.discardDerivedState(resource)
		nameToCopiedEPackageMap.clear
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected def void unloadDerivedPackages(Map<String, EPackage> nameToEPackageMap) {
		for (p : nameToEPackageMap.values) {
			unloader.unloadRoot(p)
		}
	}

}

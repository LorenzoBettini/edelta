package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.edelta.EdeltaModifyEcoreOperation
import edelta.edelta.EdeltaProgram
import edelta.interpreter.IEdeltaInterpreter
import edelta.interpreter.internal.EdeltaInterpreterConfigurator
import edelta.lib.EdeltaEcoreUtil
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import edelta.services.IEdeltaEcoreModelAssociations
import java.util.List
import java.util.Map
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.Constants
import org.eclipse.xtext.common.types.JvmGenericType
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
		var Map<String, EPackage> nameToEPackageMap = newHashMap()
		var Map<String, EPackage> nameToCopiedEPackageMap = newHashMap()

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

	def protected nameToEPackageMap(Resource resource) {
		getOrInstallAdapter(resource).nameToEPackageMap
	}

	def protected nameToCopiedEPackageMap(Resource resource) {
		getOrInstallAdapter(resource).nameToCopiedEPackageMap
	}

	override derivedEPackages(Resource resource) {
		nameToEPackageMap(resource).values
	}

	override copiedEPackages(Resource resource) {
		nameToCopiedEPackageMap(resource).values
	}

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		val program = resource.contents.head as EdeltaProgram
		val programJvmType = program.jvmElements.filter(JvmGenericType).head
		if (!preIndexingPhase) {
			val nameToEPackageMap = resource.nameToEPackageMap
			val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap

			val modifyEcoreOperations = program.modifyEcoreOperations.filter[epackage !== null]
			for (epackage : modifyEcoreOperations.map[epackage]) {
				// make sure packages under modification are copied
				getOrAddDerivedStateEPackage(epackage, nameToEPackageMap, nameToCopiedEPackageMap)
			}
			// we must add only the copied and the created EPackages
			val copies = nameToCopiedEPackageMap.values
			resource.contents += copies
			resource.contents += nameToEPackageMap.values
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource)
			// configure and run the interpreter
			interpreterConfigurator.configureInterpreter(interpreter, resource)
			val packages = (nameToCopiedEPackageMap.values + program.metamodels).toList
			runInterpreter(
				modifyEcoreOperations,
				nameToCopiedEPackageMap,
				programJvmType,
				packages
			)
		}
	}

	protected def void runInterpreter(Iterable<EdeltaModifyEcoreOperation> ops,
		Map<String, EPackage> nameToCopiedEPackageMap,
		JvmGenericType programJvmType, List<EPackage> packages
	) {
		interpreter.run(
			ops,
			nameToCopiedEPackageMap,
			programJvmType,
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

	def protected getOrAddDerivedStateEPackage(EPackage referredEPackage, Map<String, EPackage> nameToEPackageMap,
			Map<String, EPackage> nameToCopiedEPackageMap
	) {
		val referredEPackageName = referredEPackage.name
		var derivedEPackage = nameToEPackageMap.get(referredEPackageName)
		var copiedEPackage = nameToCopiedEPackageMap.get(referredEPackageName)
		if (derivedEPackage === null) {
			derivedEPackage = new EdeltaDerivedStateEPackage => [
				name = referredEPackageName
			]
			nameToEPackageMap.put(referredEPackageName, derivedEPackage)
			copiedEPackage = EdeltaEcoreUtil.copyENamedElement(referredEPackage)
			nameToCopiedEPackageMap.put(referredEPackageName, copiedEPackage)
		}
		return derivedEPackage -> copiedEPackage
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		val nameToEPackageMap = resource.nameToEPackageMap
		val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap
		unloadDerivedPackages(nameToEPackageMap)
		unloadDerivedPackages(nameToCopiedEPackageMap)
		super.discardDerivedState(resource)
		nameToEPackageMap.clear
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

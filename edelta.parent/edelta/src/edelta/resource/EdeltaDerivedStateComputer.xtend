package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import edelta.edelta.EdeltaEcoreBaseEClassManipulationWithBlockExpression
import edelta.edelta.EdeltaEcoreChangeEClassExpression
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.edelta.EdeltaEcoreReference
import edelta.edelta.EdeltaEcoreReferenceExpression
import edelta.interpreter.IEdeltaInterpreter
import edelta.interpreter.internal.EdeltaInterpreterConfigurator
import edelta.lib.EdeltaEcoreUtil
import edelta.lib.EdeltaLibrary
import edelta.scoping.EdeltaOriginalENamedElementRecorder
import edelta.services.IEdeltaEcoreModelAssociations
import java.util.List
import java.util.Map
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.Constants
import org.eclipse.xtext.EcoreUtil2
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

	@Inject extension EdeltaLibrary

	@Inject GenericUnloader unloader

	@Inject EdeltaChangeRunner changeRunner

	@Inject IEdeltaInterpreter interpreter

	@Inject EdeltaInterpreterConfigurator interpreterConfigurator

	@Inject EdeltaOriginalENamedElementRecorder originalENamedElementRecorder

	public static class EdeltaDerivedStateAdapter extends AdapterImpl {
		var Map<EObject, EObject> targetToSourceMap = newHashMap()
		var Map<String, EPackage> nameToEPackageMap = newHashMap()
		var Map<String, EPackage> nameToCopiedEPackageMap = newHashMap()
		var Map<EdeltaEcoreBaseEClassManipulationWithBlockExpression, EClass>
			opToEClassMap = newHashMap

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

	def protected derivedToSourceMap(Resource resource) {
		getOrInstallAdapter(resource).targetToSourceMap
	}

	def protected nameToEPackageMap(Resource resource) {
		getOrInstallAdapter(resource).nameToEPackageMap
	}

	def protected nameToCopiedEPackageMap(Resource resource) {
		getOrInstallAdapter(resource).nameToCopiedEPackageMap
	}

	def protected opToEClassMap(Resource resource) {
		getOrInstallAdapter(resource).opToEClassMap
	}

	override derivedEPackages(Resource resource) {
		nameToEPackageMap(resource).values
	}

	override copiedEPackages(Resource resource) {
		nameToCopiedEPackageMap(resource).values
	}

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		val programJvmType = resource.contents.head.jvmElements.filter(JvmGenericType).head
		if (!preIndexingPhase) {
			val targetToSourceMap = resource.derivedToSourceMap
			val nameToEPackageMap = resource.nameToEPackageMap
			val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap
			val opToEClassMap = resource.opToEClassMap

			val createEClassExpressions = resource.allContents.toIterable.filter(EdeltaEcoreCreateEClassExpression).toList
			for (exp : createEClassExpressions) {
				val derivedEClass = createDerivedStateEClass(exp.name, exp.ecoreReferenceSuperTypes) => [
					// could be null in an incomplete expression
					addToDerivedEPackage(nameToEPackageMap, nameToCopiedEPackageMap, exp.epackage)
				]
				targetToSourceMap.put(derivedEClass, exp)
				opToEClassMap.put(exp, derivedEClass)
				handleCreateEAttribute(exp, derivedEClass, targetToSourceMap)
			}
			val changeEClassExpressions = resource.
				allContents.toIterable.
				filter(EdeltaEcoreChangeEClassExpression).
				filter[original !== null].
				toList
			for (exp : changeEClassExpressions) {
				getOrAddDerivedStateEPackage(exp.epackage, nameToEPackageMap, nameToCopiedEPackageMap)
			}
			// we must add only the copied and the created EPackages
			resource.contents += nameToCopiedEPackageMap.values
			resource.contents += nameToEPackageMap.values
			// now that all derived EPackages are created let's start processing
			// changes to EClasses
			for (exp : changeEClassExpressions) {
				val derivedEClass = EdeltaEcoreUtil.copyENamedElement(exp.original)
				addToDerivedEPackage(derivedEClass, nameToEPackageMap, nameToCopiedEPackageMap, exp.epackage)
				targetToSourceMap.put(derivedEClass, exp)
				opToEClassMap.put(exp, derivedEClass)
				changeRunner.performChanges(derivedEClass, exp)
				handleCreateEAttribute(exp, derivedEClass, targetToSourceMap)
			}
			// record original ecore references before running the interpreter
			recordEcoreReferenceOriginalENamedElement(resource)
			// configure and run the interpreter
			interpreterConfigurator.configureInterpreter(interpreter, resource)
			runInterpreter(createEClassExpressions, opToEClassMap, programJvmType)
			runInterpreter(changeEClassExpressions, opToEClassMap, programJvmType)
		}
	}

	protected def void runInterpreter(List<? extends EdeltaEcoreBaseEClassManipulationWithBlockExpression> expressions,
		Map<EdeltaEcoreBaseEClassManipulationWithBlockExpression, EClass> opToEClassMap,
		JvmGenericType jvmGenericType
	) {
		for (e : expressions) {
			interpreter.run(
				e,
				opToEClassMap.get(e),
				jvmGenericType
			)
		}
	}

	protected def void recordEcoreReferenceOriginalENamedElement(Resource resource) {
		val references = resource.allContents.
			toIterable.filter(EdeltaEcoreReferenceExpression).toList
		for (r : references) {
			originalENamedElementRecorder.recordOriginalENamedElement(r.reference)
		}
	}

	private def void handleCreateEAttribute(EdeltaEcoreBaseEClassManipulationWithBlockExpression exp, EClass derivedEClass, Map<EObject, EObject> targetToSourceMap) {
		for (e : EcoreUtil2.getAllContentsOfType(exp, EdeltaEcoreCreateEAttributeExpression)) {
			val derivedEAttribute = newEAttribute(e.name)
			derivedEClass.EStructuralFeatures += derivedEAttribute
			targetToSourceMap.put(derivedEAttribute, e)
		}
	}

	def private createDerivedStateEClass(String name, List<EdeltaEcoreReference> refs) {
		new EdeltaDerivedStateEClass(refs) => [
			it.name = name
		]
	}

	/**
	 * We must also create fake/derived EPackages, since our EClasses must be
	 * in a package with the same name as the original referred one, but we
	 * must not add them to the original referred package or we would mess
	 * with Ecore original packages.
	 */
	def private addToDerivedEPackage(EClass created, Map<String, EPackage> nameToEPackageMap,
			Map<String, EPackage> nameToCopiedEPackageMap, EPackage referredEPackage
	) {
		if (referredEPackage !== null) {
			var derivedEPackage = getOrAddDerivedStateEPackage(referredEPackage, nameToEPackageMap, nameToCopiedEPackageMap)
			derivedEPackage.EClassifiers += created
		}
	}

	def private EPackage getOrAddDerivedStateEPackage(EPackage referredEPackage, Map<String, EPackage> nameToEPackageMap,
			Map<String, EPackage> nameToCopiedEPackageMap
	) {
		val referredEPackageName = referredEPackage.name
		var derivedEPackage = nameToEPackageMap.get(referredEPackageName)
		if (derivedEPackage === null) {
			derivedEPackage = new EdeltaDerivedStateEPackage => [
				name = referredEPackageName
			]
			nameToEPackageMap.put(referredEPackageName, derivedEPackage)
			nameToCopiedEPackageMap.put(referredEPackageName, EdeltaEcoreUtil.copyENamedElement(referredEPackage))
		}
		derivedEPackage
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		val derivedToSourceMap = resource.derivedToSourceMap
		val nameToEPackageMap = resource.nameToEPackageMap
		val nameToCopiedEPackageMap = resource.nameToCopiedEPackageMap
		val opToEClassMap = resource.opToEClassMap
		unloadDerivedPackages(nameToEPackageMap)
		unloadDerivedPackages(nameToCopiedEPackageMap)
		super.discardDerivedState(resource)
		derivedToSourceMap.clear
		nameToEPackageMap.clear
		nameToCopiedEPackageMap.clear
		opToEClassMap.clear
	}

	/**
	 * Unload (turn them into proxies) all derived Ecore elements
	 */
	protected def void unloadDerivedPackages(Map<String, EPackage> nameToEPackageMap) {
		for (p : nameToEPackageMap.values) {
			unloader.unloadRoot(p)
		}
	}

	override getPrimarySourceElement(EObject element) {
		if (element !== null) {
			val sourceElement = element.resource.derivedToSourceMap.get(element)
			if (sourceElement !== null)
				return sourceElement
		}
		return super.getPrimarySourceElement(element)
	}

}

package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.lib.EdeltaLibrary
import java.util.Map
import org.eclipse.emf.common.notify.impl.AdapterImpl
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.Constants
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.parser.antlr.IReferableElementsUnloader.GenericUnloader
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.resource.XtextResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator {

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	var String languageName;

	@Inject extension EdeltaLibrary

	@Inject GenericUnloader unloader

	public static class EdeltaDerivedStateAdapter extends AdapterImpl {
		var Map<EObject, EObject> targetToSourceMap = newHashMap()
		var Map<String, EPackage> nameToEPackageMap = newHashMap()
	
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

	def derivedEPackages(Resource resource) {
		nameToEPackageMap(resource).values
	}

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		if (!preIndexingPhase) {
			val targetToSourceMap = resource.derivedToSourceMap
			val nameToEPackageMap = resource.nameToEPackageMap
			for (exp :resource.allContents.toIterable.filter(EdeltaEcoreCreateEClassExpression)) {
				val derivedEClass = newEClass(exp.name) => [
					// could be null in an incomplete expression
					addToDerivedEPackage(nameToEPackageMap, exp.epackage)
				]
				targetToSourceMap.put(derivedEClass, exp)
				for (e : EcoreUtil2.getAllContentsOfType(exp, EdeltaEcoreCreateEAttributeExpression)) {
					val derivedEAttribute = newEAttribute(e.name)
					derivedEClass.EStructuralFeatures += derivedEAttribute
					targetToSourceMap.put(derivedEAttribute, e)
				}
			}
			// we must add only the created EPackages
			resource.contents += nameToEPackageMap.values
		}
	}

	/**
	 * We must also create fake/derived EPackages, since our EClasses must be
	 * in a package with the same name as the original referred one, but we
	 * must not add them to the original referred package or we would mess
	 * with Ecore original packages.
	 */
	def private addToDerivedEPackage(EClass created, Map<String, EPackage> nameToEPackageMap, EPackage referredEPackage) {
		if (referredEPackage !== null) {
			val referredEPackageName = referredEPackage.name
			var derivedEPackage = nameToEPackageMap.get(referredEPackageName)
			if (derivedEPackage === null) {
				derivedEPackage = EcoreFactory.eINSTANCE.createEPackage => [
					name = referredEPackageName
				]
				nameToEPackageMap.put(referredEPackageName, derivedEPackage)
			}
			derivedEPackage.EClassifiers += created
		}
	}

	override discardDerivedState(DerivedStateAwareResource resource) {
		val derivedToSourceMap = resource.derivedToSourceMap
		val nameToEPackageMap = resource.nameToEPackageMap
		unloadDerivedPackages(nameToEPackageMap)
		super.discardDerivedState(resource)
		derivedToSourceMap.clear
		nameToEPackageMap.clear
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

package edelta.resource

import com.google.inject.Inject
import com.google.inject.Singleton
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.lib.EdeltaLibrary
import java.util.Map
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.resource.DerivedStateAwareResource
import org.eclipse.xtext.xbase.jvmmodel.JvmModelAssociator

@Singleton
class EdeltaDerivedStateComputer extends JvmModelAssociator {

	@Inject extension EdeltaLibrary

	var Map<EObject, EObject> targetToSourceMap = newHashMap()

	var Map<String, EPackage> nameToEPackageMap = newHashMap()

	override installDerivedState(DerivedStateAwareResource resource, boolean preIndexingPhase) {
		super.installDerivedState(resource, preIndexingPhase)
		if (!preIndexingPhase) {
			targetToSourceMap.clear
			nameToEPackageMap.clear
			for (exp :resource.allContents.toIterable.filter(EdeltaEcoreCreateEClassExpression)) {
				val derivedEClass = newEClass(exp.name) => [
					// could be null in an incomplete expression
					addToDerivedEPackage(exp.epackage)
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
	def private addToDerivedEPackage(EClass created, EPackage referredEPackage) {
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
		super.discardDerivedState(resource)
		targetToSourceMap.clear
		nameToEPackageMap.clear
	}

	override getPrimarySourceElement(EObject jvmElement) {
		if (jvmElement !== null) {
			val sourceElement = targetToSourceMap.get(jvmElement)
			if (sourceElement !== null)
				return sourceElement
		}
		return super.getPrimarySourceElement(jvmElement)
	}

}

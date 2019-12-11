package edelta.refactorings.lib.tests

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage

abstract class AbstractTest {

	protected var factory = EcoreFactory.eINSTANCE

	protected var stringDataType = EcorePackage.eINSTANCE.EString

	protected var intDataType = EcorePackage.eINSTANCE.EInt

	protected var eClassReference = EcorePackage.eINSTANCE.EClass

	def protected createEClass(EPackage epackage, String name) {
		val c = createEClass(name)
		epackage.EClassifiers += c
		return c
	}

	protected def EClass createEClass(String name) {
		factory.createEClass => [
			it.name = name
		]
	}

	def protected createEAttribute(EClass eclass, String name) {
		val a = factory.createEAttribute => [
			it.name = name
		]
		eclass.EStructuralFeatures += a
		return a
	}

	def protected createEReference(EClass eclass, String name) {
		val a = factory.createEReference => [
			it.name = name
		]
		eclass.EStructuralFeatures += a
		return a
	}

	def protected EClasses(EPackage p) {
		p.EClassifiers.filter(EClass)
	}

	def protected findEClassifier(EPackage p, String byName) {
		p.EClassifiers.findFirst[name == byName]
	}

	def protected findEAttribute(EClass c, String byName) {
		c.EAttributes.findFirst[name == byName]
	}
}

package edelta.refactorings.lib.tests

import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EcoreFactory
import org.eclipse.emf.ecore.EcorePackage
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EEnumLiteral

abstract class AbstractTest {

	protected var factory = EcoreFactory.eINSTANCE

	protected var stringDataType = EcorePackage.eINSTANCE.EString

	protected var intDataType = EcorePackage.eINSTANCE.EInt

	protected var eClassReference = EcorePackage.eINSTANCE.EClass

	protected static final String MODIFIED = "modified/";
	protected static final String TESTECORES = "test-input-models/";
	protected static final String EXPECTATIONS = "test-output-expectations/";


	def protected createEClass(EPackage epackage, String name) {
		val c = createEClassWithoutPackage(name)
		epackage.EClassifiers += c
		return c
	}

	protected def EClass createEClassWithoutPackage(String name) {
		factory.createEClass => [
			it.name = name
		]
	}

	def protected createEEnum(EPackage epackage, String name) {
		val e = factory.createEEnum => [
			it.name = name
		]
		epackage.EClassifiers += e
		return e
	}

	def protected EEnumLiteral createEEnumLiteral(EEnum en, String name) {
		val e = factory.createEEnumLiteral => [
			it.name = name
		]
		en.ELiterals += e
		return e
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

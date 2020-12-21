package edelta.tests.input

class Inputs {
	def referenceToMetamodel() {
		'''
			metamodel "foo"
		'''
	}

	def referenceToMetamodelWithCopiedEPackage() {
		'''
			metamodel "foo"
			
			// that's required to have copied EPackages
			modifyEcore aTest foo {}
		'''
	}

	def referencesToMetamodels() {
		'''
			metamodel "foo"
			metamodel "bar"
		'''
	}

	def referencesToMetamodelsWithCopiedEPackages() {
		'''
			metamodel "foo"
			metamodel "bar"
			
			// that's required to have copied EPackages
			modifyEcore aTest foo {}
		'''
	}

	def referenceToMetamodelWithSubPackage() {
		'''
			metamodel "mainpackage"
		'''
	}

	def referenceToMetamodelWithSubPackageWithCopiedEPackages() {
		'''
			metamodel "mainpackage"
			
			// that's required to have copied EPackages
			modifyEcore aTest mainpackage {}
		'''
	}

	def useImportedJavaTypes() {
		'''
		import java.util.List

		package foo;
		
		def bar(List<String> s) {
			s.empty
		}
		'''
	}

	def operationWithInferredReturnType() {
		'''
		package foo;
		
		def bar(String s) {
			s.empty
		}
		'''
	}

	def operationWithReturnType() {
		'''
		package foo;
		
		def bar(String s) : boolean {
			s.empty
		}
		'''
	}

	def operationAccessingLib() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s)
		}
		'''
	}

	def operationNewEClassWithInitializer() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s) [
				ESuperTypes += newEClass("Base")
			]
		}
		'''
	}

	def referenceToCreatedEClass() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
			}
			modifyEcore anotherTest epackage foo {
				ecoreref(NewClass)
			}
		'''
	}

	def referenceToCreatedEAttributeSimple() {
		'''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				addNewEClass("NewClass") [
					addNewEAttribute("newAttribute", ecoreref(FooDataType))
					addNewEAttribute("newAttribute2", ecoreref(FooDataType))
				]
				ecoreref(newAttribute)
			}
		'''
	}

	def referenceToCreatedEAttributeRenamed() {
		'''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				addNewEClass("NewClass") [
					addNewEAttribute("newAttribute", ecoreref(FooDataType))
				]
			}
			
			modifyEcore changeAndAccess epackage foo {
				ecoreref(newAttribute).name = "changed"
				ecoreref(changed)
			}
		'''
	}

	def referenceToEClassRemoved() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				EClassifiers -= ecoreref(FooClass)
				ecoreref(FooClass).abstract // this doesn't exist anymore
			}
		'''
	}

	def referenceToEClassRenamed() {
		'''
			metamodel "foo"
			
			modifyEcore renaming epackage foo {
				ecoreref(FooClass).name = "Renamed"
				ecoreref(FooClass).abstract // this doesn't exist anymore
			}
		'''
	}

	def referenceToCreatedEClassRenamed() {
		'''
			metamodel "foo"
			
			modifyEcore creation epackage foo {
				addNewEClass("NewClass")
			}
			modifyEcore renaming epackage foo {
				ecoreref(NewClass).name = "changed"
			}
			modifyEcore accessing epackage foo {
				ecoreref(NewClass) // this doesn't exist anymore
				ecoreref(changed)
			}
		'''
	}

	def useAsCustomEdeltaCreatingEClass() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			modifyEcore aTest epackage foo {
				my.createANewEAttribute(
					my.createANewEClass)
			}
		'''
	}

	def useAsCustomEdeltaAsExtensionCreatingEClass() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as extension my
			
			modifyEcore aTest epackage foo {
				createANewEClass.createANewEAttribute
			}
		'''
	}

	def useAsCustomStatefulEdeltaCreatingEClass() {
		'''
			import edelta.tests.additional.MyCustomStatefulEdelta
			
			metamodel "foo"
			
			use MyCustomStatefulEdelta as my
			
			modifyEcore aTest epackage foo {
				my.createANewEAttribute(
					my.createANewEClass)
			}
			modifyEcore anotherTest epackage foo {
				my.createANewEAttribute(
					my.createANewEClass)
			}
		'''
	}

	def createEClassStealingAttribute() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					addEAttribute(ecoreref(FooClass.myAttribute))
				]
			}
		'''
	}

	def changeEClassRemovingAttribute() {
		'''
			metamodel "foo"
			
			modifyEcore aTest epackage foo {
				ecoreref(FooClass).EStructuralFeatures -= ecoreref(FooClass.myAttribute)
			}
		'''
	}

	def modifyEcoreUsingLibMethods() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("ANewClass") [
				addNewEAttribute("ANewAttribute", ecoreref(FooDataType)) [
					lowerBound = 1
				]
				addNewEReference("ANewReference", ecoreref(FooClass)) [
					lowerBound = 1
				]
			]
			addNewEEnum("ANewEnum") [
				addNewEEnumLiteral("ANewEnumLiteral") [
					value = 10
				]
			]
			addNewEDataType("ANewDataType", "java.lang.String")
			ecoreref(ANewClass)
			ecoreref(ANewClass.ANewAttribute)
			ecoreref(ANewClass.ANewReference)
			ecoreref(ANewEnum)
			ecoreref(ANewEnum.ANewEnumLiteral)
			ecoreref(ANewDataType)
		}
		'''
	}

	def personListExampleModifyEcore()
	'''
		import edelta.refactorings.lib.EdeltaRefactorings
		
		// IMPORTANT: ecores must be in a source directory
		// otherwise you can't refer to them
		package edelta.personlist.example
		
		metamodel "PersonList"
		metamodel "ecore"
		
		use EdeltaRefactorings as extension refactorings
		
		modifyEcore improvePerson epackage PersonList {
			ecoreref(PersonList.Person) => [
				// since 'refactorings' is an 'extension'
				// we use its method as an extension method
				introduceSubclasses(
					ecoreref(Person.gender),
					ecoreref(Gender)
				)
			]
			refactorings.mergeFeatures("name",
				#[ecoreref(Person.firstname), ecoreref(Person.lastname)])
		}
		
		modifyEcore introducePlace epackage PersonList {
			extractSuperclass("Place",
				#[ecoreref(LivingPlace.address), ecoreref(WorkPlace.address)])
		}
		
		modifyEcore introduceWorkingPosition epackage PersonList {
			referenceToClass("WorkingPosition", ecoreref(Person.works)) => [
				addNewEAttribute("description", ecoreref(EString))
			]
			ecoreref(WorkPlace.persons).name = "position"
		}
		
		modifyEcore improveList epackage PersonList {
			ecoreref(PersonList.List).addEReference(
				refactorings.mergeReferences("places",
					ecoreref(Place),
					#[ecoreref(List.wplaces), ecoreref(List.lplaces)]
				)
			)
		}
		
	'''
}

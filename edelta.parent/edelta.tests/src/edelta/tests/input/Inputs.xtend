package edelta.tests.input

class Inputs {
	def referenceToMetamodel() {
		'''
			metamodel "foo"
		'''
	}

	def referencesToMetamodels() {
		'''
			metamodel "foo"
			metamodel "bar"
		'''
	}

	def referenceToEPackage() {
		'''
			metamodel "foo"
			
			ecoreref(foo)
		'''
	}

	def referenceToEClass() {
		'''
			metamodel "foo"
			
			ecoreref(FooClass)
		'''
	}

	def referenceToEDataType() {
		'''
			metamodel "foo"
			
			ecoreref(FooDataType)
		'''
	}

	def referenceToEEnum() {
		'''
			metamodel "foo"
			
			ecoreref(FooEnum)
		'''
	}

	def referenceToEAttribute() {
		'''
			metamodel "foo"
			
			ecoreref(myAttribute)
		'''
	}

	def referenceToEReference() {
		'''
			metamodel "foo"
			
			ecoreref(myReference)
		'''
	}

	def referenceToEEnumLiteral() {
		'''
			metamodel "foo"
			
			ecoreref(FooEnumLiteral)
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

	def programWithMainExpression() {
		'''
		package foo;
		
		def bar(String s) {
			newEClass(s)
		}
		
		println(bar("foo"))
		'''
	}

	def ecoreReferenceExpressions() {
		'''
		package foo;
		
		metamodel "foo"
		
		ecoreref(foo)
		println(ecoreref(foo))
		ecoreref(FooClass)
		println(ecoreref(FooClass))
		ecoreref(myAttribute)
		println(ecoreref(myAttribute))
		ecoreref(FooEnum)
		println(ecoreref(FooEnum))
		ecoreref(FooEnumLiteral)
		println(ecoreref(FooEnumLiteral))
		val ref = ecoreref(myReference)
		'''
	}

	def createEClass() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo {}
			
			createEClass MyDerivedNewClass in foo {
				ESuperTypes += ecoreref(MyNewClass)
			}
		'''
	}

	def createEClassWithSuperTypes() {
		'''
			metamodel "foo"
			
			createEClass MyNewClass in foo
				extends FooClass
			{
				
			}
		'''
	}

	def createEClassWithSuperTypes2() {
		'''
			metamodel "foo"
			
			createEClass BaseClass in foo {}
			
			createEClass MyNewClass in foo
				extends FooClass, BaseClass
			{
				
			}
		'''
	}

	def referenceToCreatedEClass() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(NewClass)
		'''
	}

	def createEClassAndReferenceToExistingEDataType() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(FooDataType)
		'''
	}

	def createEClassAndReferenceToExistingEDataTypeFullyQualified() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {}
			ecoreref(foo.FooDataType)
		'''
	}

	def referenceToCreatedEClassWithTheSameNameAsAnExistingEClass() {
		'''
			metamodel "foo"
			
			createEClass FooClass in foo {}
			ecoreref(FooClass)
		'''
	}

	def referenceToChangedEClassWithTheSameNameAsAnExistingEClass() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass {}
			ecoreref(FooClass)
		'''
	}

	def referenceToChangedEClassWithANewName() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass newName RenamedClass {
				createEAttribute anotherAttr type FooDataType {
				}
			}
			ecoreref(RenamedClass)
		'''
	}

	def referenceToChangedEClassCopiedAttribute() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass {
				val attr = ecoreref(FooClass.myAttribute)
			}
		'''
	}

	def referenceToCreatedEAttributeSimple() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				createEAttribute newAttribute type FooDataType {}
				createEAttribute newAttribute2 type FooDataType {}
			}
			ecoreref(newAttribute)
		'''
	}

	def referenceToCreatedEAttributeRenamed() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				createEAttribute newAttribute type FooDataType {
					name = "changed"
				}
				createEAttribute newAttribute2 type FooDataType {}
			}
			ecoreref(newAttribute)
			ecoreref(changed)
		'''
	}

	def referenceToCreatedEAttributeRenamedInChangedEClass() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass {
				createEAttribute newAttribute type FooDataType {
					name = "changed"
				}
			}
			ecoreref(newAttribute)
			ecoreref(changed)
		'''
	}

	def referenceToCreatedEClassRenamed() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				name = "changed"
			}
			ecoreref(NewClass)
			ecoreref(changed)
		'''
	}

	def referenceToChangedEClassRenamed() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass {
				name = "changed"
			}
			ecoreref(FooClass)
			ecoreref(changed)
		'''
	}

	def useAs() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			my.myMethod()
		'''
	}

	def useAs2() {
		'''
			import edelta.tests.additional.MyCustomEdelta
			
			metamodel "foo"
			
			use MyCustomEdelta as my
			
			my.createANewEClass()
		'''
	}

	def createEClassStealingAttribute() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				val attr = ecoreref(FooClass.myAttribute)
				EStructuralFeatures += attr
			}
		'''
	}

	def changeEClassRemovingAttribute() {
		'''
			metamodel "foo"
			
			changeEClass foo.FooClass {
				val attr = ecoreref(FooClass.myAttribute)
				EStructuralFeatures -= attr
			}
		'''
	}

	def createEClassAndAddEAttributeUsingLibMethod() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				EStructuralFeatures += newEAttribute("newTestAttr") [
					EType = ecoreref(FooDataType)
				]
			}
		'''
	}

	def createEClassAndAddEAttributeUsingLibMethodAndReference() {
		'''
			metamodel "foo"
			
			createEClass NewClass in foo {
				EStructuralFeatures += newEAttribute("newTestAttr") [
					EType = ecoreref(FooDataType)
				]
			}
			
			ecoreref(newTestAttr)
		'''
	}

	def createEClassUsingLibMethods() {
		'''
		import org.eclipse.emf.ecore.EClass

		metamodel "foo"

		createEClass ANewClass in foo {
			addNewEAttribute("ANewAttribute", ecoreref(FooDataType)) [
				lowerBound = 1
			]
			addNewEReference("ANewReference", ecoreref(FooClass)) [
				lowerBound = 1
			]
			// the containing EPackage
			getEPackage => [
				addNewEEnum("ANewEnum") [
					addNewEEnumLiteral("ANewEnumLiteral") [
						value = 10
					]
				]
				addNewEDataType("ANewDataType", "java.lang.String")
			]
			ecoreref(ANewClass)
			ecoreref(ANewClass.ANewAttribute)
			ecoreref(ANewClass.ANewReference)
			ecoreref(ANewEnum)
			ecoreref(ANewEnum.ANewEnumLiteral)
			ecoreref(ANewDataType)
		}
		'''
	}

	def personListExample()
	'''
		import gssi.refactorings.MMrefactorings
		import org.eclipse.emf.ecore.EEnum
		
		package gssi.personexample
		
		metamodel "PersonList"
		metamodel "ecore"
		
		use MMrefactorings as refactorings
		
		changeEClass PersonList.Person {
			refactorings.
				introduceSubclasses(
					ecoreref(Person.gender),
					ecoreref(Person.gender).EAttributeType as EEnum,
					it
				);
			EStructuralFeatures+=
				refactorings.mergeAttributes("name",
					ecoreref(Person.firstname).EType,
					#[ecoreref(Person.firstname),ecoreref(Person.lastname)]
				);
		}
		
		createEClass Place in PersonList {
			abstract = true
			refactorings.extractSuperclass(it,
				#[ecoreref(LivingPlace.address), ecoreref(WorkPlace.address)]);
		}
		
		createEClass WorkingPosition in PersonList {
			createEAttribute description type EString {}
			refactorings.extractMetaClass(it,ecoreref(Person.works),"position","works");
		}
		
		changeEClass PersonList.List {
			EStructuralFeatures+=
				refactorings.mergeReferences("places",
					ecoreref(Place),
					#[ecoreref(List.wplaces),ecoreref(List.lplaces)]
				);	
		}
	'''

}

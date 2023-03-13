package edelta.tests.input;

public class Inputs {
	public CharSequence referenceToMetamodel() {
		return """
			metamodel "foo"
			""";
	}

	public CharSequence referenceToMetamodelWithCopiedEPackage() {
		return """
			metamodel "foo"

			// that's required to have copied EPackages
			modifyEcore aTest foo {}
			""";
	}

	public CharSequence referencesToMetamodels() {
		return """
			metamodel "foo"
			metamodel "bar"
			""";
	}

	public CharSequence referencesToMetamodelsWithCopiedEPackages() {
		return """
			metamodel "foo"
			metamodel "bar"

			// that's required to have copied EPackages
			modifyEcore aTest foo {}
			""";
	}

	public CharSequence referenceToMetamodelWithSubPackage() {
		return """
			metamodel "mainpackage"
			""";
	}

	public CharSequence referenceToMetamodelWithSubPackageWithCopiedEPackages() {
		return """
			metamodel "mainpackage"

			// that's required to have copied EPackages
			modifyEcore aTest mainpackage {}
			""";
	}

	public CharSequence useImportedJavaTypes() {
		return """
			import java.util.List

			package foo;

			def bar(List<String> s) {
				s.empty
			}
			""";
	}

	public CharSequence operationWithInferredReturnType() {
		return """
			package foo;

			def bar(String s) {
				s.empty
			}
			""";
	}

	public CharSequence operationWithReturnType() {
		return """
			package foo;

			def bar(String s) : boolean {
				s.empty
			}
			""";
	}

	public CharSequence operationAccessingLib() {
		return """
			package foo;

			metamodel "foo"

			def bar(String s) {
				newEClass(s)
				createInstance(ecoreref(FooClass)) []
			}
			""";
	}

	public CharSequence operationNewEClassWithInitializer() {
		return """
			package foo;

			def bar(String s) {
				newEClass(s) [
					ESuperTypes += newEClass("Base")
				]
			}
			""";
	}

	public CharSequence referenceToCreatedEClass() {
		return """
			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass")
			}
			modifyEcore anotherTest epackage foo {
				ecoreref(NewClass)
			}
			""";
	}

	public CharSequence referenceToCreatedEAttributeSimple() {
		return """
			metamodel "foo"

			modifyEcore creation epackage foo {
				addNewEClass("NewClass") [
					addNewEAttribute("newAttribute", ecoreref(FooDataType))
					addNewEAttribute("newAttribute2", ecoreref(FooDataType))
				]
				ecoreref(newAttribute)
			}
			""";
	}

	public CharSequence referenceToCreatedEAttributeRenamed() {
		return """
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
			""";
	}

	public CharSequence referenceToEClassRemoved() {
		return """
			metamodel "foo"

			modifyEcore aTest epackage foo {
				EClassifiers -= ecoreref(FooClass)
				ecoreref(FooClass).abstract // this doesn't exist anymore
			}
			""";
	}

	public CharSequence referenceToEClassRenamed() {
		return """
			metamodel "foo"

			modifyEcore renaming epackage foo {
				ecoreref(FooClass).name = "Renamed"
				ecoreref(FooClass).abstract // this doesn't exist anymore
			}
			""";
	}

	public CharSequence referenceToCreatedEClassRenamed() {
		return """
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
			""";
	}

	public CharSequence useAsCustomEdeltaCreatingEClass() {
		return """
			import edelta.tests.additional.MyCustomEdelta

			metamodel "foo"

			use MyCustomEdelta as my

			modifyEcore aTest epackage foo {
				my.createANewEAttribute(
					my.createANewEClass)
			}
			""";
	}

	public CharSequence useAsCustomEdeltaAsExtensionCreatingEClass() {
		return """
			import edelta.tests.additional.MyCustomEdelta

			metamodel "foo"

			use MyCustomEdelta as extension my

			modifyEcore aTest epackage foo {
				createANewEClass.createANewEAttribute
			}
			""";
	}

	public CharSequence useAsCustomStatefulEdeltaCreatingEClass() {
		return """
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
			""";
	}

	public CharSequence createEClassStealingAttribute() {
		return """
			metamodel "foo"

			modifyEcore aTest epackage foo {
				addNewEClass("NewClass") [
					addEAttribute(ecoreref(FooClass.myAttribute))
				]
			}
			""";
	}

	public CharSequence changeEClassRemovingAttribute() {
		return """
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(FooClass).EStructuralFeatures -= ecoreref(FooClass.myAttribute)
			}
			""";
	}

	public CharSequence modifyEcoreUsingLibMethods() {
		return """
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
			""";
	}

	public CharSequence personListExampleModifyEcore() {
		return """
			import edelta.refactorings.lib.EdeltaRefactorings

			// IMPORTANT: ecores must be in a source directory
			// otherwise you can't refer to them
			package edelta.personlist.example

			metamodel "PersonList"
			metamodel "ecore"

			use EdeltaRefactorings as extension refactorings

			modifyEcore improvePerson epackage PersonList {
				// since 'refactorings' is an 'extension'
				// we use its method as an extension method
				ecoreref(Person.gender).enumToSubclasses()
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
				refactorings.mergeFeatures("places",
					ecoreref(Place),
					#[ecoreref(List.wplaces), ecoreref(List.lplaces)]
				)
			}

			""";
	}
}

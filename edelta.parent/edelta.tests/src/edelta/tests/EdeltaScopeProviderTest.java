package edelta.tests;

import static com.google.common.collect.Iterables.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.join;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaPackage;
import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaScopeProviderTest extends EdeltaAbstractTest {
	@Inject
	private IScopeProvider scopeProvider;

	@Test
	public void testSuperScope() throws Exception {
		// just check that nothing wrong happens when we call super.getScope
		var scope = scopeProvider.getScope(
			getBlockLastExpression(lastModifyEcoreOperation(
				parseHelper.parse("""
				modifyEcore aTest epackage foo {
					this.
				}
				""")).getBody()),
			EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation_Body());
		assertNotNull(scope);
	}

	@Test
	public void testScopeForMetamodel() throws Exception {
		// we skip nsURI references, like http://foo
		assertScope(parseWithTestEcore(inputs.referenceToMetamodel()),
				metamodelsReference(), "foo");
	}

	@Test
	public void testScopeForMetamodels() throws Exception {
		assertScope(parseWithTestEcores(inputs.referencesToMetamodels()),
				metamodelsReference(), "foo\nbar");
	}

	@Test
	public void testScopeForEnamedElementInProgram() throws Exception {
		assertScope(
			parseWithTestEcore(inputs.referenceToMetamodelWithCopiedEPackage()),
			getEcoreReferenceENamedElement(),
			"""
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			""");
	}

	private EReference getEcoreReferenceENamedElement() {
		return EdeltaPackage.Literals.EDELTA_ECORE_REFERENCE__ENAMEDELEMENT;
	}

	@Test
	public void testScopeForEnamedElementInEcoreReferenceExpressionWithSubPackages() throws Exception {
		assertScope(lastEcoreReferenceExpression(
			parseWithTestEcoreWithSubPackage("""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(
			}
			"""))
			.getReference(),
			getEcoreReferenceENamedElement(),
			"""
			mainpackage
			MainFooClass
			myAttribute
			myReference
			MainFooDataType
			MainFooEnum
			FooEnumLiteral
			MyClass
			myClassAttribute
			mainsubpackage
			MainSubPackageFooClass
			mySubPackageAttribute
			mySubPackageReference
			MyClass
			myClassAttribute
			subsubpackage
			MyClass
			""");
	}

	@Test
	public void testScopeForSubPackageInEcoreReferenceExpressionWithSubPackages() throws Exception {
		assertScope(lastEcoreReferenceExpression(parseWithTestEcoreWithSubPackage(
			"""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.
			}
			"""))
				.getReference(),
			getEcoreReferenceENamedElement(),
			"""
			MainSubPackageFooClass
			MyClass
			subsubpackage
			""");
	}

	@Test
	public void testScopeForSubPackageEClassInEcoreReferenceExpressionWithSubPackages() throws Exception {
		assertScope(
			lastEcoreReferenceExpression(parseWithTestEcoreWithSubPackage(
			"""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.MainSubPackageFooClass.
			}
			"""))
				.getReference(),
			getEcoreReferenceENamedElement(),
			"""
			mySubPackageAttribute
			mySubPackageReference
			""");
	}

	@Test
	public void testScopeForSubSubPackageInEcoreReferenceExpressionWithSubPackages() throws Exception {
		assertScope(lastEcoreReferenceExpression(parseWithTestEcoreWithSubPackage(
			"""
			metamodel "mainpackage"

			modifyEcore aTest epackage mainpackage {
				ecoreref(mainsubpackage.subsubpackage.
			}
			""")).getReference(),
			getEcoreReferenceENamedElement(),
			"MyClass");
	}

	@Test
	public void testScopeForEnamedElementInEcoreReferenceExpression() throws Exception {
		assertScope(
			ecoreReferenceExpression("ecoreref(").getReference(),
			getEcoreReferenceENamedElement(),
			"""
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			""");
	}

	@Test
	public void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedPackage() throws Exception {
		assertScope(
			ecoreReferenceExpression("ecoreref(foo.").getReference(),
			getEcoreReferenceENamedElement(),
			"""
			FooClass
			FooDataType
			FooEnum
			""");
	}

	@Test
	public void testScopeForEnamedElementInEcoreReferenceExpressionQualifiedEClass() throws Exception {
		assertScope(
			ecoreReferenceExpression("ecoreref(foo.FooClass.").getReference(),
			getEcoreReferenceENamedElement(),
			"myAttribute\nmyReference");
	}

	@Test
	public void testScopeForReferenceToCreatedEClassWithTheSameNameAsAnExistingEClass() throws Exception {
		// our created EClass with the same name as an existing one must be
		// the one that is actually linked
		var prog = parseWithTestEcore("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
			addNewEClass("FooClass")
			ecoreref(FooClass)
		}
		""");
		var eclassExp = 
			(EdeltaEcoreReferenceExpression)
			getLastModifyEcoreOperationLastExpression(prog);
		assertSame(
			// the one copied
			getFirstEClass(head(getCopiedEPackages(prog))),
			eclassExp.getReference().getEnamedelement());
	}

	@Test
	public void testScopeForReferenceToCopiedEPackageEClassifierInModifyEcore() throws Exception {
		var prog = parseWithTestEcore("""
		metamodel "foo"

		modifyEcore aTest epackage foo {
			ecoreref(FooDataType)
		}
		""");
		var eclassExp = (EdeltaEcoreReferenceExpression) getLastModifyEcoreOperationLastExpression(prog);
		var dataType = (EDataType) eclassExp.getReference().getEnamedelement();
		// must be a reference to the copied EPackage's datatype
		assertSame(head(filter(
					head(getCopiedEPackages(prog)).getEClassifiers(),
						EDataType.class)),
				dataType);
	}

	@Test
	public void testScopeForReferenceToOriginalEPackageEClassifierInOperation() throws Exception {
		var prog = parseWithTestEcore("""
		metamodel "foo"

		def anOp() {
			ecoreref(FooDataType)
		}
		""");
		var eclassExp = (EdeltaEcoreReferenceExpression)
				getBlockLastExpression(lastOperation(prog).getBody());
		var dataType = (EDataType) eclassExp.getReference().getEnamedelement();
		// must be a reference to the original EPackage's datatype
		assertSame(
				head(filter(
					head(prog.getEPackages()).getEClassifiers(),
				EDataType.class)),
				dataType);
	}

	@Test
	public void testScopeForReferenceToCopiedEPackageEClassifierInOperationWhenTheresAModifyEcore() throws Exception {
		var prog = parseWithTestEcore("""
		metamodel "foo"

		def anOp() {
			ecoreref(FooDataType)
		}

		// this triggers copied EPackage
		modifyEcore aTest epackage foo {
			ecoreref(FooDataType)
		}
		""");
		var eclassExp = (EdeltaEcoreReferenceExpression)
				getBlockLastExpression(lastOperation(prog).getBody());
		var dataType = (EDataType) eclassExp.getReference().getEnamedelement();
		// must be a reference to the original EPackage's datatype
		assertSame(
				head(filter(
						head(getCopiedEPackages(prog)).getEClassifiers(),
							EDataType.class)),
				dataType);
	}

	@Test
	public void testScopeForReferenceToCreatedEAttribute() throws Exception {
		assertScope(
			lastEcoreReferenceExpression(
			parseWithTestEcore(inputs.referenceToCreatedEAttributeSimple()))
				.getReference(),
			getEcoreReferenceENamedElement(),
			"""
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			NewClass
			newAttribute
			newAttribute2
			""");
		// newAttributes are the ones created in the program
	}

	@Test
	public void testScopeForReferenceToCreatedEAttributeChangingNameInBody() throws Exception {
		assertScope(
			lastEcoreReferenceExpression(
			parseWithTestEcore(inputs.referenceToCreatedEAttributeRenamed()))
				.getReference(),
			getEcoreReferenceENamedElement(),
			"""
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			NewClass
			changed
			""");
		// "changed" is the one created in the program (with name "newAttribute", and whose
		// name is changed in the body
	}

	@Test
	public void testScopeForModifyEcore() throws Exception {
		assertScope(parseWithTestEcores("""
			metamodel "foo"
			metamodel "bar"

			modifyEcore aTest epackage foo {}
			"""),
			EdeltaPackage.eINSTANCE.getEdeltaModifyEcoreOperation_Epackage(),
			"foo\nbar");
	}

	@Test
	public void testScopeForRenamedEClassInModifyEcore() throws Exception {
		assertScope(getEdeltaEcoreReferenceExpression(
			getLastModifyEcoreOperationLastExpression(
			parseWithTestEcore("""
				metamodel "foo"
				metamodel "bar"
				modifyEcore aTest epackage foo {
					ecoreref(foo.FooClass).name = "RenamedClass"
					ecoreref(foo.RenamedClass)
				}
				""")))
					.getReference(),
				getEcoreReferenceENamedElement(),
				"""
				RenamedClass
				FooDataType
				FooEnum
				""");
		// we renamed FooClass, and it can be referred
	}

	@Test
	public void testScopeForEnamedElementInEcoreReferenceExpressionReferringToRenamedEClassInModifyEcore()
			throws Exception {
		assertScope(getEdeltaEcoreReferenceExpression(
			getLastModifyEcoreOperationLastExpression(
			parseWithTestEcore("""
			metamodel "foo"
			metamodel "bar"
			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(foo.RenamedClass.)
			}
			"""))).getReference(),
			getEcoreReferenceENamedElement(),
			"""
			myAttribute
			myReference
			""");
		// we renamed FooClass, but its attributes are still visible through
		// the renamed class
	}

	@Test
	public void testScopeForFeaturesOfRenamedEClassInModifyEcore() throws Exception {
		assertScope(getEdeltaEcoreReferenceExpression(
			getLastModifyEcoreOperationLastExpression(
				parseWithTestEcore("""
				metamodel "foo"
				metamodel "bar"
				modifyEcore aTest epackage foo {
					ecoreref(foo.FooClass).name = "RenamedClass"
					ecoreref(RenamedClass).EStructuralFeatures +=
						newEAttribute("addedAttribute", ecoreref(FooDataType))
					ecoreref(RenamedClass.)
				}
				"""))).getReference(),
				getEcoreReferenceENamedElement(),
				"""
				myAttribute
				myReference
				addedAttribute
				""");
		// we renamed FooClass, and added an attribute to the renamed class
	}

	@Test
	public void testLinkForRenamedEClassInModifyEcore() throws Exception {
		var prog = parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(RenamedClass)
			}
			""");
		validationTestHelper.assertNoErrors(prog);
		var referred =
			getEdeltaEcoreReferenceExpression(
				getLastModifyEcoreOperationLastExpression(prog)).getReference();
		var copiedEPackage = head(getCopiedEPackages(prog));
		assertSame(
			// the one copied by the derived state computer
			getEClassiferByName(copiedEPackage, "RenamedClass"),
			(EClass) referred.getEnamedelement());
	}

	@Test
	public void testLinkForRenamedQualifiedEClassInModifyEcore() throws Exception {
		var prog = parseWithTestEcore("""
			metamodel "foo"

			modifyEcore aTest epackage foo {
				ecoreref(foo.FooClass).name = "RenamedClass"
				ecoreref(foo.RenamedClass)
			}
			""");
		validationTestHelper.assertNoErrors(prog);
		var referred =
			(EdeltaEcoreQualifiedReference) getEdeltaEcoreReferenceExpression(
					getLastModifyEcoreOperationLastExpression(prog)).getReference();
		var copiedEPackage = head(getCopiedEPackages(prog));
		assertSame(
			// the one copied by the derived state computer
			getEClassiferByName(copiedEPackage, "RenamedClass"),
			(EClass) referred.getEnamedelement());
		assertSame(
			// the one copied by the derived state computer
			// NOT the original one
			copiedEPackage,
			(EPackage) referred.getQualification().getEnamedelement());
	}

	private EReference metamodelsReference() {
		return EdeltaPackage.eINSTANCE.getEdeltaProgram_EPackages();
	}

	@Test
	public void testScopeForEnamedElementWithSubPackageInProgram() throws Exception {
		// MyClass with myClassAttribute
		// is present in the package and in subpackages
		// so it appears several times
		assertScope(
			parseWithTestEcoreWithSubPackage(
				inputs.referenceToMetamodelWithSubPackageWithCopiedEPackages()),
			getEcoreReferenceENamedElement(),
			"""
			mainpackage
			MainFooClass
			myAttribute
			myReference
			MainFooDataType
			MainFooEnum
			FooEnumLiteral
			MyClass
			myClassAttribute
			mainsubpackage
			MainSubPackageFooClass
			mySubPackageAttribute
			mySubPackageReference
			MyClass
			myClassAttribute
			subsubpackage
			MyClass
			""");
	}

	@Test
	public void testScopeForMigrationElement() throws Exception {
		assertScope(
			parseWithTestEcore("""
			migrate "http://foo" to "http://foo/v2"

			// that's required to have copied EPackages
			modifyEcore aTest foo {}
			"""),
			getEcoreReferenceENamedElement(),
			"""
			foo
			FooClass
			myAttribute
			myReference
			FooDataType
			FooEnum
			FooEnumLiteral
			""");
	}

	@Test
	public void testScopeForMigrationElementDifferentNsURI() throws Exception {
		assertScope(
			parseWithTestEcoreDifferentNsURI("""
			migrate "http://foo.org/v2" to "http://foo.org/v3"

			// that's required to have copied EPackages
			modifyEcore aTest foo {}
			"""),
			getEcoreReferenceENamedElement(),
			"""
			foo
			RenamedFooClass
			myAttribute
			myReference
			RenamedFooDataType
			FooEnum
			FooEnumLiteral
			""");
	}

	@Test
	public void testScopeForMigrationElementUnresolvedNsURI() throws Exception {
		assertScope(
			parseWithTestEcoreDifferentNsURI("""
			migrate "http://nonexistent.org/v2" to "http://foo.org/v3"

			// that's required to have copied EPackages
			modifyEcore aTest foo {}
			"""),
			getEcoreReferenceENamedElement(),
			"");
	}

	private void assertScope(EObject context, EReference reference, CharSequence expected) {
		var end = "";
		if (expected.toString().endsWith("\n")) {
			end = "\n";
		}
		assertEqualsStrings(
			expected.toString(),
			join(map(
				scopeProvider.getScope(context, reference).getAllElements(),
				IEObjectDescription::getName), "\n")
			+ end);
	}
}

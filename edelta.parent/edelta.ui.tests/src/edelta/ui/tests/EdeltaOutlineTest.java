package edelta.ui.tests;

import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.createFile;
import static org.eclipse.xtext.ui.testing.util.IResourcesSetupUtil.waitForBuild;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.xtext.testing.Flaky;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.ui.editor.outline.IOutlineNode;
import org.eclipse.xtext.ui.testing.AbstractOutlineTest;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import edelta.testutils.EdeltaTestUtils;
import edelta.ui.internal.EdeltaActivator;
import edelta.ui.tests.utils.ProjectImportUtil;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaOutlineTest extends AbstractOutlineTest {
	@Rule
	public Flaky.Rule testRule = new Flaky.Rule();

	@BeforeClass
	public static void setTestProjectName() {
		TEST_PROJECT = "edelta.ui.tests.project";
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void setUp() {
		try {
			createjavaProject(AbstractOutlineTest.TEST_PROJECT);
		} catch (Throwable e) {
			throw Exceptions.sneakyThrow(e);
		}
	}

	/**
	 * Avoids deleting project
	 */
	@Override
	public void tearDown() {
		waitForEventProcessing();
		closeEditors();
		waitForEventProcessing();
	}

	@Override
	protected String getEditorId() {
		return EdeltaActivator.EDELTA_EDELTA;
	}

	@Override
	protected IJavaProject createjavaProject(String projectName) throws CoreException {
		try {
			IJavaProject javaProject = ProjectImportUtil.importJavaProject(AbstractOutlineTest.TEST_PROJECT);
			waitForBuild();
			return javaProject;
		} catch (Throwable e) {
			throw Exceptions.sneakyThrow(e);
		}
	}

	/**
	 * We must make sure to get rid of "\r" because in Windows
	 * Java text blocks do not contain "\r" and the comparison would fail.
	 */
	@Override
	protected String outlineStringRepresentation(IOutlineNode node) {
		return EdeltaTestUtils.removeCR(super.outlineStringRepresentation(node));
	}

	@Test
	public void testOutlineWithNoContents() throws Exception {
		assertAllLabels("", "test");
	}

	@Test
	public void testOutlineWithOperation() throws Exception {
		assertAllLabels(
			"""
			def createClass(String name) {
				newEClass(name)
			}
			""",
			"""
			test
			  createClass(String) : EClass
			""");
	}

	@Test
	@Flaky
	public void testOutlineWithCreateEClassInModifyEcore() throws Exception {
		System.out.println("*** Executing testOutlineWithCreateEClassInModifyEcore...");
		assertAllLabels("""
		import org.eclipse.emf.ecore.EClass
		
		metamodel "mypackage"
		// don't rely on ecore, since the input files are not saved
		// during the test, thus external libraries are not seen
		// metamodel "ecore"
		
		def myNewAttribute(EClass c, String name) {
			c.EStructuralFeatures += newEAttribute(name, ecoreref(MyDataType))
		}
		
		modifyEcore aModification epackage mypackage {
			EClassifiers += newEClass("A")
			myNewAttribute(ecoreref(A), "foo")
		}
		""",
		"""
		test
		  myNewAttribute(EClass, String) : boolean
		  aModification(EPackage) : void
		  mypackage
		    MyClass
		      myAttribute : EString
		      myReference : EObject
		    MyDataType [java.lang.String]
		    MyBaseClass
		      myBaseAttribute : EString
		      myBaseReference : EObject
		    MyDerivedClass -> MyBaseClass
		      myDerivedAttribute : EString
		      myDerivedReference : EObject
		      MyBaseClass
		    A
		      foo : MyDataType
		""");
	}

	@Test
	@Flaky
	public void testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs() throws Exception {
		System.out.println(
				"*** Executing testOutlineWithCreateEClassInModifyEcoreAndInterpretedNewAttributeWithUseAs...");
		createFile((AbstractOutlineTest.TEST_PROJECT + "/src/Refactorings.edelta"),
			"""
			import org.eclipse.emf.ecore.EClass
			
			package com.example
			
			metamodel "mypackage"
			
			def myNewAttribute(EClass c, String name) {
				c.EStructuralFeatures += newEAttribute(name, ecoreref(MyDataType))
			}
			""");
		waitForBuild();
		assertAllLabels("""
		package com.example

		metamodel "mypackage"
		// don't rely on ecore, since the input files are not saved
		// during the test, thus external libraries are not seen
		// metamodel "ecore"
		
		use Refactorings as my
		
		modifyEcore aModification epackage mypackage {
			EClassifiers += newEClass("A")
			my.myNewAttribute(ecoreref(A), "foo")
		}
		""",
		"""
		com.example
		  aModification(EPackage) : void
		  mypackage
		    MyClass
		      myAttribute : EString
		      myReference : EObject
		    MyDataType [java.lang.String]
		    MyBaseClass
		      myBaseAttribute : EString
		      myBaseReference : EObject
		    MyDerivedClass -> MyBaseClass
		      myDerivedAttribute : EString
		      myDerivedReference : EObject
		      MyBaseClass
		    A
		      foo : MyDataType
		""");
	}

	@Test
	@Flaky
	public void testOutlineWithRemovedElementsInModifyEcore() throws Exception {
		System.out.println("*** Executing testOutlineWithRemovedElementsInModifyEcore...");
		assertAllLabels("""
		import org.eclipse.emf.ecore.EClass
		
		metamodel "mypackage"
		
		modifyEcore aModification epackage mypackage {
			EClassifiers -= ecoreref(MyClass)
			ecoreref(MyDerivedClass).ESuperTypes -= ecoreref(MyBaseClass)
			EClassifiers -= ecoreref(MyBaseClass)
		}
		""",
		"""
		test
		  aModification(EPackage) : void
		  mypackage
		    MyDataType [java.lang.String]
		    MyDerivedClass
		      myDerivedAttribute : EString
		      myDerivedReference : EObject
		""");
	}

	@Test
	@Flaky
	public void testOutlineWhenNoElementIsModifiedThenTheEPackageIsNotShown() throws Exception {
		System.out.println("*** Executing testOutlineWhenNoElementIsModified...");
		assertAllLabels("""
		metamodel "mypackage"
		
		modifyEcore aModification epackage mypackage {

		}
		""",
		"""
		test
		  aModification(EPackage) : void
		""");
	}
}

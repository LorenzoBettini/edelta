package edelta.tests;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaEcoreQualifiedReference;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.edelta.EdeltaModifyEcoreOperation;
import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaAccessibleElement;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.tests.EdeltaInjectorProvider;
import edelta.tests.input.Inputs;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.diagnostics.Severity;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XBlockExpression;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XVariableDeclaration;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public abstract class EdeltaAbstractTest {
  @Inject
  private Provider<XtextResourceSet> resourceSetProvider;
  
  @Inject
  @Extension
  protected ParseHelper<EdeltaProgram> _parseHelper;
  
  @Inject
  @Extension
  protected ValidationTestHelper _validationTestHelper;
  
  @Inject
  @Extension
  protected IJvmModelAssociations _iJvmModelAssociations;
  
  @Extension
  protected Inputs _inputs = new Inputs();
  
  protected static String METAMODEL_PATH = "src/edelta/tests/input/models/";
  
  protected static String ECORE_ECORE = "EcoreForTests.ecore";
  
  protected static String PERSON_LIST_ECORE = "PersonList.ecore";
  
  protected static String TEST1_REFS_ECORE = "TestEcoreForReferences1.ecore";
  
  protected static String TEST2_REFS_ECORE = "TestEcoreForReferences2.ecore";
  
  /**
   * Parse several input sources and returns the parsed program corresponding
   * to the last input source.
   */
  protected EdeltaProgram parseSeveralWithTestEcore(final List<CharSequence> inputs) {
    try {
      final ResourceSet rs = this.resourceSetWithTestEcore();
      EdeltaProgram program = null;
      for (final CharSequence input : inputs) {
        program = this._parseHelper.parse(input, rs);
      }
      return program;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcore(final CharSequence input) {
    try {
      return this._parseHelper.parse(input, this.resourceSetWithTestEcore());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcores(final CharSequence input) {
    try {
      return this._parseHelper.parse(input, this.resourceSetWithTestEcores());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcoreWithSubPackage(final CharSequence input) {
    try {
      return this._parseHelper.parse(input, this.resourceSetWithTestEcoreWithSubPackage());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcoresWithReferences(final CharSequence input) {
    try {
      return this._parseHelper.parse(input, this.resourceSetWithTestEcoresWithReferences());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithLoadedEcore(final String path, final CharSequence input) {
    try {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      resourceSet.getResource(this.createFileURIFromPath((EdeltaAbstractTest.METAMODEL_PATH + EdeltaAbstractTest.ECORE_ECORE)), true);
      final URI uri = this.createFileURIFromPath(path);
      resourceSet.getResource(uri, true);
      final EdeltaProgram prog = this._parseHelper.parse(input, resourceSet);
      return prog;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected URI createFileURIFromPath(final String path) {
    return URI.createFileURI(
      Paths.get(path).toAbsolutePath().toString());
  }
  
  protected ResourceSet resourceSetWithTestEcore() {
    ResourceSet _xblockexpression = null;
    {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      _xblockexpression = this.addEPackageForTests(resourceSet);
    }
    return _xblockexpression;
  }
  
  protected ResourceSet resourceSetWithTestEcoreWithSubPackage() {
    ResourceSet _xblockexpression = null;
    {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      _xblockexpression = this.addEPackageWithSubPackageForTests(resourceSet);
    }
    return _xblockexpression;
  }
  
  protected XtextResourceSet resourceSetWithTestEcoresWithReferences() {
    XtextResourceSet _xblockexpression = null;
    {
      final XtextResourceSet resourceSet = this.resourceSetProvider.get();
      this.addEPackagesWithReferencesForTests(resourceSet);
      _xblockexpression = resourceSet;
    }
    return _xblockexpression;
  }
  
  protected void addEPackagesWithReferencesForTests(final ResourceSet resourceSet) {
    final List<EPackage> packages = this.EPackagesWithReferencesForTest();
    for (final EPackage p : packages) {
      this.createTestResource(resourceSet, p.getName(), p);
    }
  }
  
  protected ResourceSet addEPackageForTests(final ResourceSet resourceSet) {
    return this.createTestResource(resourceSet, "foo", this.EPackageForTests());
  }
  
  protected ResourceSet addEPackageWithSubPackageForTests(final ResourceSet resourceSet) {
    return this.createTestResource(resourceSet, "mainpackage", this.EPackageWithSubPackageForTests());
  }
  
  protected ResourceSet resourceSetWithTestEcores() {
    ResourceSet _xblockexpression = null;
    {
      final ResourceSet resourceSet = this.resourceSetWithTestEcore();
      _xblockexpression = this.addEPackageForTests2(resourceSet);
    }
    return _xblockexpression;
  }
  
  protected ResourceSet addEPackageForTests2(final ResourceSet resourceSet) {
    return this.createTestResource(resourceSet, "bar", this.EPackageForTests2());
  }
  
  protected ResourceSet createTestResource(final ResourceSet resourceSet, final String ecoreName, final EPackage epackage) {
    ResourceSet _xblockexpression = null;
    {
      final Resource resource = resourceSet.createResource(URI.createURI((ecoreName + ".ecore")));
      EList<EObject> _contents = resource.getContents();
      _contents.add(epackage);
      _xblockexpression = resourceSet;
    }
    return _xblockexpression;
  }
  
  protected EPackage EPackageForTests() {
    EPackage _xblockexpression = null;
    {
      EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function = (EPackage it) -> {
        it.setName("foo");
        it.setNsPrefix("foo");
        it.setNsURI("http://foo");
      };
      final EPackage fooPackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
      EList<EClassifier> _eClassifiers = fooPackage.getEClassifiers();
      EClass _createEClass = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        it.setName("FooClass");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_2 = (EAttribute it_1) -> {
          it_1.setName("myAttribute");
        };
        EAttribute _doubleArrow = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
        _eStructuralFeatures.add(_doubleArrow);
        EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
        EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
        final Procedure1<EReference> _function_3 = (EReference it_1) -> {
          it_1.setName("myReference");
        };
        EReference _doubleArrow_1 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_3);
        _eStructuralFeatures_1.add(_doubleArrow_1);
        EList<EOperation> _eOperations = it.getEOperations();
        EOperation _createEOperation = EcoreFactory.eINSTANCE.createEOperation();
        final Procedure1<EOperation> _function_4 = (EOperation it_1) -> {
          it_1.setName("myOp");
        };
        EOperation _doubleArrow_2 = ObjectExtensions.<EOperation>operator_doubleArrow(_createEOperation, _function_4);
        _eOperations.add(_doubleArrow_2);
      };
      EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      _eClassifiers.add(_doubleArrow);
      EList<EClassifier> _eClassifiers_1 = fooPackage.getEClassifiers();
      EDataType _createEDataType = EcoreFactory.eINSTANCE.createEDataType();
      final Procedure1<EDataType> _function_2 = (EDataType it) -> {
        it.setName("FooDataType");
      };
      EDataType _doubleArrow_1 = ObjectExtensions.<EDataType>operator_doubleArrow(_createEDataType, _function_2);
      _eClassifiers_1.add(_doubleArrow_1);
      EList<EClassifier> _eClassifiers_2 = fooPackage.getEClassifiers();
      EEnum _createEEnum = EcoreFactory.eINSTANCE.createEEnum();
      final Procedure1<EEnum> _function_3 = (EEnum it) -> {
        it.setName("FooEnum");
        EList<EEnumLiteral> _eLiterals = it.getELiterals();
        EEnumLiteral _createEEnumLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
        final Procedure1<EEnumLiteral> _function_4 = (EEnumLiteral it_1) -> {
          it_1.setName("FooEnumLiteral");
        };
        EEnumLiteral _doubleArrow_2 = ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_createEEnumLiteral, _function_4);
        _eLiterals.add(_doubleArrow_2);
      };
      EEnum _doubleArrow_2 = ObjectExtensions.<EEnum>operator_doubleArrow(_createEEnum, _function_3);
      _eClassifiers_2.add(_doubleArrow_2);
      _xblockexpression = fooPackage;
    }
    return _xblockexpression;
  }
  
  protected EPackage EPackageForTests2() {
    EPackage _xblockexpression = null;
    {
      EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function = (EPackage it) -> {
        it.setName("bar");
        it.setNsPrefix("bar");
        it.setNsURI("http://bar");
      };
      final EPackage fooPackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
      EList<EClassifier> _eClassifiers = fooPackage.getEClassifiers();
      EClass _createEClass = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        it.setName("BarClass");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_2 = (EAttribute it_1) -> {
          it_1.setName("myAttribute");
        };
        EAttribute _doubleArrow = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
        _eStructuralFeatures.add(_doubleArrow);
        EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
        EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
        final Procedure1<EReference> _function_3 = (EReference it_1) -> {
          it_1.setName("myReference");
        };
        EReference _doubleArrow_1 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_3);
        _eStructuralFeatures_1.add(_doubleArrow_1);
      };
      EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      _eClassifiers.add(_doubleArrow);
      EList<EClassifier> _eClassifiers_1 = fooPackage.getEClassifiers();
      EDataType _createEDataType = EcoreFactory.eINSTANCE.createEDataType();
      final Procedure1<EDataType> _function_2 = (EDataType it) -> {
        it.setName("BarDataType");
      };
      EDataType _doubleArrow_1 = ObjectExtensions.<EDataType>operator_doubleArrow(_createEDataType, _function_2);
      _eClassifiers_1.add(_doubleArrow_1);
      _xblockexpression = fooPackage;
    }
    return _xblockexpression;
  }
  
  protected List<EPackage> EPackagesWithReferencesForTest() {
    List<EPackage> _xblockexpression = null;
    {
      EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function = (EPackage it) -> {
        it.setName("testecoreforreferences1");
        it.setNsPrefix("testecoreforreferences1");
        it.setNsURI("http://my.testecoreforreferences1.org");
      };
      final EPackage p1 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
      EList<EClassifier> _eClassifiers = p1.getEClassifiers();
      EClass _createEClass = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        it.setName("Person");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_2 = (EAttribute it_1) -> {
          it_1.setName("name");
        };
        EAttribute _doubleArrow = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
        _eStructuralFeatures.add(_doubleArrow);
        EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
        EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
        final Procedure1<EReference> _function_3 = (EReference it_1) -> {
          it_1.setName("works");
          it_1.setContainment(false);
        };
        EReference _doubleArrow_1 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_3);
        _eStructuralFeatures_1.add(_doubleArrow_1);
      };
      EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      _eClassifiers.add(_doubleArrow);
      EPackage _createEPackage_1 = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function_2 = (EPackage it) -> {
        it.setName("testecoreforreferences2");
        it.setNsPrefix("testecoreforreferences2");
        it.setNsURI("http://my.testecoreforreferences2.org");
      };
      final EPackage p2 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_2);
      EList<EClassifier> _eClassifiers_1 = p2.getEClassifiers();
      EClass _createEClass_1 = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_3 = (EClass it) -> {
        it.setName("WorkPlace");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_4 = (EAttribute it_1) -> {
          it_1.setName("address");
        };
        EAttribute _doubleArrow_1 = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
        _eStructuralFeatures.add(_doubleArrow_1);
        EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
        EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
        final Procedure1<EReference> _function_5 = (EReference it_1) -> {
          it_1.setName("persons");
          it_1.setContainment(false);
          it_1.setUpperBound((-1));
        };
        EReference _doubleArrow_2 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_5);
        _eStructuralFeatures_1.add(_doubleArrow_2);
      };
      EClass _doubleArrow_1 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_3);
      _eClassifiers_1.add(_doubleArrow_1);
      final EReference works = this.getEReferenceByName(this.getEClassByName(p1, "Person"), "works");
      final EReference persons = this.getEReferenceByName(this.getEClassByName(p2, "WorkPlace"), "persons");
      works.setEOpposite(persons);
      persons.setEOpposite(works);
      _xblockexpression = Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList(p1, p2));
    }
    return _xblockexpression;
  }
  
  protected EPackage EPackageWithSubPackageForTests() {
    EPackage _xblockexpression = null;
    {
      EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function = (EPackage it) -> {
        it.setName("mainpackage");
        it.setNsPrefix("mainpackage");
        it.setNsURI("http://mainpackage");
      };
      final EPackage mainPackage = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
      EList<EClassifier> _eClassifiers = mainPackage.getEClassifiers();
      EClass _createEClass = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_1 = (EClass it) -> {
        it.setName("MainFooClass");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_2 = (EAttribute it_1) -> {
          it_1.setName("myAttribute");
        };
        EAttribute _doubleArrow = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
        _eStructuralFeatures.add(_doubleArrow);
        EList<EStructuralFeature> _eStructuralFeatures_1 = it.getEStructuralFeatures();
        EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
        final Procedure1<EReference> _function_3 = (EReference it_1) -> {
          it_1.setName("myReference");
        };
        EReference _doubleArrow_1 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_3);
        _eStructuralFeatures_1.add(_doubleArrow_1);
      };
      EClass _doubleArrow = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      _eClassifiers.add(_doubleArrow);
      EList<EClassifier> _eClassifiers_1 = mainPackage.getEClassifiers();
      EDataType _createEDataType = EcoreFactory.eINSTANCE.createEDataType();
      final Procedure1<EDataType> _function_2 = (EDataType it) -> {
        it.setName("MainFooDataType");
      };
      EDataType _doubleArrow_1 = ObjectExtensions.<EDataType>operator_doubleArrow(_createEDataType, _function_2);
      _eClassifiers_1.add(_doubleArrow_1);
      EList<EClassifier> _eClassifiers_2 = mainPackage.getEClassifiers();
      EEnum _createEEnum = EcoreFactory.eINSTANCE.createEEnum();
      final Procedure1<EEnum> _function_3 = (EEnum it) -> {
        it.setName("MainFooEnum");
        EList<EEnumLiteral> _eLiterals = it.getELiterals();
        EEnumLiteral _createEEnumLiteral = EcoreFactory.eINSTANCE.createEEnumLiteral();
        final Procedure1<EEnumLiteral> _function_4 = (EEnumLiteral it_1) -> {
          it_1.setName("FooEnumLiteral");
        };
        EEnumLiteral _doubleArrow_2 = ObjectExtensions.<EEnumLiteral>operator_doubleArrow(_createEEnumLiteral, _function_4);
        _eLiterals.add(_doubleArrow_2);
      };
      EEnum _doubleArrow_2 = ObjectExtensions.<EEnum>operator_doubleArrow(_createEEnum, _function_3);
      _eClassifiers_2.add(_doubleArrow_2);
      EList<EClassifier> _eClassifiers_3 = mainPackage.getEClassifiers();
      EClass _createEClass_1 = EcoreFactory.eINSTANCE.createEClass();
      final Procedure1<EClass> _function_4 = (EClass it) -> {
        it.setName("MyClass");
        EList<EStructuralFeature> _eStructuralFeatures = it.getEStructuralFeatures();
        EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
        final Procedure1<EAttribute> _function_5 = (EAttribute it_1) -> {
          it_1.setName("myClassAttribute");
        };
        EAttribute _doubleArrow_3 = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_5);
        _eStructuralFeatures.add(_doubleArrow_3);
      };
      EClass _doubleArrow_3 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_4);
      _eClassifiers_3.add(_doubleArrow_3);
      EList<EPackage> _eSubpackages = mainPackage.getESubpackages();
      EPackage _createEPackage_1 = EcoreFactory.eINSTANCE.createEPackage();
      final Procedure1<EPackage> _function_5 = (EPackage it) -> {
        it.setName("mainsubpackage");
        it.setNsPrefix("mainsubpackage");
        it.setNsURI("http://mainsubpackage");
        EList<EClassifier> _eClassifiers_4 = it.getEClassifiers();
        EClass _createEClass_2 = EcoreFactory.eINSTANCE.createEClass();
        final Procedure1<EClass> _function_6 = (EClass it_1) -> {
          it_1.setName("MainSubPackageFooClass");
          EList<EStructuralFeature> _eStructuralFeatures = it_1.getEStructuralFeatures();
          EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
          final Procedure1<EAttribute> _function_7 = (EAttribute it_2) -> {
            it_2.setName("mySubPackageAttribute");
          };
          EAttribute _doubleArrow_4 = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_7);
          _eStructuralFeatures.add(_doubleArrow_4);
          EList<EStructuralFeature> _eStructuralFeatures_1 = it_1.getEStructuralFeatures();
          EReference _createEReference = EcoreFactory.eINSTANCE.createEReference();
          final Procedure1<EReference> _function_8 = (EReference it_2) -> {
            it_2.setName("mySubPackageReference");
          };
          EReference _doubleArrow_5 = ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function_8);
          _eStructuralFeatures_1.add(_doubleArrow_5);
        };
        EClass _doubleArrow_4 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_6);
        _eClassifiers_4.add(_doubleArrow_4);
        EList<EClassifier> _eClassifiers_5 = it.getEClassifiers();
        EClass _createEClass_3 = EcoreFactory.eINSTANCE.createEClass();
        final Procedure1<EClass> _function_7 = (EClass it_1) -> {
          it_1.setName("MyClass");
          EList<EStructuralFeature> _eStructuralFeatures = it_1.getEStructuralFeatures();
          EAttribute _createEAttribute = EcoreFactory.eINSTANCE.createEAttribute();
          final Procedure1<EAttribute> _function_8 = (EAttribute it_2) -> {
            it_2.setName("myClassAttribute");
          };
          EAttribute _doubleArrow_5 = ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_8);
          _eStructuralFeatures.add(_doubleArrow_5);
        };
        EClass _doubleArrow_5 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_7);
        _eClassifiers_5.add(_doubleArrow_5);
        EList<EPackage> _eSubpackages_1 = it.getESubpackages();
        EPackage _createEPackage_2 = EcoreFactory.eINSTANCE.createEPackage();
        final Procedure1<EPackage> _function_8 = (EPackage it_1) -> {
          it_1.setName("subsubpackage");
          it_1.setNsPrefix("subsubpackage");
          it_1.setNsURI("http://subsubpackage");
          EList<EClassifier> _eClassifiers_6 = it_1.getEClassifiers();
          EClass _createEClass_4 = EcoreFactory.eINSTANCE.createEClass();
          final Procedure1<EClass> _function_9 = (EClass it_2) -> {
            it_2.setName("MyClass");
          };
          EClass _doubleArrow_6 = ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_4, _function_9);
          _eClassifiers_6.add(_doubleArrow_6);
        };
        EPackage _doubleArrow_6 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_2, _function_8);
        _eSubpackages_1.add(_doubleArrow_6);
      };
      EPackage _doubleArrow_4 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_5);
      _eSubpackages.add(_doubleArrow_4);
      _xblockexpression = mainPackage;
    }
    return _xblockexpression;
  }
  
  protected void assertErrorsAsStrings(final EObject o, final CharSequence expected) {
    final Function1<Issue, Boolean> _function = (Issue it) -> {
      Severity _severity = it.getSeverity();
      return Boolean.valueOf(Objects.equal(_severity, Severity.ERROR));
    };
    final Function1<Issue, String> _function_1 = (Issue it) -> {
      return it.getMessage();
    };
    this.assertEqualsStrings(expected.toString().trim(), 
      IterableExtensions.join(IterableExtensions.<String>sort(IterableExtensions.<Issue, String>map(IterableExtensions.<Issue>filter(this._validationTestHelper.validate(o), _function), _function_1)), "\n"));
  }
  
  protected void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
    Assert.assertEquals(expected.toString().replaceAll("\r", ""), actual.toString().replaceAll("\r", ""));
  }
  
  protected void assertNamedElements(final Iterable<? extends ENamedElement> elements, final CharSequence expected) {
    final Function1<ENamedElement, String> _function = (ENamedElement it) -> {
      return it.getName();
    };
    String _join = IterableExtensions.join(IterableExtensions.map(elements, _function), "\n");
    String _plus = (_join + "\n");
    this.assertEqualsStrings(expected, _plus);
  }
  
  protected void assertAccessibleElements(final EdeltaAccessibleElements elements, final CharSequence expected) {
    final Function1<EdeltaAccessibleElement, String> _function = (EdeltaAccessibleElement it) -> {
      return it.getQualifiedName().toString();
    };
    final Function1<String, String> _function_1 = (String it) -> {
      return it;
    };
    String _join = IterableExtensions.join(IterableExtensions.<String, String>sortBy(IterableExtensions.<EdeltaAccessibleElement, String>map(elements, _function), _function_1), "\n");
    String _plus = (_join + "\n");
    this.assertEqualsStrings(expected, _plus);
  }
  
  protected EPackage getEPackageByName(final EdeltaProgram context, final String packagename) {
    final Function1<XMIResource, EPackage> _function = (XMIResource it) -> {
      EObject _head = IterableExtensions.<EObject>head(it.getContents());
      return ((EPackage) _head);
    };
    final Function1<EPackage, Boolean> _function_1 = (EPackage it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, packagename));
    };
    return IterableExtensions.<EPackage>findFirst(IterableExtensions.<XMIResource, EPackage>map(Iterables.<XMIResource>filter(context.eResource().getResourceSet().getResources(), XMIResource.class), _function), _function_1);
  }
  
  protected EClassifier getEClassifierByName(final EdeltaProgram context, final String packagename, final String classifiername) {
    final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, classifiername));
    };
    return IterableExtensions.<EClassifier>findFirst(this.getEPackageByName(context, packagename).getEClassifiers(), _function);
  }
  
  protected <T extends ENamedElement> T getByName(final Iterable<T> namedElements, final String nameToSearch) {
    final Function1<T, Boolean> _function = (T it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<T>findFirst(namedElements, _function);
  }
  
  protected EdeltaModifyEcoreOperation lastModifyEcoreOperation(final EdeltaProgram p) {
    return IterableExtensions.<EdeltaModifyEcoreOperation>last(p.getModifyEcoreOperations());
  }
  
  protected EdeltaOperation lastOperation(final EdeltaProgram p) {
    return IterableExtensions.<EdeltaOperation>last(p.getOperations());
  }
  
  protected EClass getLastCopiedEPackageLastEClass(final EObject context) {
    EClass _xblockexpression = null;
    {
      final EPackage copiedEPackage = this.getLastCopiedEPackage(context);
      EClassifier _last = IterableExtensions.<EClassifier>last(copiedEPackage.getEClassifiers());
      _xblockexpression = ((EClass) _last);
    }
    return _xblockexpression;
  }
  
  protected EClass getLastCopiedEPackageFirstEClass(final EObject context) {
    EClass _xblockexpression = null;
    {
      final EPackage copiedEPackage = this.getLastCopiedEPackage(context);
      EClassifier _head = IterableExtensions.<EClassifier>head(copiedEPackage.getEClassifiers());
      _xblockexpression = ((EClass) _head);
    }
    return _xblockexpression;
  }
  
  protected EClass getLastCopiedEPackageFirstEClass(final EObject context, final String nameToSearch) {
    EClass _xblockexpression = null;
    {
      final EPackage p = this.getLastCopiedEPackage(context);
      final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
        String _name = it.getName();
        return Boolean.valueOf(Objects.equal(_name, nameToSearch));
      };
      EClassifier _findFirst = IterableExtensions.<EClassifier>findFirst(p.getEClassifiers(), _function);
      _xblockexpression = ((EClass) _findFirst);
    }
    return _xblockexpression;
  }
  
  protected EPackage getLastCopiedEPackage(final EObject context) {
    return IterableExtensions.<EPackage>last(this.getCopiedEPackages(context));
  }
  
  protected Iterable<EPackage> getCopiedEPackages(final EObject context) {
    return Iterables.<EPackage>filter(context.eResource().getContents(), EPackage.class);
  }
  
  protected EClassifier getEClassiferByName(final EPackage p, final String nameToSearch) {
    final Function1<EClassifier, Boolean> _function = (EClassifier it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EClassifier>findFirst(p.getEClassifiers(), _function);
  }
  
  protected EClass getEClassByName(final EPackage p, final String nameToSearch) {
    final Function1<EClass, Boolean> _function = (EClass it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EClass>findFirst(Iterables.<EClass>filter(p.getEClassifiers(), EClass.class), _function);
  }
  
  protected EStructuralFeature getEStructuralFeatureByName(final EClassifier e, final String nameToSearch) {
    final Function1<EStructuralFeature, Boolean> _function = (EStructuralFeature it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EStructuralFeature>findFirst(((EClass) e).getEStructuralFeatures(), _function);
  }
  
  protected EReference getEReferenceByName(final EClassifier e, final String nameToSearch) {
    final Function1<EReference, Boolean> _function = (EReference it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EReference>findFirst(Iterables.<EReference>filter(((EClass) e).getEStructuralFeatures(), EReference.class), _function);
  }
  
  protected EAttribute getEAttributeByName(final EClassifier e, final String nameToSearch) {
    final Function1<EAttribute, Boolean> _function = (EAttribute it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EAttribute>findFirst(Iterables.<EAttribute>filter(((EClass) e).getEStructuralFeatures(), EAttribute.class), _function);
  }
  
  protected EEnumLiteral getEEnumLiteralByName(final EClassifier e, final String nameToSearch) {
    final Function1<EEnumLiteral, Boolean> _function = (EEnumLiteral it) -> {
      String _name = it.getName();
      return Boolean.valueOf(Objects.equal(_name, nameToSearch));
    };
    return IterableExtensions.<EEnumLiteral>findFirst(((EEnum) e).getELiterals(), _function);
  }
  
  protected EdeltaEcoreReferenceExpression getEdeltaEcoreReferenceExpression(final XExpression e) {
    return ((EdeltaEcoreReferenceExpression) e);
  }
  
  protected EdeltaEcoreReference getEdeltaEcoreReference(final XExpression e) {
    return this.getEdeltaEcoreReferenceExpression(e).getReference();
  }
  
  protected EdeltaEcoreDirectReference getEdeltaEcoreDirectReference(final EObject e) {
    return ((EdeltaEcoreDirectReference) e);
  }
  
  protected EdeltaEcoreQualifiedReference getEdeltaEcoreQualifiedReference(final EObject e) {
    return ((EdeltaEcoreQualifiedReference) e);
  }
  
  protected XExpression getBlockLastExpression(final XExpression e) {
    return IterableExtensions.<XExpression>last(((XBlockExpression) e).getExpressions());
  }
  
  protected XExpression getBlockFirstExpression(final XExpression e) {
    return IterableExtensions.<XExpression>head(((XBlockExpression) e).getExpressions());
  }
  
  protected XBlockExpression getBlock(final XExpression e) {
    return ((XBlockExpression) e);
  }
  
  protected XVariableDeclaration getVariableDeclaration(final XExpression e) {
    return ((XVariableDeclaration) e);
  }
  
  protected EdeltaModifyEcoreOperation getModifyEcoreOperation(final XExpression e) {
    return ((EdeltaModifyEcoreOperation) e);
  }
  
  protected EClass getLastEClass(final EPackage ePackage) {
    EClassifier _last = IterableExtensions.<EClassifier>last(ePackage.getEClassifiers());
    return ((EClass) _last);
  }
  
  protected EClass getFirstEClass(final EPackage ePackage) {
    EClassifier _head = IterableExtensions.<EClassifier>head(ePackage.getEClassifiers());
    return ((EClass) _head);
  }
  
  protected EdeltaEcoreReferenceExpression ecoreReferenceExpression(final CharSequence ecoreRefString) {
    return this.lastEcoreReferenceExpression(this.parseInsideModifyEcoreWithTestMetamodelFoo(ecoreRefString));
  }
  
  protected EdeltaProgram parseInsideModifyEcoreWithTestMetamodelFoo(final CharSequence body) {
    return this.parseWithTestEcore(this.inputInsideModifyEcoreWithTestMetamodelFoo(body));
  }
  
  protected CharSequence inputInsideModifyEcoreWithTestMetamodelFoo(final CharSequence body) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {");
    _builder.newLine();
    _builder.append("\t");
    _builder.append(body, "\t");
    _builder.newLineIfNotEmpty();
    _builder.append("}");
    _builder.newLine();
    return _builder;
  }
  
  protected EdeltaEcoreReferenceExpression lastEcoreReferenceExpression(final EdeltaProgram p) {
    XExpression _blockLastExpression = this.getBlockLastExpression(this.lastModifyEcoreOperation(p).getBody());
    return ((EdeltaEcoreReferenceExpression) _blockLastExpression);
  }
  
  protected List<EdeltaEcoreReferenceExpression> getAllEcoreReferenceExpressions(final EdeltaProgram p) {
    return EcoreUtil2.<EdeltaEcoreReferenceExpression>getAllContentsOfType(p, EdeltaEcoreReferenceExpression.class);
  }
  
  protected XAbstractFeatureCall getFeatureCall(final XExpression e) {
    return ((XAbstractFeatureCall) e);
  }
}

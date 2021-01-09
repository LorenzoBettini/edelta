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
import edelta.lib.EdeltaLibrary;
import edelta.resource.derivedstate.EdeltaAccessibleElement;
import edelta.resource.derivedstate.EdeltaAccessibleElements;
import edelta.tests.input.Inputs;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
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
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.junit.Assert;

@SuppressWarnings("all")
public abstract class EdeltaAbstractTest {
  @Inject
  private Provider<XtextResourceSet> resourceSetProvider;
  
  @Inject
  @Extension
  protected ParseHelper<EdeltaProgram> parseHelper;
  
  @Inject
  @Extension
  protected ValidationTestHelper validationTestHelper;
  
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
  
  protected static String SIMPLE_ECORE = "Simple.ecore";
  
  protected static String ANOTHER_SIMPLE_ECORE = "AnotherSimple.ecore";
  
  /**
   * Parse several input sources using the "foo" EPackage
   * and returns the parsed program corresponding
   * to the last input source.
   */
  protected EdeltaProgram parseSeveralWithTestEcore(final List<CharSequence> inputs) {
    EdeltaProgram _xblockexpression = null;
    {
      final ResourceSet rs = this.resourceSetWithTestEcore();
      _xblockexpression = this.parseSeveralInputs(inputs, rs);
    }
    return _xblockexpression;
  }
  
  /**
   * Parse several input sources using the "foo" and "bar" EPackages
   * and returns the parsed program corresponding
   * to the last input source.
   */
  protected EdeltaProgram parseSeveralWithTestEcores(final List<CharSequence> inputs) {
    EdeltaProgram _xblockexpression = null;
    {
      final ResourceSet rs = this.resourceSetWithTestEcores();
      _xblockexpression = this.parseSeveralInputs(inputs, rs);
    }
    return _xblockexpression;
  }
  
  protected EdeltaProgram parseSeveralInputs(final List<CharSequence> inputs, final ResourceSet rs) {
    try {
      EdeltaProgram program = null;
      for (final CharSequence input : inputs) {
        program = this.parseHelper.parse(input, rs);
      }
      return program;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcore(final CharSequence input) {
    try {
      return this.parseHelper.parse(input, this.resourceSetWithTestEcore());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcores(final CharSequence input) {
    try {
      return this.parseHelper.parse(input, this.resourceSetWithTestEcores());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcoreWithSubPackage(final CharSequence input) {
    try {
      return this.parseHelper.parse(input, this.resourceSetWithTestEcoreWithSubPackage());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  protected EdeltaProgram parseWithTestEcoresWithReferences(final CharSequence input) {
    try {
      return this.parseHelper.parse(input, this.resourceSetWithTestEcoresWithReferences());
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
      final EdeltaProgram prog = this.parseHelper.parse(input, resourceSet);
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
  
  protected EPackage createEPackage(final String name, final String nsPrefix, final String nsURI, final Consumer<EPackage> initializer) {
    final EPackage pack = EcoreFactory.eINSTANCE.createEPackage();
    pack.setName(name);
    pack.setNsPrefix(nsPrefix);
    pack.setNsURI(nsURI);
    initializer.accept(pack);
    return pack;
  }
  
  protected void createEOperation(final EClass c, final String name) {
    final EOperation op = EcoreFactory.eINSTANCE.createEOperation();
    op.setName(name);
    EList<EOperation> _eOperations = c.getEOperations();
    _eOperations.add(op);
  }
  
  /**
   * IMPORTANT: if you add something to this ecore, which is created on the fly,
   * and you have a test for the generated Java code, then you must also
   * update testecores/foo.ecore accordingly
   */
  protected EPackage EPackageForTests() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "myAttribute", null);
        EdeltaLibrary.addNewEReference(it_1, "myReference", null);
        this.createEOperation(it_1, "myOp");
      };
      EdeltaLibrary.addNewEClass(it, "FooClass", _function_1);
      EdeltaLibrary.addNewEDataType(it, "FooDataType", null);
      final Consumer<EEnum> _function_2 = (EEnum it_1) -> {
        EdeltaLibrary.addNewEEnumLiteral(it_1, "FooEnumLiteral");
      };
      EdeltaLibrary.addNewEEnum(it, "FooEnum", _function_2);
    };
    return this.createEPackage("foo", "foo", "http://foo", _function);
  }
  
  protected EPackage EPackageForTests2() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "myAttribute", null);
        EdeltaLibrary.addNewEReference(it_1, "myReference", null);
      };
      EdeltaLibrary.addNewEClass(it, "BarClass", _function_1);
      EdeltaLibrary.addNewEDataType(it, "BarDataType", null);
    };
    return this.createEPackage("bar", "bar", "http://bar", _function);
  }
  
  protected List<EPackage> EPackagesWithReferencesForTest() {
    List<EPackage> _xblockexpression = null;
    {
      final Consumer<EPackage> _function = (EPackage it) -> {
        final Consumer<EClass> _function_1 = (EClass it_1) -> {
          EdeltaLibrary.addNewEAttribute(it_1, "name", null);
          EdeltaLibrary.addNewEReference(it_1, "works", null);
        };
        EdeltaLibrary.addNewEClass(it, "Person", _function_1);
      };
      final EPackage p1 = this.createEPackage(
        "testecoreforreferences1", 
        "testecoreforreferences1", 
        "http://my.testecoreforreferences1", _function);
      final Consumer<EPackage> _function_1 = (EPackage it) -> {
        final Consumer<EClass> _function_2 = (EClass it_1) -> {
          EdeltaLibrary.addNewEAttribute(it_1, "address", null);
          final Consumer<EReference> _function_3 = (EReference it_2) -> {
            it_2.setUpperBound((-1));
          };
          EdeltaLibrary.addNewEReference(it_1, "persons", null, _function_3);
        };
        EdeltaLibrary.addNewEClass(it, "WorkPlace", _function_2);
      };
      final EPackage p2 = this.createEPackage(
        "testecoreforreferences2", 
        "testecoreforreferences2", 
        "http://my.testecoreforreferences2", _function_1);
      final EReference works = this.getEReferenceByName(this.getEClassByName(p1, "Person"), "works");
      final EReference persons = this.getEReferenceByName(this.getEClassByName(p2, "WorkPlace"), "persons");
      works.setEOpposite(persons);
      persons.setEOpposite(works);
      _xblockexpression = Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList(p1, p2));
    }
    return _xblockexpression;
  }
  
  protected EPackage EPackageWithSubPackageForTests() {
    final Consumer<EPackage> _function = (EPackage it) -> {
      final Consumer<EClass> _function_1 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "myAttribute", null);
        EdeltaLibrary.addNewEReference(it_1, "myReference", null);
      };
      EdeltaLibrary.addNewEClass(it, "MainFooClass", _function_1);
      EdeltaLibrary.addNewEDataType(it, "MainFooDataType", null);
      final Consumer<EEnum> _function_2 = (EEnum it_1) -> {
        EdeltaLibrary.addNewEEnumLiteral(it_1, "FooEnumLiteral");
      };
      EdeltaLibrary.addNewEEnum(it, "MainFooEnum", _function_2);
      final Consumer<EClass> _function_3 = (EClass it_1) -> {
        EdeltaLibrary.addNewEAttribute(it_1, "myClassAttribute", null);
      };
      EdeltaLibrary.addNewEClass(it, "MyClass", _function_3);
      final Consumer<EPackage> _function_4 = (EPackage it_1) -> {
        final Consumer<EClass> _function_5 = (EClass it_2) -> {
          EdeltaLibrary.addNewEAttribute(it_2, "mySubPackageAttribute", null);
          EdeltaLibrary.addNewEReference(it_2, "mySubPackageReference", null);
        };
        EdeltaLibrary.addNewEClass(it_1, "MainSubPackageFooClass", _function_5);
        final Consumer<EClass> _function_6 = (EClass it_2) -> {
          EdeltaLibrary.addNewEAttribute(it_2, "myClassAttribute", null);
        };
        EdeltaLibrary.addNewEClass(it_1, "MyClass", _function_6);
        final Consumer<EPackage> _function_7 = (EPackage it_2) -> {
          EdeltaLibrary.addNewEClass(it_2, "MyClass");
        };
        EdeltaLibrary.addNewESubpackage(it_1, "subsubpackage", "subsubpackage", "http://subsubpackage", _function_7);
      };
      EdeltaLibrary.addNewESubpackage(it, "mainsubpackage", "mainsubpackage", "http://mainsubpackage", _function_4);
    };
    return this.createEPackage("mainpackage", "mainpackage", "http://mainpackage", _function);
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
      IterableExtensions.join(IterableExtensions.<String>sort(IterableExtensions.<Issue, String>map(IterableExtensions.<Issue>filter(this.validationTestHelper.validate(o), _function), _function_1)), "\n"));
  }
  
  protected void assertEqualsStrings(final CharSequence expected, final CharSequence actual) {
    Assert.assertEquals(expected.toString().replace("\r", ""), actual.toString().replace("\r", ""));
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
    String _join = IterableExtensions.join(IterableExtensions.<String, String>sortBy(ListExtensions.<EdeltaAccessibleElement, String>map(elements, _function), _function_1), "\n");
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

package edelta.refactorings.lib.tests;

import com.google.common.collect.Iterables;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaRefactorings;
import edelta.refactorings.lib.tests.AbstractTest;
import edelta.refactorings.lib.tests.utils.InMemoryLoggerAppender;
import edelta.testutils.EdeltaTestUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.iterable.ThrowingExtractor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaRefactoringsTest extends AbstractTest {
  private EdeltaRefactorings refactorings;
  
  private InMemoryLoggerAppender appender;
  
  private String testModelDirectory;
  
  private String testModelFile;
  
  @Before
  public void setup() {
    try {
      EdeltaRefactorings _edeltaRefactorings = new EdeltaRefactorings();
      this.refactorings = _edeltaRefactorings;
      InMemoryLoggerAppender _inMemoryLoggerAppender = new InMemoryLoggerAppender();
      this.appender = _inMemoryLoggerAppender;
      this.refactorings.getLogger().addAppender(this.appender);
      this.refactorings.performSanityChecks();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private String withInputModel(final String testModelDirectory, final String testModelFile) {
    String _xblockexpression = null;
    {
      this.testModelDirectory = testModelDirectory;
      _xblockexpression = this.testModelFile = testModelFile;
    }
    return _xblockexpression;
  }
  
  private void loadModelFile() {
    this.checkInputModelSettings();
    this.refactorings.loadEcoreFile(
      (((AbstractTest.TESTECORES + this.testModelDirectory) + "/") + this.testModelFile));
  }
  
  private void assertModifiedFile() {
    try {
      this.checkInputModelSettings();
      EdeltaTestUtils.assertFilesAreEquals(
        (((AbstractTest.EXPECTATIONS + this.testModelDirectory) + "/") + this.testModelFile), 
        (AbstractTest.MODIFIED + this.testModelFile));
      this.assertLogIsEmpty();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private void assertLogIsEmpty() {
    Assertions.assertThat(this.appender.getResult()).isEmpty();
  }
  
  private void assertModifiedFileIsSameAsOriginal() {
    try {
      this.checkInputModelSettings();
      EdeltaTestUtils.assertFilesAreEquals(
        (((AbstractTest.TESTECORES + this.testModelDirectory) + "/") + this.testModelFile), 
        (AbstractTest.MODIFIED + this.testModelFile));
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private void assertOppositeRefactorings(final Runnable first, final Runnable second) {
    try {
      this.loadModelFile();
      first.run();
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      second.run();
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  private AbstractStringAssert<?> checkInputModelSettings() {
    AbstractStringAssert<?> _xblockexpression = null;
    {
      Assertions.assertThat(this.testModelDirectory).isNotNull();
      _xblockexpression = Assertions.assertThat(this.testModelFile).isNotNull();
    }
    return _xblockexpression;
  }
  
  @Test
  public void test_ConstructorArgument() {
    EdeltaRefactorings _edeltaRefactorings = new EdeltaRefactorings(new AbstractEdelta() {
    });
    this.refactorings = _edeltaRefactorings;
    final EClass c = this.createEClassWithoutPackage("C1");
    this.refactorings.addMandatoryAttribute(c, "test", this.stringDataType);
    final EAttribute attr = IterableExtensions.<EAttribute>head(Iterables.<EAttribute>filter(c.getEStructuralFeatures(), EAttribute.class));
    final Function<EAttribute, String> _function = (EAttribute it) -> {
      return it.getName();
    };
    Assertions.<EAttribute>assertThat(attr).<String>returns("test", _function);
  }
  
  @Test
  public void test_addMandatoryAttribute() {
    final EClass c = this.createEClassWithoutPackage("C1");
    this.refactorings.addMandatoryAttribute(c, "test", this.stringDataType);
    final EAttribute attr = IterableExtensions.<EAttribute>head(Iterables.<EAttribute>filter(c.getEStructuralFeatures(), EAttribute.class));
    final Function<EAttribute, String> _function = (EAttribute it) -> {
      return it.getName();
    };
    final Function<EAttribute, EDataType> _function_1 = (EAttribute it) -> {
      return it.getEAttributeType();
    };
    final Function<EAttribute, Boolean> _function_2 = (EAttribute it) -> {
      return Boolean.valueOf(it.isRequired());
    };
    Assertions.<EAttribute>assertThat(attr).<String>returns("test", _function).<EDataType>returns(this.stringDataType, _function_1).<Boolean>returns(Boolean.valueOf(true), _function_2);
  }
  
  @Test
  public void test_addMandatoryReference() {
    final EClass c = this.createEClassWithoutPackage("C1");
    this.refactorings.addMandatoryReference(c, "test", this.eClassReference);
    final EReference attr = IterableExtensions.<EReference>head(Iterables.<EReference>filter(c.getEStructuralFeatures(), EReference.class));
    final Function<EReference, String> _function = (EReference it) -> {
      return it.getName();
    };
    final Function<EReference, EClass> _function_1 = (EReference it) -> {
      return it.getEReferenceType();
    };
    final Function<EReference, Boolean> _function_2 = (EReference it) -> {
      return Boolean.valueOf(it.isRequired());
    };
    Assertions.<EReference>assertThat(attr).<String>returns("test", _function).<EClass>returns(this.eClassReference, _function_1).<Boolean>returns(Boolean.valueOf(true), _function_2);
  }
  
  @Test
  public void test_mergeFeatures() {
    try {
      this.withInputModel("mergeFeatures", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("firstName");
      EStructuralFeature _eStructuralFeature_1 = person.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures("name", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_mergeFeaturesDifferent() {
    try {
      this.withInputModel("mergeFeaturesDifferent", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("firstName");
      EStructuralFeature _eStructuralFeature_1 = person.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures("name", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      final EClass student = this.refactorings.getEClass("PersonList", "Student");
      EStructuralFeature _eStructuralFeature_2 = person.getEStructuralFeature("lastName");
      EStructuralFeature _eStructuralFeature_3 = student.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures("name", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_2, _eStructuralFeature_3)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      EStructuralFeature _eStructuralFeature_4 = person.getEStructuralFeature("list");
      EStructuralFeature _eStructuralFeature_5 = person.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures("name", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_4, _eStructuralFeature_5)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Person.lastName: The two features cannot be merged:");
      _builder.newLine();
      _builder.append("ecore.ETypedElement.lowerBound:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person.firstName: 0");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person.lastName: 1");
      _builder.newLine();
      _builder.newLine();
      _builder.append("ERROR: PersonList.Student.lastName: The two features cannot be merged:");
      _builder.newLine();
      _builder.append("ecore.ENamedElement.name:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person: Person");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Student: Student");
      _builder.newLine();
      _builder.append("ecore.EStructuralFeature.eContainingClass:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person.lastName: PersonList.Person");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Student.lastName: PersonList.Student");
      _builder.newLine();
      _builder.newLine();
      _builder.append("ERROR: PersonList.Person.lastName: The two features cannot be merged:");
      _builder.newLine();
      _builder.append("different kinds:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person.list: ecore.EReference");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person.lastName: ecore.EAttribute");
      _builder.newLine();
      _builder.newLine();
      Assertions.assertThat(this.appender.getResult()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_mergeFeatures2() {
    try {
      this.withInputModel("mergeFeatures2", "PersonList.ecore");
      this.loadModelFile();
      final EClass list = this.refactorings.getEClass("PersonList", "List");
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = list.getEStructuralFeature("wplaces");
      EStructuralFeature _eStructuralFeature_1 = list.getEStructuralFeature("lplaces");
      this.refactorings.mergeFeatures(list.getEStructuralFeature("places"), 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      EStructuralFeature _eStructuralFeature_2 = person.getEStructuralFeature("firstName");
      EStructuralFeature _eStructuralFeature_3 = person.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures(person.getEStructuralFeature("name"), 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_2, _eStructuralFeature_3)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_mergeFeatures2NonCompliant() {
    this.withInputModel("mergeFeatures2", "PersonList.ecore");
    this.loadModelFile();
    final EClass list = this.refactorings.getEClass("PersonList", "List");
    final EClass person = this.refactorings.getEClass("PersonList", "Person");
    final EStructuralFeature wplaces = list.getEStructuralFeature("wplaces");
    EStructuralFeature _eStructuralFeature = list.getEStructuralFeature("places");
    EStructuralFeature _eStructuralFeature_1 = list.getEStructuralFeature("lplaces");
    this.refactorings.mergeFeatures(wplaces, 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
    wplaces.setLowerBound(1);
    EStructuralFeature _eStructuralFeature_2 = list.getEStructuralFeature("wplaces");
    EStructuralFeature _eStructuralFeature_3 = list.getEStructuralFeature("lplaces");
    this.refactorings.mergeFeatures(list.getEStructuralFeature("places"), 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_2, _eStructuralFeature_3)));
    EStructuralFeature _eStructuralFeature_4 = person.getEStructuralFeature("firstName");
    EStructuralFeature _eStructuralFeature_5 = person.getEStructuralFeature("lastName");
    this.refactorings.mergeFeatures(list.getEStructuralFeature("places"), 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_4, _eStructuralFeature_5)));
    EStructuralFeature _eStructuralFeature_6 = person.getEStructuralFeature("firstName");
    EStructuralFeature _eStructuralFeature_7 = person.getEStructuralFeature("lastName");
    this.refactorings.mergeFeatures(person.getEStructuralFeature("age"), 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_6, _eStructuralFeature_7)));
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ERROR: PersonList.List.wplaces: features not compliant with type PersonList.WorkPlace:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.List.places: PersonList.Place");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.List.lplaces: PersonList.LivingPlace");
    _builder.newLine();
    _builder.append("ERROR: PersonList.List.wplaces: The two features cannot be merged:");
    _builder.newLine();
    _builder.append("ecore.ETypedElement.lowerBound:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.List.places: 0");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.List.wplaces: 1");
    _builder.newLine();
    _builder.newLine();
    _builder.append("ERROR: PersonList.List.places: features not compliant with type PersonList.Place:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.Person.firstName: ecore.EString");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.Person.lastName: ecore.EString");
    _builder.newLine();
    _builder.append("ERROR: PersonList.Person.age: features not compliant with type ecore.EInt:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.Person.firstName: ecore.EString");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.Person.lastName: ecore.EString");
    _builder.newLine();
    Assertions.assertThat(this.appender.getResult()).isEqualTo(
      _builder.toString());
  }
  
  @Test
  public void test_mergeFeatures3() {
    try {
      this.withInputModel("mergeFeatures3", "PersonList.ecore");
      this.loadModelFile();
      final EClass list = this.refactorings.getEClass("PersonList", "List");
      final EClass place = this.refactorings.getEClass("PersonList", "Place");
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = list.getEStructuralFeature("wplaces");
      EStructuralFeature _eStructuralFeature_1 = list.getEStructuralFeature("lplaces");
      this.refactorings.mergeFeatures("places", place, 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      EStructuralFeature _eStructuralFeature_2 = person.getEStructuralFeature("firstName");
      EStructuralFeature _eStructuralFeature_3 = person.getEStructuralFeature("lastName");
      this.refactorings.mergeFeatures("name", this.stringDataType, 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature_2, _eStructuralFeature_3)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_enumToSubclasses() {
    try {
      this.withInputModel("enumToSubclasses", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("gender");
      final Collection<EClass> result = this.refactorings.enumToSubclasses(
        ((EAttribute) _eStructuralFeature));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
      final ThrowingExtractor<EClass, String, Exception> _function = (EClass it) -> {
        return it.getName();
      };
      Assertions.<EClass>assertThat(result).<String, Exception>extracting(_function).containsExactlyInAnyOrder("Male", "Female");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_enumToSubclassesNotAnEEnum() {
    try {
      this.withInputModel("enumToSubclasses", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      EStructuralFeature _eStructuralFeature = person.getEStructuralFeature("firstname");
      final Collection<EClass> result = this.refactorings.enumToSubclasses(
        ((EAttribute) _eStructuralFeature));
      Assertions.<EClass>assertThat(result).isNull();
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: PersonList.Person.firstname: Not an EEnum: ecore.EString");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_subclassesToEnum() {
    try {
      this.withInputModel("subclassesToEnum", "PersonList.ecore");
      this.loadModelFile();
      final EPackage personList = this.refactorings.getEPackage("PersonList");
      EClassifier _eClassifier = personList.getEClassifier("Male");
      EClassifier _eClassifier_1 = personList.getEClassifier("Female");
      final EAttribute result = this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(((EClass) _eClassifier), ((EClass) _eClassifier_1))));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
      final Function<EAttribute, String> _function = (EAttribute it) -> {
        return it.getName();
      };
      Assertions.<EAttribute>assertThat(result).<String>returns("gender", _function);
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_subclassesToEnumSubclassesNotEmpty() {
    try {
      this.withInputModel("subclassesToEnumSubclassesNotEmpty", "PersonList.ecore");
      this.loadModelFile();
      final EPackage personList = this.refactorings.getEPackage("PersonList");
      EClassifier _eClassifier = personList.getEClassifier("Male");
      EClassifier _eClassifier_1 = personList.getEClassifier("NotSpecified");
      EClassifier _eClassifier_2 = personList.getEClassifier("Female");
      final EAttribute result = this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(((EClass) _eClassifier), ((EClass) _eClassifier_1), ((EClass) _eClassifier_2))));
      Assertions.<EAttribute>assertThat(result).isNull();
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Male: Not an empty class: PersonList.Male:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Male.maleName");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Female: Not an empty class: PersonList.Female:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Female.femaleName");
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_subclassesToEnumSubclassesWrongSubclasses() {
    try {
      this.withInputModel("subclassesToEnumSubclassesWrongSubclasses", "PersonList.ecore");
      this.loadModelFile();
      final EPackage personList = this.refactorings.getEPackage("PersonList");
      EClassifier _eClassifier = personList.getEClassifier("Male");
      EClassifier _eClassifier_1 = personList.getEClassifier("Female");
      EClassifier _eClassifier_2 = personList.getEClassifier("FemaleEmployee");
      EClassifier _eClassifier_3 = personList.getEClassifier("Employee");
      EAttribute result = this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(((EClass) _eClassifier), ((EClass) _eClassifier_1), ((EClass) _eClassifier_2), ((EClass) _eClassifier_3))));
      Assertions.<EAttribute>assertThat(result).isNull();
      EClassifier _eClassifier_4 = personList.getEClassifier("Female");
      EClassifier _eClassifier_5 = personList.getEClassifier("AnotherFemale");
      result = this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(((EClass) _eClassifier_4), ((EClass) _eClassifier_5))));
      Assertions.<EAttribute>assertThat(result).isNull();
      EClassifier _eClassifier_6 = personList.getEClassifier("Female");
      result = this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(((EClass) _eClassifier_6))));
      Assertions.<EAttribute>assertThat(result).isNull();
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.FemaleEmployee: Expected one superclass: PersonList.FemaleEmployee instead of:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Person");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Employee");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Employee: Expected one superclass: PersonList.Employee instead of:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("empty");
      _builder.newLine();
      _builder.append("ERROR: PersonList.AnotherFemale: Wrong superclass of PersonList.AnotherFemale:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("Expected: PersonList.Person");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("Actual  : PersonList.AnotherPerson");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Person: The class has additional subclasses:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Male");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.FemaleEmployee");
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_enumToSubclasses_IsOppositeOf_subclassesToEnum() {
    this.withInputModel("enumToSubclasses", "PersonList.ecore");
    final Runnable _function = () -> {
      this.refactorings.enumToSubclasses(
        this.refactorings.getEAttribute("PersonList", "Person", "gender"));
    };
    final Runnable _function_1 = () -> {
      EClass _eClass = this.refactorings.getEClass("PersonList", "Male");
      EClass _eClass_1 = this.refactorings.getEClass("PersonList", "Female");
      this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_eClass, _eClass_1)));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_subclassesToEnum_IsOppositeOf_enumToSubclasses() {
    this.withInputModel("subclassesToEnum", "PersonList.ecore");
    final Runnable _function = () -> {
      EClass _eClass = this.refactorings.getEClass("PersonList", "Male");
      EClass _eClass_1 = this.refactorings.getEClass("PersonList", "Female");
      this.refactorings.subclassesToEnum("Gender", 
        Collections.<EClass>unmodifiableList(CollectionLiterals.<EClass>newArrayList(_eClass, _eClass_1)));
    };
    final Runnable _function_1 = () -> {
      this.refactorings.enumToSubclasses(
        this.refactorings.getEAttribute("PersonList", "Person", "gender"));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_extractClassWithAttributes() {
    try {
      this.withInputModel("extractClassWithAttributes", "PersonList.ecore");
      this.loadModelFile();
      EAttribute _eAttribute = this.refactorings.getEAttribute("PersonList", "Person", "street");
      EAttribute _eAttribute_1 = this.refactorings.getEAttribute("PersonList", "Person", "houseNumber");
      this.refactorings.extractClass("Address", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eAttribute, _eAttribute_1)), 
        "address");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractClassWithAttributesContainedInDifferentClasses() {
    try {
      this.withInputModel("extractClassWithAttributesContainedInDifferentClasses", "PersonList.ecore");
      this.loadModelFile();
      EAttribute _eAttribute = this.refactorings.getEAttribute("PersonList", "Person", "street");
      EAttribute _eAttribute_1 = this.refactorings.getEAttribute("PersonList", "Person2", "street");
      this.refactorings.extractClass("Address", 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eAttribute, _eAttribute_1)), 
        "address");
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Person: Extracted features must belong to the same class: PersonList.Person");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Person2: Extracted features must belong to the same class: PersonList.Person2");
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_extractClassWithAttributesEmpty() {
    final EClass result = this.refactorings.extractClass("Address", 
      Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList()), 
      "address");
    Assertions.<EClass>assertThat(result).isNull();
  }
  
  @Test
  public void test_referenceToClassBidirectional() {
    try {
      this.withInputModel("referenceToClassBidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClassWithCardinality() {
    try {
      this.withInputModel("referenceToClassWithCardinality", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClassUnidirectional() {
    try {
      this.withInputModel("referenceToClassUnidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClassWithContainmentReference() {
    try {
      this.withInputModel("referenceToClassWithContainmentReference", "PersonList.ecore");
      this.loadModelFile();
      final EReference ref = this.refactorings.getEReference("PersonList", "Person", "works");
      this.refactorings.referenceToClass("WorkingPosition", ref);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: PersonList.Person.works: Cannot apply referenceToClass on containment reference: PersonList.Person.works");
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_classToReferenceWhenClassIsNotReferred() {
    this.withInputModel("classToReferenceWronglyReferred", "TestEcore.ecore");
    this.loadModelFile();
    this.refactorings.classToReference(this.refactorings.getEClass("p", "CNotReferred"));
    Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: p.CNotReferred: The EClass is not referred: p.CNotReferred");
  }
  
  @Test
  public void test_classToReferenceWhenClassIsReferredMoreThanOnce() {
    this.withInputModel("classToReferenceWronglyReferred", "TestEcore.ecore");
    this.loadModelFile();
    this.refactorings.classToReference(this.refactorings.getEClass("p", "C"));
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ERROR: p.C: The EClass is referred by more than one container:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("p.C1.r1");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("p.C2.r2");
    _builder.newLine();
    Assertions.assertThat(this.appender.getResult()).isEqualTo(_builder.toString());
  }
  
  @Test
  public void test_classToReferenceUnidirectional() {
    try {
      this.withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
      this.refactorings.classToReference(cl);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_classToReferenceWithMissingTarget() {
    this.withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
    this.loadModelFile();
    final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
    EList<EStructuralFeature> _eStructuralFeatures = cl.getEStructuralFeatures();
    EStructuralFeature _eStructuralFeature = cl.getEStructuralFeature("workPlace");
    _eStructuralFeatures.remove(_eStructuralFeature);
    this.refactorings.classToReference(cl);
    Assertions.assertThat(this.appender.getResult().trim()).isEqualTo("ERROR: PersonList.WorkingPosition: Missing reference to target type: PersonList.WorkingPosition");
  }
  
  @Test
  public void test_classToReferenceWithTooManyTargets() {
    this.withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
    this.loadModelFile();
    final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
    EReference _createEReference = this.createEReference(cl, "another");
    final Procedure1<EReference> _function = (EReference it) -> {
      it.setEType(this.refactorings.getEClass("PersonList", "List"));
    };
    ObjectExtensions.<EReference>operator_doubleArrow(_createEReference, _function);
    this.refactorings.classToReference(cl);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("ERROR: PersonList.WorkingPosition: Too many references to target type:");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.WorkingPosition.workPlace");
    _builder.newLine();
    _builder.append("  ");
    _builder.append("PersonList.WorkingPosition.another");
    _builder.newLine();
    Assertions.assertThat(this.appender.getResult()).isEqualTo(_builder.toString());
  }
  
  @Test
  public void test_classToReferenceUnidirectionalWithoutOppositeIsOk() {
    try {
      this.withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
      EList<EStructuralFeature> _eStructuralFeatures = cl.getEStructuralFeatures();
      EStructuralFeature _eStructuralFeature = cl.getEStructuralFeature("person");
      final Procedure1<EStructuralFeature> _function = (EStructuralFeature it) -> {
        EReference _eOpposite = ((EReference) it).getEOpposite();
        _eOpposite.setEOpposite(null);
      };
      EStructuralFeature _doubleArrow = ObjectExtensions.<EStructuralFeature>operator_doubleArrow(_eStructuralFeature, _function);
      _eStructuralFeatures.remove(_doubleArrow);
      this.refactorings.classToReference(cl);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_classToReferenceBidirectional() {
    try {
      this.withInputModel("classToReferenceBidirectional", "PersonList.ecore");
      this.loadModelFile();
      final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
      this.refactorings.classToReference(cl);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_classToReferenceWithCardinality() {
    try {
      this.withInputModel("classToReferenceWithCardinality", "PersonList.ecore");
      this.loadModelFile();
      final EClass cl = this.refactorings.getEClass("PersonList", "WorkingPosition");
      this.refactorings.classToReference(cl);
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional() {
    this.withInputModel("referenceToClassUnidirectional", "PersonList.ecore");
    final Runnable _function = () -> {
      this.refactorings.referenceToClass("WorkingPosition", 
        this.refactorings.getEReference("PersonList", "Person", "works"));
    };
    final Runnable _function_1 = () -> {
      this.refactorings.classToReference(
        this.refactorings.getEClass("PersonList", "WorkingPosition"));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_referenceToClass_IsOppositeOf_classToReferenceUnidirectional2() {
    this.withInputModel("classToReferenceUnidirectional", "PersonList.ecore");
    final Runnable _function = () -> {
      this.refactorings.classToReference(
        this.refactorings.getEClass("PersonList", "WorkingPosition"));
    };
    final Runnable _function_1 = () -> {
      this.refactorings.referenceToClass("WorkingPosition", 
        this.refactorings.getEReference("PersonList", "Person", "works"));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional() {
    this.withInputModel("referenceToClassBidirectional", "PersonList.ecore");
    final Runnable _function = () -> {
      this.refactorings.referenceToClass("WorkingPosition", 
        this.refactorings.getEReference("PersonList", "Person", "works"));
    };
    final Runnable _function_1 = () -> {
      this.refactorings.classToReference(
        this.refactorings.getEClass("PersonList", "WorkingPosition"));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_referenceToClass_IsOppositeOf_classToReferenceBidirectional2() {
    this.withInputModel("classToReferenceBidirectional", "PersonList.ecore");
    final Runnable _function = () -> {
      this.refactorings.classToReference(
        this.refactorings.getEClass("PersonList", "WorkingPosition"));
    };
    final Runnable _function_1 = () -> {
      this.refactorings.referenceToClass("WorkingPosition", 
        this.refactorings.getEReference("PersonList", "Person", "works"));
    };
    this.assertOppositeRefactorings(_function, _function_1);
    this.assertLogIsEmpty();
  }
  
  @Test
  public void test_extractSuperClass() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Function1<EClass, EList<EAttribute>> _function_1 = (EClass it) -> {
      return it.getEAttributes();
    };
    final List<EAttribute> duplicates = IterableExtensions.<EAttribute>toList(Iterables.<EAttribute>concat(IterableExtensions.<EClass, EList<EAttribute>>map(this.EClasses(p), _function_1)));
    this.refactorings.extractSuperclass(duplicates);
    final Function1<EClassifier, String> _function_2 = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> classifiersNames = ListExtensions.<EClassifier, String>map(p.getEClassifiers(), _function_2);
    Assertions.<String>assertThat(classifiersNames).hasSize(3).containsExactly("C1", "C2", "A1Element");
    final Iterable<EClass> classes = this.EClasses(p);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[0]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[1]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes()).hasSize(1);
    final EAttribute extracted = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes());
    Assertions.assertThat(extracted.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extracted.getEAttributeType()).isEqualTo(this.stringDataType);
  }
  
  @Test
  public void test_extractSuperClassUnique() {
    EPackage _createEPackage = this.factory.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      EClass _createEClass = this.createEClass(it, "C1");
      final Procedure1<EClass> _function_1 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_2 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_2);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass, _function_1);
      EClass _createEClass_1 = this.createEClass(it, "C2");
      final Procedure1<EClass> _function_2 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_3 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_3);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_1, _function_2);
      EClass _createEClass_2 = this.createEClass(it, "C3");
      final Procedure1<EClass> _function_3 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_4 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_4);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_2, _function_3);
      EClass _createEClass_3 = this.createEClass(it, "C4");
      final Procedure1<EClass> _function_4 = (EClass it_1) -> {
        EAttribute _createEAttribute = this.createEAttribute(it_1, "A1");
        final Procedure1<EAttribute> _function_5 = (EAttribute it_2) -> {
          it_2.setEType(this.stringDataType);
          it_2.setLowerBound(2);
        };
        ObjectExtensions.<EAttribute>operator_doubleArrow(_createEAttribute, _function_5);
      };
      ObjectExtensions.<EClass>operator_doubleArrow(_createEClass_3, _function_4);
    };
    final EPackage p = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    final Function1<EClass, EList<EAttribute>> _function_1 = (EClass it) -> {
      return it.getEAttributes();
    };
    final List<EAttribute> attributes = IterableExtensions.<EAttribute>toList(Iterables.<EAttribute>concat(IterableExtensions.<EClass, EList<EAttribute>>map(this.EClasses(p), _function_1)));
    this.refactorings.extractSuperclass(
      IterableExtensions.<EAttribute>toList(IterableExtensions.<EAttribute>take(attributes, 2)));
    this.refactorings.extractSuperclass(
      attributes.stream().skip(2).collect(Collectors.<EAttribute>toList()));
    final Function1<EClassifier, String> _function_2 = (EClassifier it) -> {
      return it.getName();
    };
    final List<String> classifiersNames = ListExtensions.<EClassifier, String>map(p.getEClassifiers(), _function_2);
    Assertions.<String>assertThat(classifiersNames).containsExactly("C1", "C2", "C3", "C4", "A1Element", "A1Element1");
    final Iterable<EClass> classes = this.EClasses(p);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[0]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[1]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[2]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[3]).getEAttributes()).isEmpty();
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes()).hasSize(1);
    Assertions.<EAttribute>assertThat((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes()).hasSize(1);
    final EAttribute extractedA1NoLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[4]).getEAttributes());
    Assertions.assertThat(extractedA1NoLowerBound.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extractedA1NoLowerBound.getEAttributeType()).isEqualTo(this.stringDataType);
    Assertions.assertThat(extractedA1NoLowerBound.getLowerBound()).isZero();
    final EAttribute extractedA1WithLowerBound = IterableExtensions.<EAttribute>head((((EClass[])Conversions.unwrapArray(classes, EClass.class))[5]).getEAttributes());
    Assertions.assertThat(extractedA1WithLowerBound.getName()).isEqualTo("A1");
    Assertions.<EDataType>assertThat(extractedA1WithLowerBound.getEAttributeType()).isEqualTo(this.stringDataType);
    Assertions.assertThat(extractedA1WithLowerBound.getLowerBound()).isEqualTo(2);
  }
  
  @Test
  public void test_pullUpFeatures() {
    try {
      this.withInputModel("pullUpFeatures", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      final EClass student = this.refactorings.getEClass("PersonList", "Student");
      final EClass employee = this.refactorings.getEClass("PersonList", "Employee");
      EStructuralFeature _eStructuralFeature = student.getEStructuralFeature("name");
      EStructuralFeature _eStructuralFeature_1 = employee.getEStructuralFeature("name");
      this.refactorings.pullUpFeatures(person, 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFile();
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_pullUpFeaturesDifferent() {
    try {
      this.withInputModel("pullUpFeaturesDifferent", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      final EClass student = this.refactorings.getEClass("PersonList", "Student");
      final EClass employee = this.refactorings.getEClass("PersonList", "Employee");
      EStructuralFeature _eStructuralFeature = student.getEStructuralFeature("name");
      EStructuralFeature _eStructuralFeature_1 = employee.getEStructuralFeature("name");
      this.refactorings.pullUpFeatures(person, 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Employee.name: The two features are not equal:");
      _builder.newLine();
      _builder.append("ecore.ETypedElement.lowerBound:");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Student.name: 0");
      _builder.newLine();
      _builder.append("  ");
      _builder.append("PersonList.Employee.name: 1");
      _builder.newLine();
      _builder.newLine();
      Assertions.assertThat(this.appender.getResult()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
  
  @Test
  public void test_pullUpFeaturesNotSubclass() {
    try {
      this.withInputModel("pullUpFeaturesNotSubclass", "PersonList.ecore");
      this.loadModelFile();
      final EClass person = this.refactorings.getEClass("PersonList", "Person");
      final EClass student = this.refactorings.getEClass("PersonList", "Student");
      final EClass employee = this.refactorings.getEClass("PersonList", "Employee");
      EStructuralFeature _eStructuralFeature = student.getEStructuralFeature("name");
      EStructuralFeature _eStructuralFeature_1 = employee.getEStructuralFeature("name");
      this.refactorings.pullUpFeatures(person, 
        Collections.<EStructuralFeature>unmodifiableList(CollectionLiterals.<EStructuralFeature>newArrayList(_eStructuralFeature, _eStructuralFeature_1)));
      this.refactorings.saveModifiedEcores(AbstractTest.MODIFIED);
      this.assertModifiedFileIsSameAsOriginal();
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("ERROR: PersonList.Student.name: Not a direct subclass of destination: PersonList.Student");
      _builder.newLine();
      _builder.append("ERROR: PersonList.Employee.name: Not a direct subclass of destination: PersonList.Employee");
      _builder.newLine();
      Assertions.assertThat(this.appender.getResult()).isEqualTo(
        _builder.toString());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}

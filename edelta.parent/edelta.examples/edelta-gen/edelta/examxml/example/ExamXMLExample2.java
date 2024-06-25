package edelta.examxml.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class ExamXMLExample2 extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;

  public ExamXMLExample2(final EdeltaRuntime other) {
    super(other);
    refactorings = new EdeltaRefactorings(this);
  }

  public void removeAttributes(final EPackage it) {
    EdeltaUtils.removeElement(getEAttribute("examxml", "ExamElement", "question"));
    EdeltaUtils.removeElement(getEAttribute("examxml", "ExamElement", "optional"));
  }

  public void introduceExerciseElement(final EPackage it) {
    this.stdLib.addNewSubclass(getEClass("examxml", "ExamElement"), "ExerciseElement");
  }

  public void splitOpenElement(final EPackage it) {
    final EClass toSplit = getEClass("examxml", "OpenElement");
    final EPackage ePackage = getEPackage("examxml");
    final EReference elementsFeature = getEReference("examxml", "Exam", "elements");
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature origElementsFeature, EObject origExam, EObject newExam) -> {
        final ArrayList<EObject> newElements = CollectionLiterals.<EObject>newArrayList();
        final List<EObject> origElements = EdeltaEcoreUtil.getValueAsList(origExam, origElementsFeature);
        for (final EObject origElement : origElements) {
          {
            final EClass origElementClass = origElement.eClass();
            EClass _original = it_1.<EClass>getOriginal(toSplit);
            boolean _equals = Objects.equals(origElementClass, _original);
            if (_equals) {
              boolean _isSet = EdeltaEcoreUtil.isSet(origElement, "specificQuestion1");
              if (_isSet) {
                EObject _createFrom = it_1.createFrom(this.getEClass(ePackage, "OpenElement1"), origElement);
                newElements.add(_createFrom);
              }
              boolean _isSet_1 = EdeltaEcoreUtil.isSet(origElement, "specificQuestion2");
              if (_isSet_1) {
                EObject _createFrom_1 = it_1.createFrom(this.getEClass(ePackage, "OpenElement2"), origElement);
                newElements.add(_createFrom_1);
              }
            } else {
              EObject _migrated = it_1.getMigrated(origElement);
              newElements.add(_migrated);
            }
          }
        }
        newExam.eSet(elementsFeature, newElements);
      };
      it_1.copyRule(
        it_1.<EStructuralFeature>wasRelatedTo(elementsFeature), _function_1);
    };
    this.refactorings.splitClass(toSplit, 
      Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("OpenElement1", "OpenElement2")), _function);
    EdeltaUtils.removeElement(getEAttribute("examxml", "OpenElement1", "specificQuestion2"));
    EdeltaUtils.removeElement(getEAttribute("examxml", "OpenElement2", "specificQuestion1"));
  }

  @Override
  public void performSanityChecks() throws Exception {
    ensureEPackageIsLoaded("examxml");
    ensureEPackageIsLoaded("ecore");
  }

  @Override
  protected void doExecute() throws Exception {
    removeAttributes(getEPackage("examxml"));
    introduceExerciseElement(getEPackage("examxml"));
    splitOpenElement(getEPackage("examxml"));
  }
}

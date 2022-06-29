package edelta.examxml.example;

import com.google.common.base.Objects;
import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.emf.common.util.EList;
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

  public EObject createAndCopyFrom(final EdeltaModelMigrator modelMigrator, final EClass newClass, final EObject origElement) {
    final Consumer<EObject> _function = (EObject o) -> {
      final EList<EStructuralFeature> origElementFeatures = origElement.eClass().getEAllStructuralFeatures();
      for (final EStructuralFeature origElementFeature : origElementFeatures) {
        modelMigrator.copyFrom(o, 
          newClass.getEStructuralFeature(origElementFeature.getName()), origElement, origElementFeature);
      }
    };
    return EdeltaEcoreUtil.createInstance(newClass, _function);
  }

  public void removeAttributes(final EPackage it) {
    EdeltaUtils.removeElement(getEAttribute("examxml", "ExamElement", "question"));
    EdeltaUtils.removeElement(getEAttribute("examxml", "ExamElement", "optional"));
  }

  public void introduceExerciseElement(final EPackage it) {
    this.stdLib.addNewSubclass(getEClass("examxml", "ExamElement"), "ExerciseElement");
  }

  public void splitOpenElement(final EPackage it) {
    final EClass superClass = getEClass("examxml", "OpenElement");
    final EPackage ePackage = getEPackage("examxml");
    final EReference elementsFeature = getEReference("examxml", "Exam", "elements");
    final Consumer<EdeltaModelMigrator> _function = (EdeltaModelMigrator it_1) -> {
      final EdeltaModelMigrator.CopyProcedure _function_1 = (EStructuralFeature origElementsFeature, EObject origExam, EObject newExam) -> {
        final ArrayList<EObject> newElements = CollectionLiterals.<EObject>newArrayList();
        final List<EObject> origElements = EdeltaEcoreUtil.getValueAsList(origExam, origElementsFeature);
        for (final EObject origElement : origElements) {
          {
            final EClass origElementClass = origElement.eClass();
            EClass _original = it_1.<EClass>getOriginal(superClass);
            boolean _equals = Objects.equal(origElementClass, _original);
            if (_equals) {
              final EStructuralFeature specificQuestion1 = origElementClass.getEStructuralFeature("specificQuestion1");
              final EStructuralFeature specificQuestion2 = origElementClass.getEStructuralFeature("specificQuestion2");
              boolean _eIsSet = origElement.eIsSet(specificQuestion1);
              if (_eIsSet) {
                EObject _createAndCopyFrom = this.createAndCopyFrom(it_1, this.getEClass(ePackage, "OpenElement1"), origElement);
                newElements.add(_createAndCopyFrom);
              }
              boolean _eIsSet_1 = origElement.eIsSet(specificQuestion2);
              if (_eIsSet_1) {
                EObject _createAndCopyFrom_1 = this.createAndCopyFrom(it_1, this.getEClass(ePackage, "OpenElement2"), origElement);
                newElements.add(_createAndCopyFrom_1);
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
    this.refactorings.splitClass(superClass, 
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

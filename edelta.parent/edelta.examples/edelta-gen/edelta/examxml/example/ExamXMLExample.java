package edelta.examxml.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;

@SuppressWarnings("all")
public class ExamXMLExample extends EdeltaDefaultRuntime {
  private EdeltaRefactorings refactorings;

  public ExamXMLExample(final EdeltaRuntime other) {
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
    final EClass superClass = getEClass("examxml", "OpenElement");
    final EAttribute specificQuestion1 = getEAttribute("examxml", "OpenElement", "specificQuestion1");
    final EAttribute specificQuestion2 = getEAttribute("examxml", "OpenElement", "specificQuestion2");
    final EdeltaModelMigrator.EObjectFunction _function = (EObject origElement) -> {
      EObject _xblockexpression = null;
      {
        final EClass c = origElement.eClass();
        final EStructuralFeature origSpecificQuestion1 = c.getEStructuralFeature("specificQuestion1");
        final EStructuralFeature origSpecificQuestion2 = c.getEStructuralFeature("specificQuestion2");
        EObject _xifexpression = null;
        boolean _eIsSet = origElement.eIsSet(origSpecificQuestion1);
        if (_eIsSet) {
          final Consumer<EObject> _function_1 = (EObject o) -> {
            EdeltaEcoreUtil.setValueFrom(o, specificQuestion1, origElement, origSpecificQuestion1);
          };
          _xifexpression = EdeltaEcoreUtil.createInstance(this.getEClass(superClass.getEPackage(), "OpenElement1"), _function_1);
        } else {
          final Consumer<EObject> _function_2 = (EObject o) -> {
            EdeltaEcoreUtil.setValueFrom(o, specificQuestion2, origElement, origSpecificQuestion2);
          };
          _xifexpression = EdeltaEcoreUtil.createInstance(this.getEClass(superClass.getEPackage(), "OpenElement2"), _function_2);
        }
        _xblockexpression = _xifexpression;
      }
      return _xblockexpression;
    };
    this.refactorings.introduceSubclasses(superClass, 
      Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("OpenElement1", "OpenElement2")), _function);
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

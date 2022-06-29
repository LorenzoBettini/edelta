package edelta.examxml.example;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaEcoreUtil;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaRuntime;
import edelta.lib.EdeltaUtils;
import edelta.refactorings.lib.EdeltaRefactorings;
import java.util.Collections;
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
    final EPackage ePackage = getEPackage("examxml");
    final EdeltaModelMigrator.EObjectFunction _function = (EObject origElement) -> {
      EObject _xblockexpression = null;
      {
        final EClass c = origElement.eClass();
        final EStructuralFeature origSpecificQuestion1 = c.getEStructuralFeature("specificQuestion1");
        EObject _xifexpression = null;
        boolean _eIsSet = origElement.eIsSet(origSpecificQuestion1);
        if (_eIsSet) {
          _xifexpression = EdeltaEcoreUtil.createInstance(this.getEClass(ePackage, "OpenElement1"));
        } else {
          _xifexpression = EdeltaEcoreUtil.createInstance(this.getEClass(ePackage, "OpenElement2"));
        }
        _xblockexpression = _xifexpression;
      }
      return _xblockexpression;
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

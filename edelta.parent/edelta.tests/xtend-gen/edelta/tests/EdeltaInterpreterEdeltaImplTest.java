package edelta.tests;

import edelta.interpreter.EdeltaInterpreterDiagnosticHelper;
import edelta.interpreter.EdeltaInterpreterEdeltaImpl;
import edelta.tests.EdeltaAbstractTest;
import edelta.validation.EdeltaValidator;
import java.util.Collections;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings("all")
public class EdeltaInterpreterEdeltaImplTest extends EdeltaAbstractTest {
  private EdeltaInterpreterEdeltaImpl edelta;
  
  private EdeltaInterpreterDiagnosticHelper diagnosticHelper;
  
  @Before
  public void setup() {
    this.diagnosticHelper = Mockito.<EdeltaInterpreterDiagnosticHelper>mock(EdeltaInterpreterDiagnosticHelper.class);
    EdeltaInterpreterEdeltaImpl _edeltaInterpreterEdeltaImpl = new EdeltaInterpreterEdeltaImpl(Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList()), this.diagnosticHelper);
    this.edelta = _edeltaInterpreterEdeltaImpl;
  }
  
  @Test
  public void testFirstEPackageHasPrecedence() {
    EPackage _createEPackage = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function = (EPackage it) -> {
      it.setName("Test");
    };
    final EPackage p1 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage, _function);
    EPackage _createEPackage_1 = EcoreFactory.eINSTANCE.createEPackage();
    final Procedure1<EPackage> _function_1 = (EPackage it) -> {
      it.setName("Test");
    };
    final EPackage p2 = ObjectExtensions.<EPackage>operator_doubleArrow(_createEPackage_1, _function_1);
    EdeltaInterpreterEdeltaImpl _edeltaInterpreterEdeltaImpl = new EdeltaInterpreterEdeltaImpl(Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList(p1, p2)), this.diagnosticHelper);
    this.edelta = _edeltaInterpreterEdeltaImpl;
    Assert.assertSame(p1, this.edelta.getEPackage("Test"));
  }
  
  @Test
  public void testShowError() {
    final EClass element = EcoreFactory.eINSTANCE.createEClass();
    this.edelta.showError(element, "a message");
    Mockito.<EdeltaInterpreterDiagnosticHelper>verify(this.diagnosticHelper).addError(element, EdeltaValidator.LIVE_VALIDATION_ERROR, "a message");
  }
  
  @Test
  public void testShowWarning() {
    final EClass element = EcoreFactory.eINSTANCE.createEClass();
    this.edelta.showWarning(element, "a message");
    Mockito.<EdeltaInterpreterDiagnosticHelper>verify(this.diagnosticHelper).addWarning(element, EdeltaValidator.LIVE_VALIDATION_WARNING, "a message");
  }
}

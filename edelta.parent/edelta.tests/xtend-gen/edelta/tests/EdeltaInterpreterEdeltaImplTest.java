package edelta.tests;

import edelta.interpreter.EdeltaInterpreterEdeltaImpl;
import java.util.Collections;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.ObjectExtensions;
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1;
import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("all")
public class EdeltaInterpreterEdeltaImplTest {
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
    final EdeltaInterpreterEdeltaImpl e = new EdeltaInterpreterEdeltaImpl(Collections.<EPackage>unmodifiableList(CollectionLiterals.<EPackage>newArrayList(p1, p2)));
    Assert.assertSame(p1, e.getEPackage("Test"));
  }
}

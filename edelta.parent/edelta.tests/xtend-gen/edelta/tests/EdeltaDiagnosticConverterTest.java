package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreDirectReference;
import edelta.edelta.EdeltaFactory;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import java.util.LinkedList;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.validation.IDiagnosticConverter;
import org.eclipse.xtext.validation.Issue;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaDiagnosticConverterTest extends EdeltaAbstractTest {
  @Inject
  private IDiagnosticConverter converter;
  
  @Test
  public void testEcoreReferenceDiagnosticIsDiscarded() {
    EdeltaEcoreDirectReference _createEdeltaEcoreDirectReference = EdeltaFactory.eINSTANCE.createEdeltaEcoreDirectReference();
    final BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null, 
      new Object[] { _createEdeltaEcoreDirectReference });
    final IAcceptor<Issue> _function = (Issue issue) -> {
      Assert.fail(("unwanted issue: " + issue));
    };
    this.converter.convertValidatorDiagnostic(diagnostic, _function);
  }
  
  @Test
  public void testOtherDiagnosticAreNotDiscarded() {
    EdeltaProgram _createEdeltaProgram = EdeltaFactory.eINSTANCE.createEdeltaProgram();
    final BasicDiagnostic diagnostic = new BasicDiagnostic(Diagnostic.ERROR, null, 0, null, 
      new Object[] { _createEdeltaProgram });
    final LinkedList<Issue> issues = CollectionLiterals.<Issue>newLinkedList();
    final IAcceptor<Issue> _function = (Issue issue) -> {
      issues.add(issue);
    };
    this.converter.convertValidatorDiagnostic(diagnostic, _function);
    Assert.assertEquals(1, issues.size());
  }
}

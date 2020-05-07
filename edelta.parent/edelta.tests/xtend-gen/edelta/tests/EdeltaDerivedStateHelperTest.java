package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaEcoreReference;
import edelta.edelta.EdeltaFactory;
import edelta.resource.EdeltaDerivedState;
import edelta.resource.EdeltaDerivedStateHelper;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import edelta.util.EdeltaEcoreReferenceState;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Extension;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaDerivedStateHelperTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaDerivedStateHelper _edeltaDerivedStateHelper;
  
  private final EdeltaFactory edeltaFactory = EdeltaFactory.eINSTANCE;
  
  private final EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;
  
  @Test
  public void testGetOrInstallAdapterWithNotXtextResource() {
    ResourceImpl _resourceImpl = new ResourceImpl();
    Assert.assertNotNull(this._edeltaDerivedStateHelper.getOrInstallAdapter(_resourceImpl));
  }
  
  @Test
  public void testGetOrInstallAdapterWithXtextResourceOfADifferentLanguage() {
    final XtextResource res = new XtextResource();
    res.setLanguageName("foo");
    Assert.assertNotNull(this._edeltaDerivedStateHelper.getOrInstallAdapter(res));
  }
  
  @Test
  public void testIsAdapterFor() {
    ResourceImpl _resourceImpl = new ResourceImpl();
    final EdeltaDerivedState adapter = this._edeltaDerivedStateHelper.getOrInstallAdapter(_resourceImpl);
    Assert.assertTrue(adapter.isAdapterForType(EdeltaDerivedState.class));
    Assert.assertFalse(adapter.isAdapterForType(String.class));
  }
  
  @Test
  public void testGetEdeltaEcoreReferenceState() {
    try {
      final Resource res = this._parseHelper.parse("").eResource();
      final EdeltaEcoreReference ref = this.edeltaFactory.createEdeltaEcoreReference();
      EList<EObject> _contents = res.getContents();
      _contents.add(ref);
      Assert.assertNull(this._edeltaDerivedStateHelper.getEcoreReferenceState(ref).getOriginalEnamedelement());
      final EAttribute el = this.ecoreFactory.createEAttribute();
      EdeltaEcoreReferenceState _ecoreReferenceState = this._edeltaDerivedStateHelper.getEcoreReferenceState(ref);
      _ecoreReferenceState.setOriginalEnamedelement(el);
      Assert.assertSame(el, this._edeltaDerivedStateHelper.getEcoreReferenceState(ref).getOriginalEnamedelement());
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}

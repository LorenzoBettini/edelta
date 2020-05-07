package edelta.tests;

import com.google.inject.Inject;
import edelta.resource.EdeltaDerivedState;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProvider;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Extension;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProvider.class)
@SuppressWarnings("all")
public class EdeltaDerivedStateTest extends EdeltaAbstractTest {
  @Inject
  @Extension
  private EdeltaDerivedState _edeltaDerivedState;
  
  @Test
  public void testGetOrInstallAdapterWithNotXtextResource() {
    ResourceImpl _resourceImpl = new ResourceImpl();
    Assert.assertNotNull(this._edeltaDerivedState.getOrInstallAdapter(_resourceImpl));
  }
  
  @Test
  public void testGetOrInstallAdapterWithXtextResourceOfADifferentLanguage() {
    final XtextResource res = new XtextResource();
    res.setLanguageName("foo");
    Assert.assertNotNull(this._edeltaDerivedState.getOrInstallAdapter(res));
  }
  
  @Test
  public void testIsAdapterFor() {
    ResourceImpl _resourceImpl = new ResourceImpl();
    final EdeltaDerivedState.EdeltaDerivedStateAdapter adapter = this._edeltaDerivedState.getOrInstallAdapter(_resourceImpl);
    Assert.assertTrue(adapter.isAdapterForType(EdeltaDerivedState.EdeltaDerivedStateAdapter.class));
    Assert.assertFalse(adapter.isAdapterForType(String.class));
  }
}

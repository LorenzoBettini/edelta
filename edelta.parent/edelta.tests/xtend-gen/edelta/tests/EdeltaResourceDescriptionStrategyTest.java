package edelta.tests;

import com.google.inject.Inject;
import edelta.edelta.EdeltaProgram;
import edelta.tests.EdeltaAbstractTest;
import edelta.tests.EdeltaInjectorProviderCustom;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
@SuppressWarnings("all")
public class EdeltaResourceDescriptionStrategyTest extends EdeltaAbstractTest {
  @Inject
  private ResourceDescriptionsProvider rdp;
  
  @Test
  public void testEmptyProgram() {
    this.assertExportedEPackages("", "");
  }
  
  @Test
  public void testSingleMetamodel() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"ecore\"");
    _builder.newLine();
    this.assertExportedEPackages(_builder, "");
  }
  
  @Test
  public void testReferenceToOurMetamodel() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"ecore\"");
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    this.assertExportedEPackages(_builder, "");
  }
  
  @Test
  public void testCreateEClass() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("metamodel \"ecore\"");
    _builder.newLine();
    _builder.append("metamodel \"foo\"");
    _builder.newLine();
    _builder.newLine();
    _builder.append("modifyEcore aTest epackage foo {}");
    _builder.newLine();
    this.assertExportedEPackages(_builder, "");
  }
  
  private void assertExportedEPackages(final CharSequence input, final CharSequence expected) {
    final EdeltaProgram program = this.parseWithTestEcore(input);
    this._validationTestHelper.validate(program);
    final Function1<IEObjectDescription, QualifiedName> _function = (IEObjectDescription it) -> {
      return it.getName();
    };
    this.assertEqualsStrings(expected, 
      IterableExtensions.join(IterableExtensions.<IEObjectDescription, QualifiedName>map(this.getExportedEPackageEObjectDescriptions(program), _function), ", "));
  }
  
  private Iterable<IEObjectDescription> getExportedEPackageEObjectDescriptions(final EObject o) {
    return this.getResourceDescription(o).getExportedObjectsByType(EcorePackage.eINSTANCE.getEPackage());
  }
  
  private IResourceDescription getResourceDescription(final EObject o) {
    IResourceDescription _xblockexpression = null;
    {
      final IResourceDescriptions index = this.rdp.getResourceDescriptions(o.eResource());
      _xblockexpression = index.getResourceDescription(o.eResource().getURI());
    }
    return _xblockexpression;
  }
}

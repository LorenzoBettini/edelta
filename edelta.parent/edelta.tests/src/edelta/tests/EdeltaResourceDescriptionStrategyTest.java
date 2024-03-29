package edelta.tests;

import static org.eclipse.xtext.xbase.lib.IterableExtensions.join;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.tests.injectors.EdeltaInjectorProviderCustom;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaInjectorProviderCustom.class)
public class EdeltaResourceDescriptionStrategyTest extends EdeltaAbstractTest {
	@Inject
	private ResourceDescriptionsProvider rdp;

	@Test
	public void testEmptyProgram() throws Exception {
		assertExportedEPackages("", "");
	}

	@Test
	public void testSingleMetamodel() throws Exception {
		assertExportedEPackages("metamodel \"ecore\"", "");
	}

	@Test
	public void testReferenceToOurMetamodel() throws Exception {
		assertExportedEPackages(
			"""
				metamodel "ecore"
				metamodel "foo"
			"""
		, "");
	}

	@Test
	public void testCreateEClass() throws Exception {
		// our copied packages must not be exported
		assertExportedEPackages(
			"""
				metamodel "ecore"
				metamodel "foo"
				
				modifyEcore aTest epackage foo {}
			"""
		, "");
	}

	private void assertExportedEPackages(final CharSequence input, final CharSequence expected) throws Exception {
		var program = parseWithTestEcore(input);
		validationTestHelper.validate(program);
		assertEqualsStrings(expected,
			join(map(
				getExportedEPackageEObjectDescriptions(program),
					IEObjectDescription::getName),
			", "));
	}

	private Iterable<IEObjectDescription> getExportedEPackageEObjectDescriptions(final EObject o) {
		return getResourceDescription(o)
			.getExportedObjectsByType(EcorePackage.eINSTANCE.getEPackage());
	}

	private IResourceDescription getResourceDescription(final EObject o) {
		return rdp.getResourceDescriptions(o.eResource())
			.getResourceDescription(o.eResource().getURI());
	}
}

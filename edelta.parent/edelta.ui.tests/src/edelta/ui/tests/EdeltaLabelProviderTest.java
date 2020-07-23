package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

import edelta.edelta.EdeltaProgram;
import edelta.resource.derivedstate.EdeltaDerivedStateHelper;
import edelta.ui.labeling.EdeltaLabelProvider;

@RunWith(XtextRunner.class)
@InjectWith(EdeltaUiInjectorProvider.class)
public class EdeltaLabelProviderTest {

	@Inject
	private EdeltaLabelProvider labelProvider;

	@Inject
	private EdeltaDerivedStateHelper derivedStateHelper;

	@Inject
	private ParseHelper<EdeltaProgram> parseHelper;

	private Resource resource;

	private static EcoreFactory ecoreFactory = EcoreFactory.eINSTANCE;

	@Before
	public void setup() throws Exception {
		resource = parseHelper.parse("").eResource();
	}

	@Test
	public void testEPackageStyledString() {
		var ePackage = ecoreFactory.createEPackage();
		resource.getContents().add(ePackage);
		StyledString styledText = labelProvider.getStyledText(ePackage);
		assertThat(styledText.getStyleRanges()).isEmpty();
	}

	@Test
	public void testStyledStringWhenNotModified() {
		var element = ecoreFactory.createEClass();
		resource.getContents().add(element);
		StyledString styledText = labelProvider.getStyledText(element);
		assertThat(styledText.getStyleRanges()).isEmpty();
	}

	@Test
	public void testStyledStringWhenModified() {
		var element = ecoreFactory.createEClass();
		element.setName("aclass");
		resource.getContents().add(element);
		derivedStateHelper.getModifiedElements(resource).add(element);
		StyledString styledText = labelProvider.getStyledText(element);
		assertThat(styledText.getStyleRanges()).isNotEmpty();
	}
}

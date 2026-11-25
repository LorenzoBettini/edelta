package edelta.ui.tests;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.xtext.util.TextRegion;
import org.eclipse.xtext.xbase.XbaseFactory;
import org.junit.Test;

import edelta.edelta.EdeltaFactory;
import edelta.resource.derivedstate.EdeltaModifiedFeatures;
import edelta.resource.derivedstate.EdeltaModifiedFeatures.Change;
import edelta.ui.outline.EdeltaModelOutlineNavigation;

/** Checks conservative Ecore provenance choices and source ownership. @author Lorenzo Bettini */
public class EdeltaModelOutlineNavigationTest {
	private final EcoreFactory ecore = EcoreFactory.eINSTANCE;
	private final XbaseFactory xbase = XbaseFactory.eINSTANCE;

	@Test
	public void classNameMustHaveARealMatchingNoninitialChange() {
		var original = ecore.createEClass();
		original.setName("Child");
		var copy = ecore.createEClass();
		copy.setName("Contact");
		var expression = xbase.createXStringLiteral();
		var other = xbase.createXStringLiteral();
		var direct = new Change(expression, false);
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, original, direct, expression))
			.isSameAs(expression);
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, null, direct, expression))
			.isNull();
		original.setName("Contact");
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, original, direct, expression))
			.isNull();
		original.setName("Child");
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, original, null, expression))
			.isNull();
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, original,
			new Change(expression, true), expression)).isNull();
		assertThat(EdeltaModelOutlineNavigation.classExpression(copy, original, direct, other))
			.isNull();
	}

	@Test
	public void propertyRequiresOneRecordedVisibleChange() {
		var original = ecore.createEReference();
		original.setName("link");
		var copy = ecore.createEReference();
		copy.setName("link");
		var changes = new EdeltaModifiedFeatures();
		var lower = xbase.createXStringLiteral();
		var upper = xbase.createXStringLiteral();
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, null, changes)).isNull();
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes)).isNull();
		copy.setLowerBound(1);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes)).isNull();
		changes.recordChange(copy, EcorePackage.Literals.ETYPED_ELEMENT__LOWER_BOUND, lower);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes))
			.isSameAs(lower);
		copy.setUpperBound(3);
		changes.recordChange(copy, EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND, upper);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes)).isNull();
	}

	@Test
	public void attributeShapeAndInitialChangesAreHandledConservatively() {
		var original = ecore.createEAttribute();
		original.setName("name");
		var copy = ecore.createEAttribute();
		copy.setName("name");
		var changes = new EdeltaModifiedFeatures();
		var expression = xbase.createXStringLiteral();
		copy.setUpperBound(2);
		changes.recordInitial(copy, expression);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes)).isNull();
		changes.recordChange(copy, EcorePackage.Literals.ETYPED_ELEMENT__UPPER_BOUND, expression);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes))
			.isSameAs(expression);
		original.setUpperBound(2);
		assertThat(EdeltaModelOutlineNavigation.featureExpression(copy, original, changes)).isNull();
	}

	@Test
	public void sourceRegionRequiresTheCurrentProgramAndUsableRegion() {
		var program = EdeltaFactory.eINSTANCE.createEdeltaProgram();
		var operation = EdeltaFactory.eINSTANCE.createEdeltaModifyEcoreOperation();
		var body = xbase.createXBlockExpression();
		var expression = xbase.createXStringLiteral();
		body.getExpressions().add(expression);
		operation.setBody(body);
		program.getModifyEcoreOperations().add(operation);
		var current = new ResourceImpl(URI.createURI("platform:/resource/current.edelta"));
		current.getContents().add(program);
		var declaration = new TextRegion(1, 4);
		var selected = new TextRegion(10, 6);
		current.getContents().remove(program);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, expression, declaration,
			expr -> selected)).isSameAs(declaration);
		current.getContents().add(program);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, expression, declaration,
			expr -> selected)).isSameAs(selected);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, null, declaration,
			expr -> selected)).isSameAs(declaration);
		var external = xbase.createXStringLiteral();
		new ResourceImpl(URI.createURI("platform:/resource/library.edelta"))
			.getContents().add(external);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, external, declaration,
			expr -> selected)).isSameAs(declaration);
		var unrelatedRoot = xbase.createXStringLiteral();
		current.getContents().add(unrelatedRoot);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, unrelatedRoot, declaration,
			expr -> selected)).isSameAs(declaration);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, expression, declaration,
			expr -> null)).isSameAs(declaration);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, expression, declaration,
			expr -> new TextRegion(10, 0))).isSameAs(declaration);
		var secondary = xbase.createXStringLiteral();
		body.getExpressions().add(secondary);
		var secondRegion = new TextRegion(20, 8);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, external, secondary,
			declaration, expr -> expr == secondary ? secondRegion : selected))
			.isSameAs(secondRegion);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, expression, secondary,
			declaration, expr -> expr == expression ? selected : secondRegion))
			.isSameAs(selected);
		assertThat(EdeltaModelOutlineNavigation.sourceRegion(program, external, unrelatedRoot,
			declaration, expr -> selected)).isSameAs(declaration);
	}
}

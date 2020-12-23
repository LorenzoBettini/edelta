package edelta.refactorings.lib.helper.tests;

import static edelta.lib.EdeltaLibrary.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.emf.ecore.EcorePackage.Literals.*;

import org.eclipse.emf.ecore.EClass;
import org.junit.Test;

import edelta.refactorings.lib.helper.EdeltaFeatureDifferenceFinder;

public class EdeltaFeatureDifferenceFinderTest {

	private static EClass aType1 = newEClass("aType1");
	private static EClass aType2 = newEClass("aType2");

	@Test
	public void whenTwoFeaturesAreEqual() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var differenceFinder = new EdeltaFeatureDifferenceFinder();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesHaveDifferentFeatures() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isFalse();
	}

	@Test
	public void whenTwoFeaturesAreEqualIgnoringFeature() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		newEClass("C1", c -> addEReference(c, feature1));
		newEClass("C2", c -> addEReference(c, feature2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoring(ESTRUCTURAL_FEATURE__ECONTAINING_CLASS);
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesAreEqualIgnoringFeatures() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r2", ECLASS, f -> f.setLowerBound(1));
		newEClass("C1", c -> addEReference(c, feature1));
		newEClass("C2", c -> addEReference(c, feature2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoring(ESTRUCTURAL_FEATURE__ECONTAINING_CLASS)
			.ignoring(ENAMED_ELEMENT__NAME);
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesAreEqualIgnoringName() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r2", ECLASS, f -> f.setLowerBound(1));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoringName();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesAreEqualIgnoringContainingClass() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		newEClass("C1", c -> addEReference(c, feature1));
		newEClass("C2", c -> addEReference(c, feature2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoringContainingClass();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesAreEqualIgnoringType() {
		var feature1 = newEReference("r1", aType1, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", aType2, f -> f.setLowerBound(1));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoringType();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isTrue();
	}

	@Test
	public void whenTwoFeaturesHaveDifferentLowerBoundWithDifferenceDetails() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(2));
		newEClass("C1", c -> addEReference(c, feature1));
		newEClass("C2", c -> addEReference(c, feature2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isFalse();
		String details = differenceFinder.getDifferenceDetails();
		assertThat(details)
			.isEqualTo("ecore.ETypedElement.lowerBound:\n"
					+ "  C1.r1: 1\n"
					+ "  C2.r1: 2\n"
					+ "");
	}

	@Test
	public void whenTwoFeaturesHaveDifferentNameAndContainingClassWithDifferenceDetails() {
		var feature1 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", ECLASS, f -> f.setLowerBound(1));
		newEClass("C1", c -> addEReference(c, feature1));
		newEClass("C2", c -> addEReference(c, feature2));
		var differenceFinder = new EdeltaFeatureDifferenceFinder();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isFalse();
		String details = differenceFinder.getDifferenceDetails();
		assertThat(details)
			.isEqualTo("ecore.ENamedElement.name:\n"
				+ "  C1: C1\n"
				+ "  C2: C2\n"
				+ "ecore.EStructuralFeature.eContainingClass:\n"
				+ "  C1.r1: C1\n"
				+ "  C2.r1: C2\n"
				+ "");
	}

	@Test
	public void whenTwoFeaturesHaveDifferentKindWithDifferenceDetails() {
		var feature1 = newEReference("r1", aType1, f -> f.setLowerBound(1));
		var feature2 = newEAttribute("r1", ESTRING, f -> f.setLowerBound(1));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
			.ignoringName();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isFalse();
		String details = differenceFinder.getDifferenceDetails();
		assertThat(details)
			.isEqualTo("different kinds:\n"
					+ "  r1: ecore.EReference\n"
					+ "  r1: ecore.EAttribute\n"
					+ "");
	}

	@Test
	public void whenTwoFeaturesHaveDifferentTypeWithDifferenceDetails() {
		var feature1 = newEReference("r1", aType1, f -> f.setLowerBound(1));
		var feature2 = newEReference("r1", aType2, f -> f.setLowerBound(1));
		var differenceFinder = new EdeltaFeatureDifferenceFinder()
				.ignoringName();
		assertThat(differenceFinder.equals(feature1, feature2))
			.isFalse();
		String details = differenceFinder.getDifferenceDetails();
		assertThat(details)
			.isEqualTo("ecore.ENamedElement.name:\n"
					+ "  aType1: aType1\n"
					+ "  aType2: aType2\n"
					+ "ecore.ETypedElement.eType:\n"
					+ "  r1: aType1\n"
					+ "  r1: aType2\n"
					+ "");
	}

}

package edelta.refactorings.lib.tests;

import com.google.common.base.Objects;
import edelta.lib.AbstractEdelta;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;

import static edelta.lib.EdeltaLibrary.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.*;

import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.Before;
import org.junit.Test;

public class EdeltaBadSmellsFinderTest extends AbstractTest {
	private EdeltaBadSmellsFinder finder;

	@Before
	public void setup() {
		finder = new EdeltaBadSmellsFinder();
	}

	@Test
	public void test_ConstructorArgument() {
		finder = new EdeltaBadSmellsFinder(new AbstractEdelta() {
		});
		assertThat(finder).isNotNull();
	}

	@Test
	public void test_findDuplicateFeatures_whenNoDuplicates() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "C1", c -> {
				addNewEAttribute(c, "a1", stringDataType);
			});
			addNewEClass(pack, "C2", c -> {
				addNewEAttribute(c, "a1", intDataType);
			});
		});
		assertThat(finder.findDuplicateFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicateFeatures_withDuplicates() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "C1", c -> {
				addNewEAttribute(c, "a1", stringDataType);
			});
			addNewEClass(pack, "C2", c -> {
				addNewEAttribute(c, "a1", stringDataType);
			});
		});
		final var result = finder.findDuplicateFeatures(p);
		assertThat(result)
			.containsExactly(entry(
					findEStructuralFeature(p, "C1", "a1"),
					asList(
							findEStructuralFeature(p, "C1", "a1"),
							findEStructuralFeature(p, "C2", "a1")
					)));
	}

	@Test
	public void test_findDuplicateFeatures_withDifferingAttributesByLowerBound() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "C1", c -> {
				addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(1);
				});
			});
			addNewEClass(pack, "C2", c -> {
				addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(2);
				});
			});
		});
		assertThat(finder.findDuplicateFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicateFeatures_withDifferingContainment() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "C1", c -> {
				addNewContainmentEReference(c, "r1", eClassReference);
			});
			addNewEClass(pack, "C2", c -> {
				addNewEReference(c, "r1", eClassReference);
			});
		});
		assertThat(finder.findDuplicateFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicateFeatures_withCustomEqualityPredicate() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "C1", c -> {
				addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(1);
				});
			});
			addNewEClass(pack, "C2", c -> {
				addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(2);
				});
			});
		});
		// only check name and type, thus the different lowerBound is ignored
		// during the comparison.
		final var result = finder.findDuplicateFeaturesCustom(p,
			(f1, f2) -> 
				Objects.equal(f1.getName(), f2.getName())
						&& Objects.equal(f1.getEType(), f2.getEType()));
		assertThat(result)
			.containsExactly(entry(
				findEStructuralFeature(p, "C1", "a1"),
				asList(
					findEStructuralFeature(p, "C1", "a1"),
					findEStructuralFeature(p, "C2", "a1")
				)));
	}

	@Test
	public void test_findRedundantContainers() {
		final EPackage p = createEPackage("p", pack -> {
			EClass containedWithRedundant = addNewEClass(pack, "ContainedWithRedundant");
			EClass containedWithOpposite = addNewEClass(pack, "ContainedWithOpposite");
			EClass containedWithContained = addNewEClass(pack, "ContainedWithContained");
			EClass containedWithOptional = addNewEClass(pack, "ContainedWithOptional");
			EClass anotherClass = addNewEClass(pack, "AnotherClass");
			EClass containedWithUnrelated = addNewEClass(pack, "Unrelated");
			EClass container = addNewEClass(pack, "Container", c -> {
				addNewContainmentEReference(c, "containedWithRedundant", containedWithRedundant);
				addNewContainmentEReference(c, "containedWithUnrelated", containedWithUnrelated);
				addNewContainmentEReference(c, "containedWithOpposite", containedWithOpposite);
				addNewContainmentEReference(c, "containedWithOptional", containedWithOptional);
			});
			addNewEReference(containedWithRedundant, "redundant", container, r -> {
				r.setLowerBound(1);
			});
			addNewEReference(containedWithUnrelated, "unrelated", anotherClass, r -> {
				r.setLowerBound(1);
			});
			addNewEReference(containedWithOpposite, "correctWithOpposite", container, r -> {
				r.setLowerBound(1);
				r.setEOpposite(last(container.getEReferences()));
			});
			// this is correct since it's another containment relation
			addNewContainmentEReference(containedWithContained, "correctWithContainment", container,
				r -> r.setLowerBound(1));
			// this is correct since it's not required
			addNewEReference(containedWithOptional, "correctNotRequired", container);
		});
		// we expect the pair
		// redundant -> containedWithRedundant
		assertThat(finder.findRedundantContainers(p))
			.containsExactly(Pair.of(
				findEReference(p, "ContainedWithRedundant", "redundant"),
				findEReference(p, "Container", "containedWithRedundant")
			));
	}

	@Test
	public void test_findDeadClassifiers() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "Unused1");
			EClass used1 = addNewEClass(pack, "Used1");
			EClass used2 = addNewEClass(pack, "Used2");
			addNewEClass(pack, "Unused2", c -> {
				addNewContainmentEReference(c, "used1", used1);
				addNewEReference(c, "used2", used2);
			});
		});
		assertThat(finder.findDeadClassifiers(p))
			.containsExactly(findEClass(p, "Unused1"));
	}

	@Test
	public void test_hasNoReferenceInThisPackage() {
		final EPackage otherPackage = createEPackage("otherPackage");
		final EClass used1 = addNewEClass(otherPackage, "Used1");
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "HasNoReferenceInThisPackage", c -> {
				// has a reference to a class in a different package
				addNewEReference(c, "used1", used1);
			});
		});
		assertThat(finder.hasNoReferenceInThisPackage(head(EClasses(p))))
				.isTrue();
	}

	@Test
	public void test_findClassificationByHierarchy() {
		final EPackage p = createEPackage("p", pack -> {
			final EClass base = addNewEClass(pack, "Base");
			addNewSubclass(base, "Derived1");
			addNewSubclass(base, "Derived2");
			// not in result because has features
			addNewSubclass(base, "DerivedOK", c -> {
				addNewEAttribute(c, "anAttribute", stringDataType);
			});
			// not in result because it's referred by aRef
			final EClass referenced = addNewSubclass(base, "DerivedOK2");
			final EClass another = addNewEClass(pack, "Another", c -> {
				addNewEReference(c, "aRef", referenced);
			});
			// not in result because has several superclasses
			addNewSubclass(base, "DerivedOK3", c -> {
				c.getESuperTypes().add(another);
			});
		});
		assertThat(finder.findClassificationByHierarchy(p)).containsExactly(
				entry(findEClass(p, "Base"),
					asList(
						findEClass(p, "Derived1"),
						findEClass(p, "Derived2")
					)));
	}

	@Test
	public void test_findClassificationByHierarchy_withOneSubclass() {
		final EPackage p = createEPackage("p", pack -> {
			addNewEClass(pack, "Base", c -> {
				addNewSubclass(c, "Derived1");
			});
		});
		assertThat(finder.findClassificationByHierarchy(p)).isEmpty();
	}

	@Test
	public void test_findConcreteAbstractMetaclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass base = addNewEClass(pack, "ConcreteAbstractMetaclass");
			EClass other = addNewAbstractEClass(pack, "CorrectAbstractMetaclass");
			EClass referred = addNewEClass(pack, "NonBaseClass");
			addNewSubclass(base, "Derived1");
			addNewSubclass(other, "Derived2");
			addNewEClass(pack, "Another", c -> {
				addNewEReference(c, "aRef", referred);
			});
		});
		assertThat(finder.findConcreteAbstractMetaclasses(p))
			.containsExactly(findEClass(p, "ConcreteAbstractMetaclass"));
	}

	@Test
	public void test_findAbstractConcreteMetaclasses() {
		final EPackage p = createEPackage("p", pack -> {
			addNewAbstractEClass(pack, "AbstractConcreteMetaclass");
			addNewAbstractEClass(pack, "AbstractMetaclass", c -> {
				addNewSubclass(c, "Derived1");
			});
		});
		assertThat(finder.findAbstractConcreteMetaclasses(p))
			.containsExactly(findEClass(p, "AbstractConcreteMetaclass"));
	}

	@Test
	public void test_findAbstractSubclassesOfConcreteSuperclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass abstractSuperclass = addNewAbstractEClass(pack, "AbstractSuperclass");
			EClass concreteSuperclass1 = addNewEClass(pack, "ConcreteSuperclass1");
			EClass concreteSuperclass2 = addNewEClass(pack, "ConcreteSuperclass2");
			addNewAbstractEClass(pack, "WithoutSmell", c -> {
				c.getESuperTypes().addAll(
					asList(concreteSuperclass1, abstractSuperclass));
			});
			addNewAbstractEClass(pack, "WithSmell", c -> {
				c.getESuperTypes().addAll(
					asList(concreteSuperclass1, concreteSuperclass2));
			});
		});
		assertThat(finder.findAbstractSubclassesOfConcreteSuperclasses(p))
			.containsOnly(findEClass(p, "WithSmell"));
	}

	@Test
	public void test_directSubclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass superclass = addNewEClass(pack, "ASuperclass");
			EClass subclass1 = addNewSubclass(superclass, "ASubclass1");
			addNewSubclass(subclass1, "ASubclass1Subclass");
			addNewSubclass(superclass, "ASubclass2");
		});
		assertThat(
			map(
				finder.directSubclasses(findEClass(p, "ASuperclass")),
				ENamedElement::getName))
			.containsExactlyInAnyOrder("ASubclass1", "ASubclass2");
		assertThat(
			map(
				finder.directSubclasses(findEClass(p, "ASubclass1")),
				ENamedElement::getName))
			.containsExactlyInAnyOrder("ASubclass1Subclass");
		assertThat(finder.directSubclasses(findEClass(p, "ASubclass1Subclass"))).isEmpty();
	}

	@Test
	public void test_findDuplicateFeaturesInSubclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass superclassWithDuplicatesInSubclasses = addNewEClass(pack,
					"SuperClassWithDuplicatesInSubclasses");
			addNewSubclass(superclassWithDuplicatesInSubclasses, "C1", c -> {
				addNewEAttribute(c, "A1", stringDataType);
			});
			addNewSubclass(superclassWithDuplicatesInSubclasses, "C2", c -> {
				addNewEAttribute(c, "A1", stringDataType);
			});
			EClass superclassWithoutDuplicatesInAllSubclasses = addNewEClass(pack,
					"SuperClassWithoutDuplicatesInAllSubclasses");
			addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D1", c -> {
				addNewEAttribute(c, "A1", stringDataType);
			});
			addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D2", c -> {
				addNewEAttribute(c, "A1", stringDataType);
			});
			// all subclasses must have the duplicate
			addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D3", c -> {
				addNewEAttribute(c, "A1", intDataType); // this is not a duplicate
			});
		});
		assertThat(finder.findDuplicateFeaturesInSubclasses(p))
			.containsExactly(entry(
				findEClass(p, "SuperClassWithDuplicatesInSubclasses"),
				Map.of(
					findEStructuralFeature(p, "C1", "A1"),
					asList(
						findEStructuralFeature(p, "C1", "A1"),
						findEStructuralFeature(p, "C2", "A1")
					)
				)
			));
	}
}
package edelta.refactorings.lib.tests;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.lastOrNull;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.map;

import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.impl.ResourceImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.xbase.lib.Pair;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;

import edelta.lib.EdeltaDefaultRuntime;
import edelta.lib.EdeltaModelManager;
import edelta.refactorings.lib.EdeltaBadSmellsFinder;

public class EdeltaBadSmellsFinderTest extends AbstractEdeltaRefactoringsLibTest {
	private EdeltaBadSmellsFinder finder;

	@Before
	public void setup() throws Exception {
		finder = new EdeltaBadSmellsFinder(new EdeltaDefaultRuntime(new EdeltaModelManager()));
		assertThat(finder).isNotNull();
		finder.performSanityChecks();
	}

	@Test
	public void test_findDuplicatedFeatures_whenNoDuplicates() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType);
			});
			stdLib.addNewEClass(pack, "C2", c -> {
				stdLib.addNewEAttribute(c, "a1", intDataType);
			});
		});
		assertThat(finder.findDuplicatedFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicatedFeatures_withDuplicates() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType);
			});
			stdLib.addNewEClass(pack, "C2", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType);
			});
		});
		final var result = finder.findDuplicatedFeatures(p);
		assertThat(result)
			.containsExactly(entry(
					findEStructuralFeature(p, "C1", "a1"),
					asList(
							findEStructuralFeature(p, "C1", "a1"),
							findEStructuralFeature(p, "C2", "a1")
					)));
	}

	@Test
	public void test_findDuplicatedFeatures_withDifferingAttributesByLowerBound() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(1);
				});
			});
			stdLib.addNewEClass(pack, "C2", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(2);
				});
			});
		});
		assertThat(finder.findDuplicatedFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicatedFeatures_withDifferingContainment() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1", c -> {
				stdLib.addNewContainmentEReference(c, "r1", eClassReference);
			});
			stdLib.addNewEClass(pack, "C2", c -> {
				stdLib.addNewEReference(c, "r1", eClassReference);
			});
		});
		assertThat(finder.findDuplicatedFeatures(p)).isEmpty();
	}

	@Test
	public void test_findDuplicatedFeatures_withCustomEqualityPredicate() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "C1", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(1);
				});
			});
			stdLib.addNewEClass(pack, "C2", c -> {
				stdLib.addNewEAttribute(c, "a1", stringDataType, a -> {
					a.setLowerBound(2);
				});
			});
		});
		// only check name and type, thus the different lowerBound is ignored
		// during the comparison.
		final var result = finder.findDuplicatedFeaturesCustom(p,
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
			EClass containedWithRedundant = stdLib.addNewEClass(pack, "ContainedWithRedundant");
			EClass containedWithOpposite = stdLib.addNewEClass(pack, "ContainedWithOpposite");
			EClass containedWithContained = stdLib.addNewEClass(pack, "ContainedWithContained");
			EClass containedWithOptional = stdLib.addNewEClass(pack, "ContainedWithOptional");
			EClass anotherClass = stdLib.addNewEClass(pack, "AnotherClass");
			EClass containedWithUnrelated = stdLib.addNewEClass(pack, "Unrelated");
			EClass container = stdLib.addNewEClass(pack, "Container", c -> {
				stdLib.addNewContainmentEReference(c, "containedWithRedundant", containedWithRedundant);
				stdLib.addNewContainmentEReference(c, "containedWithUnrelated", containedWithUnrelated);
				stdLib.addNewContainmentEReference(c, "containedWithOpposite", containedWithOpposite);
				stdLib.addNewContainmentEReference(c, "containedWithOptional", containedWithOptional);
			});
			stdLib.addNewEReference(containedWithRedundant, "redundant", container, r -> {
				r.setLowerBound(1);
			});
			stdLib.addNewEReference(containedWithUnrelated, "unrelated", anotherClass, r -> {
				r.setLowerBound(1);
			});
			stdLib.addNewEReference(containedWithOpposite, "correctWithOpposite", container, r -> {
				r.setLowerBound(1);
				r.setEOpposite(lastOrNull(container.getEReferences()));
			});
			// this is correct since it's another containment relation
			stdLib.addNewContainmentEReference(containedWithContained, "correctWithContainment", container,
				r -> r.setLowerBound(1));
			// this is correct since it's not required
			stdLib.addNewEReference(containedWithOptional, "correctNotRequired", container);
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
		var p1 = createEPackage("p1", pack -> {
			stdLib.addNewEClass(pack, "Unused1");
			EClass used1 = stdLib.addNewEClass(pack, "Used1");
			EClass used2 = stdLib.addNewEClass(pack, "Used2");
			stdLib.addNewEClass(pack, "Unused2", c -> {
				stdLib.addNewContainmentEReference(c, "used1", used1);
				stdLib.addNewEReference(c, "used2", used2);
			});
		});
		assertThat(finder.findDeadClassifiers(p1))
			.containsExactly(findEClass(p1, "Unused1"));
		// create a resource set with cross reference
		var p2 = createEPackage("p2", pack -> {
			stdLib.addNewEClass(pack, "UsesUnused1", c -> {
				stdLib.addNewEReference(c, "unused1FromP1", (EClass) p1.getEClassifier("Unused1"));
			});
		});
		var resourceSet = new ResourceSetImpl();
		var p1Resource = new ResourceImpl();
		p1Resource.getContents().add(p1);
		resourceSet.getResources().add(p1Resource);
		var p2Resource = new ResourceImpl();
		p2Resource.getContents().add(p2);
		resourceSet.getResources().add(p2Resource);
		// now Unused1 is referenced by a class in another package
		// in the same resource set
		assertThat(finder.findDeadClassifiers(p1))
			.isEmpty();
	}

	@Test
	public void test_doesNotReferToClasses() {
		var otherPackage = createEPackage("otherPackage");
		var used1 = stdLib.addNewEClass(otherPackage, "Used1");
		var p = createEPackage("p", pack -> {
			stdLib.addNewEClass(pack, "HasNoReferenceInThisPackage", c -> {
				// has a reference to a class in a different package
				stdLib.addNewEReference(c, "used1", used1);
			});
		});
		assertThat(finder.doesNotReferToClasses(head(EClasses(p))))
			.isFalse();
		var hasNoReference = stdLib.addNewEClass(p, "HasNoReference");
		assertThat(finder.doesNotReferToClasses(hasNoReference))
			.isTrue();
		var onlyReferToSelf = stdLib.addNewEClass(p, "OnlyReferToSelf", c -> {
			// has a reference to self
			stdLib.addNewEReference(c, "mySelf", c);
		});
		// self references are considered
		assertThat(finder.doesNotReferToClasses(onlyReferToSelf))
			.isFalse();
		var withSuperClass = stdLib.addNewSubclass(used1, "WithSuperClass");
		// superclasses are considered
		assertThat(finder.doesNotReferToClasses(withSuperClass))
			.isFalse();
		var onlyWithAtttributes = stdLib.addNewEClass(p, "OnlyWithAttributes", c -> {
			stdLib.addNewEAttribute(c, "name", stringDataType);
		});
		// attributes, i.e., EDataType, are not considered
		assertThat(finder.doesNotReferToClasses(onlyWithAtttributes))
			.isTrue();
	}

	@Test
	public void test_isNotReferredByClassifiers() {
		var p1 = createEPackage("p1");
		var referenced = stdLib.addNewEClass(p1, "Referenced");
		var baseClass = stdLib.addNewEClass(p1, "BaseClass");
		var notReferenced = stdLib.addNewEClass(p1, "NotReferenced");
		stdLib.addNewEClass(p1, "Referring", c -> {
			stdLib.addNewEReference(c, "aRef", referenced);
		});
		stdLib.addNewSubclass(baseClass, "DerivedClass");
		assertThat(finder.isNotReferredByClassifiers(notReferenced))
			.isTrue();
		assertThat(finder.isNotReferredByClassifiers(referenced))
			.isFalse();
		// if there's a derived class, the base class is referenced
		assertThat(finder.isNotReferredByClassifiers(baseClass))
			.isFalse();
		var selfReferenced = stdLib.addNewEClass(p1, "SelfReferenced", c -> {
			stdLib.addNewEReference(c, "mySelf", c);
		});
		// self references are considered
		assertThat(finder.isNotReferredByClassifiers(selfReferenced))
			.isFalse();
		// create a resource set with cross reference
		var p2 = createEPackage("p2", pack -> {
			stdLib.addNewEClass(pack, "UsesNotReferencedInP1", c -> {
				stdLib.addNewEReference(c, "usesNotReferencedInP1", (EClass) p1.getEClassifier("NotReferenced"));
			});
		});
		var resourceSet = new ResourceSetImpl();
		var p1Resource = new ResourceImpl();
		p1Resource.getContents().add(p1);
		resourceSet.getResources().add(p1Resource);
		var p2Resource = new ResourceImpl();
		p2Resource.getContents().add(p2);
		resourceSet.getResources().add(p2Resource);
		// now it is referenced by a class in another package
		// in the same resource set
		assertThat(finder.isNotReferredByClassifiers(notReferenced))
			.isFalse();
	}

	@Test
	public void test_findClassificationByHierarchy() {
		final EPackage p = createEPackage("p", pack -> {
			final EClass base = stdLib.addNewEClass(pack, "Base");
			stdLib.addNewSubclass(base, "Derived1");
			stdLib.addNewSubclass(base, "Derived2");
			// not in result because has features
			stdLib.addNewSubclass(base, "DerivedOK", c -> {
				stdLib.addNewEAttribute(c, "anAttribute", stringDataType);
			});
			// not in result because it's referred by aRef
			final EClass referenced = stdLib.addNewSubclass(base, "DerivedOK2");
			final EClass another = stdLib.addNewEClass(pack, "Another", c -> {
				stdLib.addNewEReference(c, "aRef", referenced);
			});
			// not in result because has several superclasses
			stdLib.addNewSubclass(base, "DerivedOK3", c -> {
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
			stdLib.addNewEClass(pack, "Base", c -> {
				stdLib.addNewSubclass(c, "Derived1");
			});
		});
		assertThat(finder.findClassificationByHierarchy(p)).isEmpty();
	}

	@Test
	public void test_findConcreteAbstractMetaclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass base = stdLib.addNewEClass(pack, "ConcreteAbstractMetaclass");
			EClass other = stdLib.addNewAbstractEClass(pack, "CorrectAbstractMetaclass");
			EClass referred = stdLib.addNewEClass(pack, "NonBaseClass");
			stdLib.addNewSubclass(base, "Derived1");
			stdLib.addNewSubclass(other, "Derived2");
			stdLib.addNewEClass(pack, "Another", c -> {
				stdLib.addNewEReference(c, "aRef", referred);
			});
		});
		assertThat(finder.findConcreteAbstractMetaclasses(p))
			.containsExactly(findEClass(p, "ConcreteAbstractMetaclass"));
	}

	@Test
	public void test_findAbstractConcreteMetaclasses() {
		final EPackage p = createEPackage("p", pack -> {
			stdLib.addNewAbstractEClass(pack, "AbstractConcreteMetaclass");
			stdLib.addNewAbstractEClass(pack, "AbstractMetaclass", c -> {
				stdLib.addNewSubclass(c, "Derived1");
			});
		});
		assertThat(finder.findAbstractConcreteMetaclasses(p))
			.containsExactly(findEClass(p, "AbstractConcreteMetaclass"));
	}

	@Test
	public void test_findAbstractSubclassesOfConcreteSuperclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass abstractSuperclass = stdLib.addNewAbstractEClass(pack, "AbstractSuperclass");
			EClass concreteSuperclass1 = stdLib.addNewEClass(pack, "ConcreteSuperclass1");
			EClass concreteSuperclass2 = stdLib.addNewEClass(pack, "ConcreteSuperclass2");
			stdLib.addNewAbstractEClass(pack, "WithoutSmell", c -> {
				c.getESuperTypes().addAll(
					asList(concreteSuperclass1, abstractSuperclass));
			});
			stdLib.addNewAbstractEClass(pack, "WithSmell", c -> {
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
			EClass superclass = stdLib.addNewEClass(pack, "ASuperclass");
			EClass subclass1 = stdLib.addNewSubclass(superclass, "ASubclass1");
			stdLib.addNewSubclass(subclass1, "ASubclass1Subclass");
			stdLib.addNewSubclass(superclass, "ASubclass2");
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
	public void test_directSubclassesInResourceSet() {
		final EPackage p1 = createEPackage("p1");
		var superclass = stdLib.addNewEClass(p1, "ASuperclass");
		final EPackage p2 = createEPackage("p2");
		var subclass1 = stdLib.addNewEClass(p2, "ASubclass1");
		stdLib.addESuperType(subclass1, superclass);
		stdLib.addNewSubclass(superclass, "ASubclass2");
		var resourceSet = new ResourceSetImpl();
		var p1Resource = new ResourceImpl();
		p1Resource.getContents().add(p1);
		resourceSet.getResources().add(p1Resource);
		var p2Resource = new ResourceImpl();
		p2Resource.getContents().add(p2);
		resourceSet.getResources().add(p2Resource);
		assertThat(
			map(
				finder.directSubclasses(superclass),
				ENamedElement::getName))
			.containsExactlyInAnyOrder("ASubclass1", "ASubclass2");
	}

	@Test
	public void test_findDuplicatedFeaturesInSubclasses() {
		final EPackage p = createEPackage("p", pack -> {
			EClass superclassWithDuplicatesInSubclasses = stdLib.addNewEClass(pack,
					"SuperClassWithDuplicatesInSubclasses");
			stdLib.addNewSubclass(superclassWithDuplicatesInSubclasses, "C1", c -> {
				stdLib.addNewEAttribute(c, "A1", stringDataType);
			});
			stdLib.addNewSubclass(superclassWithDuplicatesInSubclasses, "C2", c -> {
				stdLib.addNewEAttribute(c, "A1", stringDataType);
			});
			EClass superclassWithoutDuplicatesInAllSubclasses = stdLib.addNewEClass(pack,
					"SuperClassWithoutDuplicatesInAllSubclasses");
			stdLib.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D1", c -> {
				stdLib.addNewEAttribute(c, "A1", stringDataType);
			});
			stdLib.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D2", c -> {
				stdLib.addNewEAttribute(c, "A1", stringDataType);
			});
			// all subclasses must have the duplicate
			stdLib.addNewSubclass(superclassWithoutDuplicatesInAllSubclasses, "D3", c -> {
				stdLib.addNewEAttribute(c, "A1", intDataType); // this is not a duplicate
			});
		});
		assertThat(finder.findDuplicatedFeaturesInSubclasses(p))
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

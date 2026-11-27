package edelta.lib.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import edelta.lib.EdeltaModelManager;
import edelta.lib.EdeltaModelMigrator;
import edelta.lib.EdeltaModelMigrator.CopyStage;
import edelta.lib.EdeltaModelMigrator.MigrationObserver;
import edelta.lib.EdeltaStageValueComparator;
import edelta.lib.EdeltaStageValueComparator.Mappings;

/**
 * Small in-memory model and observer for test-only operation-boundary probes.
 * Every recorded map belongs to one synchronous copy, including backup copies.
 * No source model resource is loaded from or saved to disk.
 *
 * @author Lorenzo Bettini
 */
final class StageMigrationFixture {
	record Stage(Map<EObject, EObject> objects, Map<EObject, EObject> ecores,
			List<Resource> sources, List<Resource> targets) {
		EdeltaStageValueComparator.Result compare(EObject source, EAttribute feature) {
			return EdeltaStageValueComparator.compare(source, feature, objects.get(source),
				(org.eclipse.emf.ecore.EStructuralFeature) ecores.get(feature),
				new Mappings(objects, ecores));
		}
	}

	final EdeltaModelManager originals = new EdeltaModelManager();
	final EPackage epackage = EcoreFactory.eINSTANCE.createEPackage();
	final EClass node = EcoreFactory.eINSTANCE.createEClass();
	final EAttribute value = EcoreFactory.eINSTANCE.createEAttribute();
	final EAttribute tags = EcoreFactory.eINSTANCE.createEAttribute();
	final EReference link = EcoreFactory.eINSTANCE.createEReference();
	final EObject first;
	final EObject second;
	final List<Stage> stages = new ArrayList<>();
	final List<CopyStage> completed = new ArrayList<>();
	final List<Boolean> invalidations = new ArrayList<>();
	final EdeltaModelMigrator migrator;

	StageMigrationFixture() {
		this(false);
	}

	StageMigrationFixture(boolean withTag) {
		epackage.setName("probe");
		epackage.setNsPrefix("probe");
		epackage.setNsURI("urn:edelta:stage-probe");
		node.setName("Node");
		value.setName("value");
		value.setEType(EcorePackage.Literals.ESTRING);
		tags.setName("tags");
		tags.setEType(EcorePackage.Literals.ESTRING);
		tags.setUpperBound(-1);
		link.setName("link");
		link.setEType(node);
		node.getEStructuralFeatures().addAll(List.of(value, tags, link));
		epackage.getEClassifiers().add(node);
		var resources = new ResourceSetImpl();
		var ecoreResource = new XMIResourceImpl(URI.createURI("memory:/probe.ecore"));
		resources.getResources().add(ecoreResource);
		ecoreResource.getContents().add(epackage);
		originals.getEcoreResources().add(ecoreResource);
		first = EcoreUtil.create(node);
		second = EcoreUtil.create(node);
		first.eSet(value, "hello");
		if (withTag) first.eSet(tags, List.of("tag"));
		second.eSet(value, "HELLO");
		first.eSet(link, second);
		for (var index = 0; index < 2; index++) {
			var model = new XMIResourceImpl(URI.createURI("memory:/probe" + index + ".xmi"));
			resources.getResources().add(model);
			model.getContents().add(index == 0 ? first : second);
			originals.getModelResources().add(model);
		}
		migrator = new EdeltaModelMigrator(originals) {
			{
				setMigrationObserver(new MigrationObserver() {
					@Override public void copied(CopyStage stage) {
						stages.add(new Stage(stage.objectCopies(), stage.ecoreCopies(),
							stage.sources(), stage.targets()));
					}
					@Override public void completed(CopyStage stage) {
						completed.add(stage);
					}
					@Override public void lineageInvalidated() { invalidations.add(true); }
				});
			}
		};
	}

	EAttribute evolvedValue() {
		return (EAttribute) migrator.getMigrated(value);
	}

	EReference evolvedLink() {
		return (EReference) migrator.getMigrated(link);
	}
}

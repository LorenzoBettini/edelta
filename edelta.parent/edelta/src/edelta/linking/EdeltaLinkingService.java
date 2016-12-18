package edelta.linking;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EPackage.Registry;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.conversion.IValueConverterService;
import org.eclipse.xtext.conversion.ValueConverterException;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.IllegalNodeException;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.xtext.XtextLinkingService;

import com.google.inject.Inject;

import edelta.edelta.EdeltaPackage;
import edelta.edelta.EdeltaProgram;

/**
 * Custom linking service to load {@link EPackage} from the specified
 * text in the program.
 * 
 * Adapted from {@link XtextLinkingService}.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaLinkingService extends DefaultLinkingService {

	private static final Logger log = Logger.getLogger(EdeltaLinkingService.class);

	@Inject
	private IValueConverterService valueConverterService;

	@Override
	public List<EObject> getLinkedObjects(EObject context, EReference ref, INode node) throws IllegalNodeException {
		List<EObject> linkedObjects = super.getLinkedObjects(context, ref, node);
		if (!linkedObjects.isEmpty()) {
			return linkedObjects;
		}
		if (ref == EdeltaPackage.eINSTANCE.getEdeltaProgram_Metamodels()) {
			return getPackage((EdeltaProgram)context, (ILeafNode) node);
		}
		return linkedObjects;
	}

	private List<EObject> getPackage(EdeltaProgram context, ILeafNode text) {
		String nsUri = getMetamodelNsURI(text);
		if (nsUri == null)
			return Collections.emptyList();
		EPackage pack = loadEPackage(nsUri, context.eResource().getResourceSet());
		if (pack != null)
			return Collections.<EObject>singletonList(pack);
		return Collections.emptyList();
	}

	private String getMetamodelNsURI(ILeafNode text) {
		try {
			return (String) valueConverterService.toValue(text.getText(), getLinkingHelper().getRuleNameFrom(text
					.getGrammarElement()), text);
		} catch (ValueConverterException e) {
			log.debug("Exception on leaf '" + text.getText() + "'", e);
			return null;
		}
	}

	private EPackage loadEPackage(String resourceOrNsURI, ResourceSet resourceSet) {
		Registry packageRegistry = resourceSet.getPackageRegistry();
		if (packageRegistry.containsKey(resourceOrNsURI))
			return packageRegistry.getEPackage(resourceOrNsURI);
		URI uri = URI.createURI(resourceOrNsURI);
		try {
			if ("http".equalsIgnoreCase(uri.scheme()))
				return null;
			if (uri.fragment() == null) {
				Resource resource = resourceSet.getResource(uri, true);
				if (resource.getContents().isEmpty())
					return null;
				EPackage result = (EPackage) resource.getContents().get(0);
				return result;
			}
			EPackage result = (EPackage) resourceSet.getEObject(uri, true);
			return result;
		} catch(RuntimeException ex) {
			if (uri.isPlatformResource()) {
				String platformString = uri.toPlatformString(true);
				URI platformPluginURI = URI.createPlatformPluginURI(platformString, true);
				return loadEPackage(platformPluginURI.toString(), resourceSet);
			}
			log.trace("Cannot load package with URI '" + resourceOrNsURI + "'", ex);
			return null;
		}
	}
}

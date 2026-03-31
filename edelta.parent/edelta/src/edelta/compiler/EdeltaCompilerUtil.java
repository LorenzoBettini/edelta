package edelta.compiler;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.workspace.IProjectConfigProvider;

import com.google.inject.Inject;

import edelta.edelta.EdeltaEcoreReferenceExpression;
import edelta.util.EdeltaEcoreReferenceInformationHelper;

/**
 * Utilities for Edelta compiler
 * 
 * @author Lorenzo Bettini
 */
public class EdeltaCompilerUtil {

	@Inject
	private IProjectConfigProvider projectConfigProvider;

	@Inject
	private EdeltaEcoreReferenceInformationHelper ecoreReferenceInformationHelper;

	public String getEPackageNameOrNull(final EPackage e) {
		if (e != null) {
			return e.getName();
		}
		return null;
	}

	public String getStringForEcoreReferenceExpression(final EdeltaEcoreReferenceExpression e) {
		final var reference = e.getArgument();
		if (reference == null) {
			return "null";
		}
		// Note that reference.getEnamedelement() might be null also because
		// during the interpretation we used something like EcoreUtil.delete
		// which sets to null the reference to the deleted ENamedElement
		// see https://github.com/LorenzoBettini/edelta/issues/271
		return buildMethodToCallForEcoreReference(e,
			(name, args) -> {
				final var builder = new StringBuilder();
				builder.append(name);
				builder.append("(");
				builder.append(args.stream()
					.map(it -> "\"" + it + "\"")
					.collect(Collectors.joining(", ")));
				builder.append(")");
				return builder.toString();
			});
	}

	public <T extends Object> T buildMethodToCallForEcoreReference(final EdeltaEcoreReferenceExpression e,
			final BiFunction<String, List<Object>, T> function) {
		final var info =
			ecoreReferenceInformationHelper.getOrComputeInformation(e);
		return function.apply(
			"get" + info.getType(),
			Stream.<Object>of(
				info.getEPackageName(),
				info.getEClassifierName(),
				info.getENamedElementName())
			.takeWhile(Objects::nonNull)
			.toList()
		);
	}

	/**
	 * Returns the relative path of the Resource with respect to its source folder
	 * as determined by
	 * {@link org.eclipse.xtext.workspace.IProjectConfig#findSourceFolderContaining(org.eclipse.emf.common.util.URI)}.
	 * 
	 * @param resource
	 * @return the relative path of the Resource with respect to its source folder,
	 *         or the last segment of the URI if no source folder is found.
	 */
	public String getRelativeSourcePath(Resource resource) {
		var uri = resource.getURI();
		var resourceSet = resource.getResourceSet();
		if (resourceSet != null) {
			var projectConfig = projectConfigProvider.getProjectConfig(resourceSet);
			if (projectConfig != null) {
				var sourceFolder = projectConfig.findSourceFolderContaining(uri);
				if (sourceFolder != null) {
					return uri.deresolve(sourceFolder.getPath()).toString();
				}
			}
		}
		return uri.lastSegment();
	}

}

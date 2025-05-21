package edelta.compiler;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

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
	private EdeltaEcoreReferenceInformationHelper ecoreReferenceInformationHelper;

	public String getEPackageNameOrNull(final EPackage e) {
		if (e != null) {
			return e.getName();
		}
		return null;
	}

	public String getStringForEcoreReferenceExpression(final EdeltaEcoreReferenceExpression e) {
		final var reference = e.getReference();
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

	public String getEcoreversionsRelativePath(Resource resource) {
		var segments = resource.getURI().segments();
		for (int i = 0; i < segments.length; i++) {
			if (segments[i].equals("ecoreversions")) {
				return Stream.of(segments)
					.skip(i + 1L)
					.collect(Collectors.joining("/"));
			}
		}
		return "";
	}
}

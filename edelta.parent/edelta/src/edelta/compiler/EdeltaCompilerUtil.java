package edelta.compiler;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.ecore.EPackage;

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
		if (reference == null || reference.getEnamedelement() == null) {
			return "null";
		}
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
			Stream.of(
				info.getEPackageName(),
				info.getEClassifierName(),
				info.getENamedElementName())
			.filter(Objects::nonNull)
			.collect(toList())
		);
	}
}

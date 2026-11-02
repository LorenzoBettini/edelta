package edelta.jvmmodel;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.xbase.jvmmodel.IJvmModelAssociations;

import com.google.inject.Inject;

import edelta.edelta.EdeltaOperation;
import edelta.edelta.EdeltaProgram;
import edelta.edelta.EdeltaUseAs;

/**
 * Helper methods for the Jvm model.
 * 
 * @author Lorenzo Bettini
 *
 */
public class EdeltaJvmModelHelper {
	@Inject
	private IJvmModelAssociations jvmModelAssociations;

	public JvmGenericType findJvmGenericType(EdeltaProgram program) {
		return firstByType(jvmModelAssociations.getJvmElements(program), JvmGenericType.class);
	}

	public EdeltaProgram findEdeltaProgram(JvmTypeReference typeRef) {
		return firstByType(
				jvmModelAssociations.getSourceElements(typeRef.getType()), EdeltaProgram.class);
	}

	public JvmOperation findJvmOperation(JvmGenericType jvmGenericType, String methodName) {
		return StreamSupport.stream(jvmGenericType.getAllFeatures().spliterator(), false)
			.filter(JvmOperation.class::isInstance)
			.map(JvmOperation.class::cast)
			.filter(it -> Objects.equals(it.getSimpleName(), methodName))
			.findFirst()
			.orElse(null);
	}

	public EdeltaUseAs findEdeltaUseAs(JvmField jvmField) {
		return firstByType(jvmModelAssociations.getSourceElements(jvmField), EdeltaUseAs.class);
	}

	public EdeltaOperation findEdeltaOperation(JvmOperation operation) {
		return jvmModelAssociations.getSourceElements(operation).stream()
			.findFirst()
			.map(EdeltaOperation.class::cast)
			.orElse(null);
	}

	private static <T> T firstByType(final Collection<?> unfiltered, final Class<T> desiredType) {
		return unfiltered.stream()
			.filter(desiredType::isInstance)
			.map(desiredType::cast)
			.findFirst()
			.orElse(null);
	}
}

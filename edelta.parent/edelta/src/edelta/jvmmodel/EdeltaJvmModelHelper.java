package edelta.jvmmodel;

import static com.google.common.collect.Iterables.filter;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.findFirst;
import static org.eclipse.xtext.xbase.lib.IterableExtensions.head;

import java.util.Objects;

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
		return findFirst(
				filter(jvmGenericType.getAllFeatures(), JvmOperation.class),
					it -> Objects.equals(it.getSimpleName(), methodName));
	}

	public EdeltaUseAs findEdeltaUseAs(JvmField jvmField) {
		return firstByType(jvmModelAssociations.getSourceElements(jvmField), EdeltaUseAs.class);
	}

	public EdeltaOperation findEdeltaOperation(JvmOperation operation) {
		return (EdeltaOperation) head(jvmModelAssociations.getSourceElements(operation));
	}

	private static <T> T firstByType(final Iterable<?> unfiltered, final Class<T> desiredType) {
		return head(filter(unfiltered, desiredType));
	}
}

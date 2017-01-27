/*
 * generated by Xtext 2.10.0
 */
package edelta.jvmmodel

import com.google.inject.Inject
import edelta.edelta.EdeltaProgram
import edelta.lib.AbstractEdelta
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import org.eclipse.xtext.common.types.JvmVisibility
import edelta.edelta.EdeltaEcoreCreateEClassExpression
import edelta.compiler.EdeltaCompilerUtil
import org.eclipse.emf.ecore.EClass
import edelta.edelta.EdeltaEcoreCreateEAttributeExpression
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.xtext.xbase.XExpression
import org.eclipse.emf.ecore.ENamedElement

/**
 * <p>Infers a JVM model from the source model.</p> 
 * 
 * <p>The JVM model should contain all elements that would appear in the Java code 
 * which is generated from the source model. Other models link against the JVM model rather than the source model.</p>     
 */
class EdeltaJvmModelInferrer extends AbstractModelInferrer {

	/**
	 * convenience API to build and initialize JVM types and their members.
	 */
	@Inject extension JvmTypesBuilder

	@Inject extension IQualifiedNameProvider

	@Inject extension EdeltaCompilerUtil

	/**
	 * The dispatch method {@code infer} is called for each instance of the
	 * given element's type that is contained in a resource.
	 * 
	 * @param element
	 *            the model to create one or more
	 *            {@link JvmDeclaredType declared
	 *            types} from.
	 * @param acceptor
	 *            each created
	 *            {@link JvmDeclaredType type}
	 *            without a container should be passed to the acceptor in order
	 *            get attached to the current resource. The acceptor's
	 *            {@link IJvmDeclaredTypeAcceptor#accept(org.eclipse.xtext.common.types.JvmDeclaredType)
	 *            accept(..)} method takes the constructed empty type for the
	 *            pre-indexing phase. This one is further initialized in the
	 *            indexing phase using the lambda you pass as the last argument.
	 * @param isPreIndexingPhase
	 *            whether the method is called in a pre-indexing phase, i.e.
	 *            when the global index is not yet fully updated. You must not
	 *            rely on linking using the index if isPreIndexingPhase is
	 *            <code>true</code>.
	 */
	def dispatch void infer(EdeltaProgram program, IJvmDeclaredTypeAcceptor acceptor, boolean isPreIndexingPhase) {
		val className = program.fullyQualifiedName
		acceptor.accept(program.toClass(className)) [
			superTypes += AbstractEdelta.typeRef
			for (o : program.operations) {
				members += o.toMethod(o.name, o.type ?: inferredType) [
					documentation = o.documentation
					for (p : o.params) {
						parameters += p.toParameter(p.name, p.parameterType)
					}
					body = o.body
				]
			}
			if (!program.metamodels.empty) {
				members += program.toMethod("performSanityChecks", Void.TYPE.typeRef) [
					annotations += Override.annotationRef
					exceptions += Exception.typeRef
					// for each reference to a metamodel, we generate a sanity check
					// to make sure that at run-time all the referred Ecores are loaded
					body = '''
						«FOR p : program.metamodels»
						ensureEPackageIsLoaded("«p.name»");
						«ENDFOR»
					'''
				]
			}
			val main = program.main
			// main could be null in an incomplete program, e.g. "metamodel "
			if (main !== null && !main.expressions.empty) {
				members += program.main.toMethod("doExecute", Void.TYPE.typeRef) [
					visibility = JvmVisibility.PROTECTED
					annotations += Override.annotationRef
					exceptions += Exception.typeRef
					body = program.main
				]
				main.expressions.
					filter(EdeltaEcoreCreateEClassExpression).
					filter[body !== null].
					forEach[
						e |
						members += e.toMethodForConsumer(EClass, e.body)
						e.body.expressions.
							filter(EdeltaEcoreCreateEAttributeExpression).
							filter[body !== null].
							forEach[
								ea |
								members += ea.toMethodForConsumer(EAttribute, ea.body)
							]
					]
			}
		]
	}

	def private toMethodForConsumer(XExpression e, Class<? extends ENamedElement> typeForParameter,
			XExpression bodyForExpression) {
		e.toMethod(e.methodName, Void.TYPE.typeRef) [
			parameters += e.toParameter("it", typeRef(typeForParameter))
			body = bodyForExpression
		]
	}
}

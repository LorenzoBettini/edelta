/*
 * generated by Xtext 2.10.0
 */
package edelta.jvmmodel

import com.google.inject.Inject
import edelta.compiler.EdeltaCompilerUtil
import edelta.edelta.EdeltaProgram
import edelta.lib.EdeltaDefaultRuntime
import org.eclipse.emf.ecore.EPackage
import org.eclipse.xtext.common.types.JvmDeclaredType
import org.eclipse.xtext.common.types.JvmVisibility
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.xbase.jvmmodel.AbstractModelInferrer
import org.eclipse.xtext.xbase.jvmmodel.IJvmDeclaredTypeAcceptor
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder
import edelta.lib.EdeltaRuntime

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
			superTypes += EdeltaDefaultRuntime.typeRef
			val useAsClauses = program.useAsClauses
			for (u : useAsClauses) {
				members += u.toField(u.name, u.type) [
					if (u.isExtension)
						annotations += annotationRef(Extension)
				]
			}
			members += program.toConstructor[
				parameters += program.toParameter("other", EdeltaRuntime.typeRef)
				body = '''
				super(other);
				«FOR u : program.useAsClauses»
				«u.name» = new «u.type»(other);
				«ENDFOR»
				'''
			]
			for (o : program.operations) {
				members += o.toMethod(o.name, o.type ?: inferredType) [
					documentation = o.documentation
					for (p : o.params) {
						parameters += p.toParameter(p.name, p.parameterType)
					}
					body = o.body
				]
			}
			for (o : program.modifyEcoreOperations) {
				members += o.toMethod(o.name, Void.TYPE.typeRef) [
					documentation = o.documentation
					parameters += o.toParameter("it", EPackage.typeRef)
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
			if (!program.modifyEcoreOperations.empty) {
				members += program.toMethod("doExecute", Void.TYPE.typeRef) [
					visibility = JvmVisibility.PROTECTED
					annotations += Override.annotationRef
					exceptions += Exception.typeRef
					body = '''
						«FOR o : program.modifyEcoreOperations»
						«o.name»(getEPackage("«o.epackage.EPackageNameOrNull»"));
						«ENDFOR»
					'''
				]
			}
		]
	}

}

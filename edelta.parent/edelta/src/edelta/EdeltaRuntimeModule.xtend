/*
 * generated by Xtext 2.10.0
 */
package edelta

import edelta.scoping.EdeltaQualifiedNameProvider
import edelta.typesystem.EdeltaTypeComputer
import edelta.compiler.EdeltaXbaseCompiler
import org.eclipse.xtext.xbase.compiler.XbaseCompiler
import edelta.resource.EdeltaDerivedStateComputer
import edelta.resource.EdeltaLocationInFileProvider
import edelta.resource.EdeltaResourceDescriptionStrategy
import edelta.resource.IEdeltaEcoreModelAssociations

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
class EdeltaRuntimeModule extends AbstractEdeltaRuntimeModule {

	override bindIQualifiedNameProvider() {
		EdeltaQualifiedNameProvider
	}

	override bindITypeComputer() {
		EdeltaTypeComputer
	}

	override bindIDerivedStateComputer() {
		EdeltaDerivedStateComputer
	}

	override bindILocationInFileProvider() {
		EdeltaLocationInFileProvider
	}

	override bindIDefaultResourceDescriptionStrategy() {
		EdeltaResourceDescriptionStrategy
	}

	def Class<? extends XbaseCompiler> bindXbaseCompiler() {
		EdeltaXbaseCompiler
	}

	def Class<? extends IEdeltaEcoreModelAssociations> bindIEdeltaEcoreModelAssociations() {
		EdeltaDerivedStateComputer
	}
}

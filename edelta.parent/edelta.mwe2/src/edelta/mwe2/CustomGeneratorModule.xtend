package edelta.mwe2

import org.eclipse.xtext.xtext.generator.DefaultGeneratorModule
import org.eclipse.xtext.xtext.generator.XtextGeneratorResourceSetInitializer

class CustomGeneratorModule extends DefaultGeneratorModule {
	def Class<? extends XtextGeneratorResourceSetInitializer> bindXtextGeneratorResourceSetInitializer() {
		CustomXtextGeneratorResourceSetInitializer
	}
}
